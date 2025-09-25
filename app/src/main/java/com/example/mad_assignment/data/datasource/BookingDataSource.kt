package com.example.mad_assignment.data.datasource

import android.util.Log
import com.example.mad_assignment.data.model.Booking
import com.example.mad_assignment.data.model.BookingStatus
import com.example.mad_assignment.data.model.CartItem
import com.example.mad_assignment.data.model.TravelPackage
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
        private const val PACKAGES_COLLECTION = "packages"
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
        cartItems: List<CartItem>,
        paymentId: String
    ): Result<List<String>> {
        if (cartItems.isEmpty()) {
            return Result.failure(RuntimeException("No cart items to process."))
        }

        return try {
            val priceOfBookedItems = cartItems.sumOf { it.totalPrice }

            firestore.runTransaction { transaction ->
                val bookedItemIds = cartItems.map { it.cartItemId }.toSet()

                // PHASE 1: ALL READS AND PRE-FLIGHT CHECKS
                val cartRef = firestore.collection(CARTS_COLLECTION).document(cartId)
                val cartSnapshot = transaction.get(cartRef)
                val currentCart = cartSnapshot.toObject(com.example.mad_assignment.data.model.Cart::class.java)
                    ?: throw FirebaseFirestoreException("Cart not found during update.", FirebaseFirestoreException.Code.NOT_FOUND)

                val cartItemsByPackage = cartItems.groupBy { it.packageId }
                val packageDataMap = mutableMapOf<String, TravelPackage>()

                for (packageId in cartItemsByPackage.keys) {
                    val packageRef = firestore.collection(PACKAGES_COLLECTION).document(packageId)
                    val packageDoc = transaction.get(packageRef)
                    if (!packageDoc.exists()) {
                        throw FirebaseFirestoreException("Package not found: $packageId", FirebaseFirestoreException.Code.NOT_FOUND)
                    }
                    packageDataMap[packageId] = packageDoc.toObject(TravelPackage::class.java)!!
                }

                for (cartItem in cartItems) {
                    val travelPackage = packageDataMap[cartItem.packageId]!!
                    val departureOption = travelPackage.packageOption.find { it.id == cartItem.departureId }
                        ?: throw FirebaseFirestoreException("Departure option with ID ${cartItem.departureId} not found in package.", FirebaseFirestoreException.Code.NOT_FOUND)

                    // IMPORTANT: Use numberOfPeopleBooked here
                    val availableCapacity = departureOption.capacity - departureOption.numberOfPeopleBooked
                    if (availableCapacity < cartItem.totalTravelerCount) {
                        throw FirebaseFirestoreException("Insufficient capacity for package ${cartItem.packageId}", FirebaseFirestoreException.Code.ABORTED)
                    }
                }

                // PHASE 2: ALL WRITES
                val bookingIds = mutableListOf<String>()

                // 1. Create all booking documents
                for (cartItem in cartItems) {
                    val newBookingRef = firestore.collection(BOOKINGS_COLLECTION).document()
                    val newBooking = Booking(
                        bookingId = newBookingRef.id, userId = userId, packageId = cartItem.packageId,
                        paymentId = paymentId, noOfAdults = cartItem.noOfAdults,
                        noOfChildren = cartItem.noOfChildren, totalTravelerCount = cartItem.totalTravelerCount,
                        subtotal = cartItem.basePrice, totalAmount = cartItem.totalPrice,
                        startBookingDate = cartItem.startDate, endBookingDate = cartItem.endDate,
                        createdAt = Timestamp.now(), updatedAt = Timestamp.now(), status = BookingStatus.PAID
                    )
                    transaction.set(newBookingRef, newBooking)
                    bookingIds.add(newBookingRef.id)
                }

                // 2. Update all package capacities
                for ((pkgId, itemsInPackage) in cartItemsByPackage) {
                    val packageRef = firestore.collection(PACKAGES_COLLECTION).document(pkgId)
                    val originalPackage = packageDataMap[pkgId]!!
                    val updatedOptions = originalPackage.packageOption.toMutableList()

                    for (cartItem in itemsInPackage) {
                        val optionIndex = updatedOptions.indexOfFirst { it.id == cartItem.departureId }
                        if (optionIndex != -1) {
                            val oldOption = updatedOptions[optionIndex]
                            // IMPORTANT: Update numberOfPeopleBooked with totalTravelerCount
                            updatedOptions[optionIndex] = oldOption.copy(numberOfPeopleBooked = oldOption.numberOfPeopleBooked + cartItem.totalTravelerCount)
                        }
                    }
                    transaction.update(packageRef, "packageOption", updatedOptions)
                }

                // 3. Update the cart document
                val remainingItemIds = currentCart.cartItemIds.filter { it !in bookedItemIds }
                val newTotalAmount = currentCart.totalAmount - priceOfBookedItems
                transaction.update(cartRef, mapOf(
                    "cartItemIds" to remainingItemIds,
                    "totalAmount" to newTotalAmount.coerceAtLeast(0.0),
                    "finalAmount" to newTotalAmount.coerceAtLeast(0.0),
                    "updatedAt" to Timestamp.now(),
                    "isValid" to remainingItemIds.isNotEmpty()
                ))

                // 4. Delete the individual cart item documents
                for (cartItem in cartItems) {
                    val cartItemRef = firestore.collection(CART_ITEMS_COLLECTION).document(cartItem.cartItemId)
                    transaction.delete(cartItemRef)
                }

                bookingIds
            }.await().let { Result.success(it) }
        } catch (e: Exception) {
            Log.e(TAG, "createBookingsFromCart failed", e)
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
                    // ALSO: If this cancellation is successful and a refund is issued,
                    // you would typically want to release the capacity back to the package.
                    // This involves finding the associated DepartureAndEndTime and
                    // decrementing its numberOfPeopleBooked by booking.totalTravelerCount.
                    transaction.update(
                        bookingRef,
                        "status", BookingStatus.REFUNDED.name,
                        "updatedAt", Timestamp.now()
                    )
                } else {
                    // Only update status if it's not a paid -> cancelled flow leading to REFUNDED
                    transaction.update(
                        bookingRef,
                        "status", newStatus.name,
                        "updatedAt", Timestamp.now()
                    )
                }
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
                if (booking.status == BookingStatus.COMPLETED || booking.status == BookingStatus.CANCELLED || booking.status == BookingStatus.REFUNDED) {
                    throw Exception("Cannot cancel booking with status: ${booking.status}")
                }

                val newStatus = if (booking.status == BookingStatus.PAID || booking.status == BookingStatus.CONFIRMED) {
                    // TODO: payment module process refund (this would be where the `numberOfPeopleBooked` is decremented)
                    // If a refund is processed, the status might go to REFUNDED
                    // For now, setting to CANCELLED and leaving a note for capacity adjustment
                    // You would need to add a `departureId` to your `Booking` model to make this work seamlessly.
                    BookingStatus.CANCELLED // Or REFUNDED if payment is processed
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

    // *** NEW FUNCTION ADDED HERE ***
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

    suspend fun completePastBookings(): Result<Unit> {
        return try {
            val now = Timestamp.now()
            val querySnapshot = firestore.collection(BOOKINGS_COLLECTION)
                .whereLessThan("startBookingDate", now) // Changed from "departureDate"
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

    suspend fun createBookingFromDirectPurchase(
        newBooking: Booking,
        packageId: String,
        departureId: String,
        travelerCount: Int
    ): Result<String> {
        return try {
            firestore.runTransaction { transaction ->
                val packageRef = firestore.collection(PACKAGES_COLLECTION).document(packageId)
                val packageDoc = transaction.get(packageRef)

                if (!packageDoc.exists()) {
                    throw FirebaseFirestoreException("Package not found.", FirebaseFirestoreException.Code.NOT_FOUND)
                }
                val travelPackage = packageDoc.toObject(TravelPackage::class.java)!!

                val updatedPackageOptions = travelPackage.packageOption.map { option ->
                    if (option.id == departureId) {
                        // IMPORTANT: Use numberOfPeopleBooked here
                        val availableCapacity = option.capacity - option.numberOfPeopleBooked
                        if (availableCapacity < travelerCount) {
                            throw FirebaseFirestoreException(
                                "Insufficient capacity. Available: $availableCapacity, Requested: $travelerCount",
                                FirebaseFirestoreException.Code.ABORTED
                            )
                        }
                        // IMPORTANT: Update numberOfPeopleBooked
                        option.copy(numberOfPeopleBooked = option.numberOfPeopleBooked + travelerCount)
                    } else {
                        option
                    }
                }

                if (travelPackage.packageOption.none { it.id == departureId }) {
                    throw FirebaseFirestoreException("Departure option not found for the selected dates.", FirebaseFirestoreException.Code.NOT_FOUND)
                }

                val newBookingRef = firestore.collection(BOOKINGS_COLLECTION).document(newBooking.bookingId)
                transaction.set(newBookingRef, newBooking)

                transaction.update(packageRef, "packageOption", updatedPackageOptions)

                newBooking.bookingId
            }.await().let { Result.success(it) }
        } catch (e: Exception) {
            Log.e(TAG, "createBookingFromDirectPurchase failed", e)
            Result.failure(RuntimeException("Failed to create booking from direct purchase.", e))
        }
    }
}