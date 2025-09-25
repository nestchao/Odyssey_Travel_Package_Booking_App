// src/main/java/com/example/mad_assignment/ui/recentlyviewed/RecentlyViewedUiState.kt
package com.example.mad_assignment.ui.recentlyviewed

// NO CHANGE to TravelPackage model
// NO CHANGE to TravelPackageWithImages model

import com.example.mad_assignment.data.model.TravelPackageWithImages // Import this

sealed interface RecentlyViewedUiState {
    object Loading : RecentlyViewedUiState
    data class Error(val message: String) : RecentlyViewedUiState
    data class Success(
        // Change this to store TravelPackageWithImages
        val recentlyViewedPackages: List<TravelPackageWithImages> = emptyList()
    ) : RecentlyViewedUiState
    object Empty : RecentlyViewedUiState
}