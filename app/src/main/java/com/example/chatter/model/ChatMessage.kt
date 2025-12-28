package com.example.chatter.model

import com.google.firebase.Timestamp

data class ChatMessage(
    val id: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val body: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val read: Boolean = false
)
