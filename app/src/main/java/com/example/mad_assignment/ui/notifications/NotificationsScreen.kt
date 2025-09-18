package com.example.mad_assignment.ui.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material.icons.filled.MarkChatUnread
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mad_assignment.data.datasource.NotificationsDataSource
import com.example.mad_assignment.data.model.Notification
import com.example.mad_assignment.data.respository.NotificationRepository
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun NotificationsScreen(
    onNavigateBack: () -> Unit,
    onNotificationClick: (notificationId: String) -> Unit,
    viewModel: NotificationsViewModel = viewModel(
        factory = NotificationsViewModelFactory(NotificationRepository(NotificationsDataSource(FirebaseFirestore.getInstance())))
    ),
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
            NotificationScreenHeader(onNavigateBack = onNavigateBack)
            OptionDropdownMenu(viewModel = viewModel)
            ShowNotifications(viewModel = viewModel, onNotificationClick = onNotificationClick)
        }
    }
}

@Composable
fun NotificationScreenHeader(
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
@Composable
fun ShowNotifications(
    viewModel: NotificationsViewModel,
    onNotificationClick: (notificationId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val notifications by viewModel.notifications.collectAsState()

    if (notifications.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "No notifications", style = MaterialTheme.typography.bodyLarge)
        }
    } else {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(notifications) { notification ->
                NotificationCard(
                    notification = notification,
                    onNotificationClick = onNotificationClick
                )
            }
        }
    }
}

@Composable
fun OptionDropdownMenu(
    viewModel: NotificationsViewModel
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val items = listOf(
        NotificationFilter.ALL to Icons.Default.Notifications,
        NotificationFilter.UNREAD to Icons.Default.MarkChatUnread,
        NotificationFilter.ARCHIVED to Icons.Default.Archive,
    )
    var selectedItem by rememberSaveable { mutableStateOf(items[0].first.displayName) }

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
                                text = label.displayName,
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
                            selectedItem = label.displayName
                            expanded = false
                            viewModel.setFilter(label)
                        }
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Button(
                onClick = { viewModel.markAllAsRead() },
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surface,   // button background
                    contentColor = Color.Blue       // text & icon color
                ),
                modifier = Modifier.padding(bottom = 10.dp).align(Alignment.CenterEnd)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Mark All as Read",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

    }
}

@Composable
fun NotificationCard(
    notification: Notification,
    onNotificationClick: (notificationId: String) -> Unit
) {
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
        ),
        onClick = { onNotificationClick(notification.id) }
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
                    if (notification.status == Notification.Status.UNREAD) {
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
                        modifier = if (notification.status == Notification.Status.UNREAD) Modifier.padding(start = 10.dp) else Modifier.padding(start = 0.dp)
                    )
                }
                Text(
                    text = notification.getFormattedTime(),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            Text(
                text = notification.message.take(40) + if(notification.message.length > 40) "..." else "",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}