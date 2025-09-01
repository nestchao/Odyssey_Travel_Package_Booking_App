package com.example.mad_assignment.ui.packagedetail

import com.example.mad_assignment.data.model.TravelPackage
import com.example.mad_assignment.data.model.Trip

sealed interface PackageDetailUiState {
    data class Success(
        val travelPackage: TravelPackage,
        val itinerary: List<Trip>
    ) : PackageDetailUiState
    data class Error(val message: String) : PackageDetailUiState
    object Loading : PackageDetailUiState
}