package com.example.mad_assignment.data.datasource

import com.example.mad_assignment.data.model.Payment
import com.example.mad_assignment.data.model.PaymentStatus
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import java.util.Random
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PaymentDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    companion object {
        const val PAYMENTS_COLLECTION = "payments"
    }

    /**
     * Creates a new payment document in Firestore with a PENDING status.
     * Returns the ID of the newly created payment document.
     */
    suspend fun createPayment(payment: Payment): Result<String> {
        return try {
            val docRef = firestore.collection(PAYMENTS_COLLECTION).document()
            val finalPayment = payment.copy(id = docRef.id)
            docRef.set(finalPayment).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Updates the status of an existing payment.
     */
    suspend fun updatePaymentStatus(paymentId: String, newStatus: PaymentStatus, gatewayTransactionId: String? = null): Result<Unit> {
        return try {
            val updates = mutableMapOf<String, Any>(
                "status" to newStatus.name,
                "updatedAt" to Timestamp.now()
            )
            gatewayTransactionId?.let { updates["gatewayTransactionId"] = it }

            firestore.collection(PAYMENTS_COLLECTION)
                .document(paymentId)
                .update(updates)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * (Simulated) Processes an external payment. In a real app, this would involve
     * calling a payment gateway SDK/API. For now, it just simulates success or failure.
     */
    suspend fun simulateExternalPayment(paymentId: String, amount: Double, paymentMethod: String): Result<String> {
        // Simulate network delay
        delay(2000)

        // Simulate a random success/failure
        val random = Random()
        val success = random.nextBoolean() // 50% chance of success

        if (success) {
            val gatewayId = "TXN-${System.currentTimeMillis()}-${random.nextInt(1000)}"
            updatePaymentStatus(paymentId, PaymentStatus.PAID, gatewayId).getOrThrow()
            return Result.success(gatewayId)
        } else {
            updatePaymentStatus(paymentId, PaymentStatus.FAILED).getOrThrow()
            return Result.failure(Exception("Payment failed. Please try again."))
        }
    }
}