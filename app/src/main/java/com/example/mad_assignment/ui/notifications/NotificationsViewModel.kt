package com.example.mad_assignment.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mad_assignment.data.model.Notification
import com.example.mad_assignment.data.respository.NotificationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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

    fun getNotificationById(id: String): Notification? {
        return notifications.value.find { it.id == id }
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
}

enum class NotificationFilter(val displayName: String) {
    ALL("All"),
    UNREAD("Unread"),
    ARCHIVED("Archived")
}