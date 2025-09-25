package com.example.mad_assignment.ui.cart

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mad_assignment.data.model.CartItem
import com.example.mad_assignment.data.repository.CartRepository
import com.example.mad_assignment.data.repository.TravelPackageRepository
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CartViewModel @Inject constructor(
    private val cartRepository: CartRepository,
    private val travelPackageRepository: TravelPackageRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow<CartUiState>(CartUiState.Loading)
    val uiState: StateFlow<CartUiState> = _uiState.asStateFlow()

    private fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    init {
        loadCart()
    }

    private fun loadCart() {
        viewModelScope.launch {
            _uiState.value = CartUiState.Loading

            val currentUserId = getCurrentUserId()
            if (currentUserId == null) {
                _uiState.value = CartUiState.Empty
                return@launch
            }

            try {
                val cart = cartRepository.getCartByUserId(currentUserId).getOrNull()
                if (cart == null || cart.cartItemIds.isEmpty()) {
                    _uiState.value = CartUiState.Empty
                    return@launch
                }

                val cartItems = cartRepository.getCartItemsForCart(cart.cartItemIds).getOrDefault(emptyList())

                // ** CHANGED LOGIC: Fetch ALL packages first for validation **
                val allPackageIds = cartItems.map { it.packageId }.toSet()
                val packagesWithImages = allPackageIds.mapNotNull { travelPackageRepository.getPackageWithImages(it) }
                val packagesMap = packagesWithImages.associateBy { it.travelPackage.packageId }


                // ** CHANGED LOGIC: Categorize items with capacity validation **
                val currentTime = Timestamp.now()
                val availableItems = mutableListOf<CartItem>()
                val unavailableItems = mutableListOf<CartItem>()
                val expiredItems = mutableListOf<CartItem>()

                cartItems.forEach { item ->
                    // 1. Check for expiration first
                    if (item.expiresAt != null && item.expiresAt.seconds < currentTime.seconds) {
                        expiredItems.add(item)
                        return@forEach // continue to next item
                    }

                    val travelPackage = packagesMap[item.packageId]?.travelPackage
                    val departureOption = travelPackage?.packageOption?.find { it.id == item.departureId }

                    // 2. Check if package or departure still exists and has capacity
                    if (departureOption == null) {
                        unavailableItems.add(item) // Package or date was deleted/changed
                    } else {
                        val availableCapacity = departureOption.capacity - departureOption.numberOfPeopleBooked
                        if (item.totalTravelerCount > availableCapacity) {
                            unavailableItems.add(item) // Not enough spots
                        } else {
                            availableItems.add(item) // Everything is OK
                        }
                    }
                }

                // Set selected items to only the available ones
                val initialSelectedIds = availableItems.map { it.cartItemId }.toSet()

                _uiState.value = CartUiState.Success(
                    cart = cart,
                    availableItems = availableItems,
                    unavailableItems = unavailableItems, // Pass the new list
                    expiredItems = expiredItems,
                    selectedItemIds = initialSelectedIds,
                    packages = packagesWithImages
                )

            } catch (e: Exception) {
                _uiState.value = CartUiState.Error(e.message ?: "Failed to load cart")
            }
        }
    }

    fun toggleItemSelection(cartItemId: String) {
        _uiState.update { currentState ->
            if (currentState is CartUiState.Success) {
                val newSelectedIds = if (cartItemId in currentState.selectedItemIds) {
                    currentState.selectedItemIds - cartItemId
                } else {
                    currentState.selectedItemIds + cartItemId
                }
                currentState.copy(selectedItemIds = newSelectedIds)
            } else {
                currentState
            }
        }
    }

    fun toggleSelectAll() {
        _uiState.update { currentState ->
            if (currentState is CartUiState.Success) {
                val newSelectedIds = if (currentState.allAvailableSelected) {
                    emptySet()
                } else {
                    currentState.availableItems.map { it.cartItemId }.toSet()
                }
                currentState.copy(selectedItemIds = newSelectedIds)
            } else {
                currentState
            }
        }
    }

    fun removeItem(cartItemId: String) {
        val currentState = _uiState.value
        if (currentState !is CartUiState.Success || currentState.cart == null) return

        viewModelScope.launch {
            try {
                val result = cartRepository.removeItemFromCart(currentState.cart.cartId, cartItemId)
                if (result.isSuccess) {
                    refreshCart()
                } else {
                    _uiState.value = CartUiState.Error("Failed to remove item from cart")
                }
            } catch (e: Exception) {
                _uiState.value = CartUiState.Error(e.message ?: "Failed to remove item")
            }
        }
    }

    fun startEditingItem(cartItemId: String) {
        _uiState.update { currentState ->
            if (currentState is CartUiState.Success) {
                currentState.copy(
                    isEditingItem = true,
                    editingItemId = cartItemId
                )
            } else {
                currentState
            }
        }
    }

    fun stopEditingItem() {
        _uiState.update { currentState ->
            if (currentState is CartUiState.Success) {
                currentState.copy(
                    isEditingItem = false,
                    editingItemId = null
                )
            } else {
                currentState
            }
        }
    }

    fun updateCartItemDetails(cartItem: CartItem, newAdults: Int, newChildren: Int) {
        val currentState = _uiState.value
        if (currentState !is CartUiState.Success || currentState.cart == null) return

        val cartId = currentState.cart.cartId
        if (cartId.isEmpty()) {
            _uiState.value = CartUiState.Error("Invalid Cart ID")
            return
        }

        viewModelScope.launch {
            try {
                val travelPackage = travelPackageRepository.getTravelPackage(cartItem.packageId)
                if (travelPackage == null) {
                    _uiState.value = CartUiState.Error("Could not find package details.")
                    return@launch
                }

                val departureOption = travelPackage.packageOption.find { it.id == cartItem.departureId }
                if (departureOption == null) {
                    _uiState.value = CartUiState.Error("This departure date is no longer available.")
                    refreshCart()
                    return@launch
                }

                val newTotalTravelers = newAdults + newChildren
                val availableCapacity = departureOption.capacity - departureOption.numberOfPeopleBooked

                if (newTotalTravelers > availableCapacity) {
                    val errorMessage = "Cannot update. Only $availableCapacity spots left for this date."
                    Log.e("CartViewModel", errorMessage)
                    _uiState.value = CartUiState.Error(errorMessage)
                    stopEditingItem()
                    return@launch
                }

                val pricingMap = travelPackage.pricing
                val adultPrice = pricingMap["Adult"] ?: 0.0
                val childPrice = pricingMap["Child"] ?: 0.0
                val newTotalPrice = (adultPrice * newAdults) + (childPrice * newChildren)

                val updatedCartItem = cartItem.copy(
                    noOfAdults = newAdults,
                    noOfChildren = newChildren,
                    totalTravelerCount = newTotalTravelers,
                    totalPrice = newTotalPrice,
                    updatedAt = Timestamp.now()
                )

                val result = cartRepository.updateCartItemInCart(cartId, updatedCartItem)
                if (result.isSuccess) {
                    stopEditingItem()
                    refreshCart()
                } else {
                    _uiState.value = CartUiState.Error("Failed to update cart item: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                _uiState.value = CartUiState.Error(e.message ?: "Failed to update item")
            }
        }
    }


    fun showPackageDetails(packageId: String) {
        _uiState.update { currentState ->
            if (currentState is CartUiState.Success) {
                currentState.copy(
                    showPackageDetails = true,
                    selectedPackageId = packageId
                )
            } else {
                currentState
            }
        }
    }

    fun hidePackageDetails() {
        _uiState.update { currentState ->
            if (currentState is CartUiState.Success) {
                currentState.copy(
                    showPackageDetails = false,
                    selectedPackageId = null
                )
            } else {
                currentState
            }
        }
    }

    fun proceedToCheckout() {
        val currentState = _uiState.value
        if (currentState !is CartUiState.Success || !currentState.hasSelectedItems) return

        viewModelScope.launch {
            try {
                if (currentState.cart != null) {
                    cartRepository.updateCart(currentState.cart.cartId)
                }
            } catch (e: Exception) {
                _uiState.value = CartUiState.Error(e.message ?: "Failed to proceed to checkout")
            }
        }
    }

    fun refreshCart() {
        loadCart()
    }
}