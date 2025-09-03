package com.example.mad_assignment.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.Timestamp

data class DepartureDate(
    @DocumentId val id: String = "",
    val packageId: String = "",
    val startDate: Timestamp = Timestamp.now(),
    val capacity: Int = 0
)