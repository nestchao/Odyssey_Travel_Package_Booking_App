package com.example.mad_assignment.data.model

import com.google.type.Date
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale

data class Notification(
    val id: String,
    val title: String,
    val message: String,
    val timestamp: Timestamp,
    val type: NotificationType,
    val status: Status = Status.UNREAD
) {
    enum class Status {
        ARCHIVED, READ, UNREAD, DELETED
    }

    enum class NotificationType {
        WELCOME, REMINDER, PAYMENT, ANNOUNCEMENT, PACKAGE, GENERAL
    }

    fun getFormattedTime(): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp.time

        return when {
            diff < 60_000 -> "Just Now"
            diff < 3600_000 -> "${diff / 60_000}m ago"
            diff < 86400_000 -> "${diff / 3600_000}h ago"
            diff < 604800_000 -> "${diff / 86400_000}d ago"
            else -> SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(java.util.Date(timestamp.time)
            )
        }
    }
}

