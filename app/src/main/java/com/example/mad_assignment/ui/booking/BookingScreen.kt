package com.example.mad_assignment.ui.booking

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.mad_assignment.data.model.Booking
import com.example.mad_assignment.data.model.BookingStatus
import com.example.mad_assignment.data.model.BookingType
import com.example.mad_assignment.data.model.TravelPackageWithImages
import com.example.mad_assignment.util.toDataUri
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingsScreen(
    onBookingDetailsClick: (String) -> Unit = {}
) {
    val viewModel: BookingViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Bookings",
                        style = MaterialTheme.typography.headlineMedium,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = "Calendar Icon",
                        modifier = Modifier.padding(end = 16.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (uiState) {
                is BookingUiState.Loading -> {
                    LoadingContent()
                }
                is BookingUiState.Empty -> {
                    EmptyBookingsContent()
                }
                is BookingUiState.Error -> {
                    ErrorContent(
                        message = (uiState as BookingUiState.Error).message,
                        onRetry = { viewModel.refreshBookings() }
                    )
                }
                is BookingUiState.Success -> {
                    val successState = uiState as BookingUiState.Success
                    BookingsContent(
                        state = successState,
                        onBookingDetailsClick = { booking ->
                            onBookingDetailsClick(booking.packageId)
                        },
                        onQuickViewClick = { bookingId ->
                            viewModel.showBookingDetails(bookingId)
                        },
                        onCancelBooking = { bookingId ->
                            viewModel.cancelBooking(bookingId)
                        }
                    )

                    if (successState.showBookingDetails && successState.selectedBookingId != null) {
                        BookingDetailsDialog(
                            booking = successState.selectedBooking!!,
                            packageDetails = successState.packages[successState.selectedBooking!!.packageId],
                            onDismiss = { viewModel.hideBookingDetails() },
                            onCancelBooking = { bookingId ->
                                viewModel.cancelBooking(bookingId)
                                viewModel.hideBookingDetails()
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BookingsContent(
    state: BookingUiState.Success,
    onBookingDetailsClick: (Booking) -> Unit,
    onQuickViewClick: (String) -> Unit,
    onCancelBooking: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        // Current Bookings
        if (state.hasCurrentBookings) {
            item {
                BookingSection(
                    title = "Current",
                    bookings = state.currentBookings,
                    packages = state.packages,
                    onBookingClick = onBookingDetailsClick,
                    onQuickViewClick = onQuickViewClick,
                    onCancelBooking = onCancelBooking,
                    onViewAllClick = { /* Handle View All for current */ }
                )
            }
        }

        // Upcoming Bookings
        if (state.hasUpcomingBookings) {
            item {
                BookingSection(
                    title = "Upcoming",
                    bookings = state.upcomingBookings,
                    packages = state.packages,
                    onBookingClick = onBookingDetailsClick,
                    onQuickViewClick = onQuickViewClick,
                    onCancelBooking = onCancelBooking,
                    onViewAllClick = { /* Handle View All for upcoming */ }
                )
            }
        }

        // Past Bookings
        if (state.hasPastBookings) {
            item {
                BookingSection(
                    title = "Past",
                    bookings = state.pastBookings.take(3), // Show only first 3 items
                    packages = state.packages,
                    onBookingClick = onBookingDetailsClick,
                    onQuickViewClick = onQuickViewClick,
                    onCancelBooking = onCancelBooking,
                    onViewAllClick = { /* Handle View All for past */ },
                    showViewAll = state.pastBookings.size > 3
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun BookingSection(
    title: String,
    bookings: List<Booking>,
    packages: Map<String, TravelPackageWithImages?>,
    onBookingClick: (Booking) -> Unit,
    onQuickViewClick: (String) -> Unit,
    onCancelBooking: (String) -> Unit,
    onViewAllClick: () -> Unit,
    showViewAll: Boolean = false
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )

            if (showViewAll) {
                Text(
                    text = "View All →",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable { onViewAllClick() }
                )
            }
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            bookings.forEach { booking ->
                BookingCard(
                    booking = booking,
                    packageDetails = packages[booking.packageId],
                    onClick = { onBookingClick(booking) },
                    onQuickViewClick = { onQuickViewClick(booking.bookingId) },
                    onCancelBooking = { onCancelBooking(booking.bookingId) }
                )
            }
        }
    }
}

@Composable
private fun BookingCard(
    booking: Booking,
    packageDetails: TravelPackageWithImages?,
    onClick: () -> Unit,
    onQuickViewClick: () -> Unit,
    onCancelBooking: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFC7C7C7) // Matching the grey from your image
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Background image if available
            if (!packageDetails?.images.isNullOrEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(toDataUri(packageDetails.images.firstOrNull()?.base64Data))
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                )
            }

            // Gradient overlay for better text readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.7f)
                            )
                        )
                    )
            )

            // Content
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.BottomStart
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = packageDetails?.travelPackage?.packageName
                                ?: packageDetails?.travelPackage?.location
                                ?: "Package ${booking.packageId.take(6)}...",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Status badge
                        Surface(
                            color = getStatusColor(booking.status).copy(alpha = 0.8f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = getStatusText(booking.status),
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Column(
                        horizontalAlignment = Alignment.End
                    ) {
                        if (booking.bookingType == BookingType.PAST) {
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = "View Details",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        } else if (booking.startBookingDate != null && booking.endBookingDate != null) {
                            Text(
                                text = formatDateRange(booking.startBookingDate, booking.endBookingDate),
                                color = Color.White,
                                fontSize = 14.sp,
                                textAlign = TextAlign.End,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Action buttons overlay (top right)
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Quick view button
                    IconButton(
                        onClick = onQuickViewClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Visibility,
                            contentDescription = "Quick View",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Cancel button (only for cancellable bookings)
                    if (isBookingCancellable(booking)) {
                        IconButton(
                            onClick = onCancelBooking,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Outlined.Cancel,
                                contentDescription = "Cancel Booking",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BookingDetailsDialog(
    booking: Booking,
    packageDetails: TravelPackageWithImages?,
    onDismiss: () -> Unit,
    onCancelBooking: (String) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp)
        ) {
            LazyColumn(
                modifier = Modifier.padding(24.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Booking Details",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )

                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Outlined.Close, contentDescription = "Close")
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }

                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            DetailRow("Booking ID", booking.bookingId)
                            DetailRow("Package", packageDetails?.travelPackage?.packageName ?: "Unknown")
                            DetailRow("Status", getStatusText(booking.status))
                            DetailRow("Total Amount", "RM ${"%.2f".format(booking.totalAmount)}")
                            DetailRow("Travelers", "${booking.totalTravelerCount} (${booking.noOfAdults} Adults, ${booking.noOfChildren} Children)")

                            if (booking.startBookingDate != null && booking.endBookingDate != null) {
                                DetailRow("Travel Dates", formatDateRange(booking.startBookingDate, booking.endBookingDate))
                            }

                            if (booking.createdAt != null) {
                                DetailRow("Booked On", booking.createdAt.formatDate())
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isBookingCancellable(booking)) {
                            Arrangement.spacedBy(12.dp)
                        } else {
                            Arrangement.Center
                        }
                    ) {
                        if (isBookingCancellable(booking)) {
                            OutlinedButton(
                                onClick = { onCancelBooking(booking.bookingId) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Text("Cancel Booking")
                            }
                        }

                        Button(
                            onClick = onDismiss,
                            modifier = if (isBookingCancellable(booking)) Modifier.weight(1f) else Modifier.fillMaxWidth()
                        ) {
                            Text("Close")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(0.4f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(0.6f),
                textAlign = TextAlign.End
            )
        }
    }
}

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 3.dp,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Loading your bookings...",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyBookingsContent() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Outlined.BookmarkBorder,
                contentDescription = null,
                modifier = Modifier.size(120.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "No bookings yet",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Your travel bookings will appear here once you make a purchase",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Outlined.ErrorOutline,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Something went wrong",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            OutlinedButton(
                onClick = onRetry,
                modifier = Modifier.height(40.dp)
            ) {
                Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Try Again")
            }
        }
    }
}

// Helper functions
private fun getStatusColor(status: BookingStatus): Color {
    return when (status) {
        BookingStatus.PENDING -> Color(0xFFFF9800) // Orange
        BookingStatus.CONFIRMED -> Color(0xFF2196F3) // Blue
        BookingStatus.PAID -> Color(0xFF4CAF50) // Green
        BookingStatus.CANCELLED -> Color(0xFFf44336) // Red
        BookingStatus.COMPLETED -> Color(0xFF9C27B0) // Purple
        BookingStatus.REFUNDED -> Color(0xFF607D8B) // Blue Grey
    }
}

private fun getStatusText(status: BookingStatus): String {
    return when (status) {
        BookingStatus.PENDING -> "Pending"
        BookingStatus.CONFIRMED -> "Confirmed"
        BookingStatus.PAID -> "Paid"
        BookingStatus.CANCELLED -> "Cancelled"
        BookingStatus.COMPLETED -> "Completed"
        BookingStatus.REFUNDED -> "Refunded"
    }
}

private fun isBookingCancellable(booking: Booking): Boolean {
    return booking.status in listOf(BookingStatus.PENDING, BookingStatus.CONFIRMED, BookingStatus.PAID) &&
            booking.bookingType in listOf(BookingType.UPCOMING, BookingType.CURRENT)
}

private fun formatDateRange(startDate: Timestamp, endDate: Timestamp): String {
    val sdf = SimpleDateFormat("dd MMM", Locale.getDefault())
    val yearFormat = SimpleDateFormat("yyyy", Locale.getDefault())

    val startFormatted = sdf.format(startDate.toDate())
    val endFormatted = sdf.format(endDate.toDate())
    val year = yearFormat.format(endDate.toDate())

    return "$startFormatted - $endFormatted $year"
}

// Extension function for timestamp formatting
fun Timestamp.formatDate(): String {
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    return sdf.format(this.toDate())
}