package com.example.mad_assignment.data.model

data class ScheduledNotification(
    val id: String,
    val title: String,
    val message: String,
    val type: Notification.NotificationType,
    val scheduledTime: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val status: ScheduleStatus = ScheduleStatus.PENDING
) {
    enum class ScheduleStatus {
        PENDING, SENT, CANCELLED
    }
}