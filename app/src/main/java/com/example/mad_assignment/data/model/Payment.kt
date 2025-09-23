package com.example.mad_assignment.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

data class Payment (
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val associatedPackageId: String = "",
    val amount: Double = 0.0,
    val status: PaymentStatus = PaymentStatus.PENDING,
    val paymentMethod: String? = null,
    val gatewayTransactionId: String? = null,
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
)

enum class PaymentStatus {
    PENDING,
    PAID,
    FAILED,
    REFUNDED
}
