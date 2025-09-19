package com.example.mad_assignment.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class CartItem(
    @DocumentId val cartItemId: String = "",
    val packageId: String = "",
    val departureDate: DepartureDate? = null, // TODO: update with new package data class
    val basePrice: Double = 0.0,
    val totalPrice: Double = 0.0,
    val noOfAdults: Int = 0,
    val noOfChildren: Int = 0,
    val totalTravelerCount: Int = 0,
    val durationDays: Int = 1,
    val addedAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
    val expiresAt: Timestamp? = null,
    val isAvailable: Boolean = true,
)
