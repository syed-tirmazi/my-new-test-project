package com.example.chatter.data

import com.example.chatter.model.ChatMessage
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ChatRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private fun chatIdFor(otherUserId: String): String {
        val selfId = auth.currentUser?.uid ?: error("User not signed in")
        return listOf(selfId, otherUserId).sorted().joinToString(separator = "_")
    }

    suspend fun sendMessage(receiverId: String, body: String): ChatMessage {
        val senderId = auth.currentUser?.uid ?: error("User not signed in")
        val chatId = chatIdFor(receiverId)
        val messageCollection = firestore.collection("chats")
            .document(chatId)
            .collection("messages")

        val messageData = hashMapOf(
            "senderId" to senderId,
            "receiverId" to receiverId,
            "body" to body,
            "timestamp" to Timestamp.now(),
            "read" to false
        )

        val doc = messageCollection.add(messageData).await()
        val saved = messageData.toChatMessage(doc.id)
        updateConversationSummary(chatId, saved)
        return saved
    }

    private suspend fun updateConversationSummary(chatId: String, message: ChatMessage) {
        val summary = mapOf(
            "lastMessage" to message.body,
            "lastSender" to message.senderId,
            "updatedAt" to FieldValue.serverTimestamp(),
            "participants" to listOf(message.senderId, message.receiverId)
        )
        firestore.collection("chats")
            .document(chatId)
            .set(summary, com.google.firebase.firestore.SetOptions.merge())
            .await()
    }

    fun streamMessages(withUserId: String): Flow<List<ChatMessage>> = callbackFlow {
        val chatId = chatIdFor(withUserId)
        val registration = firestore.collection("chats")
            .document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, _ ->
                val messages = snapshot?.documents?.map { doc ->
                    val data = doc.data ?: emptyMap()
                    data.toChatMessage(doc.id)
                } ?: emptyList()
                trySend(messages)
            }
        awaitClose { registration.remove() }
    }

    private fun Map<String, Any?>.toChatMessage(id: String): ChatMessage {
        val senderId = this["senderId"] as? String ?: ""
        val receiverId = this["receiverId"] as? String ?: ""
        val body = this["body"] as? String ?: ""
        val timestamp = this["timestamp"] as? Timestamp ?: Timestamp.now()
        val read = this["read"] as? Boolean ?: false
        return ChatMessage(
            id = id,
            senderId = senderId,
            receiverId = receiverId,
            body = body,
            timestamp = timestamp,
            read = read
        )
    }
}
