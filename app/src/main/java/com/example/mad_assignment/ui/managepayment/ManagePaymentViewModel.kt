package com.example.mad_assignment.ui.managepayment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mad_assignment.data.model.PaymentStatus
import com.example.mad_assignment.data.repository.PaymentRepository

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ManagePaymentViewModel @Inject constructor(
    private val paymentRepository: PaymentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ManagePaymentUiState>(ManagePaymentUiState.Loading())
    val uiState: StateFlow<ManagePaymentUiState> = _uiState.asStateFlow()

    init {
        loadPayments()
    }

    fun loadPayments() {
        viewModelScope.launch {
            try {
                _uiState.value = ManagePaymentUiState.Loading()
                val payments = paymentRepository.getAllPayments()
                _uiState.value = ManagePaymentUiState.Success(payments = payments)
            } catch (e: Exception) {
                _uiState.value = ManagePaymentUiState.Error("Failed to load payments: ${e.message}")
            }
        }
    }

    fun updatePaymentStatus(paymentId: String, newStatus: PaymentStatus) {
        viewModelScope.launch {
            try {
                paymentRepository.updatePaymentStatus(paymentId, newStatus)
                loadPayments()
            } catch (e: Exception) {
                _uiState.value = ManagePaymentUiState.Error("Failed to update payment: ${e.message}")
            }
        }
    }

    fun filterPayments(status: PaymentStatus?) {
        val currentState = _uiState.value
        if (currentState is ManagePaymentUiState.Success) {
            _uiState.value = currentState.copy(selectedFilter = status)
        }
    }
}