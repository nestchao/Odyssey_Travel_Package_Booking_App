package com.example.mad_assignment.ui.notifications

import com.google.type.Date

interface NotificationsUiState {
    /*
    Notifications:
    1. Date
    2. Time
    3. Title (Sender)
    4. Content
     */
    data class Notification(
        val date: Date,
//        val time: Time
        val title: String,
        val content: String,
    ) : NotificationsUiState {

    }
}