package com.example.mad_assignment.ui.recentlyviewed

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mad_assignment.data.model.TravelPackage
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

                // Fetch the actual travel packages
                val packages = mutableListOf<TravelPackage>()

                for (item in recentlyViewedItems) {
                    try {
                        val packageData = travelPackageRepository.getPackageWithImages(item.packageId)
                        packageData?.let {
                            packages.add(it.travelPackage)
                            Log.d("RecentlyViewedVM", "Loaded package: ${it.travelPackage.packageName}")
                        }
                    } catch (e: Exception) {
                        Log.w("RecentlyViewedVM", "Failed to load package ${item.packageId}", e)
                        // Continue loading other packages even if one fails
                    }
                }

                if (packages.isEmpty()) {
                    Log.w("RecentlyViewedVM", "No valid packages found from recently viewed items")
                    _uiState.value = RecentlyViewedUiState.Empty
                } else {
                    Log.d("RecentlyViewedVM", "Successfully loaded ${packages.size} packages")
                    _uiState.value = RecentlyViewedUiState.Success(
                        recentlyViewedPackages = packages
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