package com.example.mad_assignment.data.respository

import com.example.mad_assignment.data.datasource.NotificationsDataSource
import com.example.mad_assignment.data.model.Notification
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.sql.Timestamp
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepository @Inject constructor(
    private val dataSource: NotificationsDataSource
) {
    private var currentUserId: String = "1" // set after login

    /**
     * Set the current user ID (call this after authentication)
     */
    fun setCurrentUser(userId: String) {
        currentUserId = userId
    }

    /**
     * Get all notifications for the current user
     * Combines user-specific and global notifications
     */
    val notifications: Flow<List<Notification>> = combine(
        dataSource.getUserNotifications(currentUserId),
        dataSource.getGlobalNotifications()
    ) { userNotifications, globalNotifications ->
        (userNotifications + globalNotifications)
            .distinctBy { it.id }
            .sortedByDescending { it.timestamp }
    }

    /**
     * Get notifications for a specific user
     */
    fun getNotificationsForUser(userId: String): Flow<List<Notification>> {
        return dataSource.getUserNotifications(userId)
    }

    /**
     * Create and add a new notification
     */
    suspend fun createNotification(
        title: String,
        message: String,
        type: Notification.NotificationType = Notification.NotificationType.GENERAL,
        userId: String = currentUserId
    ): Result<String> {
        val notification = Notification(
            id = dataSource.generateNotificationId(),
            title = title,
            message = message,
            timestamp = Timestamp(System.currentTimeMillis()),
            type = type,
            status = Notification.Status.UNREAD
        )

        return dataSource.addNotification(notification, userId)
    }

    /**
     * Send notification to multiple users
     */
    suspend fun sendNotificationToUsers(
        title: String,
        message: String,
        type: Notification.NotificationType,
        userIds: List<String>
    ): Result<List<String>> {
        val notification = Notification(
            id = dataSource.generateNotificationId(),
            title = title,
            message = message,
            timestamp = Timestamp(System.currentTimeMillis()),
            type = type,
            status = Notification.Status.UNREAD
        )

        return dataSource.addNotificationForUsers(notification, userIds)
    }

    /**
     * Mark a notification as read
     */
    suspend fun markAsRead(notificationId: String): Result<Unit> {
        return dataSource.markAsRead(notificationId, currentUserId)
    }

    /**
     * Mark a notification as archived
     */
    suspend fun markAsArchived(notificationId: String): Result<Unit> {
        return dataSource.markAsArchived(notificationId, currentUserId)
    }

    /**
     * Delete a notification (soft delete)
     */
    suspend fun deleteNotification(notificationId: String): Result<Unit> {
        return dataSource.deleteNotification(notificationId, currentUserId)
    }

    /**
     * Permanently delete a notification
     */
    suspend fun permanentlyDeleteNotification(notificationId: String): Result<Unit> {
        return dataSource.permanentlyDeleteNotification(notificationId, currentUserId)
    }

    /**
     * Mark all notifications as read
     */
    suspend fun markAllAsRead(): Result<Unit> {
        return dataSource.markAllAsRead(currentUserId)
    }

    /**
     * Get unread notification count
     */
    fun getUnreadCount(): Flow<Int> {
        return dataSource.getUnreadCount(currentUserId)
    }

    /**
     * Get notifications by type
     */
    fun getNotificationsByType(type: Notification.NotificationType): Flow<List<Notification>> {
        return dataSource.getNotificationsByType(currentUserId, type)
    }

    /**
     * Get notifications by status
     */
    fun getNotificationsByStatus(status: Notification.Status): Flow<List<Notification>> {
        return dataSource.getNotificationsByStatus(currentUserId, status)
    }

    /**
     * Delete old notifications
     */
    suspend fun deleteOldNotifications(daysOld: Int = 30): Result<Unit> {
        return dataSource.deleteOldNotifications(currentUserId, daysOld)
    }

    /**
     * Sample notifications for testing/demo
     * This creates notifications in Firestore for the current user
     */
    suspend fun createSampleNotifications() {
        val sampleData = listOf(
            Triple("Welcome to Campus Connect!",
                "Get started with your digital campus experience. Explore all features available to you.",
                Notification.NotificationType.WELCOME)
        )

        sampleData.forEach { (title, message, type) ->
            createNotification(title, message, type)
        }
    }
}