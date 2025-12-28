package com.example.chatter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chatter.data.ChatRepository
import com.example.chatter.data.UserRepository
import com.example.chatter.model.ChatMessage
import com.example.chatter.model.UserProfile
import com.example.chatter.notifications.NotificationTokenManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ChatterViewModel(
    private val userRepository: UserRepository = UserRepository(),
    private val chatRepository: ChatRepository = ChatRepository(),
    private val tokenManager: NotificationTokenManager = NotificationTokenManager()
) : ViewModel() {

    private var searchJob: Job? = null
    private var chatJob: Job? = null
    private var peerObserver: Job? = null
    private var activePeerId: String? = null

    private val _profileState = MutableStateFlow(ProfileState())
    val profileState: StateFlow<ProfileState> = _profileState

    private val _searchState = MutableStateFlow(SearchState())
    val searchState: StateFlow<SearchState> = _searchState

    private val _chatState = MutableStateFlow(ChatState())
    val chatState: StateFlow<ChatState> = _chatState

    private val _activeUserProfile = MutableStateFlow<UserProfile?>(null)
    val activeUserProfile: StateFlow<UserProfile?> = _activeUserProfile

    init {
        viewModelScope.launch {
            runCatching { userRepository.ensureSignedIn() }
            tokenManager.refreshToken()
            userRepository.currentUserProfile().collectLatest { profile ->
                _profileState.value = _profileState.value.copy(isLoading = false, profile = profile)
            }
        }
    }

    fun updateProfile(username: String, displayName: String) {
        viewModelScope.launch {
            _profileState.value = _profileState.value.copy(isLoading = true)
            runCatching {
                userRepository.upsertUserProfile(username, displayName)
            }.onSuccess {
                _profileState.value = _profileState.value.copy(isLoading = false)
            }.onFailure { error ->
                _profileState.value = _profileState.value.copy(isLoading = false, error = error.message)
            }
        }
    }

    fun updateSearch(query: String) {
        _searchState.value = _searchState.value.copy(query = query)
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            if (query.length < 2) {
                _searchState.value = _searchState.value.copy(results = emptyList())
                return@launch
            }
            userRepository.searchUsers(query).collectLatest { results ->
                val filtered = results.filter { it.uid != profileState.value.profile?.uid }
                _searchState.value = _searchState.value.copy(results = filtered)
            }
        }
    }

    fun setActiveChat(peerId: String) {
        chatJob?.cancel()
        chatJob = viewModelScope.launch {
            chatRepository.streamMessages(peerId).collectLatest { messages ->
                _chatState.value = _chatState.value.copy(messages = messages)
            }
        }
    }

    fun observePeer(peerId: String) {
        if (peerId == activePeerId) return
        activePeerId = peerId
        peerObserver?.cancel()
        peerObserver = viewModelScope.launch {
            userRepository.profile(peerId).collectLatest { profile ->
                _activeUserProfile.value = profile
            }
        }
    }

    fun sendMessage(body: String) {
        val peerId = _activeUserProfile.value?.uid ?: return
        if (body.isBlank()) return
        viewModelScope.launch {
            _chatState.value = _chatState.value.copy(isSending = true)
            runCatching {
                chatRepository.sendMessage(peerId, body)
            }.onFailure { error ->
                _chatState.value = _chatState.value.copy(error = error.message)
            }
            _chatState.value = _chatState.value.copy(isSending = false)
        }
    }
}

data class ProfileState(
    val isLoading: Boolean = true,
    val profile: UserProfile? = null,
    val error: String? = null
)

data class SearchState(
    val query: String = "",
    val results: List<UserProfile> = emptyList()
)

data class ChatState(
    val messages: List<ChatMessage> = emptyList(),
    val isSending: Boolean = false,
    val error: String? = null
)
