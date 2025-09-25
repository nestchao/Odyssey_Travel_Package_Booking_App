package com.example.mad_assignment.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mad_assignment.data.model.TravelPackageWithImages
import com.example.mad_assignment.data.repository.TravelPackageRepository
import com.example.mad_assignment.data.repository.WishlistRepository // NEW
import com.example.mad_assignment.data.repository.RecentlyViewedRepository // NEW
import com.example.mad_assignment.ui.home.HomeUiState
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth // NEW - for userId
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val travelPackageRepository: TravelPackageRepository,
    private val wishlistRepository: WishlistRepository, // NEW
    private val recentlyViewedRepository: RecentlyViewedRepository, // NEW
    private val auth: FirebaseAuth // NEW
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val userId = auth.currentUser?.uid // Get current user ID

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    val searchResults: StateFlow<HomeUiState> =
        travelPackageRepository.getTravelPackagesWithImages()
            .combine(searchQuery) { packages, query ->
                if (query.isBlank()) {
                    emptyList<TravelPackageWithImages>()
                } else {
                    packages.filter {
                        it.travelPackage.packageName.contains(query, ignoreCase = true) ||
                                it.travelPackage.location.contains(query, ignoreCase = true)
                    }
                }
            }
            .map<List<TravelPackageWithImages>, HomeUiState> { filteredPackages ->
                HomeUiState.Success(filteredPackages)
            }
            .catch { exception ->
                emit(HomeUiState.Error(exception.message ?: "An error occurred"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = HomeUiState.Loading
            )

    val latestPackages: StateFlow<HomeUiState> =
        travelPackageRepository.getTravelPackagesWithImages()
            .map<List<TravelPackageWithImages>, HomeUiState> { allPackages ->
                val sortedPackages = allPackages
                    .sortedByDescending { it.travelPackage.createdAt ?: Timestamp(0, 0) }
                    .take(5) // Adjust as needed
                HomeUiState.Success(sortedPackages)
            }
            .catch { exception ->
                emit(HomeUiState.Error(exception.message ?: "Could not load latest packages"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = HomeUiState.Loading
            )

    // NEW: Wishlist packages flow
    val wishlistPackages: StateFlow<HomeUiState> = flow {
        if (userId == null) {
            emit(HomeUiState.Error("User not logged in"))
            return@flow
        }
        val wishlistItems = wishlistRepository.getWishlist(userId)
        val packagesWithImages = mutableListOf<TravelPackageWithImages>()
        for (item in wishlistItems) {
            travelPackageRepository.getPackageWithImages(item.packageId)?.let {
                packagesWithImages.add(it)
            }
        }
        emit(HomeUiState.Success(packagesWithImages))
    }
        .onStart { emit(HomeUiState.Loading) }
        .catch { exception ->
            emit(HomeUiState.Error(exception.message ?: "Could not load wishlist"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = HomeUiState.Loading
        )

    // NEW: Recently Viewed packages flow
    val recentlyViewedPackages: StateFlow<HomeUiState> = flow {
        if (userId == null) {
            emit(HomeUiState.Error("User not logged in"))
            return@flow
        }
        val recentlyViewedItems = recentlyViewedRepository.getRecentlyViewed(userId)
        val packagesWithImages = mutableListOf<TravelPackageWithImages>()
        for (item in recentlyViewedItems) {
            travelPackageRepository.getPackageWithImages(item.packageId)?.let {
                packagesWithImages.add(it)
            }
        }
        emit(HomeUiState.Success(packagesWithImages))
    }
        .onStart { emit(HomeUiState.Loading) }
        .catch { exception ->
            emit(HomeUiState.Error(exception.message ?: "Could not load recently viewed"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = HomeUiState.Loading
        )

    // Function to trigger a reload of wishlist (e.g., after an item is removed)
    fun refreshWishlist() {
        viewModelScope.launch {
            if (userId == null) {
                // Handle not logged in, perhaps update state to error
                return@launch
            }
            // Emit Loading, then re-evaluate the flow
            val wishlistItems = wishlistRepository.getWishlist(userId)
            val packagesWithImages = mutableListOf<TravelPackageWithImages>()
            for (item in wishlistItems) {
                travelPackageRepository.getPackageWithImages(item.packageId)?.let {
                    packagesWithImages.add(it)
                }
            }
            // Manually emit success to update the stateFlow
            _wishlistPackagesMutableStateFlow.value = HomeUiState.Success(packagesWithImages)
        }
    }
    private val _wishlistPackagesMutableStateFlow = MutableStateFlow<HomeUiState>(HomeUiState.Loading)

    // Function to remove an item from wishlist and refresh
    fun removeFromWishlistByPackageId(packageId: String) {
        viewModelScope.launch {
            if (userId == null) {
                // Handle not logged in
                return@launch
            }
            try {
                val wishlistItem = wishlistRepository.getWishlistItemByPackageId(userId, packageId)
                wishlistItem?.let {
                    wishlistRepository.removeFromWishlist(userId, it.id)
                    // Trigger a refresh of the wishlist
                    refreshWishlist()
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
}