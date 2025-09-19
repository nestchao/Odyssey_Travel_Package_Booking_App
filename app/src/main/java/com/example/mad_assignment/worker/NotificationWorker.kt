package com.example.mad_assignment.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.ui.platform.LocalContext
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.Timestamp
import javax.inject.Inject

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
    }

    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            try {
                val title = inputData.getString(KEY_TITLE) ?: return@withContext Result.failure()
                val message = inputData.getString(KEY_MESSAGE) ?: return@withContext Result.failure()
                val typeString = inputData.getString(KEY_TYPE) ?: "GENERAL"
                val scheduledId = inputData.getString(KEY_SCHEDULED_ID) ?: return@withContext Result.failure()

                val type = try {
                    Notification.NotificationType.valueOf(typeString)
                } catch (e: Exception) {
                    Notification.NotificationType.GENERAL
                }

                // Create the notification in the database
                val notificationRepository = NotificationRepository(
                    dataSource = NotificationsDataSource(firestore = FirebaseFirestore.getInstance()),
                    scheduledDataSource =  ScheduledNotificationDataSource(firestore = FirebaseFirestore.getInstance()),
                    context = applicationContext
                )

                val createResult = notificationRepository.createNotification(
                    title = title,
                    message = message,
                    type = type
                )

                if (createResult.isSuccess) {
                    // Show system notification
                    showSystemNotification(title, message)

                    // Update scheduled notification status to SENT
                    val scheduledDataSource = ScheduledNotificationDataSource(FirebaseFirestore.getInstance())
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

    private fun showSystemNotification(title: String, message: String) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel for Android 8.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Scheduled Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications scheduled through the app"
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Create intent to open app when notification is tapped
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build and show the notification
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification) // You'll need to add this icon
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}

// Extension functions for WorkManager integration
object NotificationScheduler {

    /**
     * Schedule a notification using WorkManager
     */
    fun scheduleNotification(
        context: Context,
        scheduledNotification: ScheduledNotification
    ) {
        val delay = scheduledNotification.scheduledTime - System.currentTimeMillis()

        if (delay > 0) {
            val workRequest = androidx.work.OneTimeWorkRequestBuilder<NotificationWorker>()
                .setInitialDelay(delay, java.util.concurrent.TimeUnit.MILLISECONDS)
                .setInputData(
                    androidx.work.workDataOf(
                        NotificationWorker.KEY_TITLE to scheduledNotification.title,
                        NotificationWorker.KEY_MESSAGE to scheduledNotification.message,
                        NotificationWorker.KEY_TYPE to scheduledNotification.type.name,
                        NotificationWorker.KEY_SCHEDULED_ID to scheduledNotification.id
                    )
                )
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
}