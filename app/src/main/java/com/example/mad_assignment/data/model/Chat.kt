package com.example.mad_assignment.data.model

data class ChatMessage(
    val text: String,
    val isFromUser: Boolean
)

data class ChatState(
    val messages: List<ChatMessage> = emptyList(),
    val prompt: String = "",
    val isLoading: Boolean = false
)