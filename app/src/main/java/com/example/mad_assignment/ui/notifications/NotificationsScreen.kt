package com.example.mad_assignment.ui.notifications

import android.R.attr.shape
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun NotificationsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
        .safeDrawingPadding()
        .fillMaxSize()
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = modifier
        ) {
            Header(onNavigateBack)
            OptionDropdownMenu()
            ShowNotifications()
        }
    }
}

@Composable
fun Header(
    onNavigateBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(15.dp)
            .statusBarsPadding()
    ) {
        Surface(
            onClick = onNavigateBack,
            shape = CircleShape,
            color = Color.Black.copy(alpha = 0.0f), // 0.4
            modifier = Modifier.size(48.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface, // white
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Text(
            text = "Notifications",
            fontWeight = FontWeight.Bold,
            fontSize = 25.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

data class NotificationItem(
    val id: Int,
    val title: String,
    val message: String,
    val time: String, // e.g., "2 hours ago"
    val isRead: Boolean
) // implement in view model/ui state

@Composable
fun ShowNotifications(modifier: Modifier = Modifier) {
    val notifications = listOf(
        NotificationItem(1, "New Message", "You have a new message from John Doe.", "5 minutes ago", false),
        NotificationItem(2, "Order Shipped", "Your order #12345 has been shipped!", "1 hour ago", true),
        NotificationItem(3, "Promotion Alert", "Don't miss out on our summer sale!", "3 hours ago", false),
        NotificationItem(4, "Reminder", "Meeting with team at 2 PM today.", "Yesterday", false),
        NotificationItem(5, "App Update", "A new version of the app is available.", "2 days ago", false),
        NotificationItem(6, "Security Alert", "Unusual login activity detected.", "3 days ago", true),
        NotificationItem(1, "New Message", "You have a new message from John Doe.", "5 minutes ago", true),
        NotificationItem(2, "Order Shipped", "Your order #12345 has been shipped!", "1 hour ago", false),
        NotificationItem(3, "Promotion Alert", "Don't miss out on our summer sale!", "3 hours ago", false),
        NotificationItem(4, "Reminder", "Meeting with team at 2 PM today.", "Yesterday", false),
        NotificationItem(5, "App Update", "A new version of the app is available.", "2 days ago", false),
        NotificationItem(6, "Security Alert", "Unusual login activity detected.", "3 days ago", false)
    )

    if (notifications.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "No new notifications", style = MaterialTheme.typography.bodyLarge)
        }
    } else {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(notifications) { notification ->
                NotificationCard(notification = notification)
            }
        }
    }
}

@Composable
fun OptionDropdownMenu() {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val items = listOf(
        "All" to Icons.Default.Notifications,
        "Archived" to Icons.Default.Archive,
    )
    var selectedItem by rememberSaveable { mutableStateOf(items[0].first) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(horizontal = 25.dp, vertical = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .wrapContentSize(Alignment.TopStart)
        ) {
            Button(
                onClick = { expanded = true },
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,   // button background
                    contentColor = MaterialTheme.colorScheme.onPrimary    // text & icon color
                ),
                modifier = Modifier.padding(bottom = 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = selectedItem,
                        fontWeight = FontWeight.SemiBold
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        modifier = Modifier.rotate(if (expanded) 180f else 0f)
                    )
                }
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .background(color = MaterialTheme.colorScheme.surface)
            ) {
                items.forEach { (label, icon) ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = label,
                                modifier = Modifier
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = icon,
                                contentDescription = null
                            )
                        },
                        onClick = {
                            selectedItem = label
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun NotificationCard(notification: NotificationItem) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp, horizontal = 10.dp)
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White, // MaterialTheme.colorScheme.onSurface,
            contentColor = Color.Black // MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!notification.isRead) {
                        Surface(
                            shape = CircleShape,
                            color = Color.Red,
                            modifier = Modifier
                                .size(10.dp)
                        ) {}
                    }
                    Text(
                        text = notification.title,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = if (!notification.isRead) Modifier.padding(start = 10.dp) else Modifier.padding(start = 0.dp)
                    )
                }
                Text(
                    text = notification.time,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            Text(
                text = notification.message,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}