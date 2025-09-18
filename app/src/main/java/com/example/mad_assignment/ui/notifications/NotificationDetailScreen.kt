package com.example.mad_assignment.ui.notifications

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mad_assignment.data.datasource.NotificationsDataSource
import com.example.mad_assignment.data.model.Notification
import com.example.mad_assignment.data.respository.NotificationRepository
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun NotificationDetailsScreen(
    notificationId: String,
    onNavigateBack: () -> Unit,
    viewModel: NotificationsViewModel = viewModel(
        factory = NotificationsViewModelFactory(NotificationRepository(NotificationsDataSource(FirebaseFirestore.getInstance())))
    ),
    modifier: Modifier = Modifier
        .safeDrawingPadding()
        .fillMaxSize()
) {
    val notification by viewModel.getNotificationById(notificationId).collectAsState(initial = null)

    if(notification?.status == Notification.Status.UNREAD)
        viewModel.markAsRead(id = notificationId)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = modifier.padding(end = 10.dp),
        ) {
            ShowNotificationDetails(notification = notification, onNavigateBack = onNavigateBack, viewModel, modifier)
        }
    }
}

@Composable
fun ShowNotificationDetails(
    notification: Notification?,
    onNavigateBack: () -> Unit,
    viewModel: NotificationsViewModel,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box {
            DetailScreenHeader(onNavigateBack = onNavigateBack, viewModel, notification)

            if (notification != null) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 80.dp, start = 30.dp, end = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                        val formattedTime = formatter.format(notification.timestamp)

                        Text(
                            text = formattedTime,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                    item {
                        Text(
                            text = notification.title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 10.dp)
                        )
                    }
                    item {
                        Text(
                            text = notification.message,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Notification not found")
                }
            }
        }
    }
}

@Composable
fun DetailScreenHeader(
    onNavigateBack: () -> Unit,
    viewModel: NotificationsViewModel,
    notification: Notification?
) {
    val context = LocalContext.current

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
        if(notification != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = {
                        if(notification.status == Notification.Status.ARCHIVED) {
                            viewModel.markAsRead(notification.id)
                            Toast.makeText(context, "Notification unarchived", Toast.LENGTH_SHORT).show()
                        }
                        else if(notification.status == Notification.Status.READ) {
                            viewModel.markAsArchived(notification.id)
                            Toast.makeText(context, "Notification archived", Toast.LENGTH_SHORT).show()
                        }
                        onNavigateBack()
                    }
                ) {
                    Icon(
                        imageVector = if (notification.status != Notification.Status.ARCHIVED) {
                            Icons.Default.Archive
                        } else {
                            Icons.Default.Unarchive
                        },
                        contentDescription = "Archive",
                        tint = Color.Gray
                    )
                }

                TextButton(
                    onClick = {
                        viewModel.deleteNotification(notification.id)
                        Toast.makeText(context, "Notification deleted", Toast.LENGTH_SHORT).show()
                        onNavigateBack()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color.Red
                    )
                }
            }
        }
    }
}