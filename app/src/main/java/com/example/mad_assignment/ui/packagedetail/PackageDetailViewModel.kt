package com.example.mad_assignment.ui.packagedetail

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mad_assignment.data.model.CartItem
import com.example.mad_assignment.data.model.DepartureAndEndTime
import com.example.mad_assignment.data.repository.CartRepository
import com.example.mad_assignment.data.repository.RecentlyViewedRepository
import com.example.mad_assignment.data.repository.TravelPackageRepository
import com.example.mad_assignment.data.repository.WishlistRepository
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PackageDetailViewModel @Inject constructor(
    private val packageRepository: TravelPackageRepository,
    // Note: paymentRepository is unused here and can be safely removed if not needed for future features.
    // private val paymentRepository: PaymentRepository,
    private val cartRepository: CartRepository,
    private val wishlistRepository: WishlistRepository,
    private val recentlyViewedRepository: RecentlyViewedRepository,
    private val auth: FirebaseAuth,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val packageId: String? = savedStateHandle.get<String>("packageId")

    private val _uiState = MutableStateFlow<PackageDetailUiState>(PackageDetailUiState.Loading)
    val uiState: StateFlow<PackageDetailUiState> = _uiState.asStateFlow()

    private val _isInWishlist = MutableStateFlow(false)
    val isInWishlist: StateFlow<Boolean> = _isInWishlist.asStateFlow()

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
                            packageDetailData.travelPackage.packageOption
                        }
                    }

                    val itineraryTrips = tripsDeferred.await()
                    val departures = departuresDeferred.await()

                    Log.d("PackageDetailVM", "Loaded ${departures.size} departure dates")

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

                val availableCapacity = departure.capacity - departure.numberOfPeopleBooked
                val currentTotalTravelers = currentState.paxCounts.values.sum()

                // ** CHANGED LOGIC **
                // If the user's current traveler count is more than the newly selected
                // date's capacity, we automatically reduce the count to the maximum available.
                // This is a much better user experience than just blocking the selection.
                if (currentTotalTravelers > availableCapacity) {
                    val adjustedPaxCounts = currentState.paxCounts.toMutableMap()
                    // Set adults to the max available spots and reset children to simplify.
                    adjustedPaxCounts["Adult"] = availableCapacity.coerceAtLeast(0)
                    adjustedPaxCounts["Child"] = 0

                    currentState.copy(
                        selectedDeparture = departure,
                        paxCounts = adjustedPaxCounts
                    )
                } else {
                    // If capacity is sufficient, just update the selected date.
                    currentState.copy(selectedDeparture = departure)
                }
            } else {
                currentState
            }
        }
    }

    fun updatePaxCount(category: String, change: Int) {
        _uiState.update { currentState ->
            if (currentState is PackageDetailUiState.Success) {
                // ** CHANGED LOGIC **
                // 1. Force the user to select a date FIRST. If no date is selected, do nothing.
                // This is the primary fix for the overbooking bug.
                val selectedDeparture = currentState.selectedDeparture ?: return@update currentState

                val currentPaxCounts = currentState.paxCounts.toMutableMap()
                val currentCount = currentPaxCounts[category] ?: 0
                val newCount = (currentCount + change).coerceAtLeast(0)

                // 2. Now that we know a date is selected, perform the capacity check.
                val potentialTotalTravelers = currentState.paxCounts.values.sum() - currentCount + newCount
                val availableCapacity = selectedDeparture.capacity - selectedDeparture.numberOfPeopleBooked

                if (availableCapacity < potentialTotalTravelers) {
                    Log.w("PackageDetailVM", "Cannot update pax count: insufficient capacity. Available: $availableCapacity, Requested: $potentialTotalTravelers")
                    return@update currentState // Capacity check failed, so we don't update the state.
                }

                // 3. If the capacity check passes, update the state.
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

            // ** CHANGED LOGIC **
            // Add a final "belt-and-suspenders" safety check. The UI logic should prevent
            // this from ever being hit, but it's good practice for data integrity.
            val availableCapacity = selectedDeparture.capacity - selectedDeparture.numberOfPeopleBooked
            if (availableCapacity < totalTravelers) {
                Log.e("AddToCart", "FATAL: Attempted to add to cart with insufficient capacity. This should have been caught by the UI logic.")
                // You could show a final error Toast to the user here.
                return@launch
            }

            // Create the CartItem object
            val cartItem = CartItem(
                packageId = packageId,
                departureId = selectedDeparture.id,
                basePrice = currentState.totalPrice,
                totalPrice = currentState.totalPrice,
                noOfAdults = currentState.paxCounts["Adult"] ?: 0,
                noOfChildren = currentState.paxCounts["Child"] ?: 0,
                totalTravelerCount = totalTravelers,
                startDate = selectedDeparture.startDate,
                endDate = selectedDeparture.endDate,
                durationDays = currentState.packageDetail.travelPackage.durationDays,
                addedAt = Timestamp.now(),
                updatedAt = Timestamp.now(),
                expiresAt = Timestamp(System.currentTimeMillis() / 1000 + 86400 * 7, 0),
                available = true
            )

            try {
                val userCart = cartRepository.getCartByUserId(userId).getOrNull()
                val result = cartRepository.addItemToCart(userId, userCart?.cartId, cartItem)
                if (result.isSuccess) {
                    Log.d("AddToCart", "Successfully added item to cart. New item ID: ${result.getOrNull()}")
                } else {
                    Log.e("AddToCart", "Failed to add item to cart", result.exceptionOrNull())
                }
            } catch (e: Exception) {
                Log.e("AddToCart", "An exception occurred while adding to cart", e)
            }
        }
    }

    private fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }
    private fun trackRecentlyViewed() {
        viewModelScope.launch {
            try {
                val userId = getCurrentUserId()
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