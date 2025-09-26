package com.example.mad_assignment.ui.managebooking

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mad_assignment.data.model.Booking
import com.example.mad_assignment.data.model.BookingStatus
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ManageBookingScreen(
    viewModel: ManageBookingViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
    ) {
        // Header
        EnhancedBookingHeader(
            title = "Manage Bookings",
            subtitle = when (val state = uiState) {
                is ManageBookingUiState.Success -> "${state.bookings.size} total bookings"
                else -> ""
            },
            onNavigateBack = onNavigateBack
        )

        when (val state = uiState) {
            is ManageBookingUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFF6366F1)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Loading bookings...",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            is ManageBookingUiState.Error -> {
                ErrorState(
                    message = state.message,
                    onRetry = { viewModel.loadBookings() }
                )
            }

            is ManageBookingUiState.Success -> {
                ManageBookingContent(
                    bookings = state.bookings,
                    selectedFilter = state.selectedFilter,
                    onFilterChanged = viewModel::filterBookings,
                    viewModel = viewModel,
                    showEditDialog = state.showEditDialog,
                    showDeleteDialog = state.showDeleteDialog,
                    selectedBooking = state.selectedBooking,
                    onEditBookingClicked = viewModel::onEditBookingClicked,
                    onDeleteBookingClicked = viewModel::onDeleteBookingClicked,
                    onDismissDialog = viewModel::onDismissDialog
                )
            }
        }
    }
}


@Composable
private fun EnhancedBookingHeader(
    title: String,
    subtitle: String,
    onNavigateBack: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(25.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF3F4F6))
            ) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color(0xFF374151),
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111827)
                )
                if (subtitle.isNotEmpty()) {
                    Text(
                        text = subtitle,
                        fontSize = 14.sp,
                        color = Color(0xFF6B7280),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.ErrorOutline,
            contentDescription = null,
            tint = Color(0xFFEF4444),
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Something went wrong",
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF111827)
        )

        Text(
            text = message,
            fontSize = 14.sp,
            color = Color(0xFF6B7280),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF6366F1)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Refresh,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Try Again")
        }
    }
}

@Composable
private fun ManageBookingContent(
    bookings: List<Booking>,
    selectedFilter: BookingStatus?,
    onFilterChanged: (BookingStatus?) -> Unit,
    viewModel: ManageBookingViewModel,
    showEditDialog: Boolean,
    showDeleteDialog: Boolean,
    selectedBooking: Booking?,
    onEditBookingClicked: (Booking) -> Unit,
    onDeleteBookingClicked: (Booking) -> Unit,
    onDismissDialog: () -> Unit
) {

    val filteredBookings = if (selectedFilter != null) {
        bookings.filter { it.status == selectedFilter }
    } else {
        bookings
    }

    Column {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                // Filter Section
                Column {
                    Text(
                        text = "Filter Bookings by Status",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF374151),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            EnhancedFilterChip(
                                label = "All",
                                count = bookings.size,
                                selected = selectedFilter == null,
                                onClick = { onFilterChanged(null) }
                            )
                        }
                        BookingStatus.values().forEach { status ->
                            item {
                                EnhancedFilterChip(
                                    label = status.name.lowercase().replaceFirstChar { it.titlecase() },
                                    count = bookings.count { it.status == status },
                                    selected = selectedFilter == status,
                                    onClick = { onFilterChanged(status) }
                                )
                            }
                        }
                    }
                }
            }

            if (filteredBookings.isEmpty()) {
                item {
                    EmptyState(
                        filter = selectedFilter
                    )
                }
            } else {
                items(
                    items = filteredBookings,
                    key = { booking -> booking.bookingId }
                ) { booking ->
                    EnhancedBookingCard(
                        booking = booking,
                        onEdit = {
                            onEditBookingClicked(booking)
                        },
                        onDelete = {
                            onDeleteBookingClicked(booking)
                        }
                    )
                }
            }
        }
    }


    // Dialogs
    if (showEditDialog && selectedBooking != null) {
        EnhancedEditBookingDialog(
            booking = selectedBooking,
            onDismiss = onDismissDialog,
            onConfirm = { updatedBooking ->
                viewModel.updateBookingInfo(updatedBooking)
            }
        )
    }

    if (showDeleteDialog && selectedBooking != null) {
        DeleteConfirmationDialog(
            booking = selectedBooking,
            onDismiss = onDismissDialog,
            onConfirm = {
                viewModel.deleteBooking(selectedBooking.bookingId)
            }
        )
    }
}


@Composable
private fun EnhancedFilterChip(
    label: String,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    val containerColor = if (selected) Color(0xFF6366F1) else Color.White
    val labelColor = if (selected) Color.White else Color(0xFF6B7280)
    val borderColor = if (selected) Color(0xFF6366F1) else Color(0xFFE5E7EB)

    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = "$label ($count)",
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = containerColor,
            labelColor = labelColor,
            selectedContainerColor = containerColor,
            selectedLabelColor = labelColor
        ),
        border = BorderStroke(1.dp, borderColor)
    )
}

@Composable
private fun EnhancedBookingCard(
    booking: Booking,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    fun formatDate(timestamp: Timestamp?): String {
        return timestamp?.toDate()?.let {
            SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(it)
        } ?: "N/A"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // Header Row: Booking ID and Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Booking ID",
                        fontSize = 12.sp,
                        color = Color(0xFF6B7280)
                    )
                    Text(
                        text = booking.bookingId,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF111827),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Badge(
                    containerColor = when (booking.status) {
                        BookingStatus.CONFIRMED, BookingStatus.PAID -> Color(0xFFDCFCE7)
                        BookingStatus.PENDING -> Color(0xFFFEF3C7)
                        BookingStatus.COMPLETED -> Color(0xFFE0E7FF)
                        BookingStatus.CANCELLED, BookingStatus.REFUNDED -> Color(0xFFFEE2E2)
                    },
                    contentColor = when (booking.status) {
                        BookingStatus.CONFIRMED, BookingStatus.PAID -> Color(0xFF166534)
                        BookingStatus.PENDING -> Color(0xFF92400E)
                        BookingStatus.COMPLETED -> Color(0xFF4338CA)
                        BookingStatus.CANCELLED, BookingStatus.REFUNDED -> Color(0xFF991B1B)
                    },
                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                ) {
                    Text(
                        text = booking.status.name.lowercase().replaceFirstChar { it.titlecase() },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Divider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFF3F4F6))

            // Details Section
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp) // Adds space between each row
            ) {
                // Row 1: IDs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    InfoItem(
                        label = "User ID",
                        value = booking.userId,
                        modifier = Modifier.weight(1f) // Ensures items can shrink if needed
                    )
                    InfoItem(
                        label = "Package ID",
                        value = booking.packageId,
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.End // Aligns this item to the right
                    )
                }

                // Row 2: Dates
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    InfoItem(
                        label = "Start Date",
                        value = formatDate(booking.startBookingDate)
                    )
                    InfoItem(
                        label = "End Date",
                        value = formatDate(booking.endBookingDate),
                        horizontalAlignment = Alignment.End
                    )
                }

                // Row 3: Counts and Cost
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    InfoItem(
                        label = "Travelers",
                        value = booking.totalTravelerCount.toString()
                    )
                    InfoItem(
                        label = "Total Amount",
                        value = "RM${String.format(Locale.US, "%.2f", booking.totalAmount)}",
                        horizontalAlignment = Alignment.End
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF3F4F6))
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit booking",
                        tint = Color(0xFF6B7280),
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFEE2E2))
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete booking",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start
) {
    Column(
        modifier = modifier,
        horizontalAlignment = horizontalAlignment
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color(0xFF6B7280)
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF374151),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun EmptyState(
    filter: BookingStatus?,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Filled.EventBusy,
            contentDescription = null,
            tint = Color(0xFFD1D5DB),
            modifier = Modifier.size(80.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (filter != null) "No ${filter.name.lowercase()} bookings" else "No bookings found",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF374151)
        )
        Text(
            text = if (filter != null) "Try selecting a different status" else "There are currently no bookings to display",
            fontSize = 14.sp,
            color = Color(0xFF6B7280),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
        }
    }


@Composable
private fun DeleteConfirmationDialog(
    booking: Booking,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = null,
                tint = Color(0xFFEF4444),
                modifier = Modifier.size(24.dp)
            )
        },
        title = {
            Text(
                text = "Delete Booking",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF111827)
            )
        },
        text = {
            Text(
                text = "Are you sure you want to delete booking ${booking.bookingId}? This action cannot be undone.",
                fontSize = 16.sp,
                color = Color(0xFF6B7280)
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFEF4444)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Delete", color = Color.White)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFF6B7280)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
private fun EnhancedEditBookingDialog(
    booking: Booking,
    onDismiss: () -> Unit,
    onConfirm: (Booking) -> Unit
) {
    var status by remember { mutableStateOf(booking.status) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Edit Booking Status",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF111827)
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Select the new status for booking ID: ${booking.bookingId}",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                // Status Selection Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BookingStatus.values().forEach { statusOption ->
                        FilterChip(
                            selected = status == statusOption,
                            onClick = { status = statusOption },
                            label = {
                                Text(
                                    text = statusOption.name.lowercase().replaceFirstChar { it.titlecase() },
                                    fontSize = 14.sp
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF6366F1),
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(booking.copy(status = status))
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6366F1)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = Color(0xFF6B7280)
                )
            ) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}