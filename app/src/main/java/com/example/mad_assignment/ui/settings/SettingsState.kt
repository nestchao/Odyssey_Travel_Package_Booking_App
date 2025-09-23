package com.example.mad_assignment.ui.settings

sealed interface SettingsUiState {
    data class Loading(
        val isLoading: Boolean = false
    ) : SettingsUiState

    data class Success(
        val isLoading: Boolean = false
    ) : SettingsUiState

    data class Error(
        val message: String,
        val isLoading: Boolean = false
    ) : SettingsUiState
}