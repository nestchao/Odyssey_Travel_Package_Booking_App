package com.example.mad_assignment.data.datasource

import android.system.Os.close
import com.example.mad_assignment.data.model.Notification
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.sql.Timestamp
import com.google.firebase.Timestamp as FirebaseTimestamp
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationsDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    companion object {
        private const val NOTIFICATIONS_COLLECTION = "notifications"
        private const val USERS_COLLECTION = "users"
        private const val USER_NOTIFICATIONS_SUBCOLLECTION = "user_notifications"

        // field names
        private const val FIELD_ID = "id"
        private const val FIELD_TITLE = "title"
        private const val FIELD_MESSAGE = "message"
        private const val FIELD_TIMESTAMP = "timestamp"
        private const val FIELD_TYPE = "type"
        private const val FIELD_STATUS = "status"
        private const val FIELD_USER_ID = "userId"
        private const val FIELD_CREATED_AT = "createdAt"
        private const val FIELD_UPDATED_AT = "updatedAt"
    }

    /**
     * Data class for Firestore notification document
     */
    data class FirestoreNotification(
        val id: String = "",
        val title: String = "",
        val message: String = "",
        val timestamp: FirebaseTimestamp = FirebaseTimestamp.now(),
        val type: String = Notification.NotificationType.GENERAL.name,
        val status: String = Notification.Status.UNREAD.name,
        val userId: String? = "",
        val createdAt: FirebaseTimestamp = FirebaseTimestamp.now(),
        val updatedAt: FirebaseTimestamp = FirebaseTimestamp.now()
    ) {
        /**
         * Convert to domain Notification model
         */
        fun toNotification(): Notification {
            return Notification(
                id = id,
                title = title,
                message = message,
                timestamp = Timestamp(timestamp.toDate().time),
                type = try {
                    Notification.NotificationType.valueOf(type)
                } catch (e: Exception) {
                    Notification.NotificationType.GENERAL
                },
                status = try {
                    Notification.Status.valueOf(status)
                } catch (e: Exception) {
                    Notification.Status.UNREAD
                }
            )
        }

        companion object {
            /**
             * Create FirestoreNotification from domain Notification model
             */
            fun fromNotification(notification: Notification, userId: String?): FirestoreNotification {
                return FirestoreNotification(
                    id = notification.id,
                    title = notification.title,
                    message = notification.message,
                    timestamp = FirebaseTimestamp(notification.timestamp),
                    type = notification.type.name,
                    status = notification.status.name,
                    userId = userId,
                    createdAt = FirebaseTimestamp.now(),
                    updatedAt = FirebaseTimestamp.now()
                )
            }
        }
    }

    /**
     * Get all user IDs from database
     */
    suspend fun getAllUserIds(): Result<List<String>> {
        return try {
            val usersSnapshot = firestore.collection(USERS_COLLECTION).get().await()
            val userIds = usersSnapshot.documents.map { it.id }
            Result.success(userIds)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get all notifications for a specific user as Flow
     */
    fun getUserNotifications(userId: String?): Flow<List<Notification>> {
        return callbackFlow {
            val listenerRegistration = firestore
                .collection(USERS_COLLECTION)
                .document(userId.toString())
                .collection(USER_NOTIFICATIONS_SUBCOLLECTION)
                .orderBy(FIELD_TIMESTAMP, Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }

                    val notifications = snapshot?.documents?.mapNotNull { doc ->
                        doc.toObject<FirestoreNotification>()?.toNotification()
                    } ?: emptyList()

                    trySend(notifications)
                }

            awaitClose {
                listenerRegistration.remove()
            }
        }
    }

    /**
     * Get all global notifications (broadcast to all users)
     */
    fun getGlobalNotifications(): Flow<List<Notification>> {
        return callbackFlow {
            val listenerRegistration = firestore
                .collection(NOTIFICATIONS_COLLECTION)
                .whereEqualTo("isGlobal", true)
                .orderBy(FIELD_TIMESTAMP, Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }

                    val notifications = snapshot?.documents?.mapNotNull { doc ->
                        doc.toObject<FirestoreNotification>()?.toNotification()
                    } ?: emptyList()

                    trySend(notifications)
                }

            awaitClose {
                listenerRegistration.remove()
            }
        }
    }

    /**
     * Get a specific notification by ID for a user
     */
    fun getNotificationById(userId: String?, id: String): Flow<Notification?> {
        return callbackFlow {
            val listenerRegistration = firestore
                .collection(USERS_COLLECTION)
                .document(userId.toString())
                .collection(USER_NOTIFICATIONS_SUBCOLLECTION)
                .document(id)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }
                    val notification = snapshot?.toObject(FirestoreNotification::class.java)?.toNotification()
                    trySend(notification)
                }

            awaitClose {
                listenerRegistration.remove()
            }
        }
    }

    /**
     * Add a new notification for a user
     */
    suspend fun addNotification(notification: Notification, userId: String?): Result<String> {
        return try {
            val firestoreNotification = FirestoreNotification.fromNotification(notification, userId)

            val documentRef = firestore
                .collection(USERS_COLLECTION)
                .document(userId.toString())
                .collection(USER_NOTIFICATIONS_SUBCOLLECTION)
                .document(notification.id)

            documentRef.set(firestoreNotification).await()
            Result.success(notification.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Add a notification for multiple users
     */
    suspend fun addNotificationForUsers(
        notification: Notification,
        userIds: List<String>
    ): Result<List<String>> {
        return try {
            val batch = firestore.batch()
            val addedIds = mutableListOf<String>()

            userIds.forEach { userId ->
                val firestoreNotification = FirestoreNotification.fromNotification(notification, userId)
                val docRef = firestore
                    .collection(USERS_COLLECTION)
                    .document(userId)
                    .collection(USER_NOTIFICATIONS_SUBCOLLECTION)
                    .document(notification.id)

                batch.set(docRef, firestoreNotification)
                addedIds.add(notification.id)
            }

            batch.commit().await()
            Result.success(addedIds)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update notification status
     */
    suspend fun updateNotificationStatus(
        notificationId: String,
        userId: String?,
        status: Notification.Status
    ): Result<Unit> {
        return try {
            firestore
                .collection(USERS_COLLECTION)
                .document(userId.toString())
                .collection(USER_NOTIFICATIONS_SUBCOLLECTION)
                .document(notificationId)
                .update(
                    mapOf(
                        FIELD_STATUS to status.name,
                        FIELD_UPDATED_AT to FirebaseTimestamp.now()
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Mark notification as read
     */
    suspend fun markAsRead(notificationId: String, userId: String?): Result<Unit> {
        return updateNotificationStatus(notificationId, userId, Notification.Status.READ)
    }

    /**
     * Mark notification as archived
     */
    suspend fun markAsArchived(notificationId: String, userId: String?): Result<Unit> {
        return updateNotificationStatus(notificationId, userId, Notification.Status.ARCHIVED)
    }

    /**
     * Delete a notification (soft delete by updating status)
     */
    suspend fun deleteNotification(notificationId: String, userId: String?): Result<Unit> {
        return updateNotificationStatus(notificationId, userId, Notification.Status.DELETED)
    }

    /**
     * Permanently delete a notification
     */
    suspend fun permanentlyDeleteNotification(notificationId: String, userId: String?): Result<Unit> {
        return try {
            firestore
                .collection(USERS_COLLECTION)
                .document(userId.toString())
                .collection(USER_NOTIFICATIONS_SUBCOLLECTION)
                .document(notificationId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Mark all notifications as read for a user
     */
    suspend fun markAllAsRead(userId: String?): Result<Unit> {
        return try {
            val batch = firestore.batch()

            val unreadNotifications = firestore
                .collection(USERS_COLLECTION)
                .document(userId.toString())
                .collection(USER_NOTIFICATIONS_SUBCOLLECTION)
                .whereEqualTo(FIELD_STATUS, Notification.Status.UNREAD.name)
                .get()
                .await()

            unreadNotifications.documents.forEach { doc ->
                batch.update(
                    doc.reference,
                    mapOf(
                        FIELD_STATUS to Notification.Status.READ.name,
                        FIELD_UPDATED_AT to FirebaseTimestamp.now()
                    )
                )
            }

            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get unread notification count for a user
     */
    fun getUnreadCount(userId: String?): Flow<Int> {
        return callbackFlow {
            val listenerRegistration = firestore
                .collection(USERS_COLLECTION)
                .document(userId.toString())
                .collection(USER_NOTIFICATIONS_SUBCOLLECTION)
                .whereEqualTo(FIELD_STATUS, Notification.Status.UNREAD.name)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(0)
                        return@addSnapshotListener
                    }

                    trySend(snapshot?.size() ?: 0)
                }

            awaitClose {
                listenerRegistration.remove()
            }
        }
    }

    /**
     * Get notifications by type for a user
     */
    fun getNotificationsByType(
        userId: String?,
        type: Notification.NotificationType
    ): Flow<List<Notification>> {
        return callbackFlow {
            val listenerRegistration = firestore
                .collection(USERS_COLLECTION)
                .document(userId.toString())
                .collection(USER_NOTIFICATIONS_SUBCOLLECTION)
                .whereEqualTo(FIELD_TYPE, type.name)
                .orderBy(FIELD_TIMESTAMP, Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }

                    val notifications = snapshot?.documents?.mapNotNull { doc ->
                        doc.toObject<FirestoreNotification>()?.toNotification()
                    } ?: emptyList()

                    trySend(notifications)
                }

            awaitClose {
                listenerRegistration.remove()
            }
        }
    }

    /**
     * Get notifications by status for a user
     */
    fun getNotificationsByStatus(
        userId: String?,
        status: Notification.Status
    ): Flow<List<Notification>> {
        return callbackFlow {
            val listenerRegistration = firestore
                .collection(USERS_COLLECTION)
                .document(userId.toString())
                .collection(USER_NOTIFICATIONS_SUBCOLLECTION)
                .whereEqualTo(FIELD_STATUS, status.name)
                .orderBy(FIELD_TIMESTAMP, Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }

                    val notifications = snapshot?.documents?.mapNotNull { doc ->
                        doc.toObject<FirestoreNotification>()?.toNotification()
                    } ?: emptyList()

                    trySend(notifications)
                }

            awaitClose {
                listenerRegistration.remove()
            }
        }
    }

    /**
     * Delete old notifications (older than specified days)
     */
    suspend fun deleteOldNotifications(userId: String?, daysOld: Int): Result<Unit> {
        return try {
            val cutoffDate = FirebaseTimestamp(
                java.util.Date(System.currentTimeMillis() - (daysOld * 24 * 60 * 60 * 1000L))
            )

            val batch = firestore.batch()

            val oldNotifications = firestore
                .collection(USERS_COLLECTION)
                .document(userId.toString())
                .collection(USER_NOTIFICATIONS_SUBCOLLECTION)
                .whereLessThan(FIELD_TIMESTAMP, cutoffDate)
                .get()
                .await()

            oldNotifications.documents.forEach { doc ->
                batch.delete(doc.reference)
            }

            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Generate a unique notification ID
     */
    fun generateNotificationId(): String {
        return firestore.collection(NOTIFICATIONS_COLLECTION).document().id
    }
}