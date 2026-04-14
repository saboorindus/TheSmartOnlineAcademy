package com.echologics.thesmartonlineacademy.ui.review

import com.echologics.thesmartonlineacademy.data.model.Booking
import com.echologics.thesmartonlineacademy.data.model.Review
import com.echologics.thesmartonlineacademy.data.repository.ReviewRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class ReviewUiState(
    val booking: Booking? = null,
    val rating: Int = 0,         // 1-5 stars
    val comment: String = "",
    val isLoading: Boolean = false,
    val isSubmitted: Boolean = false,
    val alreadyReviewed: Boolean = false,
    val error: String? = null
)

class ReviewViewModel(
    private val reviewRepository: ReviewRepository = ReviewRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReviewUiState())
    val uiState: StateFlow<ReviewUiState> = _uiState.asStateFlow()

    private val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    fun setBooking(booking: Booking) {
        _uiState.value = _uiState.value.copy(booking = booking)
        checkAlreadyReviewed(booking.id)
    }

    private fun checkAlreadyReviewed(bookingId: String) {
        viewModelScope.launch {
            val reviewed = reviewRepository.hasReviewed(bookingId)
            _uiState.value = _uiState.value.copy(alreadyReviewed = reviewed)
        }
    }

    fun onRatingChange(stars: Int) {
        _uiState.value = _uiState.value.copy(rating = stars, error = null)
    }

    fun onCommentChange(text: String) {
        _uiState.value = _uiState.value.copy(comment = text, error = null)
    }

    fun canSubmit(): Boolean {
        val s = _uiState.value
        return s.rating > 0 && !s.alreadyReviewed && !s.isLoading
    }

    fun submit() {
        val state = _uiState.value
        val booking = state.booking ?: return
        if (state.rating == 0) {
            _uiState.value = state.copy(error = "Please select a star rating")
            return
        }
        _uiState.value = state.copy(isLoading = true, error = null)
        viewModelScope.launch {
            val studentName = getStudentName()
            val review = Review(
                bookingId = booking.id,
                teacherId = booking.teacherId,
                studentId = currentUid,
                studentName = studentName,
                rating = state.rating.toFloat(),
                comment = state.comment.trim()
            )
            val result = reviewRepository.submitReview(review)
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(isLoading = false, isSubmitted = true)
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
                }
            )
        }
    }

    private suspend fun getStudentName(): String {
        return try {
            val doc = FirebaseFirestore.getInstance()
                .collection("students").document(currentUid).get().await()
            doc.getString("fullName") ?: "Student"
        } catch (e: Exception) { "Student" }
    }
}