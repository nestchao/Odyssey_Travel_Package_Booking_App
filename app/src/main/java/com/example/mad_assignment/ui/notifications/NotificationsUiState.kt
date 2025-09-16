package com.example.mad_assignment.ui.notifications

import com.google.type.Date
import java.sql.Time

sealed interface NotificationsUiState {

    enum class Status {
        Archived,
        Unread,
        Deleted,
        Read
    }

    data class Notification(
        val id: String,
        val title: String,
        val message: String,
        val date: Date, // if today, show hours/min passed else show days
        val time: Time,
        var status: Status
    ) : NotificationsUiState {

    }
}