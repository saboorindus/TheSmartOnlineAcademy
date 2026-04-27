package com.echologics.thesmartonlineacademy.ui.messaging

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.echologics.thesmartonlineacademy.data.model.Conversation
import com.echologics.thesmartonlineacademy.data.repository.MessagingRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ConversationListUiState(
    val conversations: List<Conversation> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

class ConversationListViewModel(
    private val repo: MessagingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConversationListUiState())
    val uiState: StateFlow<ConversationListUiState> = _uiState.asStateFlow()

    private var listener: ListenerRegistration? = null
    private val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    init {
        startListening()
    }

    private fun startListening() {
        _uiState.value = _uiState.value.copy(isLoading = true)
        listener = repo.listenToConversations(currentUid) { conversations ->
            _uiState.value = _uiState.value.copy(
                conversations = conversations,
                isLoading = false
            )
        }
    }

    fun otherParticipantName(conversation: Conversation): String {
        return conversation.participantNames
            .entries
            .firstOrNull { it.key != currentUid }
            ?.value ?: "Unknown"
    }

    fun unreadCount(conversation: Conversation): Int {
        return conversation.unreadCount[currentUid] ?: 0
    }

    override fun onCleared() {
        super.onCleared()
        listener?.remove()
    }
}