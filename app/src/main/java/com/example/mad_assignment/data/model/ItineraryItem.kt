package com.example.mad_assignment.data.model

import java.util.UUID

data class ItineraryItem(
    val itineraryId: String = UUID.randomUUID().toString(),
    val day: Int = 0,
    val description: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val tripId: String = ""
)
