package com.example.mad_assignment.data.repository

import android.util.Log
import com.example.mad_assignment.data.datasource.BookingDataSource
import com.example.mad_assignment.data.model.Booking
import com.example.mad_assignment.data.model.BookingStatus
import com.example.mad_assignment.data.model.CartItem
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookingRepository @Inject constructor(
    private val bookingDataSource: BookingDataSource
) {
    companion object {
        private const val TAG = "BookingRepository"
    }

    suspend fun createBooking(newBooking: Booking): Result<String> {
        return bookingDataSource.createBooking(newBooking)
            .onFailure { Log.e(TAG, "createBooking failed", it) }
    }

    suspend fun createBookingsFromCart(
        userId: String,
        cartId: String,
        cartItems: List<CartItem>
    ): Result<List<String>> {
        return bookingDataSource.createBookingsFromCart(userId, cartId, cartItems)
            .onFailure { Log.e(TAG, "createBookingsFromCart failed", it) }
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
        return bookingDataSource.cancelBooking(bookingId)
            .onFailure { Log.e(TAG, "cancelBooking failed", it) }
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
}