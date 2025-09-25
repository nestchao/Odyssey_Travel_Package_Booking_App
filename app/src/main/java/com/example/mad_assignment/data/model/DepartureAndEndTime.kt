package com.example.mad_assignment.data.model

import com.google.firebase.Timestamp
import java.util.UUID

data class DepartureAndEndTime(
    val id: String = UUID.randomUUID().toString(),
    val startDate: Timestamp = Timestamp.now(),
    val endDate: Timestamp = Timestamp.now(),
    val capacity: Int = 0,
    val numberOfPeopleBooked: Int = 0
)
