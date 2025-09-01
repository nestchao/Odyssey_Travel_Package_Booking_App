package com.example.mad_assignment.data.model

import com.google.firebase.firestore.DocumentId

data class TravelPackage(
    @DocumentId val packageId: String = "",
    val packageName: String = "",
    val packageDescription: String = "",
    val imageUrls: List<String> = emptyList(),
    val pricing: Map<String, Double> = emptyMap(),
    val durationDays: Int = 0,
    val status: PackageStatus = PackageStatus.AVAILABLE, 
    val itinerary: List<Trip> = emptyList(),
    val isFeatured: Boolean = false
)