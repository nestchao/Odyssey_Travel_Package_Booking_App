// src/main/java/com/example/mad_assignment/ui/recentlyviewed/RecentlyViewedViewModel.kt
package com.example.mad_assignment.ui.recentlyviewed

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
// NO CHANGE to TravelPackage model import, it's not directly used here for the list type
import com.example.mad_assignment.data.model.TravelPackageWithImages // Import this
import com.example.mad_assignment.data.repository.TravelPackageRepository
import com.example.mad_assignment.data.repository.RecentlyViewedRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecentlyViewedViewModel @Inject constructor(
    private val recentlyViewedRepository: RecentlyViewedRepository,
    private val travelPackageRepository: TravelPackageRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow<RecentlyViewedUiState>(RecentlyViewedUiState.Loading)
    val uiState: StateFlow<RecentlyViewedUiState> = _uiState.asStateFlow()

    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    init {
        loadRecentlyViewed()
    }

    fun loadRecentlyViewed() {
        viewModelScope.launch {
            val currentUserId = getCurrentUserId()
            _uiState.value = RecentlyViewedUiState.Loading

            try {
                Log.d("RecentlyViewedVM", "Loading recently viewed packages for user: $currentUserId")

                val recentlyViewedItems = recentlyViewedRepository.getRecentlyViewed(currentUserId)

                if (recentlyViewedItems.isEmpty()) {
                    Log.d("RecentlyViewedVM", "No recently viewed items found")
                    _uiState.value = RecentlyViewedUiState.Empty
                    return@launch
                }

                Log.d("RecentlyViewedVM", "Found ${recentlyViewedItems.size} recently viewed items")

                // IMPORTANT CHANGE: Store TravelPackageWithImages directly
                val packagesWithImages = mutableListOf<TravelPackageWithImages>()

                for (item in recentlyViewedItems) {
                    try {
                        val packageData = travelPackageRepository.getPackageWithImages(item.packageId)
                        packageData?.let {
                            packagesWithImages.add(it) // Add the whole TravelPackageWithImages object
                            Log.d("RecentlyViewedVM", "Loaded package: ${it.travelPackage.packageName}")
                        }
                    } catch (e: Exception) {
                        Log.w("RecentlyViewedVM", "Failed to load package ${item.packageId}", e)
                        // Continue loading other packages even if one fails
                    }
                }

                if (packagesWithImages.isEmpty()) {
                    Log.w("RecentlyViewedVM", "No valid packages found from recently viewed items")
                    _uiState.value = RecentlyViewedUiState.Empty
                } else {
                    Log.d("RecentlyViewedVM", "Successfully loaded ${packagesWithImages.size} packages")
                    _uiState.value = RecentlyViewedUiState.Success(
                        recentlyViewedPackages = packagesWithImages // Pass the list of TravelPackageWithImages
                    )
                }

            } catch (e: Exception) {
                Log.e("RecentlyViewedVM", "Error loading recently viewed packages", e)
                _uiState.value = RecentlyViewedUiState.Error(
                    message = e.message ?: "Failed to load recently viewed packages"
                )
            }
        }
    }

    fun clearRecentlyViewed() {
        viewModelScope.launch {
            val currentUserId = getCurrentUserId()
            try {
                Log.d("RecentlyViewedVM", "Clearing recently viewed items")
                recentlyViewedRepository.clearRecentlyViewed(currentUserId)
                _uiState.value = RecentlyViewedUiState.Empty
            } catch (e: Exception) {
                Log.e("RecentlyViewedVM", "Error clearing recently viewed", e)
                // Show error state but keep existing packages visible
                _uiState.value = RecentlyViewedUiState.Error(
                    message = "Failed to clear recently viewed items"
                )
            }
        }
    }
}