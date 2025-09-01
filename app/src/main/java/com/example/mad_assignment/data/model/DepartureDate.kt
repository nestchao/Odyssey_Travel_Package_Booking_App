package com.example.mad_assignment.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class DepartureDate(
    @DocumentId val id: String = "",
    val startDate: Timestamp = Timestamp.now(),
    val endDate: Timestamp = Timestamp.now(),
    val maxCapacity: Int = 25,
    val currentBookings: Int = 0,
    val status: String = "AVAILABLE"
)