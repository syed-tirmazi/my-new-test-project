package com.example.chatter.model

data class UserProfile(
    val uid: String = "",
    val username: String = "",
    val displayName: String = "",
    val photoUrl: String? = null,
    val fcmToken: String? = null
)
