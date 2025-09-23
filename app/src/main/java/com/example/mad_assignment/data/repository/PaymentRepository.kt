package com.example.mad_assignment.data.repository

import com.example.mad_assignment.data.datasource.PaymentDataSource
import com.example.mad_assignment.data.model.Payment
import com.example.mad_assignment.data.model.PaymentStatus
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PaymentRepository @Inject constructor(
    private val paymentDataSource: PaymentDataSource
) {
    suspend fun initiatePayment(payment: Payment): Result<String> {
        return paymentDataSource.createPayment(payment)
    }

    suspend fun processPayment(paymentId: String, amount: Double, paymentMethod: String): Result<String> {
        return paymentDataSource.simulateExternalPayment(paymentId, amount, paymentMethod)
    }

    suspend fun updatePaymentStatus(paymentId: String, newStatus: PaymentStatus, gatewayTransactionId: String? = null): Result<Unit> {
        return paymentDataSource.updatePaymentStatus(paymentId, newStatus, gatewayTransactionId)
    }
}