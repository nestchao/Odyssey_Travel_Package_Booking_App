package com.example.mad_assignment.ui.home

sealed interface HomeUiState{
    data class Success(val packages: List<TravelPackageWithImages>) : HomeUiState
    data class Error(val message: String): HomeUiState
    object Loading : HomeUiState
}