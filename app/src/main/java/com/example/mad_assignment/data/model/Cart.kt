package com.example.mad_assignment.data.model

import com.google.firebase.firestore.DocumentId
import java.time.LocalDateTime

data class Cart(
    @DocumentId val cartId: String = "",
    val userId: String = "",
    val cartItemIds: List<String> = emptyList(),
    val totalAmount: Double = 0.0,
    val finalAmount: Double = 0.0,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val isValid: Boolean = true // invalid if user cancel booking or something happened - logic problem
)
