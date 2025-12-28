package com.example.chatter.data

import com.example.chatter.model.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.tasks.await

class UserRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    suspend fun ensureSignedIn(): FirebaseUser {
        val current = auth.currentUser
        if (current != null) return current
        return auth.signInAnonymously().await().user
            ?: error("Unable to sign in anonymously")
    }

    fun currentUserId(): String? = auth.currentUser?.uid

    suspend fun upsertUserProfile(username: String, displayName: String) {
        val user = ensureSignedIn()
        val profile = mapOf(
            "uid" to user.uid,
            "username" to username,
            "searchUsername" to username.lowercase(),
            "displayName" to displayName,
            "photoUrl" to user.photoUrl?.toString()
        )
        firestore.collection("users")
            .document(user.uid)
            .set(profile, SetOptions.merge())
            .await()
    }

    suspend fun saveFcmToken(token: String) {
        val user = ensureSignedIn()
        firestore.collection("users")
            .document(user.uid)
            .set(mapOf("fcmToken" to token), SetOptions.merge())
            .await()
    }

    fun currentUserProfile(): Flow<UserProfile?> = callbackFlow {
        val currentId = auth.currentUser?.uid
        if (currentId == null) {
            trySend(null)
            close()
            return@callbackFlow
        }
        val registration = firestore.collection("users")
            .document(currentId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(null)
                    return@addSnapshotListener
                }
                val data = snapshot?.data
                if (data != null) {
                    trySend(snapshot.toUserProfile())
                }
            }
        awaitClose { registration.remove() }
    }

    fun profile(userId: String): Flow<UserProfile?> = callbackFlow {
        val registration = firestore.collection("users")
            .document(userId)
            .addSnapshotListener { snapshot, _ ->
                val data = snapshot?.data
                if (data != null) {
                    trySend(snapshot.toUserProfile())
                }
            }
        awaitClose { registration.remove() }
    }

    fun searchUsers(usernameQuery: String): Flow<List<UserProfile>> = callbackFlow {
        if (usernameQuery.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val lower = usernameQuery.lowercase()
        val registration = firestore.collection("users")
            .orderBy("searchUsername", Query.Direction.ASCENDING)
            .startAt(lower)
            .endAt("$lower\uf8ff")
            .addSnapshotListener { snapshot, _ ->
                val results = snapshot?.documents
                    ?.mapNotNull { it.toUserProfile() }
                    ?: emptyList()
                trySend(results)
            }
        awaitClose { registration.remove() }
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toUserProfile(): UserProfile? {
        val username = getString("username") ?: return null
        val uid = getString("uid") ?: id
        val displayName = getString("displayName") ?: username
        val photoUrl = getString("photoUrl")
        val fcmToken = getString("fcmToken")
        return UserProfile(uid = uid, username = username, displayName = displayName, photoUrl = photoUrl, fcmToken = fcmToken)
    }
}
