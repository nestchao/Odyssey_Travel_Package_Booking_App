package com.example.mad_assignment.ui.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mad_assignment.data.model.CartItem
import com.example.mad_assignment.data.model.TravelPackageWithImages
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
                // get user cart
                val cartResult = cartRepository.getCartByUserId(currentUserId)
                val cart = cartResult.getOrNull()

                if (cart == null || cart.cartItemIds.isEmpty()) {
                    _uiState.value = CartUiState.Empty
                    return@launch
                }

                // get all cart items in user cart
                val cartItemsResult = cartRepository.getCartItemsForCart(cart.cartItemIds)
                val cartItems = cartItemsResult.getOrDefault(emptyList())

                // separate cart items
                val currentTime = Timestamp.now()
                val availableItems = mutableListOf<CartItem>()
                val expiredItems = mutableListOf<CartItem>()

                cartItems.forEach { item ->
                    if (item.available && (item.expiresAt == null || item.expiresAt.seconds > currentTime.seconds)) {
                        availableItems.add(item)
                    } else {
                        expiredItems.add(item)
                    }
                }

                val packageIds = availableItems.map { it.packageId }.toSet()

                // load packages in cart
                val packages = mutableListOf<TravelPackageWithImages?>()
                packageIds.forEach { id ->
                    val packageResult = travelPackageRepository.getPackageWithImages(id)
                    packages.add(packageResult)
                }

                // set selected items to all available items
                val initialSelectedIds = availableItems.map { it.cartItemId }.toSet()

                _uiState.value = CartUiState.Success(
                    cart = cart,
                    availableItems = availableItems,
                    expiredItems = expiredItems,
                    selectedItemIds = initialSelectedIds,
                    packages = packages
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
                val packageDetails = travelPackageRepository.getTravelPackage(cartItem.packageId)
                val pricingMap = packageDetails?.pricing

                val currentItem = currentState.availableItems.find { it.cartItemId == cartItem.cartItemId }
                if (currentItem == null) {
                    _uiState.value = CartUiState.Error("Cart item not found")
                    return@launch
                }

                val adultPrice = pricingMap?.get("Adult") ?: 0.0
                val childPrice = pricingMap?.get("Child") ?: 0.0
                val newTotalPrice = (adultPrice * newAdults) + (childPrice * newChildren)

                val updatedCartItem = currentItem.copy(
                    noOfAdults = newAdults,
                    noOfChildren = newChildren,
                    totalTravelerCount = newAdults + newChildren,
                    totalPrice = newTotalPrice,
                    updatedAt = Timestamp.now()
                )

                // CORRECTION: Correct method call with cartId.
                val result = cartRepository.updateCartItemInCart(cartId, updatedCartItem)
                if (result.isSuccess) {
                    stopEditingItem()
                    // Refresh cart to ensure all totals are recalculated and UI is consistent.
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
                    // Re-validates the cart totals before proceeding
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