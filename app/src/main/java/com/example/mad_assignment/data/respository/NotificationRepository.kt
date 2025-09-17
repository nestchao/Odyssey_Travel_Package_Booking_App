package com.example.mad_assignment.data.respository

import com.example.mad_assignment.data.model.Notification
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

class NotificationRepository {
    private val _notifications = MutableStateFlow(generateSampleNotifications())
    val notifications: Flow<List<Notification>> = _notifications.asStateFlow()

    private fun generateSampleNotifications(): List<Notification> {
        return listOf(
            Notification(
                id = "1", title = "Welcome to Odyssey!", message = "Thanks for joining our fitness community.",
                type = Notification.NotificationType.WELCOME,
                status = Notification.Status.READ
            ),
            Notification(
                "2", "Workout Reminder", "Don't forget your workout today!",
                type = Notification.NotificationType.REMINDER,
                status = Notification.Status.READ
            ),
            Notification(
                "3", "Payment Successful", "Thank you for your payment.",
                type = Notification.NotificationType.PAYMENT,
                status = Notification.Status.READ
            ),
            Notification(
                "4", "Class Replacement", "Your class has been rescheduled to tomorrow.",
                type = Notification.NotificationType.CLASS,
                status = Notification.Status.ARCHIVED
            ),
            Notification(
                "5", "New Package Available", "Explore our new premium package!",
                type = Notification.NotificationType.PACKAGE,
                status = Notification.Status.READ
            )
        ).sortedByDescending { it.timestamp }
    }

    suspend fun getNotifications(): List<Notification> = _notifications.value

    suspend fun getNotificationById(id: String): Notification? =
        _notifications.value.find { it.id == id }

    suspend fun markAsRead(id: String) {
        val currentList = _notifications.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == id }
        if (index != -1) {
            currentList[index] = currentList[index].copy(status = Notification.Status.READ)
            _notifications.value = currentList
        }
    }

    suspend fun markAsArchived(id: String) {
        val currentList = _notifications.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == id }
        if (index != -1) {
            currentList[index] = currentList[index].copy(status = Notification.Status.ARCHIVED)
            _notifications.value = currentList
        }
    }

    suspend fun deleteNotification(id: String) {
        _notifications.value = _notifications.value.filter { it.id != id }
    }

    suspend fun markAllAsRead() {
        _notifications.value = _notifications.value.map { notification ->
            if (notification.status == Notification.Status.UNREAD) {
                notification.copy(status = Notification.Status.READ)
            } else notification
        }
    }

    fun getUnreadCount(): Flow<Int> =
        notifications.map { list -> list.count { it.status == Notification.Status.UNREAD } }
}
