package com.example.mad_assignment.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

data class Activity(
    @DocumentId val id: String = "",
    val description: String = "",
    val type: ActivityType = ActivityType.UNKNOWN,
    @ServerTimestamp val timestamp: Timestamp? = null,
    val userId: String? = null,
    val relatedId: String? = null
)

enum class ActivityType {
    USER_REGISTRATION,
    BOOKING_CREATED,
    PAYMENT_COMPLETED,
    BOOKING_CANCELLED,
    USER_UPDATED,
    UNKNOWN
}