package com.example.mad_assignment.data.datasource

import com.example.mad_assignment.data.model.ScheduledNotification
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import com.google.firebase.Timestamp as FirebaseTimestamp
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScheduledNotificationDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    companion object {
        private const val SCHEDULED_NOTIFICATIONS_COLLECTION = "scheduled_notifications"

        // Field names
        private const val FIELD_ID = "id"
        private const val FIELD_TITLE = "title"
        private const val FIELD_MESSAGE = "message"
        private const val FIELD_TYPE = "type"
        private const val FIELD_SCHEDULED_TIME = "scheduledTime"
        private const val FIELD_CREATED_AT = "createdAt"
        private const val FIELD_STATUS = "status"
        private const val FIELD_USER_ID = "userId"
    }

    /**
     * Data class for Firestore scheduled notification document
     */
    data class FirestoreScheduledNotification(
        val id: String = "",
        val title: String = "",
        val message: String = "",
        val type: String = "",
        val scheduledTime: FirebaseTimestamp = FirebaseTimestamp.now(),
        val createdAt: FirebaseTimestamp = FirebaseTimestamp.now(),
        val status: String = "PENDING",
        val userId: String? = ""
    ) {
        /**
         * Convert to domain ScheduledNotification model
         */
        fun toScheduledNotification(): ScheduledNotification {
            return ScheduledNotification(
                id = id,
                title = title,
                message = message,
                type = try {
                    com.example.mad_assignment.data.model.Notification.NotificationType.valueOf(type)
                } catch (e: Exception) {
                    com.example.mad_assignment.data.model.Notification.NotificationType.GENERAL
                },
                scheduledTime = scheduledTime.toDate().time,
                createdAt = createdAt.toDate().time,
                status = try {
                    ScheduledNotification.ScheduleStatus.valueOf(status)
                } catch (e: Exception) {
                    ScheduledNotification.ScheduleStatus.PENDING
                }
            )
        }

        companion object {
            /**
             * Create FirestoreScheduledNotification from domain ScheduledNotification model
             */
            fun fromScheduledNotification(scheduledNotification: ScheduledNotification, userId: String?): FirestoreScheduledNotification {
                return FirestoreScheduledNotification(
                    id = scheduledNotification.id,
                    title = scheduledNotification.title,
                    message = scheduledNotification.message,
                    type = scheduledNotification.type.name,
                    scheduledTime = FirebaseTimestamp(java.util.Date(scheduledNotification.scheduledTime)),
                    createdAt = FirebaseTimestamp(java.util.Date(scheduledNotification.createdAt)),
                    status = scheduledNotification.status.name,
                    userId = userId
                )
            }
        }
    }

    /**
     * Get all scheduled notifications for a user
     */
    fun getScheduledNotifications(userId: String?): Flow<List<ScheduledNotification>> {
        return callbackFlow {
            val listenerRegistration = firestore
                .collection(SCHEDULED_NOTIFICATIONS_COLLECTION)
                .whereEqualTo(FIELD_USER_ID, userId)
                .orderBy(FIELD_SCHEDULED_TIME, Query.Direction.ASCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }

                    val scheduledNotifications = snapshot?.documents?.mapNotNull { doc ->
                        doc.toObject<FirestoreScheduledNotification>()?.toScheduledNotification()
                    } ?: emptyList()

                    trySend(scheduledNotifications)
                }

            awaitClose {
                listenerRegistration.remove()
            }
        }
    }

    /**
     * Add a new scheduled notification
     */
    suspend fun addScheduledNotification(scheduledNotification: ScheduledNotification, userId: String?): Result<String> {
        return try {
            val firestoreScheduledNotification = FirestoreScheduledNotification.fromScheduledNotification(scheduledNotification, userId)

            firestore
                .collection(SCHEDULED_NOTIFICATIONS_COLLECTION)
                .document(scheduledNotification.id)
                .set(firestoreScheduledNotification)
                .await()

            Result.success(scheduledNotification.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update scheduled notification status
     */
    suspend fun updateScheduledNotificationStatus(
        notificationId: String,
        status: ScheduledNotification.ScheduleStatus
    ): Result<Unit> {
        return try {
            firestore
                .collection(SCHEDULED_NOTIFICATIONS_COLLECTION)
                .document(notificationId)
                .update(
                    mapOf(
                        FIELD_STATUS to status.name
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Delete a scheduled notification
     */
    suspend fun deleteScheduledNotification(notificationId: String): Result<Unit> {
        return try {
            firestore
                .collection(SCHEDULED_NOTIFICATIONS_COLLECTION)
                .document(notificationId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get scheduled notifications that are due for delivery
     */
    suspend fun getDueScheduledNotifications(): Result<List<ScheduledNotification>> {
        return try {
            val now = FirebaseTimestamp.now()
            val snapshot = firestore
                .collection(SCHEDULED_NOTIFICATIONS_COLLECTION)
                .whereEqualTo(FIELD_STATUS, ScheduledNotification.ScheduleStatus.PENDING.name)
                .whereLessThanOrEqualTo(FIELD_SCHEDULED_TIME, now)
                .get()
                .await()

            val dueNotifications = snapshot.documents.mapNotNull { doc ->
                doc.toObject<FirestoreScheduledNotification>()?.toScheduledNotification()
            }

            Result.success(dueNotifications)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Generate a unique scheduled notification ID
     */
    fun generateScheduledNotificationId(): String {
        return firestore.collection(SCHEDULED_NOTIFICATIONS_COLLECTION).document().id
    }
}