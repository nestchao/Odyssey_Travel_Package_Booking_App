package com.example.mad_assignment.ui.managepayment

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mad_assignment.data.model.Payment
import com.example.mad_assignment.data.model.PaymentStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ManagePaymentScreen(
    viewModel: ManagePaymentViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        AdminScreenHeader(
            title = "Manage Payments",
            onNavigateBack = onNavigateBack
        )

        when (val state = uiState) {
            is ManagePaymentUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is ManagePaymentUiState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(text = "Error loading payments")
                    Button(onClick = { viewModel.loadPayments() }) {
                        Text("Retry")
                    }
                }
            }

            is ManagePaymentUiState.Success -> {
                ManagePaymentContent(
                    payments = state.payments,
                    selectedFilter = state.selectedFilter,
                    onFilterChanged = viewModel::filterPayments,
                    onStatusUpdate = viewModel::updatePaymentStatus
                )
            }
        }
    }
}

@Composable
private fun ManagePaymentContent(
    payments: List<Payment>,
    selectedFilter: PaymentStatus?,
    onFilterChanged: (PaymentStatus?) -> Unit,
    onStatusUpdate: (String, PaymentStatus) -> Unit
) {
    val filteredPayments = if (selectedFilter != null) {
        payments.filter { it.status == selectedFilter }
    } else {
        payments
    }
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Filter chips in a light purple background
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF3F4F6))
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedFilter == null,
                    onClick = { onFilterChanged(null) },
                    label = { Text("All") },
                    modifier = Modifier.clip(RoundedCornerShape(16.dp))
                )
            }
            PaymentStatus.values().forEach { status ->
                item {
                    FilterChip(
                        selected = selectedFilter == status,
                        onClick = { onFilterChanged(status) },
                        label = { Text(status.name) },
                        modifier = Modifier.clip(RoundedCornerShape(16.dp))
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(filteredPayments.size) { index ->
                PaymentCard(
                    payment = filteredPayments[index],
                    onStatusUpdate = onStatusUpdate
                )
            }
        }
    }
}

@Composable
private fun PaymentCard(
    payment: Payment,
    onStatusUpdate: (String, PaymentStatus) -> Unit
) {
    var showStatusMenu by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Payment ID: ${payment.paymentId}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Amount: $${payment.amount}",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = "Method: ${payment.paymentMethod}",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )

                    Box {
                        Button(
                            onClick = { showStatusMenu = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = when (payment.status) {
                                    PaymentStatus.PENDING -> Color(0xFFFFA500)
                                    PaymentStatus.SUCCESS -> Color(0xFF10B981)
                                    PaymentStatus.FAILED -> Color(0xFFEF4444)
                                    PaymentStatus.REFUNDED -> Color(0xFF6B7280)
                                }
                            )
                        ) {
                            Text(payment.status.name)
                        }

                        DropdownMenu(
                            expanded = showStatusMenu,
                            onDismissRequest = { showStatusMenu = false }
                        ) {
                            PaymentStatus.values().forEach { status ->
                                DropdownMenuItem(
                                    text = { Text(status.name) },
                                    onClick = {
                                        onStatusUpdate(payment.paymentId, status)
                                        showStatusMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Additional payment details
            Text(
                text = "Created: ${payment.createdAt?.toDate()?.formatToPattern("MMM dd HH:mm:ss") ?: "N/A"}",
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.padding(top = 8.dp)
            )

            if (payment.gatewayTransactionId != null) {
                Text(
                    text = "Transaction ID: ${payment.gatewayTransactionId}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

fun Date.formatToPattern(pattern: String): String {
    val formatter = SimpleDateFormat(pattern, Locale.getDefault())
    return formatter.format(this)
}

@Composable
fun AdminScreenHeader(
    title: String,
    onNavigateBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onNavigateBack,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.Black
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = title,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
    }
}