package com.example.mad_assignment.ui.wishlist

import com.example.mad_assignment.data.model.TravelPackageWithImages // Import this

sealed interface WishlistUiState {
    object Loading : WishlistUiState
    data class Error(val message: String) : WishlistUiState
    data class Success(
        val wishlistPackages: List<TravelPackageWithImages> = emptyList()
    ) : WishlistUiState
    object Empty : WishlistUiState
}