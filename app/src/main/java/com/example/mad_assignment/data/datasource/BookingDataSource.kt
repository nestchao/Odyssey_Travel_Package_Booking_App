package com.example.mad_assignment.data.datasource

import android.util.Log
import com.example.mad_assignment.data.model.Booking
import com.example.mad_assignment.data.model.BookingStatus
import com.example.mad_assignment.data.model.CartItem
import com.example.mad_assignment.data.model.TravelPackage
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.Timestamp
import com.google.firebase.firestore.AggregateField
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

// Helper function to convert the data class to a map Firestore understands perfectly
fun com.example.mad_assignment.data.model.DepartureAndEndTime.toMap(): Map<String, Any> {
    return mapOf(
        "id" to this.id,
        "startDate" to this.startDate,
        "endDate" to this.endDate,
        "capacity" to this.capacity,
        "numberOfPeopleBooked" to this.numberOfPeopleBooked
    )
}

@Singleton
class BookingDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    companion object {
        private const val BOOKINGS_COLLECTION = "bookings"
        private const val CARTS_COLLECTION = "carts"
        private const val CART_ITEMS_COLLECTION = "cartItems"
        private const val PACKAGES_COLLECTION = "packages"
        // Add the payments collection constant
        private const val PAYMENTS_COLLECTION = "payments"
        private const val TAG = "BookingDataSource"
    }

    suspend fun getAllBookings(): Result<List<Booking>> {
        return try {
            val snapshot = firestore.collection(BOOKINGS_COLLECTION)
                .orderBy("createdAt", Query.Direction.DESCENDING) // Shows newest bookings first
                .get()
                .await()
            val bookings = snapshot.toObjects(Booking::class.java)
            Result.success(bookings)
        } catch (e: Exception) {
            Log.e(TAG, "getAllBookings failed", e)
            Result.failure(RuntimeException("Failed to retrieve all bookings", e))
        }
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

                    val availableCapacity = departureOption.capacity - departureOption.numberOfPeopleBooked
                    if (availableCapacity < cartItem.totalTravelerCount) {
                        throw FirebaseFirestoreException("Insufficient capacity for package ${cartItem.packageId}", FirebaseFirestoreException.Code.ABORTED)
                    }
                }

                val bookingIds = mutableListOf<String>()

                for (cartItem in cartItems) {
                    val newBookingRef = firestore.collection(BOOKINGS_COLLECTION).document()
                    val newBooking = Booking(
                        bookingId = newBookingRef.id, userId = userId, packageId = cartItem.packageId,
                        departureId = cartItem.departureId,
                        paymentId = paymentId, noOfAdults = cartItem.noOfAdults,
                        noOfChildren = cartItem.noOfChildren, totalTravelerCount = cartItem.totalTravelerCount,
                        subtotal = cartItem.basePrice, totalAmount = cartItem.totalPrice,
                        startBookingDate = cartItem.startDate, endBookingDate = cartItem.endDate,
                        createdAt = Timestamp.now(), updatedAt = Timestamp.now(), status = BookingStatus.PAID
                    )
                    Log.d("DATA_TYPE_CHECK", "Saving booking. totalAmount is: ${newBooking.totalAmount}")
                    Log.d("DATA_TYPE_CHECK", "The data type is: ${newBooking.totalAmount::class.java.simpleName}")
                    transaction.set(newBookingRef, newBooking)
                    bookingIds.add(newBookingRef.id)
                }

                for ((pkgId, itemsInPackage) in cartItemsByPackage) {
                    val packageRef = firestore.collection(PACKAGES_COLLECTION).document(pkgId)
                    val originalPackage = packageDataMap[pkgId]!!
                    val updatedOptions = originalPackage.packageOption.toMutableList()

                    for (cartItem in itemsInPackage) {
                        val optionIndex = updatedOptions.indexOfFirst { it.id == cartItem.departureId }
                        if (optionIndex != -1) {
                            val oldOption = updatedOptions[optionIndex]
                            updatedOptions[optionIndex] = oldOption.copy(numberOfPeopleBooked = oldOption.numberOfPeopleBooked + cartItem.totalTravelerCount)
                        }
                    }

                    val optionsAsMaps = updatedOptions.map { it.toMap() }
                    transaction.update(packageRef, "packageOption", optionsAsMaps)
                }

                val remainingItemIds = currentCart.cartItemIds.filter { it !in bookedItemIds }
                val newTotalAmount = currentCart.totalAmount - priceOfBookedItems
                transaction.update(cartRef, mapOf(
                    "cartItemIds" to remainingItemIds,
                    "totalAmount" to newTotalAmount.coerceAtLeast(0.0),
                    "finalAmount" to newTotalAmount.coerceAtLeast(0.0),
                    "updatedAt" to Timestamp.now(),
                    "isValid" to remainingItemIds.isNotEmpty()
                ))

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

                if (booking.status == BookingStatus.PAID && newStatus == BookingStatus.CANCELLED) {
                    transaction.update(
                        bookingRef,
                        "status", BookingStatus.REFUNDED.name,
                        "updatedAt", Timestamp.now()
                    )
                } else {
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
                    ?: throw FirebaseFirestoreException("Booking not found.", FirebaseFirestoreException.Code.NOT_FOUND)

                Log.d(TAG, "Cancelling Booking ID: ${booking.bookingId} for Package ID: ${booking.packageId}")
                Log.d(TAG, "Departure ID: ${booking.departureId}, Travelers to refund: ${booking.totalTravelerCount}")

                if (booking.status == BookingStatus.COMPLETED || booking.status == BookingStatus.CANCELLED || booking.status == BookingStatus.REFUNDED) {
                    throw Exception("Cannot cancel booking with status: ${booking.status}")
                }

                if (booking.packageId.isNotBlank() && booking.departureId.isNotBlank()) {
                    val packageRef = firestore.collection(PACKAGES_COLLECTION).document(booking.packageId)
                    val packageSnapshot = transaction.get(packageRef)
                    val travelPackage = packageSnapshot.toObject(TravelPackage::class.java)
                        ?: throw FirebaseFirestoreException("Associated package ${booking.packageId} not found.", FirebaseFirestoreException.Code.NOT_FOUND)

                    Log.d(TAG, "Package found. Current options: ${travelPackage.packageOption}")

                    var wasOptionFound = false
                    val updatedOptions = travelPackage.packageOption.map { option ->
                        if (option.id == booking.departureId) {
                            wasOptionFound = true
                            Log.d(TAG, "Found matching departure option. Before: ${option.numberOfPeopleBooked} booked.")
                            val newBookedCount = (option.numberOfPeopleBooked - booking.totalTravelerCount).coerceAtLeast(0)
                            Log.d(TAG, "After cancellation, new booked count will be: $newBookedCount")
                            option.copy(numberOfPeopleBooked = newBookedCount)
                        } else {
                            option
                        }
                    }

                    if (!wasOptionFound) {
                        Log.w(TAG, "Could not find matching departureId '${booking.departureId}' in package options.")
                    } else {
                        val optionsAsMaps = updatedOptions.map { it.toMap() }
                        Log.d(TAG, "Updating package with new options map: $optionsAsMaps")
                        transaction.update(packageRef, "packageOption", optionsAsMaps)
                    }

                } else {
                    Log.w(TAG, "Cannot release package capacity for booking $bookingId: packageId or departureId is missing.")
                }

                val newStatus: BookingStatus
                if (booking.status == BookingStatus.PAID || booking.status == BookingStatus.CONFIRMED) {
                    newStatus = BookingStatus.REFUNDED

                    // If a refund is happening, update the payment document as well.
                    if (booking.paymentId.isNotBlank()) {
                        val paymentRef = firestore.collection(PAYMENTS_COLLECTION).document(booking.paymentId)
                        transaction.update(paymentRef, mapOf(
                            "status" to com.example.mad_assignment.data.model.PaymentStatus.REFUNDED.name,
                            "updatedAt" to Timestamp.now()
                        ))
                        Log.d(TAG, "Updating payment ${booking.paymentId} to REFUNDED.")
                    } else {
                        Log.w(TAG, "Booking ${booking.bookingId} was paid but has no paymentId. Cannot update payment status.")
                    }
                } else {
                    newStatus = BookingStatus.CANCELLED
                }

                Log.d(TAG, "Setting booking status to: ${newStatus.name}")

                transaction.update(
                    bookingRef,
                    "status", newStatus.name,
                    "updatedAt", Timestamp.now()
                )
            }.await().let { Result.success(Unit) }
        } catch (e: Exception) {
            Log.e(TAG, "cancelBooking transaction failed", e)
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
                .whereLessThan("startBookingDate", now)
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

                Log.d(TAG, "Direct Purchase: Found package. Current options: ${travelPackage.packageOption}")

                var wasOptionFound = false
                val updatedPackageOptions = travelPackage.packageOption.map { option ->
                    if (option.id == departureId) {
                        wasOptionFound = true
                        Log.d(TAG, "Direct Purchase: Found option. Before: ${option.numberOfPeopleBooked} booked.")
                        val availableCapacity = option.capacity - option.numberOfPeopleBooked
                        if (availableCapacity < travelerCount) {
                            throw FirebaseFirestoreException(
                                "Insufficient capacity. Available: $availableCapacity, Requested: $travelerCount",
                                FirebaseFirestoreException.Code.ABORTED
                            )
                        }
                        val newBookedCount = option.numberOfPeopleBooked + travelerCount
                        Log.d(TAG, "Direct Purchase: After booking, new booked count will be: $newBookedCount")
                        option.copy(numberOfPeopleBooked = newBookedCount)
                    } else {
                        option
                    }
                }

                if (!wasOptionFound) {
                    throw FirebaseFirestoreException("Departure option not found for the selected dates.", FirebaseFirestoreException.Code.NOT_FOUND)
                }

                val newBookingRef = firestore.collection(BOOKINGS_COLLECTION).document(newBooking.bookingId)
                transaction.set(newBookingRef, newBooking)

                val optionsAsMaps = updatedPackageOptions.map { it.toMap() }
                Log.d(TAG, "Direct Purchase: Updating package with new options map: $optionsAsMaps")
                transaction.update(packageRef, "packageOption", optionsAsMaps)

                newBooking.bookingId
            }.await().let { Result.success(it) }
        } catch (e: Exception) {
            Log.e(TAG, "createBookingFromDirectPurchase failed", e)
            Result.failure(RuntimeException("Failed to create booking from direct purchase.", e))
        }
    }
    suspend fun countAllBookings(): Result<Long> {
        return try {
            val snapshot = firestore.collection(BOOKINGS_COLLECTION)
                .count().get(AggregateSource.SERVER).await()
            Result.success(snapshot.count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun countPendingBookings(): Result<Long> {
        return try {
            val snapshot = firestore.collection(BOOKINGS_COLLECTION)
                .whereEqualTo("status", BookingStatus.PENDING.name)
                .count().get(AggregateSource.SERVER).await()
            Result.success(snapshot.count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTotalRevenue(): Result<Double> {
        return try {
            val snapshot = firestore.collection(BOOKINGS_COLLECTION)
                .whereIn("status", listOf(BookingStatus.PAID, BookingStatus.COMPLETED))
                .aggregate(AggregateField.sum("totalAmount")) // Sum the 'totalAmount' field
                .get(AggregateSource.SERVER).await()

            Result.success(snapshot.getDouble(AggregateField.sum("totalAmount")) ?: 0.0)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTotalTripsByUserId(userId: String): Result<Long> {
        return try {
            val snapshot = firestore.collection(BOOKINGS_COLLECTION)
                .whereEqualTo("userId", userId)
                .count()
                .get(AggregateSource.SERVER)
                .await()
            Result.success(snapshot.count)
        } catch (e: Exception) {
            Log.e(TAG, "getTotalTripsByUserId failed for user: $userId", e)
            Result.failure(e)
        }
    }
}