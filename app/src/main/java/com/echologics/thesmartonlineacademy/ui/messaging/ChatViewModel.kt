package com.echologics.thesmartonlineacademy.ui.messaging

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.echologics.thesmartonlineacademy.data.model.Conversation
import com.echologics.thesmartonlineacademy.data.model.Message
import com.echologics.thesmartonlineacademy.data.repository.MessagingRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class ChatUiState(
    val messages: List<Message> = emptyList(),
    val input: String = "",
    val isLoading: Boolean = true,
    val isSending: Boolean = false,
    val otherName: String = "",
    val error: String? = null
)

class ChatViewModel(
    private val repo: MessagingRepository = MessagingRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var messageListener: ListenerRegistration? = null
    private var currentConvoId: String = ""

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    val currentUid: String get() = auth.currentUser?.uid ?: ""

    fun initChat(conversation: Conversation, otherName: String) {
        currentConvoId = conversation.id
        _uiState.value = _uiState.value.copy(otherName = otherName, isLoading = true)

        // Mark messages as read
        viewModelScope.launch {
            repo.markRead(conversation.id, currentUid)
        }

        // Start real-time listener
        messageListener = repo.listenToMessages(conversation.id) { messages ->
            _uiState.value = _uiState.value.copy(messages = messages, isLoading = false)
        }
    }

    fun onInputChange(text: String) {
        _uiState.value = _uiState.value.copy(input = text)
    }

    fun sendMessage(otherId: String, otherName: String) {
        val text = _uiState.value.input.trim()
        if (text.isBlank() || _uiState.value.isSending) return

        _uiState.value = _uiState.value.copy(input = "", isSending = true)

        viewModelScope.launch {
            val myName = getMyName()
            val result = repo.sendMessage(
                senderId = currentUid,
                senderName = myName,
                receiverId = otherId,
                receiverName = otherName,
                text = text
            )
            result.fold(
                onSuccess = { _uiState.value = _uiState.value.copy(isSending = false) },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isSending = false,
                        input = text, // restore input on failure
                        error = e.message
                    )
                }
            )
        }
    }

    private suspend fun getMyName(): String {
        return try {
            // Try student profile first, then teacher
            val student = db.collection("students").document(currentUid).get().await()
            if (student.exists()) {
                student.getString("fullName") ?: "User"
            } else {
                val teacher = db.collection("teachers").document(currentUid).get().await()
                teacher.getString("fullName") ?: "User"
            }
        } catch (e: Exception) {
            "User"
        }
    }

    override fun onCleared() {
        super.onCleared()
        messageListener?.remove()
    }
}