package com.example.mad_assignment.ui.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mad_assignment.data.model.Cart
import com.example.mad_assignment.data.model.CartItem
import com.example.mad_assignment.data.model.TravelPackage
import com.example.mad_assignment.data.repository.CartRepository
import com.example.mad_assignment.data.repository.TravelPackageRepository
import com.example.mad_assignment.ui.packagedetail.PackageDetailData
import com.google.firebase.Timestamp
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
    private val travelPackageRepository: TravelPackageRepository
) : ViewModel() {

    // Placeholder Value
    /*
    val cartItemA = CartItem(
        cartItemId = "itemA",
        packageId = "Xz1AZkbiOY7lR1W3xGDU",
        basePrice = 3000.00,
        totalPrice = 3000.00,
        noOfAdults = 2,
        noOfChildren = 1,
        totalTravelerCount = 3,
        durationDays = 5,
        addedAt = Timestamp.now(),
        updatedAt = Timestamp.now(),
        expiresAt = Timestamp(System.currentTimeMillis() / 1000 + 86400*7, 0), // Expires in 1 hour
        isAvailable = true
    )
    val cartItemB = CartItem(
        cartItemId = "itemB",
        packageId = "Xz1AZkbiOY7lR1W3xGDU",
        basePrice = 4500.00,
        totalPrice = 4500.00,
        noOfAdults = 1,
        noOfChildren = 0,
        totalTravelerCount = 1,
        durationDays = 7,
        addedAt = Timestamp.now(),
        updatedAt = Timestamp.now(),
        expiresAt = Timestamp(System.currentTimeMillis() / 1000 + 86400*9, 0), // Expires in 2 hours
        isAvailable = true
    )

    val expiredCartItem = CartItem(
        cartItemId = "itemC",
        packageId = "Xz1AZkbiOY7lR1W3xGDU",
        basePrice = 1500.00,
        totalPrice = 1800.00,
        noOfAdults = 1,
        noOfChildren = 1,
        totalTravelerCount = 2,
        durationDays = 5,
        addedAt = Timestamp.now(),
        updatedAt = Timestamp.now(),
        expiresAt = Timestamp(System.currentTimeMillis() / 1000 - 86400, 0),
        isAvailable = false
    )

    val cartUser = Cart(
        cartId = "cart12345",
        userId = "user123",
        cartItemIds = listOf(cartItemA.cartItemId, cartItemB.cartItemId),
        totalAmount = 7500.00,
        finalAmount = 7500.00,
        createdAt = Timestamp.now(),
        updatedAt = Timestamp.now(),
        isValid = true
    )

    val cartUserNull = Cart()
    */
    // Placeholder Value

    private val _uiState = MutableStateFlow<CartUiState>(CartUiState.Loading)
    val uiState: StateFlow<CartUiState> = _uiState.asStateFlow()

    private val currentUserId = "SgxyJlBfRpXK6U5bvWLoguwEHlB2" // TODO: Get from user account module

    init {
        loadCart()
    }

    private fun loadCart() {
        viewModelScope.launch {
            _uiState.value = CartUiState.Loading

            try {
                // get user cart
                val cartResult = cartRepository.getCartByUserId(currentUserId)
                val cart = cartResult.getOrNull()

                if (cart == null || cart.cartItemIds.isEmpty()) {
                    _uiState.value = CartUiState.Empty
                    return@launch
                }

                // get all cart items in user cart
                val cartItemsResult = cartRepository.getCartItemsForCart(cartResult.getOrNull()?.cartItemIds
                    ?: emptyList())
                val cartItems = cartItemsResult.getOrDefault(emptyList())

                // separate cart items
                val currentTime = Timestamp.now()
                val availableItems = mutableListOf<CartItem>()
                val expiredItems = mutableListOf<CartItem>()

                cartItems.forEach { item ->
                    if (item.isAvailable && (item.expiresAt == null || item.expiresAt.seconds > currentTime.seconds)) {
                        availableItems.add(item)
                    } else {
                        expiredItems.add(item)
                    }
                }

                val packageIds = availableItems.map { it.packageId }.toSet()

                // load packages in cart
                val packages = mutableListOf<PackageDetailData?>()
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

                val result = cartRepository.updateCartItemInCart(cartItem.cartItemId, updatedCartItem)
                if (result.isSuccess) {
                    _uiState.update { state ->
                        if (state is CartUiState.Success) {
                            val updatedAvailableItems = state.availableItems.map { item ->
                                if (item.cartItemId == cartItem.cartItemId) {
                                    updatedCartItem
                                } else {
                                    item
                                }
                            }
                            state.copy(
                                availableItems = updatedAvailableItems,
                                isEditingItem = false,
                                editingItemId = null
                            )
                        } else {
                            state
                        }
                    }
                } else {
                    _uiState.value = CartUiState.Error("Failed to update cart item")
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

    fun updateCartItem(cartId: String, updatedCartItem: CartItem) {
        val currentState = _uiState.value
        if (currentState !is CartUiState.Success || currentState.cart == null) return

        viewModelScope.launch {
            try {
                val result = Result.success(Unit) // TODO: Update cart item - cartRepository.updateCartItemInCart(cartId, updatedCartItem)
                if (result.isSuccess) {
                    _uiState.update { state ->
                        if (state is CartUiState.Success) {
                            val updatedAvailableItems = state.availableItems.map { item ->
                                if (item.cartItemId == updatedCartItem.cartItemId) {
                                    updatedCartItem
                                } else {
                                    item
                                }
                            }
                            state.copy(availableItems = updatedAvailableItems)
                        } else {
                            state
                        }
                    }
                } else {
                    _uiState.value = CartUiState.Error("Failed to update cart item")
                }
            } catch (e: Exception) {
                _uiState.value = CartUiState.Error(e.message ?: "Failed to update item")
            }
        }
    }

    fun proceedToCheckout() {
        val currentState = _uiState.value
        if (currentState !is CartUiState.Success || !currentState.hasSelectedItems) return

        viewModelScope.launch {
            try {
                if (currentState.cart != null) {
                    // TODO: Update cart - cartRepository.updateCart(currentState.cart.cartId)
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