package com.example.mad_assignment.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.mad_assignment.MainActivity
import com.example.mad_assignment.R
import com.example.mad_assignment.data.datasource.NotificationsDataSource
import com.example.mad_assignment.data.datasource.ScheduledNotificationDataSource
import com.example.mad_assignment.data.model.Notification
import com.example.mad_assignment.data.model.ScheduledNotification
import com.example.mad_assignment.data.respository.NotificationRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.sql.Timestamp
import java.util.UUID

class NotificationWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val CHANNEL_ID = "scheduled_notifications"
        const val NOTIFICATION_ID = 1001

        // Input data keys
        const val KEY_TITLE = "title"
        const val KEY_MESSAGE = "message"
        const val KEY_TYPE = "type"
        const val KEY_SCHEDULED_ID = "scheduled_id"
        const val KEY_BROADCAST_TO_ALL = "broadcast_to_all"
        const val KEY_USE_FCM = "use_fcm"
        const val KEY_TARGET_USER_ID = "target_user_id"
    }

    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            try {
                val title = inputData.getString(KEY_TITLE) ?: return@withContext Result.failure()
                val message = inputData.getString(KEY_MESSAGE) ?: return@withContext Result.failure()
                val typeString = inputData.getString(KEY_TYPE) ?: "GENERAL"
                val scheduledId = inputData.getString(KEY_SCHEDULED_ID) ?: return@withContext Result.failure()
                val broadcastToAll = inputData.getBoolean(KEY_BROADCAST_TO_ALL, true)
                val useFCM = inputData.getBoolean(KEY_USE_FCM, true)
                val targetUserId = inputData.getString(KEY_TARGET_USER_ID) // Get target user ID

                val type = try {
                    Notification.NotificationType.valueOf(typeString)
                } catch (e: Exception) {
                    Notification.NotificationType.GENERAL
                }

                val notificationsDataSource = NotificationsDataSource(firestore = FirebaseFirestore.getInstance())
                val scheduledDataSource = ScheduledNotificationDataSource(firestore = FirebaseFirestore.getInstance())
                val notificationRepository = NotificationRepository(
                    dataSource = notificationsDataSource,
                    scheduledDataSource = scheduledDataSource,
                    context = applicationContext
                )

                val notification = Notification(
                    id = UUID.randomUUID().toString(),
                    title = title,
                    message = message,
                    timestamp = Timestamp(System.currentTimeMillis()),
                    type = type,
                    status = Notification.Status.UNREAD
                )

                // Send via FCM if enabled
                if (useFCM) {
                    if (broadcastToAll) {
                        sendFCMNotificationToAll(title, message, type)
                    } else if (targetUserId != null) {
                        sendFCMNotificationToUser(targetUserId, title, message, type)
                    }
                }

                val createResult = if (broadcastToAll) {
                    notificationRepository.addNotificationForAllUsers(notification)
                } else if (targetUserId != null) {
                    // Send to specific user only
                    notificationRepository.createNotificationForUser(notification, targetUserId)
                } else {
                    val currentUserId = getCurrentUserId(applicationContext)
                    notificationRepository.createNotificationForUser(notification, currentUserId)
                }

                if (createResult.isSuccess) {
                    showSystemNotification(title, message)

                    scheduledDataSource.updateScheduledNotificationStatus(
                        scheduledId,
                        ScheduledNotification.ScheduleStatus.SENT
                    )

                    Result.success()
                } else {
                    Result.failure()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure()
            }
        }
    }

    private fun getCurrentUserId(context: Context): String {
        // Implement user authentication logic here TODO
        // Return the current user's ID from SharedPreferences, Auth, etc.
        val sharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        return sharedPreferences.getString("user_id", "default_user_id") ?: "default_user_id"
    }

    // Add this method for sending FCM to specific user
    private suspend fun sendFCMNotificationToUser(userId: String, title: String, message: String, type: Notification.NotificationType) {
        try {
            val userDoc = FirebaseFirestore.getInstance().collection("users").document(userId).get().await()
            val fcmToken = userDoc.getString("fcmToken")

            if (fcmToken != null && fcmToken.isNotBlank()) {
                sendFCMMessage(fcmToken, title, message, type)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun sendFCMNotificationToAll(title: String, message: String, type: Notification.NotificationType) {
        try {
            val allUserIds = getAdminUserIds() // Get admin users who can send notifications
            val fcmTokens = getFCMTokensForUsers(allUserIds)

            fcmTokens.forEach { token ->
                sendFCMMessage(token, title, message, type)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // TODO: Add this method to get admin user IDs from Firestore
    private suspend fun getAdminUserIds(): List<String> {
        val db = FirebaseFirestore.getInstance()
        val snapshot = db.collection("users")
            .whereEqualTo("canReceiveNotifications", true)
            .get()
            .await()

        return snapshot.documents.map { it.id }
    }

    private suspend fun getFCMTokensForUsers(userIds: List<String>): List<String> {
        val db = FirebaseFirestore.getInstance()
        val tokens = mutableListOf<String>()

        userIds.forEach { userId ->
            val userDoc = db.collection("users").document(userId).get().await()
            userDoc.getString("fcmToken")?.let { token ->
                if (token.isNotBlank()) {
                    tokens.add(token)
                }
            }
        }

        return tokens
    }

    private suspend fun sendFCMMessage(token: String, title: String, message: String, type: Notification.NotificationType) {
        val messageData = mapOf(
            "title" to title,
            "message" to message,
            "type" to type.name,
            "click_action" to "FLUTTER_NOTIFICATION_CLICK"
        )

        try {
            FirebaseMessaging.getInstance().send(
                com.google.firebase.messaging.RemoteMessage.Builder("$token@fcm.googleapis.com")
                    .setMessageId(java.util.UUID.randomUUID().toString())
                    .addData("title", title)
                    .addData("message", message)
                    .addData("type", type.name)
                    .build()
            )
        } catch (e: Exception) {
            // Fallback to local notification
            showSystemNotification(title, message)
        }
    }

    private fun showSystemNotification(title: String, message: String) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel for Android 8.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Scheduled Notifications",
                NotificationManager.IMPORTANCE_HIGH // Changed to HIGH for better visibility
            ).apply {
                description = "Notifications scheduled through the app"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 250, 500)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Create intent to open app when notification is tapped
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("from_notification", true)
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build and show the notification
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 500, 250, 500))
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}

object NotificationScheduler {
    /**
     * Schedule a notification using WorkManager with FCM support
     */
    fun scheduleNotification(
        context: Context,
        scheduledNotification: ScheduledNotification,
        broadcastToAll: Boolean = true,
        useFCM: Boolean = true,
        targetUserId: String? = null
    ) {
        val delay = scheduledNotification.scheduledTime - System.currentTimeMillis()

        val inputData = androidx.work.workDataOf(
            NotificationWorker.KEY_TITLE to scheduledNotification.title,
            NotificationWorker.KEY_MESSAGE to scheduledNotification.message,
            NotificationWorker.KEY_TYPE to scheduledNotification.type.name,
            NotificationWorker.KEY_SCHEDULED_ID to scheduledNotification.id,
            NotificationWorker.KEY_BROADCAST_TO_ALL to broadcastToAll,
            NotificationWorker.KEY_USE_FCM to useFCM,
            NotificationWorker.KEY_TARGET_USER_ID to targetUserId
        )

        if (delay > 0) {
            val workRequest = androidx.work.OneTimeWorkRequestBuilder<NotificationWorker>()
                .setInitialDelay(delay, java.util.concurrent.TimeUnit.MILLISECONDS)
                .setInputData(inputData)
                .addTag("scheduled_notification_${scheduledNotification.id}")
                .build()

            androidx.work.WorkManager.getInstance(context).enqueue(workRequest)
        } else {
            val workRequest = androidx.work.OneTimeWorkRequestBuilder<NotificationWorker>()
                .setInputData(inputData)
                .addTag("scheduled_notification_${scheduledNotification.id}")
                .build()

            androidx.work.WorkManager.getInstance(context).enqueue(workRequest)
        }
    }

    /**
     * Cancel a scheduled notification
     */
    fun cancelScheduledNotification(context: Context, scheduledNotificationId: String) {
        androidx.work.WorkManager.getInstance(context)
            .cancelAllWorkByTag("scheduled_notification_$scheduledNotificationId")
    }

    /**
     * Cancel all scheduled notifications
     */
    fun cancelAllScheduledNotifications(context: Context) {
        androidx.work.WorkManager.getInstance(context)
            .cancelAllWorkByTag("scheduled_notification")
    }

    /**
     * Reschedule all pending notifications (call this on app start)
     */
    suspend fun rescheduleAllNotifications(context: Context, userId: String) {
        val scheduledDataSource = ScheduledNotificationDataSource(firestore = FirebaseFirestore.getInstance())
        val result = scheduledDataSource.getDueScheduledNotifications()

        if (result.isSuccess) {
            result.getOrNull()?.forEach { scheduledNotification ->
                if (scheduledNotification.status == ScheduledNotification.ScheduleStatus.PENDING) {
                    scheduleNotification(
                        context = context,
                        scheduledNotification = scheduledNotification,
                        broadcastToAll = true,
                        useFCM = true
                    )
                }
            }
        }
    }
}