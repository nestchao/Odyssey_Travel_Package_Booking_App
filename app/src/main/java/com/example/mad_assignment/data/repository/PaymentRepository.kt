package com.example.mad_assignment.data.repository

import android.util.Log
import com.example.mad_assignment.data.datasource.PaymentDataSource
import com.example.mad_assignment.data.model.Payment
import com.example.mad_assignment.data.model.PaymentStatus
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PaymentRepository @Inject constructor(
    private val paymentDataSource: PaymentDataSource
) {
    companion object {
        private const val TAG = "PaymentRepository"
    }

    suspend fun initiatePayment(payment: Payment): Result<String> {
        return paymentDataSource.createPayment(payment)
            .onFailure { Log.e(TAG, "initiatePayment failed", it) }
    }

    suspend fun processPayment(paymentId: String, amount: Double, paymentMethod: String): Result<String> {
        // In a real app, this would interact with a payment SDK. Here we call our simulation.
        return paymentDataSource.simulateExternalPayment(paymentId, amount, paymentMethod)
            .onFailure { Log.e(TAG, "processPayment failed", it) }
    }

    suspend fun updatePaymentStatus(
        paymentId: String,
        newStatus: PaymentStatus,
        bookingIds: List<String>? = null,
        gatewayTransactionId: String? = null
    ): Result<Unit> {
        return paymentDataSource.updatePaymentStatus(paymentId, newStatus, bookingIds, gatewayTransactionId)
            .onFailure { Log.e(TAG, "updatePaymentStatus failed", it) }
    }
}