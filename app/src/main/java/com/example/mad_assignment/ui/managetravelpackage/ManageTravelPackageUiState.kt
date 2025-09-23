package com.example.mad_assignment.ui.managetravelpackage

import android.net.Uri
import com.example.mad_assignment.data.model.DepartureAndEndTime
import com.example.mad_assignment.data.model.ItineraryItem
import com.example.mad_assignment.data.model.PackageImage
import com.example.mad_assignment.data.model.Trip
import com.google.firebase.Timestamp

data class ManageTravelPackageUiState(
    val packageId: String? = null,
    val packageName: String = "",
    val packageDescription: String = "",
    val location: String = "",
    val durationDays: String = "",
    val pricing: Map<String, String> = mapOf("Adult" to "", "Child" to ""),
    val itineraries: List<ItineraryItem> = emptyList(),
    val packageOptions: List<DepartureAndEndTime> = emptyList(),

    val createdAt: Timestamp? = null,

    val initialImages: List<PackageImage> = emptyList(),
    val newImageUris: List<Uri> = emptyList(),
    val removedImageIds: Set<String> = emptySet(),

    val availableTrips: List<Trip> = emptyList(),

    val validationErrors: Map<String, String?> = emptyMap(),

    val editingItineraryItem: ItineraryItem? = null,

    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val error: String? = null,
    val saveSuccess: Boolean = false
) {
    val isEditing: Boolean get() = packageId != null

    val displayedImages: List<Any>
        get() = initialImages.filter { it.imageId !in removedImageIds } + newImageUris
}