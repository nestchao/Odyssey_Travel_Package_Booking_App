package com.example.mad_assignment.ui.managepayment

import com.example.mad_assignment.data.model.Payment
import com.example.mad_assignment.data.model.PaymentStatus

sealed interface ManagePaymentUiState {
    data class Loading(val isRefreshing: Boolean = false) : ManagePaymentUiState
    data class Success(
        val payments: List<Payment>,
        val isRefreshing: Boolean = false,
        val selectedFilter: PaymentStatus? = null
    ) : ManagePaymentUiState
    data class Error(val message: String, val payments: List<Payment> = emptyList()) : ManagePaymentUiState
}