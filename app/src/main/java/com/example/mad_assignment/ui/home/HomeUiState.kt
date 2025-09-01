package com.example.mad_assignment.ui.home

import com.example.mad_assignment.data.model.TravelPackage

sealed interface HomeUiState{
    data class Success(val packages: List<TravelPackage>) : HomeUiState
    data class Error(val message: String): HomeUiState
    object Loading : HomeUiState
}