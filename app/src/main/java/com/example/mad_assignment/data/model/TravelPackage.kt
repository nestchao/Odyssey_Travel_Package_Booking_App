package com.example.mad_assignment.data.model

import com.google.firebase.firestore.DocumentId

data class TravelPackage(
    @DocumentId val packageId: String = "",
    val packageName: String = "",
    val packageDescription: String = "",
    val imageUrls: List<String> = emptyList(),
    val location: String = "",
    val durationDays: Int = 0,
    val pricing: Map<String, Double> = emptyMap(),
    val itineraries: List<ItineraryItem> = emptyList()
)