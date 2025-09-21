package com.example.mad_assignment.data.respository

import android.content.Context
import com.example.mad_assignment.data.datasource.NotificationsDataSource
import com.example.mad_assignment.data.datasource.ScheduledNotificationDataSource
import com.example.mad_assignment.data.model.Notification
import com.example.mad_assignment.data.model.ScheduledNotification
import com.example.mad_assignment.worker.NotificationScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.sql.Timestamp
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepository @Inject constructor(
    private val dataSource: NotificationsDataSource,
    private val scheduledDataSource: ScheduledNotificationDataSource,
    private val context: Context
) {
    // TODO: GET CURRENT USER ID
    private var currentUserId: String = "current_user_id"

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
     * Get scheduled notifications for the current user
     */
    val scheduledNotificationsFlow: Flow<List<ScheduledNotification>> =
        scheduledDataSource.getScheduledNotifications(getCurrentUserId())

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
     * Schedule a notification for future delivery
     */
    suspend fun scheduleNotification(
        title: String,
        message: String,
        type: Notification.NotificationType,
        scheduledTime: Long,
        userId: String = currentUserId
    ): Result<String> {
        val scheduledNotification = ScheduledNotification(
            id = scheduledDataSource.generateScheduledNotificationId(),
            title = title,
            message = message,
            type = type,
            scheduledTime = scheduledTime
        )

        return try {
            // Save to database
            val saveResult = scheduledDataSource.addScheduledNotification(scheduledNotification, userId)

            if (saveResult.isSuccess) {
                // Schedule with WorkManager
                NotificationScheduler.scheduleNotification(context, scheduledNotification)
                Result.success(scheduledNotification.id)
            } else {
                saveResult
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
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
     * Get a notification by ID
     */
    fun getNotificationById(notificationId: String): Flow<Notification?> {
        return dataSource.getNotificationById(currentUserId, notificationId)
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

    /**
     * Process scheduled notifications that are due
     * This would typically be called by a background service
     */
    suspend fun processDueScheduledNotifications(): Result<Unit> {
        return try {
            val dueNotifications = scheduledDataSource.getDueScheduledNotifications()

            dueNotifications.onSuccess { notifications ->
                notifications.forEach { scheduledNotification ->
                    // Create the actual notification
                    createNotification(
                        title = scheduledNotification.title,
                        message = scheduledNotification.message,
                        type = scheduledNotification.type,
                        userId = currentUserId
                    )

                    // Update status to SENT
                    scheduledDataSource.updateScheduledNotificationStatus(
                        scheduledNotification.id,
                        ScheduledNotification.ScheduleStatus.SENT
                    )
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Add a scheduled notification
     */
    suspend fun addScheduledNotification(scheduledNotification: ScheduledNotification): Result<String> {
        return scheduledDataSource.addScheduledNotification(scheduledNotification, getCurrentUserId())
    }

    /**
     * Delete a scheduled notification
     */
    suspend fun deleteScheduledNotification(notificationId: String): Result<Unit> {
        return scheduledDataSource.deleteScheduledNotification(notificationId)
    }

    /**
     * Get all scheduled notifications for current user
     */
    fun getScheduledNotifications(): Flow<List<ScheduledNotification>> {
        return scheduledDataSource.getScheduledNotifications(getCurrentUserId())
    }

    // TODO: Replace with actual user authentication logic
    /**
     * Get current user ID - implement this based on your auth system
     * For now, using a placeholder. Replace with your actual user ID logic.
     */
    private fun getCurrentUserId(): String {
        // For example: FirebaseAuth.getInstance().currentUser?.uid ?: "default_user" / BOON YEW CODE
        return "default_user" // Placeholder
    }

    suspend fun createNotificationForUser(notification: Notification, userId: String): Result<String> {
        return dataSource.addNotification(notification, userId)
    }

    suspend fun addNotificationForAllUsers(notification: Notification): Result<List<String>> {
        val userIds = dataSource.getAllUserIds().getOrElse { emptyList() }
        return dataSource.addNotificationForUsers(notification, userIds)
    }
}