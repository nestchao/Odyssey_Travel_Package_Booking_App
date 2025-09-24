package com.example.mad_assignment.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class Booking(
    @DocumentId val bookingId: String = "",
    val userId: String = "", // get from cart
    val packageId: String = "", // get from cart item
    val paymentId: String = "", // get from payment
    val noOfAdults: Int = 0, // get from cart item
    val noOfChildren: Int = 0, // get from cart item
    val totalTravelerCount: Int = 0, // get from cart item
    val subtotal: Double = 0.0, // get from cart item
    val discountAmount: Double = 0.0, // get from payment
    val taxAmount: Double = 0.0, // get from payment
    val totalAmount: Double = 0.0, // update from payment the total price after tax, discount, etc
    val startBookingDate: Timestamp? = null, // get from cart item
    val endBookingDate: Timestamp? = null, // get from cart item
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
    val status: BookingStatus = BookingStatus.PENDING,
    val bookingType: BookingType = BookingType.UPCOMING
)