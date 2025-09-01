package com.example.mad_assignment.ui.packagedetail

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mad_assignment.data.model.TravelPackage
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import coil.compose.AsyncImage
import com.example.mad_assignment.data.model.DepartureDate
import com.google.firebase.Timestamp
import androidx.compose.foundation.lazy.items

@Composable
fun PackageDetailScreen(onNavigateBack: () -> Unit) {
    val viewModel: PackageDetailViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val paxCounts by viewModel.paxCounts.collectAsStateWithLifecycle()
    val departures by viewModel.departures.collectAsStateWithLifecycle()
    val selectedDepartureId by viewModel.selectedDepartureId.collectAsStateWithLifecycle()

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when (val state = uiState) {
                is PackageDetailUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                is PackageDetailUiState.Success -> {
                    val travelPackage = state.travelPackage
                    val actionBarHeight = 90.dp
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding(),
                        contentPadding = WindowInsets.navigationBars
                            .add(WindowInsets(bottom = actionBarHeight))
                            .asPaddingValues()
                    ) {
                        item { ImageHeader(travelPackage, onNavigateBack) }
                        item { PackageInfoBar(travelPackage) }
                        item { SectionTitle("Description") }
                        item { DescriptionSection(travelPackage.packageDescription) }
                        item { SectionTitle("Choose Date") }
                        item {
                            DateSelector(
                                departures = departures,
                                selectedId = selectedDepartureId,
                                onDateSelected = { id -> viewModel.selectDeparture(id) }
                            )
                        }
                        item { SectionTitle("Number of Pax") }
                        item {
                            PaxSelector(
                                pricing = travelPackage.pricing,
                                paxCounts = paxCounts,
                                onPaxChanged = { category, change ->
                                    viewModel.updatePaxCount(category, change)
                                }
                            )
                        }
                        item { SectionTitle("Location") }
                        item { LocationMapPlaceholder() }
                    }

                    BookingActionBar(
                        travelPackage = travelPackage,
                        paxCounts = paxCounts,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                    )
                }

                is PackageDetailUiState.Error -> {
                    Text(text = state.message, modifier = Modifier.align(Alignment.Center))
                }
            }
        }
    }
}

@Composable
fun DateSelector(
    departures: List<DepartureDate>,
    selectedId: String?,
    onDateSelected: (String) -> Unit
) {
    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp)) {
        items(items = departures, key = { it.id }) { departure ->
            val isSelected = departure.id == selectedId
            // This logic is now correct, because it's checking the
            // properties of the individual departure date.
            val isSoldOut = departure.currentBookings >= departure.maxCapacity

            FilterChip(
                selected = isSelected,
                onClick = { onDateSelected(departure.id) },
                label = { Text(formatDateRange(departure.startDate, departure.endDate)) },
                modifier = Modifier.padding(end = 8.dp),
                enabled = !isSoldOut // This will now correctly disable the sold-out date
            )
        }
    }
}

private fun formatDateRange(start: Timestamp, end: Timestamp): String {
    val sdf = java.text.SimpleDateFormat("dd MMM", java.util.Locale.getDefault())
    return "${sdf.format(start.toDate())} - ${sdf.format(end.toDate())}"
}

@Composable
fun DescriptionSection(description: String) {
    Text(
        text = description,
        style = MaterialTheme.typography.bodyMedium,
        color = Color.Gray,
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}

@Composable
fun PackageInfoBar(travelPackage: TravelPackage) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            InfoColumn(title = "RATING", value = "5.0", icon = {
                Icon(Icons.Default.Star, contentDescription = "Rating", tint = Color.Yellow)
            })
            InfoColumn(title = "ESTIMATE", value = "${travelPackage.durationDays}D ${travelPackage.durationDays - 1}N")
            InfoColumn(title = "LOCATION", value = travelPackage.itinerary.firstOrNull()?.location?.name ?: "N/A", icon = {
                Icon(Icons.Default.LocationOn, contentDescription = "Location")
            })
        }
        HorizontalDivider()
    }
}

@Composable
fun RowScope.InfoColumn(title: String, value: String, icon: (@Composable () -> Unit)? = null) {
    Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            icon?.invoke()
            if (icon != null) Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
@Composable
fun LocationMapPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp)
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImageHeader(travelPackage: TravelPackage, onNavigateBack: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { travelPackage.imageUrls.size })
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(500.dp)
            .clip(RoundedCornerShape(24.dp))
    ) {
        HorizontalPager(state = pagerState) { page ->
            AsyncImage(
                model = travelPackage.imageUrls[page],
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack, modifier = Modifier.background(Color.Gray.copy(alpha=0.5f), CircleShape)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Row {
                IconButton(onClick = {}, modifier = Modifier.background(Color.Gray.copy(alpha=0.5f), CircleShape)) {
                    Icon(
                        Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = Color.White
                    )
                }
            }
        }
        Row(
            Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(pagerState.pageCount) { iteration ->
                val color = if (pagerState.currentPage == iteration) Color.White else Color.LightGray
                Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
            }
        }
    }
}

@Composable
fun PaxSelector(
    pricing: Map<String, Double>,
    paxCounts: Map<String, Int>,
    onPaxChanged: (category: String, change: Int) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            pricing.forEach { (category, price) ->
                PaxRow(
                    category = category.replaceFirstChar { it.uppercase() },
                    price = price,
                    count = paxCounts[category] ?: 0,
                    onCountChanged = { change -> onPaxChanged(category, change) }
                )
                if (category != pricing.keys.last()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }
            }
        }
    }
}

@Composable
fun PaxRow(category: String, price: Double, count: Int, onCountChanged: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.weight(1f)) {
            Text(category, style = MaterialTheme.typography.bodyLarge)
            Text(text = "RM $price /pax", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onCountChanged(-1) }, enabled = count > 0) {
                Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Decrease")
            }
            Text(text = "$count", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.width(30.dp), textAlign = TextAlign.Center)
            IconButton(onClick = { onCountChanged(1) }) {
                Icon(Icons.Default.AddCircleOutline, contentDescription = "Increase")
            }
        }
    }
}

@Composable
fun BookingActionBar(
    travelPackage: TravelPackage,
    paxCounts: Map<String, Int>,
    modifier: Modifier = Modifier
) {
    var totalPrice by remember(paxCounts) {
        mutableStateOf(
            paxCounts.entries.sumOf { (category, count) ->
                (travelPackage.pricing[category] ?: 0.0) * count
            }
        )
    }

    Surface(modifier = modifier.fillMaxWidth(), shadowElevation = 8.dp) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "RM ${"%.2f".format(totalPrice)}", style = MaterialTheme.typography.headlineSmall)
            Row {
                OutlinedButton(onClick = { /* TODO */ }) { Text("Add to cart") }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = { /* TODO */ }) { Text("Book now") }
            }
        }
    }
}