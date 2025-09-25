package com.example.mad_assignment.ui.booking

import com.example.mad_assignment.data.model.Booking
import com.example.mad_assignment.data.model.TravelPackageWithImages

sealed interface BookingUiState {
    object Loading : BookingUiState
    data class Error(val message: String) : BookingUiState
    data class Success(
        val currentBookings: List<Booking> = emptyList(),
        val upcomingBookings: List<Booking> = emptyList(),
        val pastBookings: List<Booking> = emptyList(),
        val packages: Map<String, TravelPackageWithImages?> = emptyMap(),
        val showBookingDetails: Boolean = false,
        val selectedBookingId: String? = null
    ) : BookingUiState {
        val hasCurrentBookings: Boolean get() = currentBookings.isNotEmpty()
        val hasUpcomingBookings: Boolean get() = upcomingBookings.isNotEmpty()
        val hasPastBookings: Boolean get() = pastBookings.isNotEmpty()
        val hasAnyBookings: Boolean get() = hasCurrentBookings || hasUpcomingBookings || hasPastBookings

        val selectedBooking: Booking?
            get() = selectedBookingId?.let { id ->
                (currentBookings + upcomingBookings + pastBookings).find { it.bookingId == id }
            }
    }
    object Empty : BookingUiState
}