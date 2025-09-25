package com.example.mad_assignment.data.repository

import android.util.Log
import com.example.mad_assignment.data.datasource.BookingDataSource
import com.example.mad_assignment.data.model.Activity
import com.example.mad_assignment.data.model.ActivityType
import com.example.mad_assignment.data.model.Booking
import com.example.mad_assignment.data.model.BookingStatus
import com.example.mad_assignment.data.model.CartItem
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookingRepository @Inject constructor(
    private val bookingDataSource: BookingDataSource,
    private val activityRepository: ActivityRepository
) {
    companion object {
        private const val TAG = "BookingRepository"
    }

    suspend fun getAllBookings(): Result<List<Booking>> {
        return bookingDataSource.getAllBookings()
            .onFailure { Log.e(TAG, "getAllBookings failed", it) }
    }

    suspend fun createBooking(newBooking: Booking): Result<String> {
        val result = bookingDataSource.createBooking(newBooking)

        if (result.isSuccess) {
            val bookingId = result.getOrNull()
            val activity = Activity(
                description = "New booking created. ID: $bookingId",
                type = ActivityType.BOOKING_CREATED,
                userId = newBooking.userId,
                relatedId = bookingId
            )
            activityRepository.createActivity(activity)
        }

        return result.onFailure { Log.e(TAG, "createBooking failed", it) }
    }

    suspend fun createBookingsFromCart(
        userId: String,
        cartId: String,
        cartItems: List<CartItem>,
        paymentId: String
    ): Result<List<String>> {
        val result = bookingDataSource.createBookingsFromCart(userId, cartId, cartItems, paymentId)

        if (result.isSuccess) {
            val newBookingIds = result.getOrNull() ?: emptyList()

            newBookingIds.zip(cartItems).forEach { (bookingId, cartItem) ->
                val activity = Activity(
                    description = "New booking created from cart. ID: $bookingId",
                    type = ActivityType.BOOKING_CREATED,
                    userId = userId,
                    relatedId = bookingId
                )
                activityRepository.createActivity(activity)
            }
        }

        return result.onFailure { Log.e(TAG, "createBookingsFromCart failed", it) }
    }


    suspend fun deleteBooking(bookingId: String): Result<Unit> {
        return bookingDataSource.deleteBooking(bookingId)
            .onFailure { Log.e(TAG, "deleteBooking failed", it) }
    }

    suspend fun getBookingById(bookingId: String): Result<Booking?> {
        return bookingDataSource.getBookingById(bookingId)
            .onFailure { Log.e(TAG, "getBookingById failed", it) }
    }

    // get all bookings made by user
    suspend fun getBookingsByUserId(userId: String): Result<List<Booking>> {
        return bookingDataSource.getBookingsByUserId(userId)
            .onFailure { Log.e(TAG, "getBookingsByUserId failed", it) }
    }

    // no filter booking status and user
    suspend fun getBookingsByPackageId(packageId: String): Result<List<Booking>> {
        return bookingDataSource.getBookingsByPackageId(packageId)
            .onFailure { Log.e(TAG, "getBookingsByPackageId failed", it) }
    }

    // no filter booking status
    suspend fun getBookingByUserIdAndPackageId(userId: String, packageId: String): Result<Booking?> {
        return bookingDataSource.getBookingByUserIdAndPackageId(userId, packageId)
            .onFailure { Log.e(TAG, "getBookingByUserIdAndPackageId failed", it) }
    }

    // no filter user and package
    suspend fun getBookingsByStatus(status: BookingStatus): Result<List<Booking>> {
        return bookingDataSource.getBookingsByStatus(status)
            .onFailure { Log.e(TAG, "getBookingsByStatus failed", it) }
    }

    suspend fun updateBooking(bookingId: String, updatedBooking: Booking): Result<Unit> {
        return bookingDataSource.updateBooking(bookingId, updatedBooking)
            .onFailure { Log.e(TAG, "updateBooking failed", it) }
    }

    suspend fun updateBookingStatus(bookingId: String, newStatus: BookingStatus): Result<Unit> {
        return bookingDataSource.updateBookingStatus(bookingId, newStatus)
            .onFailure { Log.e(TAG, "updateBookingStatus failed", it) }
    }

    suspend fun cancelBooking(bookingId: String): Result<Unit> {
        val result = bookingDataSource.cancelBooking(bookingId)

        if (result.isSuccess) {
            val activity = Activity(
                description = "Booking cancelled. ID: $bookingId",
                type = ActivityType.BOOKING_CANCELLED,
                relatedId = bookingId
            )
            activityRepository.createActivity(activity)
        }

        return result.onFailure { Log.e(TAG, "cancelBooking failed", it) }
    }

    suspend fun completeBooking(bookingId: String): Result<Unit> {
        return bookingDataSource.completeBooking(bookingId)
            .onFailure { Log.e(TAG, "completeBooking failed", it) }
    }

    // change when package start and end timestamp is available
    // call need service past bookings, upcoming/current/past booking type
    suspend fun completePastBookings(): Result<Unit> {
        return bookingDataSource.completePastBookings()
            .onFailure { Log.e(TAG, "completePastBookings failed", it) }
    }

    suspend fun createBookingFromDirectPurchase(
        newBooking: Booking,
        packageId: String,
        departureId: String,
        travelerCount: Int
    ): Result<String> {
        return bookingDataSource.createBookingFromDirectPurchase(newBooking, packageId, departureId, travelerCount)
            .onFailure { Log.e(TAG, "createBookingFromDirectPurchase failed", it) }
    }

    suspend fun countAllBookings(): Long {
        return bookingDataSource.countAllBookings().getOrDefault(0)
    }

    suspend fun countPendingBookings(): Long {
        return bookingDataSource.countPendingBookings().getOrDefault(0)
    }

    suspend fun getTotalRevenue(): Double {
        return bookingDataSource.getTotalRevenue().getOrDefault(0.0)
    }
}