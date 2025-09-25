package com.example.mad_assignment.ui.checkout

import com.example.mad_assignment.data.model.User

sealed interface CheckoutUiState {
    object Loading : CheckoutUiState
    data class Error(val message: String) : CheckoutUiState
    data class Success(
        // It now holds a list of items, not a single package detail
        val displayItems: List<CheckoutDisplayItem>,
        val user: User,
        val totalPrice: Double,
        val isProcessingPayment: Boolean = false
    ) : CheckoutUiState
}

// CheckoutResultEvent remains the same
sealed interface CheckoutResultEvent {
    object Success : CheckoutResultEvent
    data class Failure(val message: String) : CheckoutResultEvent
}