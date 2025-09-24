package com.example.mad_assignment.data.repository

import android.util.Log
import com.example.mad_assignment.data.datasource.CartDataSource
import com.example.mad_assignment.data.model.Cart
import com.example.mad_assignment.data.model.CartItem
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CartRepository @Inject constructor(
    private val cartDataSource: CartDataSource
) {
    companion object {
        private const val TAG = "CartRepository"
    }

    suspend fun createCart(newCart: Cart): Result<String> {
        return cartDataSource.createCart(newCart)
            .onFailure { Log.e(TAG, "createCart failed", it) }
    }

    suspend fun deleteCart(cartId: String): Result<Unit> {
        return cartDataSource.deleteCart(cartId)
            .onFailure { Log.e(TAG, "deleteCart failed", it) }
    }

    suspend fun getCartById(cartId: String): Result<Cart?> {
        return cartDataSource.getCartById(cartId)
            .onFailure { Log.e(TAG, "getCartById failed", it) }
    }

    suspend fun getCartItemById(cartItemId: String): Result<CartItem?> {
        return cartDataSource.getCartItemById(cartItemId)
            .onFailure { Log.e(TAG, "getCartItemById failed", it) }
    }

    suspend fun getCartByUserId(userId: String): Result<Cart?> {
        return cartDataSource.getCartByUserId(userId)
            .onFailure { Log.e(TAG, "getCartByUserId failed", it) }
    }

    suspend fun getCartItemsForCart(cartItemIds: List<String>): Result<List<CartItem>> {
        return cartDataSource.getCartItemsForCart(cartItemIds)
            .onFailure { Log.e(TAG, "getCartItemsForCart failed", it) }
    }

    suspend fun addItemToCart(userId: String, cartId: String?, newCartItem: CartItem): Result<String> {
        return cartDataSource.addItemToCart(userId, cartId, newCartItem)
            .onFailure { Log.e(TAG, "addItemToCart failed for userId=$userId", it) }
    }

    suspend fun removeItemFromCart(cartId: String, cartItemIdToRemove: String): Result<Unit> {
        return cartDataSource.removeItemFromCart(cartId, cartItemIdToRemove)
            .onFailure { Log.e(TAG, "removeItemFromCart failed for cartId=$cartId", it) }
    }

    suspend fun updateCart(cartId: String): Result<Unit> {
        return cartDataSource.updateCart(cartId)
            .onFailure { Log.e(TAG, "updateCart failed", it) }
    }

    suspend fun updateCartItemInCart(cartId: String, updatedCartItem: CartItem): Result<Unit> {
        return cartDataSource.updateCartItemInCart(cartId, updatedCartItem)
            .onFailure { Log.e(TAG, "updateCartItemInCart failed for cartId=$cartId", it) }
    }

    suspend fun clearCart(cartId: String, cartItemIds: List<String>): Result<Unit> {
        return cartDataSource.clearCart(cartId, cartItemIds)
            .onFailure { Log.e(TAG, "clearCart failed for cartId=$cartId", it) }
    }
}