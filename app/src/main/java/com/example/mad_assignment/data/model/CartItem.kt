package com.example.mad_assignment.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class CartItem(
    @DocumentId val cartItemId: String = "",
    val packageId: String = "",
    val departureDate: DepartureDate? = null,
    val basePrice: Double = 0.0, // get from travel package pricing
    val totalPrice: Double = 0.0,
    val travelerCount: Int = 1,
    val durationDays: Int = 1,
    val addedAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
    val expiresAt: Timestamp? = null,
    val isAvailable: Boolean = true,
)
