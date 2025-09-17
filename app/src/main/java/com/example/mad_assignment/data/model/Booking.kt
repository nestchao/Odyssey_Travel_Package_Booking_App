package com.example.mad_assignment.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class Booking(
    @DocumentId val bookingId: String = "",
    val userId: String = "",
    val packageId: String = "",
    val paymentId: String = "", // get from payment
    val subtotal: Double = 0.0, // cart item base price
    val discountAmount: Double = 0.0, // get from payment
    val taxAmount: Double = 0.0, // get from payment
    val totalAmount: Double = 0.0, // update from payment the total price after tax, discount, etc
    val departureDate: DepartureDate? = null,
    val bookingDate: Timestamp? = null,
    val updatedAt: Timestamp? = null,
    val status: BookingStatus = BookingStatus.PENDING
)