package com.example.mad_assignment.ui.checkout

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.mad_assignment.util.toDataUri
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    onNavigateBack: () -> Unit,
    onPaymentSuccess: () -> Unit
) {
    val viewModel: CheckoutViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val checkoutResult by viewModel.checkoutResult.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var cardholderName by rememberSaveable { mutableStateOf("") }
    var cardNumber by rememberSaveable { mutableStateOf("") }
    var expiryDate by rememberSaveable { mutableStateOf("") }
    var cvc by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(checkoutResult) {
        when (val result = checkoutResult) {
            is CheckoutResultEvent.Success -> {
                Toast.makeText(context, "Payment Successful!", Toast.LENGTH_LONG).show()
                onPaymentSuccess()
            }
            is CheckoutResultEvent.Failure -> {
                Toast.makeText(context, "Error: ${result.message}", Toast.LENGTH_LONG).show()
            }
            null -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Confirm & Pay") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        contentWindowInsets = WindowInsets(0,0,0,0)
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when (val state = uiState) {
                is CheckoutUiState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                is CheckoutUiState.Error -> Text(
                    text = "Error: ${state.message}",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center).padding(16.dp)
                )
                is CheckoutUiState.Success -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        state.displayItems.forEach { item ->
                            PackageSummaryCard(item)
                        }

                        UserDetailsCard(state)

                        PaymentDetailsCard(
                            cardholderName = cardholderName,
                            onCardholderNameChange = { cardholderName = it },
                            cardNumber = cardNumber,
                            onCardNumberChange = { if (it.length <= 16) cardNumber = it },
                            expiryDate = expiryDate,
                            onExpiryDateChange = { if (it.length <= 4) expiryDate = it },
                            cvc = cvc,
                            onCvcChange = { if (it.length <= 3) cvc = it }
                        )

                        PriceSummaryCard(state)

                        Spacer(Modifier.weight(1f))

                        ConfirmButton(
                            state = state,
                            onConfirmAndPay = {
                                val isFormValid = cardholderName.isNotBlank() &&
                                        cardNumber.length == 16 &&
                                        expiryDate.length == 4 &&
                                        cvc.length == 3

                                if (isFormValid) {
                                    viewModel.onConfirmAndPay("Credit Card")
                                } else {
                                    Toast.makeText(context, "Please fill all payment details correctly.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PackageSummaryCard(item: CheckoutDisplayItem) {
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    Card(
        modifier = Modifier.padding(16.dp).fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(toDataUri(item.imageUri)).build(),
//                model = ImageRequest.Builder(LocalContext.current).data(item.imageUri).crossfade(true).build(),
                contentDescription = "Package Image",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(180.dp)
            )
            Column(Modifier.padding(16.dp)) {
                Text(item.packageName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                InfoRow(icon = Icons.Default.LocationOn, text = item.location)
                Spacer(Modifier.height(4.dp))
                item.departureDate?.let {
                    InfoRow(icon = Icons.Default.CalendarToday, text = "Departure: ${sdf.format(it.toDate())}")
                }
                Spacer(Modifier.height(4.dp))
                InfoRow(icon = Icons.Default.People, text = item.paxInfo)
            }
        }
    }
}

@Composable
private fun PriceSummaryCard(state: CheckoutUiState.Success) {
    Card(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Price Breakdown", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

            state.displayItems.forEach { item ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = item.packageName, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Text(text = "RM ${"%.2f".format(item.price)}", style = MaterialTheme.typography.bodyMedium)
                }
            }

            Divider()
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Total Price", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                Text(
                    text = "RM ${"%.2f".format(state.totalPrice)}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

// ... Keep UserDetailsCard, PaymentDetailsCard, InfoRow, and ConfirmButton as they were in the previous step. They are correct.
@Composable
private fun UserDetailsCard(state: CheckoutUiState.Success) {
    Card(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Contact Details",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Divider()
            InfoRow(
                icon = Icons.Default.Person,
                text = "${state.user.firstName} ${state.user.lastName}"
            )
            InfoRow(
                icon = Icons.Default.Email,
                text = state.user.userEmail
            )
            InfoRow(
                icon = Icons.Default.Phone,
                text = state.user.userPhoneNumber
            )
        }
    }
}


@Composable
private fun PaymentDetailsCard(
    cardholderName: String,
    onCardholderNameChange: (String) -> Unit,
    cardNumber: String,
    onCardNumberChange: (String) -> Unit,
    expiryDate: String,
    onExpiryDateChange: (String) -> Unit,
    cvc: String,
    onCvcChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Payment Details",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Divider()

            // Cardholder Name
            OutlinedTextField(
                value = cardholderName,
                onValueChange = onCardholderNameChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Cardholder Name") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                singleLine = true
            )

            // Card Number
            OutlinedTextField(
                value = cardNumber,
                onValueChange = onCardNumberChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Card Number") },
                leadingIcon = { Icon(Icons.Default.CreditCard, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                visualTransformation = CreditCardVisualTransformation(),
                singleLine = true
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Expiry Date
                OutlinedTextField(
                    value = expiryDate,
                    onValueChange = onExpiryDateChange,
                    modifier = Modifier.weight(1f),
                    label = { Text("Expiry (MMYY)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    visualTransformation = ExpiryDateVisualTransformation(),
                    singleLine = true
                )

                // CVC
                OutlinedTextField(
                    value = cvc,
                    onValueChange = onCvcChange,
                    modifier = Modifier.weight(1f),
                    label = { Text("CVC") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    singleLine = true
                )
            }
        }
    }
}


@Composable
private fun InfoRow(icon: ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}


@Composable
private fun ConfirmButton(state: CheckoutUiState.Success, onConfirmAndPay: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 8.dp
    ) {
        Button(
            onClick = onConfirmAndPay,
            enabled = !state.isProcessingPayment,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            if (state.isProcessingPayment) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = "Confirm & Pay RM ${"%.2f".format(state.totalPrice)}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}