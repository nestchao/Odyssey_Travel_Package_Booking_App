package com.example.mad_assignment.ui.home

import com.example.mad_assignment.data.model.TravelPackage

sealed interface HomeUiState{
    data class Success(
        val featuredPackages: List<TravelPackage>,
        val allPackages: List<TravelPackage>
    ) : HomeUiState
    data class Error(val message: String): HomeUiState
    object Loading : HomeUiState
}