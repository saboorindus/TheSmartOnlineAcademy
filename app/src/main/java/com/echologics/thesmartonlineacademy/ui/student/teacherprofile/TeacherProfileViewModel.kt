package com.echologics.thesmartonlineacademy.ui.student.teacherprofile

import com.echologics.thesmartonlineacademy.data.model.TeacherProfile
import com.echologics.thesmartonlineacademy.data.repository.TeacherRepository


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.echologics.thesmartonlineacademy.data.model.Review
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TeacherProfileUiState(
    val teacher: TeacherProfile? = null,
    val reviews: List<Review> = emptyList(),
    val averageRating: Float = 0f,
    val isLoading: Boolean = false,
    val error: String? = null
)

class TeacherProfileViewModel(
    private val teacherRepository: TeacherRepository = TeacherRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(TeacherProfileUiState())
    val uiState: StateFlow<TeacherProfileUiState> = _uiState.asStateFlow()

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
}