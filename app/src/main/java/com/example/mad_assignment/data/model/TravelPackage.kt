package com.example.mad_assignment.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class TravelPackage(
    @DocumentId val packageId: String = "",
    val packageName: String = "",
    val packageDescription: String = "",
    val imageDocsId: List<String> = emptyList(),
    val location: String = "",
    val durationDays: Int = 0,
    val pricing: Map<String, Double> = emptyMap(),
    val itineraries: List<ItineraryItem> = emptyList(),
    val packageOption: List<DepartureAndEndTime> = emptyList(),
    val status: PackageStatus = PackageStatus.AVAILABLE,
    val createdAt: Timestamp?= null,
    val deletedAt: Timestamp? = null
) {
    enum class PackageStatus {
        AVAILABLE,
        SOLD_OUT,
        CANCELLED,
        EXPIRED
    }
}