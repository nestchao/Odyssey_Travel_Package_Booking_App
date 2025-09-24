package com.example.mad_assignment.data.datasource

import android.util.Log
import com.example.mad_assignment.data.model.Payment
import com.example.mad_assignment.data.model.PaymentStatus
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class PaymentDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    companion object {
        private const val PAYMENTS_COLLECTION = "payments"
        private const val TAG = "PaymentDataSource"
    }

    suspend fun createPayment(payment: Payment): Result<String> {
        return try {
            val paymentWithTimestamp = payment.copy(
                createdAt = Timestamp.now(),
                updatedAt = Timestamp.now()
            )
            val documentRef = firestore.collection(PAYMENTS_COLLECTION).add(paymentWithTimestamp).await()
            Result.success(documentRef.id)
        } catch (e: Exception) {
            Log.e(TAG, "createPayment failed", e)
            Result.failure(RuntimeException("Failed to create new payment record", e))
        }
    }

    suspend fun updatePaymentStatus(
        paymentId: String,
        newStatus: PaymentStatus,
        bookingIds: List<String>? = null,
        gatewayTransactionId: String? = null
    ): Result<Unit> {
        return try {
            val paymentRef = firestore.collection(PAYMENTS_COLLECTION).document(paymentId)
            val updates = mutableMapOf<String, Any>(
                "status" to newStatus.name,
                "updatedAt" to Timestamp.now()
            )
            gatewayTransactionId?.let { updates["gatewayTransactionId"] = it }
            bookingIds?.let { updates["bookingIds"] = it }

            paymentRef.update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "updatePaymentStatus failed for paymentId: $paymentId", e)
            Result.failure(RuntimeException("Failed to update payment status", e))
        }
    }

    /**
     * This is a placeholder for a real payment gateway integration (e.g., Stripe, PayPal).
     * It simulates a network call and a success/failure response.
     */
    suspend fun simulateExternalPayment(
        paymentId: String,
        amount: Double,
        paymentMethod: String
    ): Result<String> {
        return try {
            Log.d(TAG, "Simulating payment for paymentId: $paymentId, amount: $amount")
            // Simulate network delay
            delay(2000)

            // Simulate a success/failure scenario
            val isSuccess = true// 50% chance of success for simulation

            if (isSuccess) {
                val transactionId = "sim_${UUID.randomUUID()}"
                Log.d(TAG, "Payment simulation successful. Transaction ID: $transactionId")
                Result.success(transactionId)
            } else {
                Log.w(TAG, "Payment simulation failed.")
                Result.failure(Exception("Payment declined by gateway."))
            }
        } catch (e: Exception) {
            Log.e(TAG, "simulateExternalPayment encountered an error", e)
            Result.failure(e)
        }
    }
}