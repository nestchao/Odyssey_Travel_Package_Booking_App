package com.example.mad_assignment.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import com.example.mad_assignment.data.model.TravelPackage
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale

@Composable
fun HomeScreen(
    onPackageClick: (String) -> Unit
) {
    val viewModel: HomeViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        bottomBar = {
            AppBottomNavigationBar()
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            when (val state = uiState) {
                is HomeUiState.Loading -> {
                    CircularProgressIndicator()
                }
                is HomeUiState.Success -> {
                    HomeContent(
                        featuredPackages = state.featuredPackages,
                        packages = state.allPackages,
                        onPackageClick = onPackageClick
                    )
                }
                is HomeUiState.Error -> {
                    Text(text = "Error: ${state.message}")
                }
            }
        }
    }
}

@Composable
fun HomeContent(
    packages: List<TravelPackage>,
    featuredPackages: List<TravelPackage>,
    onPackageClick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        item { HomeHeader() }
        item { AroundYouSection(featuredPackages = featuredPackages,
            onPackageClick = onPackageClick) }
        item { SectionHeader(title = "PACKAGES", onViewMoreClick = { /* TODO */ }) }
        items(packages) { travelPackage ->
            PackageCard(packageData = travelPackage,
                onClick = { onPackageClick(travelPackage.packageId) }
            )
        }
    }
}

@Composable
fun HomeHeader() {
    Column(modifier = Modifier.padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = "",
                onValueChange = {},
//                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                shape = RoundedCornerShape(50),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { /* TODO */ }) {
                Icon(Icons.Outlined.ShoppingCart, contentDescription = "Shopping Cart")
            }
            IconButton(onClick = { /* TODO */ }) {
                Icon(Icons.Outlined.Notifications, contentDescription = "Notifications")
            }
        }
        Spacer(Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.LocationOn, contentDescription = "Location", tint = Color.Red)
            Spacer(Modifier.width(8.dp))
            Column {
                Text(
                    text = "You are currently located at",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                Text(
                    text = "TARUMT KL",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, onViewMoreClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        TextButton(onClick = onViewMoreClick) {
            Text("View More ->")
        }
    }
}

@Composable
fun AroundYouSection(featuredPackages: List<TravelPackage>, onPackageClick: (String) -> Unit) {
    Column {
        SectionHeader(title = "AROUND YOU", onViewMoreClick = { /* TODO */ })
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(featuredPackages) { packageItem ->
                AroundYouCard(
                    title = packageItem.packageName,
                    rating = "4.9",
                    imageUrl = packageItem.imageUrls.firstOrNull() ?: "",
                    onClick = { onPackageClick(packageItem.packageId) }
                )
            }
        }
    }
}

@Composable
fun AroundYouCard(title: String, rating: String, imageUrl: String,  onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.size(160.dp, 220.dp),
        onClick = onClick
    ) {
        Box(contentAlignment = Alignment.BottomStart) {
            AsyncImage(
                model = imageUrl,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier.fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                        startY = 400f
                    ))
            )
            Column(modifier = Modifier.padding(12.dp)) {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = "Rating", tint = Color.Yellow, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(rating, color = Color.White, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PackageCard(packageData: TravelPackage, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            AsyncImage(
                model = packageData.imageUrls.firstOrNull(),
                contentDescription = packageData.packageName,
                modifier = Modifier.fillMaxWidth().height(180.dp),
                contentScale = ContentScale.Crop
            )
            Text(
                text = packageData.packageName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Composable
fun AppBottomNavigationBar() {
    NavigationBar(
        modifier = Modifier.shadow(elevation = 8.dp)
    ) {
        val currentRoute = "Home"

        NavigationBarItem(
            selected = currentRoute == "Home",
            onClick = { /* TODO: Navigate to Home */ },
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
            label = { Text("Home") }
        )
        NavigationBarItem(
            selected = currentRoute == "Booking",
            onClick = { /* TODO: Navigate to Booking */ },
            icon = { Icon(Icons.Default.Book, contentDescription = "Booking") },
            label = { Text("Booking") }
        )
        NavigationBarItem(
            selected = currentRoute == "Profile",
            onClick = { /* TODO: Navigate to Profile */ },
            icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
            label = { Text("Profile") }
        )
    }
}