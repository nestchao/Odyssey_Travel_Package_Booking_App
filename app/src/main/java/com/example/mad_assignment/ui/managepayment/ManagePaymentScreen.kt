package com.example.mad_assignment.ui.managepayment

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mad_assignment.data.model.Payment
import com.example.mad_assignment.data.model.PaymentStatus
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ManagePaymentScreen(
    viewModel: ManagePaymentViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFF8FAFC),
                        Color(0xFFF1F5F9)
                    )
                )
            )
    ) {
        // Enhanced Header with gradient background
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            shadowElevation = 4.dp
        ) {
            AdminScreenHeader(
                title = "Payment Management",
                subtitle = "Manage and track all payment transactions",
                onNavigateBack = onNavigateBack
            )
        }

        when (val state = uiState) {
            is ManagePaymentUiState.Loading -> {
                LoadingState()
            }

            is ManagePaymentUiState.Error -> {
                ErrorState(
                    message = state.message,
                    onRetry = { viewModel.loadPayments() }
                )
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
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                strokeWidth = 4.dp,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Loading payments...",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }
    }
}

@Composable
private fun ErrorState(
    message: String?,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = null,
            tint = Color.Red,
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Oops! Something went wrong",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = message ?: "Error loading payments",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        FilledTonalButton(
            onClick = onRetry,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Try Again")
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

    Column(modifier = Modifier.fillMaxSize()) {
        // Enhanced Statistics Cards
        PaymentStatsRow(payments = payments)

        // Enhanced Filter Section
        EnhancedFilterSection(
            selectedFilter = selectedFilter,
            onFilterChanged = onFilterChanged,
            totalCount = payments.size,
            filteredCount = filteredPayments.size
        )

        // Payments List
        if (filteredPayments.isEmpty()) {
            EmptyState(selectedFilter = selectedFilter)
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredPayments) { payment ->
                    EnhancedPaymentCard(
                        payment = payment,
                        onStatusUpdate = onStatusUpdate
                    )
                }

                // Bottom padding
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun PaymentStatsRow(payments: List<Payment>) {
    val totalAmount = payments.sumOf { it.amount }
    val successfulPayments = payments.count { it.status == PaymentStatus.SUCCESS }
    val pendingPayments = payments.count { it.status == PaymentStatus.PENDING }

    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            StatsCard(
                title = "Total Revenue",
                value = formatCurrency(totalAmount),
                icon = Icons.Default.AttachMoney,
                color = Color(0xFF10B981),
                backgroundColor = Color(0xFFECFDF5)
            )
        }
        item {
            StatsCard(
                title = "Successful",
                value = successfulPayments.toString(),
                icon = Icons.Default.CheckCircle,
                color = Color(0xFF059669),
                backgroundColor = Color(0xFFD1FAE5)
            )
        }
        item {
            StatsCard(
                title = "Pending",
                value = pendingPayments.toString(),
                icon = Icons.Default.Schedule,
                color = Color(0xFFF59E0B),
                backgroundColor = Color(0xFFFEF3C7)
            )
        }
    }
}

@Composable
private fun StatsCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    backgroundColor: Color
) {
    Card(
        modifier = Modifier
            .width(140.dp)
            .height(80.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = color.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EnhancedFilterSection(
    selectedFilter: PaymentStatus?,
    onFilterChanged: (PaymentStatus?) -> Unit,
    totalCount: Int,
    filteredCount: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Filter Payments",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "$filteredCount of $totalCount",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedFilter == null,
                        onClick = { onFilterChanged(null) },
                        label = { Text("All") },
                        leadingIcon = if (selectedFilter == null) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
                PaymentStatus.values().forEach { status ->
                    item {
                        FilterChip(
                            selected = selectedFilter == status,
                            onClick = { onFilterChanged(status) },
                            label = { Text(status.name) },
                            leadingIcon = if (selectedFilter == status) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = getStatusColor(status),
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(selectedFilter: PaymentStatus?) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Receipt,
            contentDescription = null,
            tint = Color.Gray.copy(alpha = 0.5f),
            modifier = Modifier.size(80.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (selectedFilter != null) "No ${selectedFilter.name.lowercase()} payments found" else "No payments found",
            style = MaterialTheme.typography.titleMedium,
            color = Color.Gray
        )

        Text(
            text = "Payments will appear here once they are created",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray.copy(alpha = 0.7f),
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EnhancedPaymentCard(
    payment: Payment,
    onStatusUpdate: (String, PaymentStatus) -> Unit
) {
    var showStatusMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Color.Black.copy(alpha = 0.1f)
            ),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Header with Payment ID and Amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "ID: ${payment.paymentId}",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.Gray,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                    Text(
                        text = formatCurrency(payment.amount),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                // Status Button with enhanced styling
                Box {
                    Button(
                        onClick = { showStatusMenu = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = getStatusColor(payment.status)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = getStatusIcon(payment.status),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = payment.status.name,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    DropdownMenu(
                        expanded = showStatusMenu,
                        onDismissRequest = { showStatusMenu = false },
                        modifier = Modifier
                            .background(Color.White)
                            .border(1.dp, Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                    ) {
                        PaymentStatus.values().forEach { status ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = getStatusIcon(status),
                                            contentDescription = null,
                                            tint = getStatusColor(status),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(status.name)
                                    }
                                },
                                onClick = {
                                    onStatusUpdate(payment.paymentId, status)
                                    showStatusMenu = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Payment Details with improved layout
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PaymentDetailRow(
                        label = "Payment Method",
                        value = payment.paymentMethod ?: "N/A",
                        icon = Icons.Default.CreditCard
                    )

                    PaymentDetailRow(
                        label = "Created",
                        value = payment.createdAt?.toDate()?.formatToPattern("MMM dd, yyyy HH:mm") ?: "N/A",
                        icon = Icons.Default.Schedule
                    )

                    if (payment.gatewayTransactionId != null) {
                        PaymentDetailRow(
                            label = "Transaction ID",
                            value = payment.gatewayTransactionId!!,
                            icon = Icons.Default.Receipt,
                            isMonospace = true
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PaymentDetailRow(
    label: String,
    value: String,
    icon: ImageVector,
    isMonospace: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.Gray,
            modifier = Modifier.size(16.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = if (isMonospace) androidx.compose.ui.text.font.FontFamily.Monospace else androidx.compose.ui.text.font.FontFamily.Default,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun AdminScreenHeader(
    title: String,
    subtitle: String = "",
    onNavigateBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        Color.Gray.copy(alpha = 0.1f),
                        RoundedCornerShape(12.dp)
                    )
            ) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.Black
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = title,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                if (subtitle.isNotEmpty()) {
                    Text(
                        text = subtitle,
                        fontSize = 14.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}

// Utility functions
private fun getStatusColor(status: PaymentStatus): Color {
    return when (status) {
        PaymentStatus.PENDING -> Color(0xFFF59E0B)
        PaymentStatus.SUCCESS -> Color(0xFF10B981)
        PaymentStatus.FAILED -> Color(0xFFEF4444)
        PaymentStatus.REFUNDED -> Color(0xFF6B7280)
    }
}

private fun getStatusIcon(status: PaymentStatus): ImageVector {
    return when (status) {
        PaymentStatus.PENDING -> Icons.Default.Schedule
        PaymentStatus.SUCCESS -> Icons.Default.CheckCircle
        PaymentStatus.FAILED -> Icons.Default.Cancel
        PaymentStatus.REFUNDED -> Icons.Default.Undo
    }
}

private fun formatCurrency(amount: Double): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale("en", "MY"))
    return formatter.format(amount).replace("MYR", "RM")
}

fun Date.formatToPattern(pattern: String): String {
    val formatter = SimpleDateFormat(pattern, Locale.getDefault())
    return formatter.format(this)
}