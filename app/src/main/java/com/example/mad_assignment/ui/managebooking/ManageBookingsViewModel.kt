package com.example.mad_assignment.ui.managebooking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mad_assignment.data.model.Booking
import com.example.mad_assignment.data.model.BookingStatus
import com.example.mad_assignment.data.repository.BookingRepository // Assuming you have this
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ManageBookingViewModel @Inject constructor(
    private val bookingRepository: BookingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ManageBookingUiState>(ManageBookingUiState.Loading)
    val uiState: StateFlow<ManageBookingUiState> = _uiState.asStateFlow()

    init {
        loadBookings()
    }

    fun loadBookings() {
        viewModelScope.launch {
            _uiState.value = ManageBookingUiState.Loading
            try {
                val bookings = bookingRepository.getAllBookings().getOrThrow()
                _uiState.value = ManageBookingUiState.Success(bookings = bookings)
            } catch (e: Exception) {
                _uiState.value = ManageBookingUiState.Error("Failed to load bookings: ${e.message}")
            }
        }
    }

    fun filterBookings(status: BookingStatus?) {
        val currentState = _uiState.value
        if (currentState is ManageBookingUiState.Success) {
            _uiState.value = currentState.copy(selectedFilter = status)
        }
    }

    fun onEditBookingClicked(booking: Booking) {
        val currentState = _uiState.value
        if (currentState is ManageBookingUiState.Success) {
            _uiState.value = currentState.copy(showEditDialog = true, selectedBooking = booking)
        }
    }

    fun onDeleteBookingClicked(booking: Booking) {
        val currentState = _uiState.value
        if (currentState is ManageBookingUiState.Success) {
            _uiState.value = currentState.copy(showDeleteDialog = true, selectedBooking = booking)
        }
    }

    fun onDismissDialog() {
        val currentState = _uiState.value
        if (currentState is ManageBookingUiState.Success) {
            _uiState.value = currentState.copy(
                showEditDialog = false,
                showDeleteDialog = false,
                selectedBooking = null
            )
        }
    }

    fun updateBookingInfo(updatedBooking: Booking) {
        viewModelScope.launch {
            try {
                onDismissDialog()
                bookingRepository.updateBooking(updatedBooking.bookingId, updatedBooking).getOrThrow()
                loadBookings()
            } catch (e: Exception) {
                _uiState.value = ManageBookingUiState.Error("Failed to update booking: ${e.message}")
            }
        }
    }

    fun deleteBooking(bookingId: String) {
        viewModelScope.launch {
            try {
                onDismissDialog()
                bookingRepository.deleteBooking(bookingId).getOrThrow()
                loadBookings()
            } catch (e: Exception) {
                _uiState.value = ManageBookingUiState.Error("Failed to delete booking: ${e.message}")
            }
        }
    }
}