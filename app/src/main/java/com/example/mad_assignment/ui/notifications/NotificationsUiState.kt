package com.example.mad_assignment.ui.notifications

import com.example.mad_assignment.data.model.Notification

sealed interface NotificationsUiState {
    object Loading : NotificationsUiState
    object Empty : NotificationsUiState
    data class Success(val notifications: List<Notification>) : NotificationsUiState
    data class Error(val message: String) : NotificationsUiState
}