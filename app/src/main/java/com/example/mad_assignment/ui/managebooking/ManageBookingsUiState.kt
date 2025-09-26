package com.example.mad_assignment.ui.managebooking

import com.example.mad_assignment.data.model.Booking
import com.example.mad_assignment.data.model.BookingStatus

sealed interface ManageBookingUiState {
    data object Loading : ManageBookingUiState
    data class Success(
        val bookings: List<Booking>,
        val selectedFilter: BookingStatus? = null,
        val showEditDialog: Boolean = false,
        val showDeleteDialog: Boolean = false,
        val selectedBooking: Booking? = null
    ) : ManageBookingUiState
    data class Error(val message: String) : ManageBookingUiState
}