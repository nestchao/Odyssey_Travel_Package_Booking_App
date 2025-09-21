package com.example.mad_assignment.ui.wishlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mad_assignment.data.model.WishlistItem
import com.example.mad_assignment.data.respository.WishlistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WishlistViewModel @Inject constructor(
    private val wishlistRepository: WishlistRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow<WishlistUiState>(WishlistUiState.Loading)
    val uiState: StateFlow<WishlistUiState> = _uiState.asStateFlow()

    // TODO: Replace with actual user ID
    private val userId = "current_user_id"

    init {
        loadWishlist()
    }

    fun loadWishlist() {
        viewModelScope.launch {
            try {
                val wishlistItems = wishlistRepository.getWishlist(userId)
                _uiState.value = WishlistUiState.Success(wishlistItems)
            } catch (e: Exception) {
                _uiState.value = WishlistUiState.Error(e.message ?: "Failed to load wishlist")
            }
        }
    }

    fun removeFromWishlist(wishlistItemId: String) {
        viewModelScope.launch {
            try {
                wishlistRepository.removeFromWishlist(userId, wishlistItemId)
                loadWishlist() // Refresh the list
            } catch (e: Exception) {
                _uiState.value = WishlistUiState.Error("Failed to remove item: ${e.message}")
            }
        }
    }

    fun addToWishlist(packageId: String) {
        viewModelScope.launch {
            wishlistRepository.addToWishlist(userId, packageId)
            loadWishlist() // Refresh the list
        }
    }

    suspend fun isPackageInWishlist(packageId: String): Boolean {
        return try {
            wishlistRepository.isInWishlist(userId, packageId)
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getWishlistItemByPackageId(packageId: String): WishlistItem? {
        return try {
            wishlistRepository.getWishlistItemByPackageId(userId, packageId)
        } catch (e: Exception) {
            null
        }
    }
}