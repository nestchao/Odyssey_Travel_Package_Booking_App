package com.example.mad_assignment.data.model

import com.google.firebase.Timestamp

data class WishlistItem(
    val id: String = "",
    val packageId: String = "",
    val addedAt: Timestamp = Timestamp.now()
)