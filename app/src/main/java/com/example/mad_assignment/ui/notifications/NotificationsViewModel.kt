package com.example.mad_assignment.ui.notifications

import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.mad_assignment.data.model.Notification
import com.example.mad_assignment.data.model.ScheduledNotification
import com.example.mad_assignment.data.respository.NotificationRepository
import com.example.mad_assignment.worker.NotificationWorker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.TimeUnit

class NotificationsViewModel(
    private val repository: NotificationRepository
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
     * Schedule a new notification
     */
    fun scheduleNotification(
        title: String,
        message: String,
        type: Notification.NotificationType,
        scheduledTime: Long
    ) {
        viewModelScope.launch {
            val scheduledNotification = ScheduledNotification(
                id = UUID.randomUUID().toString(),
                title = title,
                message = message,
                type = type,
                scheduledTime = scheduledTime
            )

            val currentList = _scheduledNotifications.value.toMutableList()
            currentList.add(scheduledNotification)
            _scheduledNotifications.value = currentList.sortedBy { it.scheduledTime }

            // TODO: Here you would integrate with Android's AlarmManager or WorkManager
            // to actually schedule the notification delivery
            scheduleNotificationDelivery(scheduledNotification)
        }
    }

    /**
     * Delete a scheduled notification
     */
    fun deleteScheduledNotification(id: String) {
        viewModelScope.launch {
            val currentList = _scheduledNotifications.value.toMutableList()
            currentList.removeAll { it.id == id }
            _scheduledNotifications.value = currentList

            // Cancel the scheduled delivery
            cancelScheduledNotificationDelivery(id)
        }
    }

    /**
     * Load scheduled notifications (in a real app, this would be from a database)
     */
    private fun loadScheduledNotifications() {
        viewModelScope.launch {
            // For demo purposes, create some sample scheduled notifications
            val sampleScheduled = listOf(
                ScheduledNotification(
                    id = UUID.randomUUID().toString(),
                    title = "Weekly Report Reminder",
                    message = "Don't forget to submit your weekly report by Friday",
                    type = Notification.NotificationType.REMINDER,
                    scheduledTime = System.currentTimeMillis() + (24 * 60 * 60 * 1000) // Tomorrow
                ),
                ScheduledNotification(
                    id = UUID.randomUUID().toString(),
                    title = "Payment Due",
                    message = "Your monthly payment is due in 3 days",
                    type = Notification.NotificationType.PAYMENT,
                    scheduledTime = System.currentTimeMillis() + (3 * 24 * 60 * 60 * 1000) // In 3 days
                )
            )
            _scheduledNotifications.value = sampleScheduled.sortedBy { it.scheduledTime }
        }
    }

    /**
     * Schedule notification delivery using system scheduler
     */
    private fun scheduleNotificationDelivery(scheduledNotification: ScheduledNotification) {
        // TODO: Implement with WorkManager
        // Example:
         val workRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
             .setInitialDelay(scheduledNotification.scheduledTime - System.currentTimeMillis(), TimeUnit.MILLISECONDS)
             .setInputData(workDataOf(
                 "title" to scheduledNotification.title,
                 "message" to scheduledNotification.message,
                 "type" to scheduledNotification.type.name
             ))
             .build()
         WorkManager.getInstance(context).enqueue(workRequest)
    }

    /**
     * Cancel scheduled notification delivery
     */
    private fun cancelScheduledNotificationDelivery(id: String) {
        // TODO: Implement with WorkManager
         WorkManager.getInstance(context).cancelWorkById(UUID.fromString(id))
    }

    /**
     * Check for notifications that should be sent now and send them
     * This would typically run periodically or be triggered by the scheduler
     */
    fun processScheduledNotifications() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val toSend = _scheduledNotifications.value.filter {
                it.scheduledTime <= now && it.status == ScheduledNotification.ScheduleStatus.PENDING
            }

            toSend.forEach { scheduledNotification ->
                // Create and send the actual notification
                repository.createNotification(
                    title = scheduledNotification.title,
                    message = scheduledNotification.message,
                    type = scheduledNotification.type
                )

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

enum class NotificationFilter(val displayName: String) {
    ALL("All"),
    UNREAD("Unread"),
    ARCHIVED("Archived")
}