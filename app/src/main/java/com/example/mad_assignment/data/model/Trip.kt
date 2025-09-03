package com.example.mad_assignment.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.GeoPoint

data class Trip(
    @DocumentId val tripId: String = "",
    val tripName: String = "",
    val geoPoint: GeoPoint? = null
)
