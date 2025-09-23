package com.example.mad_assignment.ui.home

import com.example.mad_assignment.data.model.TravelPackageWithImages

sealed interface HomeUiState{
    data class Success(
        val packages: List<TravelPackageWithImages>,
        val isAdmin: Boolean = false
    ) : HomeUiState
    data class Error(val message: String): HomeUiState
    object Loading : HomeUiState
}