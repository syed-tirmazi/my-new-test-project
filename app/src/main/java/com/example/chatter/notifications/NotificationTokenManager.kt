package com.example.chatter.notifications

import com.example.chatter.data.UserRepository
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

class NotificationTokenManager(
    private val userRepository: UserRepository = UserRepository()
) {
    suspend fun refreshToken() {
        val token = FirebaseMessaging.getInstance().token.await()
        userRepository.saveFcmToken(token)
    }
}
