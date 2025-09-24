package com.example.mad_assignment.data.model

enum class BookingStatus {
    PENDING,
    CONFIRMED, // confirm when user in booking process
    PAID, // paid when user finish payment process
    CANCELLED, // cancel when user cancel booking -> refund
    COMPLETED, // completed when booking is completed - function to constantly check maybe
    REFUNDED
}