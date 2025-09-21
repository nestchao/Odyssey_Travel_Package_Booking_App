package com.example.mad_assignment.ui.notifications

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mad_assignment.data.model.Notification
import com.example.mad_assignment.data.model.ScheduledNotification
import com.example.mad_assignment.data.respository.NotificationRepository
import com.example.mad_assignment.worker.NotificationScheduler
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

class NotificationsViewModel(
    private val repository: NotificationRepository,
    private val context: Context
) : ViewModel() {
    private val _filter = MutableStateFlow(NotificationFilter.ALL)
    val filter: StateFlow<NotificationFilter> = _filter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _scheduledNotifications = MutableStateFlow<List<ScheduledNotification>>(emptyList())
    val scheduledNotifications: StateFlow<List<ScheduledNotification>> = _scheduledNotifications.asStateFlow()

    init {
        loadScheduledNotifications()
    }

    fun loadSampleData() {
        viewModelScope.launch {
            repository.createSampleNotifications()
        }
    }

    val notifications: StateFlow<List<Notification>> = combine(
        repository.notifications,
        _filter,
        _searchQuery
    ) { notifications, filter, query ->
        var filtered = when (filter) {
            NotificationFilter.ALL -> notifications.filter {
                it.status != Notification.Status.DELETED && it.status != Notification.Status.ARCHIVED
            }
            NotificationFilter.UNREAD -> notifications.filter {
                it.status == Notification.Status.UNREAD
            }
            NotificationFilter.ARCHIVED -> notifications.filter {
                it.status == Notification.Status.ARCHIVED
            }
        }

        if (query.isNotBlank()) {
            filtered = filtered.filter {
                it.title.contains(query, ignoreCase = true) ||
                        it.message.contains(query, ignoreCase = true)
            }
        }

        filtered
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val unreadCount: StateFlow<Int> = repository.getUnreadCount()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    fun setFilter(filter: NotificationFilter) {
        _filter.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun markAsRead(id: String) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.markAsRead(id)
                .onSuccess {
                    // Successfully marked as read
                }
                .onFailure { exception ->
                    _errorMessage.value = "Failed to mark as read: ${exception.message}"
                }
            _isLoading.value = false
        }
    }

    fun markAsArchived(id: String) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.markAsArchived(id)
                .onSuccess {
                    // Successfully archived
                }
                .onFailure { exception ->
                    _errorMessage.value = "Failed to archive: ${exception.message}"
                }
            _isLoading.value = false
        }
    }

    fun deleteNotification(id: String) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.deleteNotification(id)
                .onSuccess {
                    // Successfully deleted
                }
                .onFailure { exception ->
                    _errorMessage.value = "Failed to delete: ${exception.message}"
                }
            _isLoading.value = false
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.markAllAsRead()
                .onSuccess {
                    // All notifications marked as read
                }
                .onFailure { exception ->
                    _errorMessage.value = "Failed to mark all as read: ${exception.message}"
                }
            _isLoading.value = false
        }
    }

    fun getNotificationById(id: String): Flow<Notification?> {
        return repository.getNotificationById(id)
    }

    /**
     * Create a test notification
     */
    fun createTestNotification() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.createNotification(
                title = "Test Notification",
                message = "This is a test notification from the app",
                type = Notification.NotificationType.GENERAL
            ).onFailure { exception ->
                _errorMessage.value = "Failed to create notification: ${exception.message}"
            }
            _isLoading.value = false
        }
    }

    /**
     * Clean up old notifications
     */
    fun cleanupOldNotifications(daysOld: Int = 30) {
        viewModelScope.launch {
            repository.deleteOldNotifications(daysOld)
                .onFailure { exception ->
                    _errorMessage.value = "Failed to cleanup: ${exception.message}"
                }
        }
    }

    /**
     * Schedule a new notification (broadcasts to all users by default)
     */
    fun scheduleNotification(
        title: String,
        message: String,
        type: Notification.NotificationType,
        scheduledTime: Long,
        broadcastToAll: Boolean = true
    ) {
        viewModelScope.launch {
            val scheduledNotification = ScheduledNotification(
                id = UUID.randomUUID().toString(),
                title = title,
                message = message,
                type = type,
                scheduledTime = scheduledTime
            )

            // Add to repository/database
            repository.addScheduledNotification(scheduledNotification)
                .onSuccess {
                    // Schedule using WorkManager to broadcast to all users
                    NotificationScheduler.scheduleNotification(
                        context = context,
                        scheduledNotification = scheduledNotification,
                        broadcastToAll = broadcastToAll
                    )
                }
                .onFailure { exception ->
                    _errorMessage.value = "Failed to schedule notification: ${exception.message}"
                }
        }
    }

    /**
     * Delete a scheduled notification
     */
    fun deleteScheduledNotification(id: String) {
        viewModelScope.launch {
            // Remove from repository
            repository.deleteScheduledNotification(id)
                .onSuccess {
                    // Update local list
                    val currentList = _scheduledNotifications.value.toMutableList()
                    currentList.removeAll { it.id == id }
                    _scheduledNotifications.value = currentList

                    // Cancel the scheduled delivery
                    NotificationScheduler.cancelScheduledNotification(context, id)
                }
                .onFailure { exception ->
                    _errorMessage.value = "Failed to delete scheduled notification: ${exception.message}"
                }
        }
    }

    /**
     * Load scheduled notifications from repository
     */
    private fun loadScheduledNotifications() {
        viewModelScope.launch {
            repository.getScheduledNotifications().collect { scheduledNotifications ->
                _scheduledNotifications.value = scheduledNotifications.sortedBy { it.scheduledTime }
            }
        }
    }

    /**
     * Check for notifications that should be sent now and send them
     * This method is kept for manual processing if needed
     */
    fun processScheduledNotifications() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val toSend = _scheduledNotifications.value.filter {
                it.scheduledTime <= now && it.status == ScheduledNotification.ScheduleStatus.PENDING
            }

            toSend.forEach { scheduledNotification ->
                // Create notification using repository method that broadcasts to all users
                val notification = Notification(
                    id = UUID.randomUUID().toString(),
                    title = scheduledNotification.title,
                    message = scheduledNotification.message,
                    timestamp = java.sql.Timestamp(System.currentTimeMillis()),
                    type = scheduledNotification.type,
                    status = Notification.Status.UNREAD
                )

                repository.addNotificationForAllUsers(notification)
                    .onSuccess {
                        // Update the status to SENT
                        val updatedList = _scheduledNotifications.value.map {
                            if (it.id == scheduledNotification.id) {
                                it.copy(status = ScheduledNotification.ScheduleStatus.SENT)
                            } else {
                                it
                            }
                        }
                        _scheduledNotifications.value = updatedList
                    }
            }
        }
    }

    /**
     * Schedule a new notification with FCM support
     */
    fun scheduleNotification(
        title: String,
        message: String,
        type: Notification.NotificationType,
        scheduledTime: Long,
        broadcastToAll: Boolean = true,
        useFCM: Boolean = true
    ) {
        viewModelScope.launch {
            val scheduledNotification = ScheduledNotification(
                id = UUID.randomUUID().toString(),
                title = title,
                message = message,
                type = type,
                scheduledTime = scheduledTime
            )

            // Add to repository/database
            repository.addScheduledNotification(scheduledNotification)
                .onSuccess {
                    // Schedule using WorkManager to broadcast to all users
                    NotificationScheduler.scheduleNotification(
                        context = context,
                        scheduledNotification = scheduledNotification,
                        broadcastToAll = broadcastToAll,
                        useFCM = useFCM
                    )
                }
                .onFailure { exception ->
                    _errorMessage.value = "Failed to schedule notification: ${exception.message}"
                }
        }
    }

    fun reschedulePendingNotifications() {
        viewModelScope.launch {
            NotificationScheduler.rescheduleAllNotifications(context, getCurrentUserId())
        }
    }

    // TODO: Replace with actual user ID retrieval
    private fun getCurrentUserId(): String {
        return "current_user_id"
    }

    /**
     * Schedule a notification (can broadcast to all or send to specific user)
     */
    fun scheduleNotification(
        title: String,
        message: String,
        type: Notification.NotificationType,
        scheduledTime: Long,
        broadcastToAll: Boolean = true,
        useFCM: Boolean = true,
        targetUserId: String? = null
    ) {
        viewModelScope.launch {
            val scheduledNotification = ScheduledNotification(
                id = UUID.randomUUID().toString(),
                title = title,
                message = message,
                type = type,
                scheduledTime = scheduledTime
            )

            repository.addScheduledNotification(scheduledNotification)
                .onSuccess {
                    // Schedule using WorkManager
                    NotificationScheduler.scheduleNotification(
                        context = context,
                        scheduledNotification = scheduledNotification,
                        broadcastToAll = broadcastToAll,
                        useFCM = useFCM,
                        targetUserId = targetUserId
                    )
                }
                .onFailure { exception ->
                    _errorMessage.value = "Failed to schedule notification: ${exception.message}"
                }
        }
    }

    /**
     * Send a notification to a specific user
     */
    fun sendUserNotification(
        userId: String,
        title: String,
        message: String,
        type: Notification.NotificationType = Notification.NotificationType.GENERAL
    ) {
        viewModelScope.launch {
            val notification = Notification(
                id = UUID.randomUUID().toString(),
                title = title,
                message = message,
                timestamp = java.sql.Timestamp(System.currentTimeMillis()),
                type = type,
                status = Notification.Status.UNREAD
            )

            repository.createNotificationForUser(notification, userId)
                .onSuccess {
                    sendFCMNotificationToUser(userId, title, message, type)
                }
                .onFailure { exception ->
                    _errorMessage.value = "Failed to send notification: ${exception.message}"
                }
        }
    }

    // Send FCM to specific user
    private suspend fun sendFCMNotificationToUser(userId: String, title: String, message: String, type: Notification.NotificationType) {
        try {
            val userDoc = FirebaseFirestore.getInstance().collection("users").document(userId).get().await()
            val fcmToken = userDoc.getString("fcmToken")

            if (fcmToken != null && fcmToken.isNotBlank()) {
                println("Would send FCM to user $userId with token $fcmToken")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

enum class NotificationFilter(val displayName: String) {
    ALL("All"),
    UNREAD("Unread"),
    ARCHIVED("Archived")
}