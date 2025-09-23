package com.example.mad_assignment.ui.packagedetail

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.mad_assignment.data.model.DepartureAndEndTime
import com.example.mad_assignment.data.model.TravelPackage
import com.example.mad_assignment.data.model.TravelPackageWithImages
import com.example.mad_assignment.data.model.Trip
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale
import com.example.mad_assignment.util.toDataUri
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.alpha

@Composable
fun PackageDetailScreen(onNavigateBack: () -> Unit) {
    val viewModel: PackageDetailViewModel = hiltViewModel()

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)) {
            when (val state = uiState) {
                is PackageDetailUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 3.dp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Loading package details...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                is PackageDetailUiState.Success -> {
                    val packageDetail = state.packageDetail
                    val actionBarHeight = 100.dp

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding(),
                        contentPadding = WindowInsets.navigationBars
                            .add(WindowInsets(bottom = actionBarHeight))
                            .asPaddingValues()
                    ) {
                        item {
                            EnhancedImageHeader(state.packageDetail, onNavigateBack)
                        }
                        item {
                            PackageTitle(state.packageDetail.travelPackage)
                        }
                        item {
                            EnhancedPackageInfoBar(
                                travelPackage = state.packageDetail.travelPackage,
                                itineraryTrips = state.itineraryTrips
                            )
                        }
                        item {
                            EnhancedDescriptionSection(state.packageDetail.travelPackage.packageDescription)
                        }
                        item {
                            EnhancedDateSelector(
                                departures = state.departures,
                                selectedDeparture = state.selectedDeparture,
                                onDateSelected = { departure -> viewModel.selectDeparture(departure) }
                            )
                        }
                        item {
                            EnhancedPaxSelector(
                                pricing = state.packageDetail.travelPackage.pricing,
                                paxCounts = state.paxCounts,
                                onPaxChanged = { category, change ->
                                    viewModel.updatePaxCount(category, change)
                                }
                            )
                        }
                        item {
                            EnhancedLocationSection(itineraryTrips = state.itineraryTrips)
                        }
                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }

                    EnhancedBookingActionBar(
                        state = state,
                        // TODO: pass the viewModel function (viewModel.addToCart()) here
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                    )
                }

                is PackageDetailUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Outlined.ErrorOutline,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Oops! Something went wrong",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 32.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun PackageTitle(travelPackage: TravelPackage) {
    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
        Text(
            text = travelPackage.packageName,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (travelPackage.packageDescription.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = travelPackage.packageDescription.take(100) + if (travelPackage.packageDescription.length > 100) "..." else "",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun EnhancedDateSelector(
    departures: List<DepartureAndEndTime>,
    selectedDeparture: DepartureAndEndTime?,
    onDateSelected: (DepartureAndEndTime) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 16.dp)) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Outlined.CalendarMonth,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Choose Your Travel Dates",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        if (departures.isEmpty()) {
            Text(
                text = "No departure dates available for this package.",
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(items = departures, key = { it.id }) { departure ->
                    val isSelected = departure.id == selectedDeparture?.id
                    val spotsLeft = departure.capacity
                    val isSoldOut = spotsLeft <= 0

                    val backgroundColor by animateColorAsState(
                        targetValue = when {
                            isSoldOut -> MaterialTheme.colorScheme.errorContainer
                            isSelected -> MaterialTheme.colorScheme.primaryContainer
                            else -> MaterialTheme.colorScheme.surface
                        },
                        animationSpec = tween(300), label = ""
                    )

                    val contentColor by animateColorAsState(
                        targetValue = when {
                            isSoldOut -> MaterialTheme.colorScheme.onErrorContainer
                            isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
                            else -> MaterialTheme.colorScheme.onSurface
                        },
                        animationSpec = tween(300), label = ""
                    )

                    Card(
                        onClick = { if (!isSoldOut) onDateSelected(departure) },
                        modifier = Modifier
                            .width(140.dp)
                            .shadow(if (isSelected) 8.dp else 2.dp, RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = backgroundColor),
                        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                        enabled = !isSoldOut
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = formatDate(departure.startDate),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = contentColor,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (isSoldOut) "Sold Out" else "$spotsLeft spots left",
                                style = MaterialTheme.typography.bodySmall,
                                color = contentColor.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatDate(start: Timestamp): String {
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    return sdf.format(start.toDate())
}

@Composable
fun EnhancedDescriptionSection(description: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Description,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "About This Package",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.4
            )
        }
    }
}

@Composable
fun EnhancedPackageInfoBar(
    travelPackage: TravelPackage,
    itineraryTrips: Map<Int, List<Trip>>
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            EnhancedInfoColumn(
                title = "DURATION",
                value = "${travelPackage.durationDays}D ${travelPackage.durationDays - 1}N",
                icon = Icons.Outlined.Schedule
            )
            VerticalDivider(
                modifier = Modifier.height(60.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )
            EnhancedInfoColumn(
                title = "LOCATION",
                value = travelPackage.location,
                icon = Icons.Outlined.LocationOn,
                iconColor = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
fun RowScope.EnhancedInfoColumn(
    title: String,
    value: String,
    icon: ImageVector,
    iconColor: Color = MaterialTheme.colorScheme.primary
) {
    Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

class CustomMapWebViewClient : WebViewClient() {
    private val TAG = "MapWebViewClient"

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        Log.d(TAG, "Page started loading: $url")
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        Log.d(TAG, "Page finished loading: $url")
    }

    override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
        super.onReceivedError(view, request, error)
        val errorMessage = "Error loading URL: ${request?.url} - Description: ${error?.description} (Code: ${error?.errorCode})"
        Log.e(TAG, errorMessage)
        // Optionally, display a Toast or a message in the UI
    }

    // This method is for API < 23
    @Suppress("DEPRECATION")
    override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
        super.onReceivedError(view, errorCode, description, failingUrl)
        val errorMessage = "Error loading URL (deprecated): $failingUrl - Description: $description (Code: $errorCode)"
        Log.e(TAG, errorMessage)
    }

    // Prevents URLs from opening in the default browser when clicked inside the WebView
    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        // Only load if it's the iframe source, otherwise handle as needed
        // For maps, usually we want to keep it in the WebView.
        return false // Allow WebView to load the URL
    }
}

@Composable
fun EnhancedLocationSection(itineraryTrips: Map<Int, List<Trip>>) {
    // --- DYNAMIC LOGIC: Find the very first trip of the whole itinerary ---
    // We use remember to avoid recalculating this on every recomposition.
    val previewTrip = remember(itineraryTrips) {
        if (itineraryTrips.isEmpty()) {
            null
        } else {
            // Find the earliest day (e.g., Day 1)
            val earliestDay = itineraryTrips.keys.minOrNull()
            // Get the first trip from that earliest day's list
            earliestDay?.let { day -> itineraryTrips[day]?.firstOrNull { it.geoPoint != null } }
        }
    }

    // --- DYNAMIC LOGIC: Create the iframe HTML from the found trip ---
    val mapHtml = if (previewTrip != null && previewTrip.geoPoint != null) {
        val lat = previewTrip.geoPoint.latitude
        val lon = previewTrip.geoPoint.longitude

        """
        <iframe
          src="https://maps.google.com/maps?q=@$lat,$lon&z=15&output=embed"
          width="600"
          height="450"
          style="border:0;"
          allowfullscreen
          loading="lazy">
        </iframe>
        """.trimIndent()
    } else {
        ""
    }

    val context = LocalContext.current

    Column(modifier = Modifier.padding(vertical = 16.dp)) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Outlined.Map, contentDescription = null,
                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Route & Locations",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            // Check if we found a valid trip with a location to show
            if (previewTrip != null && previewTrip.geoPoint != null) {
                Box {
                    AndroidView(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        factory = {
                            WebView(it).apply {
                                settings.javaScriptEnabled = true
                                webViewClient = WebViewClient()
                                loadDataWithBaseURL(null, mapHtml, "text/html", "UTF-8", null)
                            }
                        },
                        // Update block to reload if the underlying data changes
                        update = {
                            it.loadDataWithBaseURL(null, mapHtml, "text/html", "UTF-8", null)
                        }
                    )

                    ExtendedFloatingActionButton(
                        onClick = {
                            // --- DYNAMIC LOGIC: Build route from ALL itinerary trips ---
                            val allTripsSorted = itineraryTrips.keys.sorted()
                                .flatMap { day -> itineraryTrips[day].orEmpty() }
                                .filter { it.geoPoint != null } // Only use trips that have a location

                            if (allTripsSorted.isEmpty()) {
                                Toast.makeText(context, "No locations available for this route.", Toast.LENGTH_SHORT).show()
                                return@ExtendedFloatingActionButton
                            }

                            val origin = allTripsSorted.first()
                            val finalDestination = allTripsSorted.last()

                            // Waypoints are the locations in between the start and end
                            val waypoints = if (allTripsSorted.size > 2) {
                                allTripsSorted.subList(1, allTripsSorted.size - 1)
                            } else {
                                emptyList()
                            }

                            // Build the waypoints string: lat,lng|lat,lng|...
                            val waypointsString = waypoints.joinToString("|") {
                                "${it.geoPoint!!.latitude},${it.geoPoint.longitude}"
                            }

                            // Build the full directions URL
                            val intentUri = Uri.parse(
                                "https://www.google.com/maps/dir/?api=1" +
                                        "&origin=${origin.geoPoint!!.latitude},${origin.geoPoint.longitude}" +
                                        "&destination=${finalDestination.geoPoint!!.latitude},${finalDestination.geoPoint.longitude}" +
                                        "&waypoints=$waypointsString"
                            )

                            val mapIntent = Intent(Intent.ACTION_VIEW, intentUri)
                            mapIntent.setPackage("com.google.android.apps.maps")

                            try {
                                context.startActivity(mapIntent)
                            } catch (e: ActivityNotFoundException) {
                                Toast.makeText(context, "Google Maps app not installed.", Toast.LENGTH_LONG).show()
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp),
                        text = { Text("Open Route") },
                        icon = { Icon(Icons.Outlined.Directions, contentDescription = "Directions") }
                    )
                }
            } else {
                // Fallback view if there are no trips with locations in the itinerary
                Box(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No locations available.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EnhancedImageHeader(
    packageDetail: TravelPackageWithImages,
    onNavigateBack: () -> Unit
) {
    val images = packageDetail.images
    val pagerState = rememberPagerState(pageCount = { images.size })

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
        ) { page ->
            Box {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current).data(toDataUri(images.getOrNull(page)?.base64Data)).build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.3f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.7f)
                                ),
                                startY = 0f,
                                endY = Float.POSITIVE_INFINITY
                            )
                        )
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .statusBarsPadding(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                onClick = onNavigateBack,
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.4f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(onClick = { /* TODO: Wishlist */ }, shape = CircleShape, color = Color.Black.copy(alpha = 0.4f), modifier = Modifier.size(48.dp)) {
                    Box(contentAlignment = Alignment.Center) { Icon(Icons.Outlined.FavoriteBorder, contentDescription = "Favorite", tint = Color.White, modifier = Modifier.size(24.dp)) }
                }
            }
        }

        if (pagerState.pageCount > 1) {
            Row(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(pagerState.pageCount) { iteration ->
                    val isSelected = pagerState.currentPage == iteration
                    Box(
                        modifier = Modifier
                            .size(if (isSelected) 24.dp else 8.dp, 8.dp)
                            .background(
                                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(4.dp)
                            )
                    )
                }
            }
        }
    }
}

@Composable
fun EnhancedPaxSelector(
    pricing: Map<String, Double>,
    paxCounts: Map<String, Int>,
    onPaxChanged: (category: String, change: Int) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 16.dp)) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Outlined.Group,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Number of Travelers",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                pricing.entries.forEachIndexed { index, (category, price) ->
                    EnhancedPaxRow(
                        category = category.replaceFirstChar { it.uppercase() },
                        price = price,
                        count = paxCounts[category] ?: 0,
                        onCountChanged = { change -> onPaxChanged(category, change) }
                    )
                    if (index != pricing.entries.size - 1) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 16.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EnhancedPaxRow(
    category: String,
    price: Double,
    count: Int,
    onCountChanged: (Int) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = category,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "RM ${"%.2f".format(price)} per person",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                onClick = { onCountChanged(-1) },
                enabled = count > 0,
                shape = CircleShape,
                color = if (count > 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Remove,
                        contentDescription = "Decrease",
                        modifier = Modifier.size(20.dp),
                        tint = if (count > 0) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.width(50.dp)
            ) {
                Text(
                    text = "$count",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            Surface(
                onClick = { onCountChanged(1) },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Increase",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
fun EnhancedBookingActionBar(
    state: PackageDetailUiState.Success,
    // TODO: onAddToCartClick: () -> Unit
    modifier: Modifier = Modifier
) {
    val totalPrice = state.totalPrice
    val totalPax = state.paxCounts.values.sum()
    val hasSelection = totalPax > 0 && state.selectedDeparture != null

    Surface(
        modifier = modifier.fillMaxWidth(),
        shadowElevation = 16.dp,
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column {
            if (hasSelection) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Total for $totalPax ${if (totalPax == 1) "person" else "people"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "RM ${"%.2f".format(totalPrice)}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = "View breakdown",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(8.dp)
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = if (hasSelection) Arrangement.spacedBy(12.dp) else Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (hasSelection) {
                    OutlinedButton(
                        onClick = { /* TODO: add to the cart  (onAddToCartClick)*/ },
                        modifier = Modifier.weight(1f).height(56.dp),
                        border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Outlined.ShoppingCart, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add to Cart", fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = { /* TODO: navigate to checkout page */ },
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Outlined.EventAvailable, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Book Now", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Please select dates and travelers to continue",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}