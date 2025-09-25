package com.example.mad_assignment.ui.packagedetail

import android.util.Log
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mad_assignment.data.model.CartItem
import com.example.mad_assignment.data.model.DepartureAndEndTime
import com.example.mad_assignment.data.repository.CartRepository
import com.example.mad_assignment.data.repository.PaymentRepository
import com.example.mad_assignment.data.repository.TravelPackageRepository
import com.example.mad_assignment.data.repository.RecentlyViewedRepository
import com.example.mad_assignment.data.repository.WishlistRepository
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PackageDetailViewModel @Inject constructor(
    private val packageRepository: TravelPackageRepository,
    private val paymentRepository: PaymentRepository,
    private val cartRepository: CartRepository,
    private val wishlistRepository: WishlistRepository,
    private val recentlyViewedRepository: RecentlyViewedRepository,
    private val auth: FirebaseAuth,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val packageId: String? = savedStateHandle.get<String>("packageId")

    private val _uiState = MutableStateFlow<PackageDetailUiState>(PackageDetailUiState.Loading)
    val uiState: StateFlow<PackageDetailUiState> = _uiState.asStateFlow()

    init {
        Log.d("PackageDetailVM", "Initializing with packageId: $packageId")
        if (packageId != null) {
            loadPackageAndRelatedData()
            trackRecentlyViewed()
            checkWishlistStatus()
        } else {
            Log.e("PackageDetailVM", "Package ID is null")
            _uiState.value = PackageDetailUiState.Error("Package ID not provided")
        }
    }

    private fun loadPackageAndRelatedData() {
        viewModelScope.launch {
            _uiState.value = PackageDetailUiState.Loading

            try {
                Log.d("PackageDetailVM", "Loading package details for ID: $packageId")

                val packageDetailData = packageRepository.getPackageWithImages(packageId!!)
                if (packageDetailData == null) {
                    Log.e("PackageDetailVM", "Package not found for ID: $packageId")
                    _uiState.value = PackageDetailUiState.Error("Package not found.")
                    return@launch
                }

                Log.d("PackageDetailVM", "Package loaded: ${packageDetailData.travelPackage.packageName}")

                coroutineScope {
                    val tripsDeferred = async {
                        try {
                            Log.d("PackageDetailVM", "Loading trips for package")
                            packageRepository.resolveTripsForPackage(packageDetailData.travelPackage)
                        } catch (e: Exception) {
                            Log.e("PackageDetailVM", "Error loading trips", e)
                            emptyMap()
                        }
                    }

                    val departuresDeferred = async {
                        try {
                            Log.d("PackageDetailVM", "Loading departure dates for package")
                            packageRepository.getDepartureDates(packageId)
                        } catch (e: Exception) {
                            Log.e("PackageDetailVM", "Error loading departures", e)
                            // Return the package options from the travel package itself as fallback
                            packageDetailData.travelPackage.packageOption
                        }
                    }

                    val itineraryTrips = tripsDeferred.await()
                    val departures = departuresDeferred.await()

                    Log.d("PackageDetailVM", "Loaded ${departures.size} departure dates")

                    // Initialize pax counts with pricing categories set to 0
                    val initialPaxCounts = packageDetailData.travelPackage.pricing.keys.associateWith { 0 }

                    _uiState.value = PackageDetailUiState.Success(
                        packageDetail = packageDetailData,
                        itineraryTrips = itineraryTrips,
                        departures = departures,
                        paxCounts = initialPaxCounts
                    )
                }
            } catch (e: Exception) {
                Log.e("PackageDetailVM", "Error loading package details", e)
                _uiState.value = PackageDetailUiState.Error(e.message ?: "An unknown error occurred")
            }
        }
    }

    fun selectDeparture(departure: DepartureAndEndTime) {
        _uiState.update { currentState ->
            if (currentState is PackageDetailUiState.Success) {
                Log.d("PackageDetailVM", "Selected departure: ${departure.id}")
                currentState.copy(selectedDeparture = departure)
            } else {
                currentState
            }
        }
    }

    fun updatePaxCount(category: String, change: Int) {
        _uiState.update { currentState ->
            if (currentState is PackageDetailUiState.Success) {
                val currentPaxCounts = currentState.paxCounts.toMutableMap()
                val currentCount = currentPaxCounts[category] ?: 0
                val newCount = (currentCount + change).coerceAtLeast(0)


                currentPaxCounts[category] = newCount
                Log.d("PackageDetailVM", "Updated pax count for $category: $newCount")

                currentState.copy(paxCounts = currentPaxCounts)
            } else {
                currentState
            }
        }
    }

    fun addToCart() {
        viewModelScope.launch {
            val currentState = _uiState.value
            if (currentState !is PackageDetailUiState.Success) {
                Log.e("AddToCart", "UI state is not Success, cannot add to cart.")
                return@launch
            }

            // Safety checks
            val userId = getCurrentUserId()
            if (userId == null) {
                Log.e("AddToCart", "User is not logged in.")
                // Here you might want to emit an event to the UI to show a login prompt
                return@launch
            }
            if (packageId == null) {
                Log.e("AddToCart", "Package ID is null.")
                return@launch
            }
            val selectedDeparture = currentState.selectedDeparture
            if (selectedDeparture == null) {
                Log.e("AddToCart", "No departure date selected.")
                return@launch
            }
            val totalTravelers = currentState.paxCounts.values.sum()
            if (totalTravelers == 0) {
                Log.e("AddToCart", "No travelers selected.")
                return@launch
            }

            // Create the CartItem object
            val cartItem = CartItem(
                packageId = packageId,
                departureId = selectedDeparture.id,
                basePrice = currentState.totalPrice, // Base price is the calculated total for this booking
                totalPrice = currentState.totalPrice,
                noOfAdults = currentState.paxCounts["Adult"] ?: 0,
                noOfChildren = currentState.paxCounts["Child"] ?: 0,
                totalTravelerCount = totalTravelers,
                startDate = selectedDeparture.startDate,
                endDate = selectedDeparture.endDate,
                durationDays = currentState.packageDetail.travelPackage.durationDays,
                addedAt = Timestamp.now(),
                updatedAt = Timestamp.now(),
                // Set an expiry time, e.g., 7 days from now
                expiresAt = Timestamp(System.currentTimeMillis() / 1000 + 86400 * 7, 0),
                available = true
            )

            try {
                // Get the user's existing cart or null if it doesn't exist
                val userCart = cartRepository.getCartByUserId(userId).getOrNull()

                // Add item to cart (this will create a new cart if one doesn't exist)
                val result = cartRepository.addItemToCart(userId, userCart?.cartId, cartItem)
                if (result.isSuccess) {
                    Log.d("AddToCart", "Successfully added item to cart. New item ID: ${result.getOrNull()}")
                    // Optionally, you can emit a success event to the UI here
                } else {
                    Log.e("AddToCart", "Failed to add item to cart", result.exceptionOrNull())
                }
            } catch (e: Exception) {
                Log.e("AddToCart", "An exception occurred while adding to cart", e)
            }
        }
    }


    private val _isInWishlist = MutableStateFlow(false)
    val isInWishlist: StateFlow<Boolean> = _isInWishlist.asStateFlow()

    private fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }
    private fun trackRecentlyViewed() {
        viewModelScope.launch {
            try {
                val userId = getCurrentUserId()
                Log.d("DEBUG", "UserID: $userId, PackageID: $packageId")

                if (userId == null) {
                    Log.e("DEBUG", "USER ID IS NULL - THIS IS CREATING NULL DOCUMENT!")
                    return@launch
                }
                packageId?.let {
                    recentlyViewedRepository.addToRecentlyViewed(userId, it)
                    Log.d("PackageDetailVM", "Added to recently viewed: $it")
                }
            } catch (e: Exception) {
                Log.e("PackageDetailVM", "Error adding to recently viewed", e)
            }
        }
    }

    private fun checkWishlistStatus() {
        viewModelScope.launch {
            try {
                val currentUserId = getCurrentUserId()
                packageId?.let {
                    val inWishlist = wishlistRepository.isInWishlist(currentUserId, it)
                    _isInWishlist.value = inWishlist
                    Log.d("PackageDetailVM", "Wishlist status for $it: $inWishlist")
                }
            } catch (e: Exception) {
                Log.e("PackageDetailVM", "Error checking wishlist status", e)
            }
        }
    }

    fun toggleFavButton() {
        viewModelScope.launch {
            try {
                val currentUserId = getCurrentUserId()
                packageId?.let {
                    val isCurrentlyInWishlist = _isInWishlist.value

                    if (isCurrentlyInWishlist) {
                        val wishlistItem = wishlistRepository.getWishlistItemByPackageId(currentUserId, it)
                        wishlistItem?.let { item ->
                            wishlistRepository.removeFromWishlist(currentUserId, item.id)
                            Log.d("PackageDetailVM", "Removed from wishlist: $it")
                            _isInWishlist.value = false
                        }
                    } else {
                        // Add to wishlist
                        wishlistRepository.addToWishlist(currentUserId, it)
                        Log.d("PackageDetailVM", "Added to wishlist: $it")
                        _isInWishlist.value = true
                    }
                }
            } catch (e: Exception) {
                Log.e("PackageDetailVM", "Error toggling wishlist", e)
            }
        }
    }
}