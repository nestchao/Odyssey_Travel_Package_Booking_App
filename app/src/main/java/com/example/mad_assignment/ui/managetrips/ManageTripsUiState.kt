package com.example.mad_assignment.ui.managetrip

import com.google.firebase.firestore.GeoPoint

data class ManageTripUiState(
    val tripId: String? = null,
    val tripName: String = "",
    val latitude: String = "",
    val longitude: String = "",

    val validationErrors: Map<String, String?> = emptyMap(),

    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val error: String? = null,
    val saveSuccess: Boolean = false
) {
    val isEditing: Boolean get() = tripId != null
}