package com.example.mad_assignment.ui.profile

import com.example.mad_assignment.data.model.User

sealed interface ProfileUiState {
    data class Loading(
        val isRefreshing: Boolean = false
    ) : ProfileUiState

    data class Success(
        val user: User,
        val isRefreshing: Boolean = false
    ) : ProfileUiState {
        val displayName: String get() = user.userName.ifEmpty { user.userEmail }
        val totalTrips: Int get() = 1 // You can extend User model to include these stats
        val totalReviews: Int get() = 1
        val yearsOnOdyssey: Int get() = 1
        val upcomingBookings: Int get() = 0
        val wishlistCount: Int get() = 0
    }

    data class Error(
        val message: String,
        val user: User? = null,
        val isRefreshing: Boolean = false
    ) : ProfileUiState
}