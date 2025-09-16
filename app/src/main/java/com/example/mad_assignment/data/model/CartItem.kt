package com.example.mad_assignment.data.model

import com.google.firebase.firestore.DocumentId
import java.time.LocalDateTime

data class CartItem(
    @DocumentId val cartItemId: String = "",
    val packageId: String = "",
    val startDate: DepartureDate? = null,
    val basePrice: Double = 0.0,
    val totalPrice: Double = 0.0,
    val travelerCount: Int = 1,
    val durationDays: Int = 1,
    val addedAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val expiresAt: LocalDateTime,
    val isAvailable: Boolean = true,
)
