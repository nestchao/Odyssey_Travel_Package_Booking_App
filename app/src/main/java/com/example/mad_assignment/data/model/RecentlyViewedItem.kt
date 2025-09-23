package com.example.mad_assignment.data.model

import com.google.firebase.Timestamp

data class RecentlyViewedItem(
    val id: String = "",
    val packageId: String = "",
    val viewedAt: Timestamp = Timestamp.now()
)