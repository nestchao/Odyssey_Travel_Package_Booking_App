package com.example.mad_assignment.ui.wishlist

import com.example.mad_assignment.data.model.WishlistItem

sealed interface WishlistUiState {
    object Loading : WishlistUiState
    data class Error(val message: String) : WishlistUiState
    data class Success(val wishlistItems: List<WishlistItem>) : WishlistUiState
}