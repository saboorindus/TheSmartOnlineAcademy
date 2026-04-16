package com.echologics.thesmartonlineacademy.ui.onboarding.student


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.echologics.thesmartonlineacademy.data.model.StudentProfile
import com.echologics.thesmartonlineacademy.data.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.TimeZone

data class StudentOnboardingUiState(
    val fullName: String = "",
    val currentStep: Int = 1,
    val totalSteps: Int = 3,
    // Step 1 — Goals
    val subjects: List<String> = emptyList(),
    val level: String = "",
    val goal: String = "",
    // Step 2 — Preferences
    val preferredLanguage: String = "English",
    val timezone: String = TimeZone.getDefault().id,
    val availabilityPrefs: List<String> = emptyList(),
    // State
    val isLoading: Boolean = false,
    val error: String? = null,
    val isComplete: Boolean = false
)

class StudentOnboardingViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(StudentOnboardingUiState())
    val uiState: StateFlow<StudentOnboardingUiState> = _uiState.asStateFlow()
    fun onFullNameChange(v: String) { _uiState.value = _uiState.value.copy(fullName = v, error = null) }

    fun nextStep() {
        val s = _uiState.value
        if (s.currentStep < s.totalSteps) {
            _uiState.value = s.copy(currentStep = s.currentStep + 1, error = null)
        }
    }

    fun prevStep() {
        val s = _uiState.value
        if (s.currentStep > 1) _uiState.value = s.copy(currentStep = s.currentStep - 1)
    }

    // Step 1
    fun toggleSubject(subject: String) {
        val current = _uiState.value.subjects.toMutableList()
        if (current.contains(subject)) current.remove(subject) else current.add(subject)
        _uiState.value = _uiState.value.copy(subjects = current)
    }
    fun onLevelChange(v: String) { _uiState.value = _uiState.value.copy(level = v) }
    fun onGoalChange(v: String) { _uiState.value = _uiState.value.copy(goal = v) }

    // Step 2
    fun onLanguageChange(v: String) { _uiState.value = _uiState.value.copy(preferredLanguage = v) }
    fun onTimezoneChange(v: String) { _uiState.value = _uiState.value.copy(timezone = v) }
    fun toggleAvailabilityPref(pref: String) {
        val current = _uiState.value.availabilityPrefs.toMutableList()
        if (current.contains(pref)) current.remove(pref) else current.add(pref)
        _uiState.value = _uiState.value.copy(availabilityPrefs = current)
    }

    // Step 3 — Save
    fun submit() {
        val s = _uiState.value
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: run {
            _uiState.value = s.copy(error = "Not authenticated")
            return
        }

        when {
            s.fullName.isBlank() -> { _uiState.value = s.copy(error = "Full name is required"); return }
        }

        _uiState.value = s.copy(isLoading = true, error = null)
        val profile = StudentProfile(
            fullName = s.fullName,
            uid = uid,
            subjects = s.subjects,
            level = s.level,
            goal = s.goal,
            preferredLanguage = s.preferredLanguage,
            timezone = s.timezone,
            availabilityPrefs = s.availabilityPrefs
        )
        viewModelScope.launch {
            val result = authRepository.saveStudentProfile(profile)
            result.fold(
                onSuccess = { _uiState.value = _uiState.value.copy(isLoading = false, isComplete = true) },
                onFailure = { e -> _uiState.value = _uiState.value.copy(isLoading = false, error = e.message) }
            )
        }
    }
}