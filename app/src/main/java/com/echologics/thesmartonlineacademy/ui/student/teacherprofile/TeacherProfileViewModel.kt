package com.echologics.thesmartonlineacademy.ui.student.teacherprofile

import com.echologics.thesmartonlineacademy.data.model.TeacherProfile
import com.echologics.thesmartonlineacademy.data.repository.TeacherRepository


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.echologics.thesmartonlineacademy.data.model.Conversation
import com.echologics.thesmartonlineacademy.data.model.Review
import com.echologics.thesmartonlineacademy.data.repository.MessagingRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class TeacherProfileUiState(
    val teacher: TeacherProfile? = null,
    val reviews: List<Review> = emptyList(),
    val averageRating: Float = 0f,
    val isLoading: Boolean = false,
    val error: String? = null,
    val conversationReady: Conversation? = null,
    val chatOtherName: String = "",
    val chatOtherId: String = "",
    val isChatLoading: Boolean = false
)

class TeacherProfileViewModel(
    private val teacherRepository: TeacherRepository,
    private val messagingRepository: MessagingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TeacherProfileUiState())
    val uiState: StateFlow<TeacherProfileUiState> = _uiState.asStateFlow()

    private val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    fun loadTeacher(teacherId: String) {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            val teacherResult = teacherRepository.getTeacherById(teacherId)
            val reviewsResult = teacherRepository.getReviewsForTeacher(teacherId)
            val avgRating = teacherRepository.getAverageRating(teacherId)

            teacherResult.fold(
                onSuccess = { teacher ->
                    _uiState.value = _uiState.value.copy(
                        teacher = teacher,
                        reviews = reviewsResult.getOrDefault(emptyList()),
                        averageRating = avgRating,
                        isLoading = false
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
            )
        }
    }

    fun startChat() {
        val teacher = _uiState.value.teacher ?: return
        _uiState.value = _uiState.value.copy(isChatLoading = true)
        viewModelScope.launch {
            val myName = getMyName()
            val result = messagingRepository.getOrCreateConversation(
                myId = currentUid,
                myName = myName,
                otherId = teacher.uid,
                otherName = teacher.fullName
            )
            result.fold(
                onSuccess = { conversation ->
                    _uiState.value = _uiState.value.copy(
                        conversationReady = conversation,
                        chatOtherName = teacher.fullName,
                        chatOtherId = teacher.uid,
                        isChatLoading = false
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isChatLoading = false,
                        error = "Could not start chat: ${e.message}"
                    )
                }
            )
        }
    }

    // Clear navigation signal after it has been consumed
    fun onChatNavigated() {
        _uiState.value = _uiState.value.copy(conversationReady = null)
    }

    private suspend fun getMyName(): String {
        return try {
            val doc = FirebaseFirestore.getInstance()
                .collection("students").document(currentUid).get().await()
            doc.getString("fullName") ?: "Student"
        } catch (e: Exception) { "Student" }
    }
}