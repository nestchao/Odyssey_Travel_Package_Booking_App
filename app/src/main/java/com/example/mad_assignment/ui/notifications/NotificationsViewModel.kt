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

    fun markAsRead(id: String) {
        viewModelScope.launch {
            repository.markAsRead(id)
        }
    }

    fun markAsArchived(id: String) {
        viewModelScope.launch {
            repository.markAsArchived(id)
        }
    }

    fun deleteNotification(id: String) {
        viewModelScope.launch {
            repository.deleteNotification(id)
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            repository.markAllAsRead()
        }
    }

    fun getNotificationById(id: String): Notification? {
        return notifications.value.find { it.id == id }
    }
}

enum class NotificationFilter(val displayName: String) {
    ALL("All"),
    UNREAD("Unread"),
    ARCHIVED("Archived")
}