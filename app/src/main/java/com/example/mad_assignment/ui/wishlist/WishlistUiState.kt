package com.example.mad_assignment.ui.wishlist

import com.example.mad_assignment.data.model.TravelPackage
import com.example.mad_assignment.data.model.WishlistItem

sealed interface WishlistUiState {
    object Loading : WishlistUiState
    data class Error(val message: String) : WishlistUiState
    data class Success(val wishlistPackages: List<TravelPackage> = emptyList()) : WishlistUiState
    object Empty : WishlistUiState
}