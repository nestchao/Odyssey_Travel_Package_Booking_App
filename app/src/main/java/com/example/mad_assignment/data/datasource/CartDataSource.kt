package com.example.mad_assignment.data.datasource

import android.util.Log
import com.example.mad_assignment.data.model.Cart
import com.example.mad_assignment.data.model.CartItem
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObjects
import com.google.firebase.Timestamp
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CartDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    companion object {
        private const val CARTS_COLLECTION = "carts"
        private const val CART_ITEMS_COLLECTION = "cartItems"
        private const val TAG = "CartDataSource"
    }

    suspend fun createCart(newCart: Cart): Result<String> {
        return try {
            val documentRef = firestore.collection(CARTS_COLLECTION).add(newCart).await()
            Result.success(documentRef.id)
        } catch (e: Exception) {
            Log.e(TAG, "createCart failed", e)
            Result.failure(RuntimeException("Failed to create new cart", e))
        }
    }

    // delete cart with the cart items inside of it
    suspend fun deleteCart(cartId: String): Result<Unit> {
        return try {
            firestore.runTransaction { transaction ->
                val cartRef = firestore.collection(CARTS_COLLECTION).document(cartId)
                val cartSnapshot = transaction.get(cartRef)
                val cart = cartSnapshot.toObject(Cart::class.java)
                    ?: throw Exception("Cart not found.")

                for (itemId in cart.cartItemIds) {
                    val itemRef = firestore.collection(CART_ITEMS_COLLECTION).document(itemId)
                    transaction.delete(itemRef)
                }

                transaction.delete(cartRef)
            }.await().let { Result.success(Unit) }
        } catch (e: Exception) {
            Log.e(TAG, "deleteCart failed", e)
            Result.failure(RuntimeException("Failed to delete cart and its items", e))
        }
    }

    suspend fun getCartById(cartId: String): Result<Cart?> {
        return try {
            val document = firestore.collection(CARTS_COLLECTION).document(cartId).get().await()
            val cart = document.toObject(Cart::class.java)
            Result.success(cart)
        } catch (e: Exception) {
            Log.e(TAG, "getCartById failed", e)
            Result.failure(RuntimeException("Failed to get cart by cart id", e))
        }
    }

    suspend fun getCartItemById(cartItemId: String): Result<CartItem?> {
        return try {
            val document = firestore.collection(CART_ITEMS_COLLECTION).document(cartItemId).get().await()
            val cartItem = document.toObject(CartItem::class.java)
            Result.success(cartItem)
        } catch (e: Exception) {
            Log.e(TAG, "getCartItemById failed", e)
            Result.failure(RuntimeException("Failed to get cart item by card id", e))
        }
    }

    suspend fun getCartByUserId(userId: String): Result<Cart?> {
        return try {
            val snapshot = firestore.collection(CARTS_COLLECTION)
                .whereEqualTo("userId", userId)
                .get()
                .await()
            val cart = snapshot.toObjects(Cart::class.java).firstOrNull()
            Result.success(cart)
        } catch (e: Exception) {
            Log.e(TAG, "getCartByUserId failed", e)
            Result.failure(RuntimeException("Failed to get cart by user id", e))
        }
    }

    suspend fun getCartItemsForCart(cartItemIds: List<String>): Result<List<CartItem>> {
        if (cartItemIds.isEmpty()) return Result.success(emptyList())
        return try {
            val snapshot = firestore.collection(CART_ITEMS_COLLECTION)
                .whereIn(FieldPath.documentId(), cartItemIds)
                .get()
                .await()
            Result.success(snapshot.toObjects())
        } catch (e: Exception) {
            Log.e(TAG, "getCartItemsForCart failed", e)
            Result.failure(RuntimeException("Failed to fetch cart items", e))
        }
    }

    suspend fun addItemToCart(userId: String, cartId: String?, newCartItem: CartItem): Result<String> {
        return try {
            firestore.runTransaction { transaction ->
                val finalCartId = cartId ?: firestore.collection(CARTS_COLLECTION).document().id
                val cartRef = firestore.collection(CARTS_COLLECTION).document(finalCartId)

                val existingCart: Cart? = if (cartId != null) {
                    val cartSnapshot = transaction.get(cartRef)
                    cartSnapshot.toObject(Cart::class.java)
                } else {
                    null // new cart
                }

                val newCartItemRef = firestore.collection(CART_ITEMS_COLLECTION).document()
                val updatedCartItem = newCartItem.copy(cartItemId = newCartItemRef.id, updatedAt = Timestamp.now())
                transaction.set(newCartItemRef, updatedCartItem)

                val updatedItemIds = existingCart?.cartItemIds?.plus(newCartItemRef.id)
                    ?: listOf(newCartItemRef.id)
                val updatedTotalAmount = (existingCart?.totalAmount ?: 0.0) + updatedCartItem.totalPrice

                val cartData = Cart(
                    cartId = finalCartId,
                    userId = userId,
                    cartItemIds = updatedItemIds,
                    totalAmount = updatedTotalAmount,
                    finalAmount = updatedTotalAmount,
                    createdAt = existingCart?.createdAt ?: Timestamp.now(),
                    updatedAt = Timestamp.now(),
                    isValid = false // need to check cart valid or not again
                )
                transaction.set(cartRef, cartData)

                newCartItemRef.id
            }.await().let { newItemId -> Result.success(newItemId) }
        } catch (e: Exception) {
            Log.e(TAG, "addItemToCart failed for userId: $userId, cartId: $cartId", e)
            Result.failure(RuntimeException("Failed to add item to cart", e))
        }
    }

    suspend fun removeItemFromCart(cartId: String, cartItemIdToRemove: String): Result<Unit> {
        return try {
            firestore.runTransaction { transaction ->
                val cartRef = firestore.collection(CARTS_COLLECTION).document(cartId)
                val cartItemRef = firestore.collection(CART_ITEMS_COLLECTION).document(cartItemIdToRemove)

                val cartSnapshot = transaction.get(cartRef)
                val cart = cartSnapshot.toObject(Cart::class.java)
                val cartItemSnapshot = transaction.get(cartItemRef)
                val cartItem = cartItemSnapshot.toObject(CartItem::class.java)

                if (cart == null) {
                    throw Exception("Cart: $cartId not found.")
                }

                if (cartItem == null) {
                    throw Exception("CartItem: $cartItemIdToRemove not found.")
                }

                val updatedItemIds = cart.cartItemIds.filter { it != cartItemIdToRemove }
                val updatedTotalAmount = cart.totalAmount - cartItem.totalPrice

                transaction.update(
                    cartRef,
                    "cartItemIds", updatedItemIds,
                    "totalAmount", updatedTotalAmount,
                    "finalAmount", updatedTotalAmount,
                    "updatedAt", Timestamp.now(),
                    "isValid", false
                )
                transaction.delete(cartItemRef)
            }.await().let { Result.success(Unit) }
        } catch (e: Exception) {
            Log.e(TAG, "removeItemFromCart failed", e)
            Result.failure(RuntimeException("Failed to remove item from cart", e))
        }
    }

    // always update to validate cart
    suspend fun updateCart(cartId: String): Result<Unit> {
        return try {
            val cartRef = firestore.collection(CARTS_COLLECTION).document(cartId)
            val cartSnapshot = cartRef.get().await()
            val cart = cartSnapshot.toObject(Cart::class.java)
                ?: throw Exception("Cart not found.")

            if (cart.cartItemIds.isEmpty()) {
                firestore.runTransaction { transaction ->
                    transaction.update(
                        cartRef,
                        "totalAmount", 0.0,
                        "finalAmount", 0.0,
                        "updatedAt", Timestamp.now(),
                        "isValid", false
                    )
                }.await()
                return Result.success(Unit)
            }

            val cartItemsSnapshot = firestore.collection(CART_ITEMS_COLLECTION)
                .whereIn(FieldPath.documentId(), cart.cartItemIds)
                .get()
                .await()

            val newTotalAmount = cartItemsSnapshot
                .toObjects<CartItem>()
                .sumOf { it.totalPrice }

            firestore.runTransaction { transaction ->
                transaction.update(
                    cartRef,
                    "totalAmount", newTotalAmount,
                    "finalAmount", newTotalAmount,
                    "updatedAt", Timestamp.now(),
                    "isValid", true
                )
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "updateCart failed", e)
            Result.failure(RuntimeException("Failed to update cart totals", e))
        }
    }


    suspend fun updateCartItemInCart(cartId: String, updatedCartItem: CartItem): Result<Unit> {
        return try {
            firestore.runTransaction { transaction ->
                val cartRef = firestore.collection(CARTS_COLLECTION).document(cartId)
                val cartItemRef = firestore.collection(CART_ITEMS_COLLECTION).document(updatedCartItem.cartItemId)

                val cartSnapshot = transaction.get(cartRef)
                val cart = cartSnapshot.toObject(Cart::class.java)
                val oldCartItemSnapshot = transaction.get(cartItemRef)
                val oldCartItem = oldCartItemSnapshot.toObject(CartItem::class.java)

                if (cart == null) {
                    throw Exception("Cart not found.")
                }

                if (oldCartItem == null) {
                    throw Exception("Old CartItem not found.")
                }

                val updatedTotalAmount = cart.totalAmount - oldCartItem.totalPrice + updatedCartItem.totalPrice

                transaction.set(cartItemRef, updatedCartItem)
                transaction.update(cartRef,
                    "totalAmount", updatedTotalAmount,
                    "finalAmount", updatedTotalAmount,
                    "updatedAt", Timestamp.now(),
                    "isValid", false
                )
            }.await().let { Result.success(Unit) }
        } catch (e: Exception) {
            Log.e(TAG, "updateCartItemInCart failed", e)
            Result.failure(RuntimeException("Failed to update cart item", e))
        }
    }

    suspend fun clearCart(cartId: String, cartItemIds: List<String>): Result<Unit> {
        return try {
            firestore.runTransaction { transaction ->
                val cartRef = firestore.collection(CARTS_COLLECTION).document(cartId)
                val cartSnapshot = transaction.get(cartRef)
                cartSnapshot.toObject(Cart::class.java)
                    ?: throw Exception("Cart not found.")

                for (itemId in cartItemIds) {
                    val itemRef = firestore.collection(CART_ITEMS_COLLECTION).document(itemId)
                    transaction.delete(itemRef)
                }

                transaction.update(
                    cartRef,
                    "cartItemIds", emptyList<String>(),
                    "totalAmount", 0.0,
                    "finalAmount", 0.0,
                    "updatedAt", Timestamp.now(),
                    "isValid", false
                )
            }.await().let { Result.success(Unit) }
        } catch (e: Exception) {
            Log.e(TAG, "clearCart failed", e)
            Result.failure(RuntimeException("Failed to clear cart", e))
        }
    }
}