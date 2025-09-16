package com.example.mad_assignment.data.datasource

import com.example.mad_assignment.data.model.Cart
import com.example.mad_assignment.data.model.CartItem
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import com.google.firebase.firestore.toObjects
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import com.google.firebase.firestore.FieldPath

@Singleton
class CartDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    companion object {
        private const val CARTS_COLLECTION = "carts"
        private const val CART_ITEMS_COLLECTION = "cartItems"
    }

    fun getCartByUserId(userId: String): Flow<Cart?> {
        return firestore.collection(CARTS_COLLECTION)
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects<Cart>().firstOrNull()
            }
    }

    suspend fun getCartItemsForCart(cartItemIds: List<String>): Result<List<CartItem>> {
        if (cartItemIds.isEmpty()) {
            return Result.success(emptyList())
        }
        return try {
            val snapshot = firestore.collection(CART_ITEMS_COLLECTION)
                .whereIn(FieldPath.documentId(), cartItemIds)
                .get()
                .await()
            Result.success(snapshot.toObjects())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addItemToCart(userId: String, cartId: String?, newCartItem: CartItem): Result<String> {
        return try {
            firestore.runTransaction { transaction ->
                val cartRef = if (cartId.isNullOrEmpty()) {
                    firestore.collection(CARTS_COLLECTION).document()
                } else {
                    firestore.collection(CARTS_COLLECTION).document(cartId)
                }

                val cartSnapshot = transaction.get(cartRef)
                val existingCart = cartSnapshot.toObject(Cart::class.java)

                // Add the new cart item and get its ID
                val newCartItemRef = firestore.collection(CART_ITEMS_COLLECTION).document()
                transaction.set(newCartItemRef, newCartItem.copy(cartItemId = newCartItemRef.id))

                // Update or create the cart document
                val updatedItemIds = existingCart?.cartItemIds?.plus(newCartItemRef.id) ?: listOf(newCartItemRef.id)
                val updatedTotalAmount = (existingCart?.totalAmount ?: 0.0) + newCartItem.totalPrice

                val cartData = Cart(
                    cartId = cartRef.id,
                    userId = userId,
                    cartItemIds = updatedItemIds,
                    totalAmount = updatedTotalAmount,
                    finalAmount = updatedTotalAmount,
                    createdAt = existingCart?.createdAt ?: newCartItem.addedAt,
                    updatedAt = newCartItem.addedAt,
                    isValid = true
                )
                transaction.set(cartRef, cartData)

                newCartItemRef.id
            }.await().let { Result.success(it) }
        } catch (e: Exception) {
            Result.failure(e)
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

                if (cart == null || cartItem == null) {
                    throw Exception("Cart or CartItem not found.")
                }

                // remove the item from the cart item list and update the total
                val updatedItemIds = cart.cartItemIds.filter { it != cartItemIdToRemove }
                val updatedTotalAmount = cart.totalAmount - cartItem.totalPrice

                transaction.update(cartRef, "cartItemIds", updatedItemIds, "totalAmount", updatedTotalAmount, "finalAmount", updatedTotalAmount)
                transaction.delete(cartItemRef)
            }.await().let { Result.success(Unit) }
        } catch (e: Exception) {
            Result.failure(e)
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

                if (cart == null || oldCartItem == null) {
                    throw Exception("Cart or CartItem not found.")
                }

                // update the total amount on the cart document
                val newTotalPrice = updatedCartItem.totalPrice
                val oldTotalPrice = oldCartItem.totalPrice
                val updatedTotalAmount = cart.totalAmount - oldTotalPrice + newTotalPrice

                transaction.set(cartItemRef, updatedCartItem)
                transaction.update(cartRef, "totalAmount", updatedTotalAmount, "finalAmount", updatedTotalAmount)
            }.await().let { Result.success(Unit) }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun clearCart(cartId: String, cartItemIds: List<String>): Result<Unit> {
        return try {
            firestore.runTransaction { transaction ->
                val cartRef = firestore.collection(CARTS_COLLECTION).document(cartId)
                val cartSnapshot = transaction.get(cartRef)
                cartSnapshot.toObject(Cart::class.java) ?: throw Exception("Cart not found.")

                // delete all cart items
                for (itemId in cartItemIds) {
                    val itemRef = firestore.collection(CART_ITEMS_COLLECTION).document(itemId)
                    transaction.delete(itemRef)
                }

                // update the cart document
                transaction.update(cartRef, "cartItemIds", emptyList<String>(), "totalAmount", 0.0, "finalAmount", 0.0)
            }.await().let { Result.success(Unit) }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
