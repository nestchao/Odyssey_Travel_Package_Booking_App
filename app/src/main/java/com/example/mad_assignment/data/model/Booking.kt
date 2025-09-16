package com.example.mad_assignment.data.model

import com.google.firebase.firestore.DocumentId
import java.time.LocalDateTime

data class Booking(
    @DocumentId val bookingId: String = "",
    val userId: String = "",
    val packageId: String = "",
    val cartId: String = "",
    val cartItemId: String = "",
    val paymentId: String = "",
    val subtotal: Double = 0.0,
    val discountAmount: Double = 0.0,
    val taxAmount: Double = 0.0,
    val totalAmount: Double = 0.0,
    val bookingDate: LocalDateTime,
    val updatedAt: LocalDateTime,
    val status: BookingStatus = BookingStatus.PENDING
)