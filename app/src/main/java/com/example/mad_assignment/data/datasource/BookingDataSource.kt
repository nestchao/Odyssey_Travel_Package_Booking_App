package com.example.mad_assignment.data.datasource

import android.os.Build
import androidx.annotation.RequiresApi
import com.example.mad_assignment.data.model.Booking
import com.example.mad_assignment.data.model.BookingStatus
import com.example.mad_assignment.data.model.CartItem
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton
import com.google.firebase.firestore.FirebaseFirestoreException

@Singleton
class BookingDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    companion object {
        private const val BOOKINGS_COLLECTION = "bookings"
        private const val CARTS_COLLECTION = "carts"
        private const val CART_ITEMS_COLLECTION = "cartItems"
        private const val DEPARTURE_DATES_COLLECTION = "departureDates"
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun createBookingsFromCart(
        userId: String,
        cartId: String,
        cartItems: List<CartItem>,
        paymentId: String
    ): Result<List<String>> {
        return try {
            firestore.runTransaction { transaction ->
                val bookingIds = mutableListOf<String>()
                val now = LocalDateTime.now() // require api

                // create booking for each cart item in cart
                for (cartItem in cartItems) {
                    val departureDateRef = firestore.collection(DEPARTURE_DATES_COLLECTION).document(cartItem.startDate?.id ?: "")
                    val departureDateSnapshot = transaction.get(departureDateRef)
                    val capacity = departureDateSnapshot.getLong("capacity") ?: 0
                    val currentCapacity = 0 // get bookings for this package

                    if (currentCapacity + cartItem.travelerCount > capacity) {
                        throw FirebaseFirestoreException("Package is at full capacity.", FirebaseFirestoreException.Code.ABORTED)
                    }

                    // create booking document
                    val newBookingRef = firestore.collection(BOOKINGS_COLLECTION).document()
                    val newBooking = Booking(
                        bookingId = newBookingRef.id,
                        userId = userId,
                        packageId = cartItem.packageId,
                        cartId = cartId,
                        cartItemId = cartItem.cartItemId,
                        paymentId = paymentId,
                        subtotal = cartItem.totalPrice, // In this simple model, subtotal is the item price
                        totalAmount = cartItem.totalPrice,
                        bookingDate = now,
                        updatedAt = now,
                        status = BookingStatus.PAID
                    )
                    transaction.set(newBookingRef, newBooking)
                    bookingIds.add(newBookingRef.id)
                }

                // clear cart items
                val cartRef = firestore.collection(CARTS_COLLECTION).document(cartId)
                transaction.update(cartRef, "cartItemIds", emptyList<String>(), "totalAmount", 0.0, "finalAmount", 0.0, "isValid", false)

                for (cartItem in cartItems) {
                    val cartItemRef = firestore.collection(CART_ITEMS_COLLECTION).document(cartItem.cartItemId)
                    transaction.delete(cartItemRef)
                }

                bookingIds
            }.await().let { Result.success(it) }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getBookingById(bookingId: String): Result<Booking?> {
        return try {
            val document = firestore.collection(BOOKINGS_COLLECTION).document(bookingId).get().await()
            val booking = document.toObject(Booking::class.java)
            Result.success(booking)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getBookingsByUserId(userId: String): Result<List<Booking>> {
        return try {
            val snapshot = firestore.collection(BOOKINGS_COLLECTION)
                .whereEqualTo("userId", userId)
                .get()
                .await()
            val bookings = snapshot.toObjects(Booking::class.java)
            Result.success(bookings)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateBookingStatus(bookingId: String, newStatus: BookingStatus): Result<Unit> {
        return try {
            val bookingRef = firestore.collection(BOOKINGS_COLLECTION).document(bookingId)
            firestore.runTransaction { transaction ->
                val bookingSnapshot = transaction.get(bookingRef)
                val booking = bookingSnapshot.toObject(Booking::class.java) ?: throw Exception("Booking not found.")

                if (booking.status == BookingStatus.PAID && newStatus == BookingStatus.CANCELLED) {
                    // payment module: process refund
                }

                transaction.update(bookingRef, "status", newStatus.name)
            }.await().let { Result.success(Unit) }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}