package com.example.mad_assignment.data.datasource

import android.util.Log
import com.example.mad_assignment.data.model.Booking
import com.example.mad_assignment.data.model.BookingStatus
import com.example.mad_assignment.data.model.CartItem
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.Timestamp
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookingDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    companion object {
        private const val BOOKINGS_COLLECTION = "bookings"
        private const val CARTS_COLLECTION = "carts"
        private const val CART_ITEMS_COLLECTION = "cartItems"
        private const val DEPARTURE_DATES_COLLECTION = "departureDates"
        private const val TAG = "BookingDataSource"
    }

    suspend fun createBooking(newBooking: Booking): Result<String> {
        return try {
            val documentRef = firestore.collection(BOOKINGS_COLLECTION).add(newBooking).await()
            Result.success(documentRef.id)
        } catch (e: Exception) {
            Log.e(TAG, "createBooking failed", e)
            Result.failure(RuntimeException("Failed to create new booking", e))
        }
    }

    suspend fun createBookingsFromCart(
        userId: String,
        cartId: String,
        cartItems: List<CartItem>
    ): Result<List<String>> {
        return try {
            firestore.runTransaction { transaction ->
                val bookingIds = mutableListOf<String>()

                // create booking for each cart item in cart
                for (cartItem in cartItems) {
                    val departureDateRef = firestore.collection(DEPARTURE_DATES_COLLECTION).document(cartItem.departureDate?.id ?: "")
                    val departureDateSnapshot = transaction.get(departureDateRef)
                    val capacity = departureDateSnapshot.getLong("capacity") ?: 0
                    val currentCapacity = 0 // TODO: get current booked capacity for this package

                    if (currentCapacity + cartItem.travelerCount > capacity) {
                        throw FirebaseFirestoreException("Package is at full capacity.", FirebaseFirestoreException.Code.ABORTED)
                    }

                    // create booking document
                    val newBookingRef = firestore.collection(BOOKINGS_COLLECTION).document()
                    val newBooking = Booking(
                        bookingId = newBookingRef.id,
                        userId = userId,
                        packageId = cartItem.packageId,
                        subtotal = cartItem.basePrice,
                        totalAmount = cartItem.totalPrice,
                        departureDate = cartItem.departureDate,
                        bookingDate = Timestamp.now(),
                        updatedAt = Timestamp.now(),
                        status = BookingStatus.CONFIRMED
                    )
                    transaction.set(newBookingRef, newBooking)
                    bookingIds.add(newBookingRef.id)
                }

                // clear cart items
                val cartRef = firestore.collection(CARTS_COLLECTION).document(cartId)
                transaction.update(
                    cartRef,
                    "cartItemIds", emptyList<String>(),
                    "totalAmount", 0.0,
                    "finalAmount", 0.0,
                    "updatedAt", Timestamp.now(),
                    "isValid", false
                )

                // delete cart items
                for (cartItem in cartItems) {
                    val cartItemRef = firestore.collection(CART_ITEMS_COLLECTION).document(cartItem.cartItemId)
                    transaction.delete(cartItemRef)
                }

                bookingIds
            }.await().let { Result.success(it) }
        } catch (e: Exception) {
            Log.e(TAG, "createBookingsFromCart failed for userId: $userId, cartId: $cartId", e)
            Result.failure(RuntimeException("Failed to create bookings from cart", e))
        }
    }

    suspend fun deleteBooking(bookingId: String): Result<Unit> {
        return try {
            firestore.collection(BOOKINGS_COLLECTION).document(bookingId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "deleteBooking failed", e)
            Result.failure(RuntimeException("Failed to delete booking", e))
        }
    }

    suspend fun getBookingById(bookingId: String): Result<Booking?> {
        return try {
            val document = firestore.collection(BOOKINGS_COLLECTION).document(bookingId).get().await()
            val booking = document.toObject(Booking::class.java)
            Result.success(booking)
        } catch (e: Exception) {
            Log.e(TAG, "getBookingById failed", e)
            Result.failure(RuntimeException("Failed to get booking by ID", e))
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
            Log.e(TAG, "getBookingsByUserId failed", e)
            Result.failure(RuntimeException("Failed to get bookings by user ID", e))
        }
    }

    suspend fun getBookingsByPackageId(packageId: String): Result<List<Booking>> {
        return try {
            val snapshot = firestore.collection(BOOKINGS_COLLECTION)
                .whereEqualTo("packageId", packageId)
                .get()
                .await()
            val bookings = snapshot.toObjects(Booking::class.java)
            Result.success(bookings)
        } catch (e: Exception) {
            Log.e(TAG, "getBookingsByPackageId failed", e)
            Result.failure(RuntimeException("Failed to get bookings by package ID", e))
        }
    }

    suspend fun getBookingByUserIdAndPackageId(userId: String, packageId: String): Result<Booking?> {
        return try {
            val snapshot = firestore.collection(BOOKINGS_COLLECTION)
                .whereEqualTo("userId", userId)
                .whereEqualTo("packageId", packageId)
                .get()
                .await()
            val booking = snapshot.toObjects(Booking::class.java).firstOrNull()
            Result.success(booking)
        } catch (e: Exception) {
            Log.e(TAG, "getBookingByUserIdAndPackageId failed", e)
            Result.failure(RuntimeException("Failed to get booking by user ID and package ID", e))
        }
    }

    suspend fun getBookingsByStatus(status: BookingStatus): Result<List<Booking>> {
        return try {
            val snapshot = firestore.collection(BOOKINGS_COLLECTION)
                .whereEqualTo("status", status.name)
                .get()
                .await()
            val bookings = snapshot.toObjects(Booking::class.java)
            Result.success(bookings)
        } catch (e: Exception) {
            Log.e(TAG, "getBookingsByStatus failed", e)
            Result.failure(RuntimeException("Failed to get bookings by status", e))
        }
    }

    suspend fun updateBooking(bookingId: String, updatedBooking: Booking): Result<Unit> {
        return try {
            val bookingRef = firestore.collection(BOOKINGS_COLLECTION).document(bookingId)
            val updatedBookingWithTimestamp = updatedBooking.copy(
                updatedAt = Timestamp.now()
            )
            bookingRef.set(updatedBookingWithTimestamp).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "updateBooking failed", e)
            Result.failure(RuntimeException("Failed to update booking", e))
        }
    }

    suspend fun updateBookingStatus(bookingId: String, newStatus: BookingStatus): Result<Unit> {
        return try {
            firestore.runTransaction { transaction ->
                val bookingRef = firestore.collection(BOOKINGS_COLLECTION).document(bookingId)
                val bookingSnapshot = transaction.get(bookingRef)
                val booking = bookingSnapshot.toObject(Booking::class.java)
                    ?: throw Exception("Booking not found.")

                // Handle refund logic for cancelled paid bookings
                if (booking.status == BookingStatus.PAID && newStatus == BookingStatus.CANCELLED) {
                    // TODO: payment module process refund
                    transaction.update(
                        bookingRef,
                        "status", BookingStatus.REFUNDED.name,
                        "updatedAt", Timestamp.now()
                    )
                }

                transaction.update(
                    bookingRef,
                    "status", newStatus.name,
                    "updatedAt", Timestamp.now()
                )
            }.await().let { Result.success(Unit) }
        } catch (e: Exception) {
            Log.e(TAG, "updateBookingStatus failed", e)
            Result.failure(RuntimeException("Failed to update booking status", e))
        }
    }

    suspend fun cancelBooking(bookingId: String): Result<Unit> {
        return try {
            firestore.runTransaction { transaction ->
                val bookingRef = firestore.collection(BOOKINGS_COLLECTION).document(bookingId)
                val bookingSnapshot = transaction.get(bookingRef)
                val booking = bookingSnapshot.toObject(Booking::class.java)
                    ?: throw Exception("Booking not found.")

                // check if booking can be cancelled
                if (booking.status == BookingStatus.COMPLETED || booking.status == BookingStatus.CANCELLED) {
                    throw Exception("Cannot cancel booking with status: ${booking.status}")
                }

                val newStatus = if (booking.status == BookingStatus.PAID) {
                    // TODO: payment module process refund
                    BookingStatus.CANCELLED
                } else {
                    BookingStatus.CANCELLED
                }

                transaction.update(
                    bookingRef,
                    "status", newStatus.name,
                    "updatedAt", Timestamp.now()
                )
            }.await().let { Result.success(Unit) }
        } catch (e: Exception) {
            Log.e(TAG, "cancelBooking failed", e)
            Result.failure(RuntimeException("Failed to cancel booking", e))
        }
    }

    suspend fun completeBooking(bookingId: String): Result<Unit> {
        return try {
            firestore.runTransaction { transaction ->
                val bookingRef = firestore.collection(BOOKINGS_COLLECTION).document(bookingId)
                val bookingSnapshot = transaction.get(bookingRef)
                val booking = bookingSnapshot.toObject(Booking::class.java)
                    ?: throw Exception("Booking not found.")

                // check if booking can be completed
                if (booking.status != BookingStatus.PAID && booking.status != BookingStatus.CONFIRMED) {
                    throw Exception("Cannot complete booking with status: ${booking.status}")
                }

                transaction.update(
                    bookingRef,
                    "status", BookingStatus.COMPLETED.name,
                    "updatedAt", Timestamp.now()
                )
            }.await().let { Result.success(Unit) }
        } catch (e: Exception) {
            Log.e(TAG, "completeBooking failed", e)
            Result.failure(RuntimeException("Failed to complete booking", e))
        }
    }

    // called when scheduled job is triggered
    suspend fun completePastBookings(): Result<Unit> {
        return try {
            val now = Timestamp.now()
            val querySnapshot = firestore.collection(BOOKINGS_COLLECTION)
                .whereLessThan("departureDate", now)
                .whereIn("status", listOf(BookingStatus.PAID.name, BookingStatus.CONFIRMED.name))
                .get()
                .await()

            if (querySnapshot.isEmpty) {
                Log.d(TAG, "No bookings found to complete.")
                return Result.success(Unit)
            }

            firestore.runBatch { batch ->
                for (document in querySnapshot.documents) {
                    val bookingRef = document.reference
                    batch.update(
                        bookingRef,
                        mapOf(
                            "status" to BookingStatus.COMPLETED.name,
                            "updatedAt" to now
                        )
                    )
                }
            }.await()
            Log.d(TAG, "Successfully completed ${querySnapshot.size()} past bookings.")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "completePastBookings failed", e)
            Result.failure(RuntimeException("Failed to complete past bookings", e))
        }
    }
}