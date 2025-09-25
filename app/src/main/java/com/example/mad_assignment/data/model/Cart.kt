package com.example.mad_assignment.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class Cart(
    @DocumentId val cartId: String = "",
    val userId: String = "",
    val cartItemIds: List<String> = emptyList(),
    val totalAmount: Double = 0.0,
    val finalAmount: Double = 0.0,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
    val isValid: Boolean = false // invalid if user cancel booking or something happened - logic problem
)