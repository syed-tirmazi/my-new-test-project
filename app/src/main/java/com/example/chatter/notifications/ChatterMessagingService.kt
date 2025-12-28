package com.example.chatter.notifications

import com.example.chatter.data.UserRepository
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ChatterMessagingService : FirebaseMessagingService() {

    private val userRepository = UserRepository()

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        CoroutineScope(Dispatchers.IO).launch {
            runCatching { userRepository.saveFcmToken(token) }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val senderName = message.data["senderName"] ?: "New message"
        val body = message.data["body"] ?: message.notification?.body ?: ""
        NotificationHelper.showMessageNotification(applicationContext, senderName, body)
    }
}
