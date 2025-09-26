package com.example.mad_assignment.ui.booking

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
fun UpcomingBookingsScreen(
    onBookingDetailsClick: (String) -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
    val viewModel: BookingViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Upcoming Bookings", // <-- Title Changed
                        style = MaterialTheme.typography.headlineMedium,
                        fontSize = 20.sp, // Adjusted size for longer title
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
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
                is BookingUiState.Error -> {
                    ErrorContent(
                        message = (uiState as BookingUiState.Error).message,
                        onRetry = { viewModel.refreshBookings() }
                    )
                }
                is BookingUiState.Success -> {
                    val successState = uiState as BookingUiState.Success

                    if (successState.upcomingBookings.isEmpty()) {
                        EmptyUpcomingContent()
                    } else {
                        UpcomingBookingsContent(
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
                    }

                    // Dialog logic remains the same
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
                // The original Empty state might mean "no bookings at all".
                // We handle the "no upcoming bookings" case specifically above.
                is BookingUiState.Empty -> {
                    EmptyUpcomingContent()
                }
            }
        }
    }
}

/**
 * A simplified content composable that only displays a list of upcoming bookings.
 */
@Composable
private fun UpcomingBookingsContent(
    state: BookingUiState.Success,
    onBookingDetailsClick: (Booking) -> Unit,
    onQuickViewClick: (String) -> Unit,
    onCancelBooking: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 24.dp, bottom = 24.dp)
    ) {
        items(state.upcomingBookings, key = { it.bookingId }) { booking ->
            BookingCard(
                booking = booking,
                packageDetails = state.packages[booking.packageId],
                onClick = { onBookingDetailsClick(booking) },
                onQuickViewClick = { onQuickViewClick(booking.bookingId) },
                onCancelBooking = { onCancelBooking(booking.bookingId) }
            )
        }
    }
}

/**
 * A new empty state specific to having no UPCOMING bookings.
 */
@Composable
private fun EmptyUpcomingContent() {
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
                Icons.Outlined.EventBusy, // <-- More relevant icon
                contentDescription = null,
                modifier = Modifier.size(120.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "No Upcoming Trips", // <-- Updated Title
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "You don't have any trips scheduled. Time to plan a new adventure!", // <-- Updated Message
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
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

