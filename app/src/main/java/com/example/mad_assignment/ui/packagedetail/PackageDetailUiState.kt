package com.example.mad_assignment.ui.packagedetail

import com.example.mad_assignment.data.model.TravelPackage

sealed interface PackageDetailUiState {
    data class Success(val travelPackage: TravelPackage) : PackageDetailUiState
    data class Error(val message: String) : PackageDetailUiState
    object Loading : PackageDetailUiState
}
