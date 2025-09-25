package com.example.mad_assignment.ui.recentlyviewed

import com.example.mad_assignment.data.model.TravelPackage

sealed interface RecentlyViewedUiState {
    object Loading : RecentlyViewedUiState
    data class Error(val message: String) : RecentlyViewedUiState
    data class Success(
        val recentlyViewedPackages: List<TravelPackage> = emptyList()
    ) : RecentlyViewedUiState
    object Empty : RecentlyViewedUiState
}