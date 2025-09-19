package com.example.mad_assignment.ui.cart

import com.example.mad_assignment.data.model.Cart
import com.example.mad_assignment.data.model.CartItem
import com.example.mad_assignment.data.model.TravelPackage

sealed interface CartUiState {
    object Loading : CartUiState
    data class Error(val message: String) : CartUiState
    data class Success(
        val cart: Cart? = null,
        val availableItems: List<CartItem> = emptyList(),
        val expiredItems: List<CartItem> = emptyList(),
        val selectedItemIds: Set<String> = emptySet(),
        val isEditingItem: Boolean = false,
        val editingItemId: String? = null,
        val showPackageDetails: Boolean = false,
        val selectedPackageId: String? = null,
        val packages: List<TravelPackage?> = emptyList()
    ) : CartUiState {
        val totalSelectedPrice: Double
            get() = availableItems
                .filter { it.cartItemId in selectedItemIds }
                .sumOf { it.totalPrice }

        val selectedItemsCount: Int
            get() = selectedItemIds.size

        val hasSelectedItems: Boolean
            get() = selectedItemIds.isNotEmpty()

        val allAvailableSelected: Boolean
            get() = availableItems.isNotEmpty() &&
                    availableItems.all { it.cartItemId in selectedItemIds }

        val editingItem: CartItem?
            get() = editingItemId?.let { id ->
                availableItems.find { it.cartItemId == id }
            }

        val selectedPackage: TravelPackage?
            get() = selectedPackageId?.let { id ->
                packages.find { it?.packageId == id }
            }
    }
    object Empty : CartUiState
}