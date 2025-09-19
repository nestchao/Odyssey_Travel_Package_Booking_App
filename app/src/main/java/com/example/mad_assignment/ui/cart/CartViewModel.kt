package com.example.mad_assignment.ui.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mad_assignment.data.model.Cart
import com.example.mad_assignment.data.model.CartItem
import com.example.mad_assignment.data.model.DepartureDate
import com.example.mad_assignment.data.model.TravelPackage
import com.example.mad_assignment.data.repository.CartRepository
import com.example.mad_assignment.data.repository.TravelPackageRepository
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
    val cartItemA = CartItem(
        cartItemId = "itemA",
        packageId = "Xz1AZkbiOY7lR1W3xGDU",
        departureDate = DepartureDate(
            id = "departureDate1",
            packageId = "Xz1AZkbiOY7lR1W3xGDU",
            startDate = Timestamp(System.currentTimeMillis() / 1000 + 86400, 0),
            capacity = 10
        ),
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
        departureDate = DepartureDate(
            id = "departureDate2",
            packageId = "Xz1AZkbiOY7lR1W3xGDU",
            startDate = Timestamp(System.currentTimeMillis() / 1000 + 86400*2, 0),
            capacity = 10
        ),
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
        departureDate = DepartureDate(
            id = "departureDate3",
            packageId = "Xz1AZkbiOY7lR1W3xGDU",
            startDate = Timestamp(System.currentTimeMillis() / 1000, 0),
            capacity = 10
        ),
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
    // Placeholder Value

    private val _uiState = MutableStateFlow<CartUiState>(CartUiState.Loading)
    val uiState: StateFlow<CartUiState> = _uiState.asStateFlow()

    private val currentUserId = "user123" // TODO: Get from user account module

    init {
        loadCart()
    }

    private fun loadCart() {
        viewModelScope.launch {
            _uiState.value = CartUiState.Loading

            try {
                // get user cart
                val cartResult = Result.success(cartUser) // TODO: Load user cart - cartRepository.getCartByUserId(currentUserId)
                val cart = cartResult.getOrNull()

                if (cart == null || cart.cartItemIds.isEmpty()) {
                    _uiState.value = CartUiState.Empty
                    return@launch
                }

                // get all cart items in user cart
                val cartItemsResult = Result.success(listOf(cartItemA, cartItemB, expiredCartItem)) // TODO: Get cart items from CartRepository
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
                val packages = mutableListOf<TravelPackage?>()
                packageIds.forEach { id ->
                    val packageResult = travelPackageRepository.getTravelPackage(id)
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
                val result = Result.success(Unit) // TODO: Remove item from cart - cartRepository.removeItemFromCart(cartItemId)
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

    fun updateCartItemDetails(cartItemId: String, newAdults: Int, newChildren: Int) {
        val currentState = _uiState.value
        if (currentState !is CartUiState.Success || currentState.cart == null) return

        viewModelScope.launch {
            try {
                // TODO: Get package pricing details from TravelPackageRepository
                // val packageDetails = travelPackageRepository.getTravelPackage(packageId)
                // val pricingMap = packageDetails.pricingMap // Map<String, Double> for adult/child pricing

                val currentItem = currentState.availableItems.find { it.cartItemId == cartItemId }
                if (currentItem == null) {
                    _uiState.value = CartUiState.Error("Cart item not found")
                    return@launch
                }

                // TODO: Calculate new pricing based on package pricing map
                // val adultPrice = pricingMap["adult"] ?: 0.0
                // val childPrice = pricingMap["child"] ?: 0.0
                // val newTotalPrice = (adultPrice * newAdults) + (childPrice * newChildren)

                // Placeholder price calculation
                val pricePerTraveler = currentItem.totalPrice / currentItem.totalTravelerCount
                val newTotalPrice = pricePerTraveler * (newAdults + newChildren)

                val updatedCartItem = currentItem.copy(
                    noOfAdults = newAdults,
                    noOfChildren = newChildren,
                    totalTravelerCount = newAdults + newChildren,
                    totalPrice = newTotalPrice,
                    updatedAt = Timestamp.now()
                )

                val result = Result.success(Unit) // TODO: Update cart item - cartRepository.updateCartItemInCart(cartItemId, updatedCartItem)
                if (result.isSuccess) {
                    _uiState.update { state ->
                        if (state is CartUiState.Success) {
                            val updatedAvailableItems = state.availableItems.map { item ->
                                if (item.cartItemId == cartItemId) {
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