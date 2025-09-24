package com.example.mad_assignment.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

data class Payment (
    @DocumentId
    val paymentId: String = "",
    val userId: String = "",
    val bookingIds: List<String> = emptyList(),
    val amount: Double = 0.0,
    val paymentMethod: String = "",
    val status: PaymentStatus = PaymentStatus.PENDING,
    val gatewayTransactionId: String? = null,
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
)

enum class PaymentStatus {
    PENDING,
    SUCCESS,
    FAILED,
    REFUNDED
}