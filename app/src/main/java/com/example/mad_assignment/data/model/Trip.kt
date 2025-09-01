package com.example.mad_assignment.data.model

import com.google.firebase.firestore.DocumentId

data class Trip(
    @DocumentId val tripId: String = "",
    val tripName: String = "",
    val location: Location = Location()
)
