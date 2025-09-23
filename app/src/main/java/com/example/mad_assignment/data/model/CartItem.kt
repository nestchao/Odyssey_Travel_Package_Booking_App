package com.example.mad_assignment.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class CartItem(
    @DocumentId val cartItemId: String = "",
    val packageId: String = "",
    val basePrice: Double = 0.0,
    val totalPrice: Double = 0.0,
    val noOfAdults: Int = 0,
    val noOfChildren: Int = 0,
    val totalTravelerCount: Int = 0,
    val startDate: Timestamp? = null,
    val endDate: Timestamp? = null,
    val durationDays: Int = 1,
    val addedAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
    val expiresAt: Timestamp? = null,
    val isAvailable: Boolean = true,
)
