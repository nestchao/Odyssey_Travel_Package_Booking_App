package com.example.mad_assignment.ui.wishlist

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mad_assignment.data.model.TravelPackage
import com.example.mad_assignment.data.model.WishlistItem
import com.example.mad_assignment.data.repository.WishlistRepository
import com.example.mad_assignment.data.repository.TravelPackageRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WishlistViewModel @Inject constructor(
    private val wishlistRepository: WishlistRepository,
    private val travelPackageRepository: TravelPackageRepository,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {
    private val _uiState = MutableStateFlow<WishlistUiState>(WishlistUiState.Loading)
    val uiState: StateFlow<WishlistUiState> = _uiState.asStateFlow()
    private val userId = getCurrentUserId()

    private fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    init {
        loadWishlist()
    }

    fun loadWishlist() {
        viewModelScope.launch {
            _uiState.value = WishlistUiState.Loading

            try {
                Log.d("WishlistVM", "Loading wishlist for user: $userId")

                val wishlistItems = wishlistRepository.getWishlist(userId)

                if (wishlistItems.isEmpty()) {
                    Log.d("WishlistVM", "No wishlist items found")
                    _uiState.value = WishlistUiState.Empty
                    return@launch
                }

                Log.d("WishlistVM", "Found ${wishlistItems.size} wishlist items")

                // Fetch the actual travel packages
                val packages = mutableListOf<TravelPackage>()

                for (item in wishlistItems) {
                    try {
                        val packageData = travelPackageRepository.getPackageWithImages(item.packageId)
                        packageData?.let {
                            packages.add(it.travelPackage)
                            Log.d("WishlistVM", "Loaded package: ${it.travelPackage.packageName}")
                        }
                    } catch (e: Exception) {
                        Log.w("WishlistVM", "Failed to load package ${item.packageId}", e)
                        // Continue loading other packages even if one fails
                    }
                }

                if (packages.isEmpty()) {
                    Log.w("WishlistVM", "No valid packages found from wishlist items")
                    _uiState.value = WishlistUiState.Empty
                } else {
                    Log.d("WishlistVM", "Successfully loaded ${packages.size} packages")
                    _uiState.value = WishlistUiState.Success(
                        wishlistPackages = packages
                    )
                }

            } catch (e: Exception) {
                Log.e("WishlistVM", "Error loading wishlist", e)
                _uiState.value = WishlistUiState.Error(
                    message = e.message ?: "Failed to load wishlist"
                )
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

    fun removeFromWishlistByPackageId(packageId: String) {
        viewModelScope.launch {
            try {
                val wishlistItem = wishlistRepository.getWishlistItemByPackageId(userId, packageId)
                wishlistItem?.let {
                    wishlistRepository.removeFromWishlist(userId, it.id)
                    loadWishlist() // Refresh the list
                }
            } catch (e: Exception) {
                _uiState.value = WishlistUiState.Error("Failed to remove item: ${e.message}")
            }
        }
    }

    fun clearWishlist() {
        viewModelScope.launch {
            try {
                Log.d("WishlistVM", "Clearing wishlist items")
                val wishlistItems = wishlistRepository.getWishlist(userId)
                for (item in wishlistItems) {
                    wishlistRepository.removeFromWishlist(userId, item.id)
                }
                _uiState.value = WishlistUiState.Empty
            } catch (e: Exception) {
                Log.e("WishlistVM", "Error clearing wishlist", e)
                _uiState.value = WishlistUiState.Error(
                    message = "Failed to clear wishlist"
                )
            }
        }
    }

    fun addToWishlist(packageId: String) {
        viewModelScope.launch {
            try {
                wishlistRepository.addToWishlist(userId, packageId)
                loadWishlist() // Refresh the list
            } catch (e: Exception) {
                _uiState.value = WishlistUiState.Error("Failed to add item: ${e.message}")
            }
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