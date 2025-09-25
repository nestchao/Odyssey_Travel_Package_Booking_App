package com.example.mad_assignment.ui.checkout

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mad_assignment.data.model.*
import com.example.mad_assignment.data.repository.*
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.URLDecoder
import java.util.UUID
import javax.inject.Inject
import androidx.lifecycle.viewModelScope
import com.example.mad_assignment.data.model.*
import com.example.mad_assignment.data.repository.*
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

@HiltViewModel
class CheckoutViewModel @Inject constructor(
    private val packageRepository: TravelPackageRepository,
    private val userRepository: UserRepository,
    private val bookingRepository: BookingRepository,
    private val paymentRepository: PaymentRepository,
    private val cartRepository: CartRepository,
    private val auth: FirebaseAuth,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val checkoutMode =
        if (savedStateHandle.get<String>("cartId") != null) CheckoutMode.FROM_CART
        else CheckoutMode.DIRECT_BUY

    private val _uiState = MutableStateFlow<CheckoutUiState>(CheckoutUiState.Loading)
    val uiState = _uiState.asStateFlow()

    private val _checkoutResult = MutableStateFlow<CheckoutResultEvent?>(null)
    val checkoutResult = _checkoutResult.asStateFlow()

    init {
        loadCheckoutDetails()
    }

    private fun loadCheckoutDetails() = viewModelScope.launch {
        _uiState.value = CheckoutUiState.Loading
        val userId = auth.currentUser?.uid ?: run {
            _uiState.value = CheckoutUiState.Error("User not logged in.")
            return@launch
        }

        try {
            val user = userRepository.getUserById(userId) ?: run {
                _uiState.value = CheckoutUiState.Error("Could not load user details.")
                return@launch
            }

            if (checkoutMode == CheckoutMode.DIRECT_BUY) {
                loadDirectBuyDetails(user)
            } else {
                loadCartDetails(user)
            }

        } catch (e: Exception) {
            _uiState.value = CheckoutUiState.Error(e.message ?: "An unknown error occurred.")
        }
    }

    private suspend fun loadDirectBuyDetails(user: User) {
        val packageId: String? = savedStateHandle.get("packageId")
        val departureId: String? = savedStateHandle.get("departureId")
        val paxCountsJson: String? = savedStateHandle.get("paxCountsJson")

        if (packageId == null || departureId == null || paxCountsJson == null) {
            _uiState.value = CheckoutUiState.Error("Missing details for direct purchase.")
            return
        }

        val packageDetail = packageRepository.getPackageWithImages(packageId) ?: run {
            _uiState.value = CheckoutUiState.Error("Could not load package details.")
            return
        }

        val decodedJson = URLDecoder.decode(paxCountsJson, "UTF-8")
        val type = object : TypeToken<Map<String, Int>>() {}.type
        val paxCounts: Map<String, Int> = Gson().fromJson(decodedJson, type)

        val totalPrice = paxCounts.entries.sumOf { (category, count) ->
            (packageDetail.travelPackage.pricing[category] ?: 0.0) * count
        }

        _uiState.value = CheckoutUiState.Success(
            user = user,
            displayItems = listOf(CheckoutDisplayItem.from(packageDetail, paxCounts, totalPrice, departureId)),
            totalPrice = totalPrice
        )
    }

    private suspend fun loadCartDetails(user: User) {
        val cartId: String? = savedStateHandle.get("cartId")
        // Get the new argument
        val selectedItemIdsJson: String? = savedStateHandle.get("selectedItemIdsJson")

        if (cartId == null || selectedItemIdsJson == null) {
            _uiState.value = CheckoutUiState.Error("Cart details are missing.")
            return
        }

        // Deserialize the list of selected IDs
        val decodedJson = URLDecoder.decode(selectedItemIdsJson, "UTF-8")
        val type = object : TypeToken<List<String>>() {}.type
        val selectedItemIds: List<String> = Gson().fromJson(decodedJson, type)

        if (selectedItemIds.isEmpty()) {
            _uiState.value = CheckoutUiState.Error("No items were selected for checkout.")
            return
        }

        // Fetch ONLY the selected cart items
        val cartItems = cartRepository.getCartItemsForCart(selectedItemIds).getOrDefault(emptyList())

        val displayItems = cartItems.mapNotNull { cartItem ->
            packageRepository.getPackageWithImages(cartItem.packageId)?.let { pkg ->
                CheckoutDisplayItem.from(pkg, cartItem)
            }
        }

        // Calculate the total price based on ONLY the selected items
        val totalPrice = cartItems.sumOf { it.totalPrice }

        _uiState.value = CheckoutUiState.Success(
            user = user,
            displayItems = displayItems,
            totalPrice = totalPrice // Use the newly calculated price
        )
    }

    fun onConfirmAndPay(
        paymentMethod: String,
        cardholderName: String,
        cardNumber: String,
        expiryDate: String,
        cvc: String
    ) = viewModelScope.launch {
        val currentState = _uiState.value
        if (currentState !is CheckoutUiState.Success || currentState.isProcessingPayment) return@launch

        // ** ADDED VALIDATION BLOCK **
        if (cardholderName.isBlank() || cardNumber.length != 16 || cvc.length != 3) {
            _checkoutResult.value = CheckoutResultEvent.Failure("Please fill all payment fields correctly.")
            return@launch
        }

        if (!isExpiryDateValid(expiryDate)) {
            _checkoutResult.value = CheckoutResultEvent.Failure("Your card has expired or the date is invalid.")
            return@launch
        }

        // --- Payment processing logic starts here ---
        _uiState.update { state ->
            if (state is CheckoutUiState.Success) {
                state.copy(isProcessingPayment = true)
            } else {
                state
            }
        }

        val userId = auth.currentUser?.uid ?: return@launch
        val totalAmount = currentState.totalPrice

        val initialPayment = Payment(userId = userId, amount = totalAmount, paymentMethod = paymentMethod)
        val initiationResult = paymentRepository.initiatePayment(initialPayment)

        initiationResult.fold(
            onSuccess = { paymentId ->
                val processingResult = paymentRepository.processPayment(paymentId, totalAmount, paymentMethod)

                processingResult.fold(
                    onSuccess = { gatewayTransactionId ->
                        val bookingCreationResult = if (checkoutMode == CheckoutMode.DIRECT_BUY) {
                            createSingleBooking(userId, paymentId)
                        } else {
                            createBookingsFromCart(userId, paymentId)
                        }

                        bookingCreationResult.fold(
                            onSuccess = { bookingIds ->
                                paymentRepository.updatePaymentStatus(paymentId, PaymentStatus.SUCCESS, bookingIds, gatewayTransactionId)
                                _checkoutResult.value = CheckoutResultEvent.Success
                            },
                            onFailure = { bookingError ->
                                paymentRepository.updatePaymentStatus(paymentId, PaymentStatus.REFUNDED)
                                _checkoutResult.value = CheckoutResultEvent.Failure("Booking failed: ${bookingError.message}. A refund was issued.")
                            }
                        )
                    },
                    onFailure = { paymentError ->
                        paymentRepository.updatePaymentStatus(paymentId, PaymentStatus.FAILED)
                        _checkoutResult.value = CheckoutResultEvent.Failure(paymentError.message ?: "Payment failed.")
                    }
                )
            },
            onFailure = { initError ->
                _checkoutResult.value = CheckoutResultEvent.Failure("Could not start payment process: ${initError.message}")
            }
        )

        _uiState.update { state ->
            if (state is CheckoutUiState.Success) {
                state.copy(isProcessingPayment = false)
            } else {
                state
            }
        }
    }

    private suspend fun createSingleBooking(userId: String, paymentId: String): Result<List<String>> {
        val packageId: String? = savedStateHandle.get("packageId")
        val departureId: String? = savedStateHandle.get("departureId")
        val paxCountsJson: String? = savedStateHandle.get("paxCountsJson")

        if (packageId == null || departureId == null || paxCountsJson == null) {
            return Result.failure(Exception("Booking details are missing."))
        }

        // Fetch package details again to get duration
        val packageDetail = packageRepository.getPackageWithImages(packageId) ?: return Result.failure(Exception("Could not fetch package details for booking."))

        val decodedJson = URLDecoder.decode(paxCountsJson, "UTF-8")
        val type = object : TypeToken<Map<String, Int>>() {}.type
        val paxCounts: Map<String, Int> = Gson().fromJson(decodedJson, type)

        val state = _uiState.value as? CheckoutUiState.Success ?: return Result.failure(Exception("Invalid UI State"))
        val selectedDepartureTimestamp = state.displayItems.firstOrNull()?.departureDate ?: return Result.failure(Exception("Departure date not found."))

        // *** FIX #1: CALCULATE THE CORRECT END DATE ***
        val calendar = Calendar.getInstance()
        calendar.time = selectedDepartureTimestamp.toDate()
        // Subtract 1 because a 3-day trip is start date + 2 days
        calendar.add(Calendar.DAY_OF_YEAR, packageDetail.travelPackage.durationDays - 1)
        val endBookingDate = Timestamp(calendar.time)

        val noOfAdults = paxCounts["Adult"] ?: 0
        val noOfChildren = paxCounts["Child"] ?: 0
        val totalTravelers = noOfAdults + noOfChildren

        val newBooking = Booking(
            bookingId = UUID.randomUUID().toString(),
            userId = userId,
            packageId = packageId,
            departureId = departureId,
            paymentId = paymentId,
            noOfAdults = noOfAdults,
            noOfChildren = noOfChildren,
            totalTravelerCount = totalTravelers,
            totalAmount = state.totalPrice,
            subtotal = state.totalPrice, // You might have a different logic for subtotal vs total
            startBookingDate = selectedDepartureTimestamp,
            endBookingDate = endBookingDate, // Use the calculated end date
            createdAt = Timestamp.now(),
            updatedAt = Timestamp.now(),
            status = BookingStatus.PAID
        )

        // This call will now work correctly because the newBooking object is complete
        return bookingRepository.createBookingFromDirectPurchase(newBooking, packageId, departureId, totalTravelers)
            .map { listOf(it) } // Wrap single ID in a list to match function signature
    }

    private suspend fun createBookingsFromCart(userId: String, paymentId: String): Result<List<String>> {
        val cartId: String? = savedStateHandle.get("cartId")
        // Get the new argument again
        val selectedItemIdsJson: String? = savedStateHandle.get("selectedItemIdsJson")

        if (cartId == null || selectedItemIdsJson == null) {
            return Result.failure(Exception("Cart details for booking are missing."))
        }

        // Deserialize the list of selected IDs
        val decodedJson = URLDecoder.decode(selectedItemIdsJson, "UTF-8")
        val type = object : TypeToken<List<String>>() {}.type
        val selectedItemIds: List<String> = Gson().fromJson(decodedJson, type)

        // Fetch ONLY the selected cart items to create bookings for
        val cartItemsToBook = cartRepository.getCartItemsForCart(selectedItemIds).getOrDefault(emptyList())

        // The createBookingsFromCart function needs to be updated to not clear the whole cart
        // For now, let's proceed, but this is a potential future improvement.
        return bookingRepository.createBookingsFromCart(userId, cartId, cartItemsToBook, paymentId)
    }
}

private fun isExpiryDateValid(expiryDate: String): Boolean {
    if (expiryDate.length != 4) return false

    val month = expiryDate.substring(0, 2).toIntOrNull()
    val year = expiryDate.substring(2, 4).toIntOrNull()

    if (month == null || year == null || month !in 1..12) return false

    val calendar = Calendar.getInstance()
    val currentYear = calendar.get(Calendar.YEAR) % 100 // Get last two digits of the year (e.g., 24 for 2024)
    val currentMonth = calendar.get(Calendar.MONTH) + 1 // Calendar months are 0-11

    if (year < currentYear) return false // Year is in the past
    if (year == currentYear && month < currentMonth) return false // Same year, but month is in the past

    return true
}

// Helper enum for clarity
enum class CheckoutMode {
    DIRECT_BUY, FROM_CART
}