package com.echologics.thesmartonlineacademy.ui.onboarding.teacher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.echologics.thesmartonlineacademy.data.model.ApprovalStatus
import com.echologics.thesmartonlineacademy.data.model.TeacherProfile
import com.echologics.thesmartonlineacademy.data.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TeacherOnboardingUiState(
    val currentStep: Int = 1,
    val totalSteps: Int = 6,
    // Step 1 — Personal info
    val fullName: String = "",
    val country: String = "",
    val languages: List<String> = emptyList(),
    val bio: String = "",
    // Step 2 — Teaching details
    val subjects: List<String> = emptyList(),
    val levels: List<String> = emptyList(),
    val teachingStyles: List<String> = emptyList(),
    // Step 3 — Credentials
    val yearsExperience: String = "",
    val education: String = "",
    // Step 4 — Rate & session lengths
    val ratePerTenMin: String = "",          // e.g. "250"
    val currency: String = "PKR",
    val trialSessionEnabled: Boolean = false,
    val trialRate: String = "",
    // Step 5 — Availability
    val availabilitySlots: Map<String, List<String>> = emptyMap(),
    // Step 6 — Review & submit
    val isLoading: Boolean = false,
    val error: String? = null,
    val isComplete: Boolean = false
)

class TeacherOnboardingViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(TeacherOnboardingUiState())
    val uiState: StateFlow<TeacherOnboardingUiState> = _uiState.asStateFlow()

    fun nextStep() {
        val s = _uiState.value
        if (s.currentStep < s.totalSteps) {
            _uiState.value = s.copy(currentStep = s.currentStep + 1, error = null)
        }
    }

    fun prevStep() {
        val s = _uiState.value
        if (s.currentStep > 1) {
            _uiState.value = s.copy(currentStep = s.currentStep - 1, error = null)
        }
    }

    // Step 1
    fun onFullNameChange(v: String) { _uiState.value = _uiState.value.copy(fullName = v) }
    fun onCountryChange(v: String) { _uiState.value = _uiState.value.copy(country = v) }
    fun onBioChange(v: String) { _uiState.value = _uiState.value.copy(bio = v) }
    fun toggleLanguage(lang: String) {
        val current = _uiState.value.languages.toMutableList()
        if (current.contains(lang)) current.remove(lang) else current.add(lang)
        _uiState.value = _uiState.value.copy(languages = current)
    }

    // Step 2
    fun toggleSubject(subject: String) {
        val current = _uiState.value.subjects.toMutableList()
        if (current.contains(subject)) current.remove(subject) else current.add(subject)
        _uiState.value = _uiState.value.copy(subjects = current)
    }
    fun toggleLevel(level: String) {
        val current = _uiState.value.levels.toMutableList()
        if (current.contains(level)) current.remove(level) else current.add(level)
        _uiState.value = _uiState.value.copy(levels = current)
    }
    fun toggleTeachingStyle(style: String) {
        val current = _uiState.value.teachingStyles.toMutableList()
        if (current.contains(style)) current.remove(style) else current.add(style)
        _uiState.value = _uiState.value.copy(teachingStyles = current)
    }

    // Step 3
    fun onYearsExperienceChange(v: String) { _uiState.value = _uiState.value.copy(yearsExperience = v) }
    fun onEducationChange(v: String) { _uiState.value = _uiState.value.copy(education = v) }

    // Step 4
    fun onRatePerTenMinChange(v: String) {
        // Only allow numeric input
        if (v.isEmpty() || v.all { it.isDigit() }) {
            _uiState.value = _uiState.value.copy(ratePerTenMin = v)
        }
    }
    fun onCurrencyChange(c: String) { _uiState.value = _uiState.value.copy(currency = c) }
    fun onTrialRateChange(v: String) { _uiState.value = _uiState.value.copy(trialRate = v) }
    fun onTrialToggle(v: Boolean) { _uiState.value = _uiState.value.copy(trialSessionEnabled = v) }

//    fun onHourlyRateChange(v: String) { _uiState.value = _uiState.value.copy(hourlyRate = v) }
//    fun onTrialRateChange(v: String) { _uiState.value = _uiState.value.copy(trialRate = v) }
//    fun onTrialSessionToggle(v: Boolean) { _uiState.value = _uiState.value.copy(trialSessionEnabled = v) }
//    fun toggleSessionLength(length: String) {
//        val current = _uiState.value.sessionLengths.toMutableList()
//        if (current.contains(length)) current.remove(length) else current.add(length)
//        _uiState.value = _uiState.value.copy(sessionLengths = current)
//    }

    // Step 5
    fun toggleSlot(day: String, slot: String) {
        val current = _uiState.value.availabilitySlots.toMutableMap()
        val daySlots = current[day]?.toMutableList() ?: mutableListOf()
        if (daySlots.contains(slot)) daySlots.remove(slot) else daySlots.add(slot)
        current[day] = daySlots
        _uiState.value = _uiState.value.copy(availabilitySlots = current)
    }

    // Preview helpers for the review step
    fun rateDisplay(): String {
        val rate = _uiState.value.ratePerTenMin
        val cur = _uiState.value.currency
        return if (rate.isBlank()) "—" else "$cur $rate / 10 min"
    }

    fun examplePrices(): String {
        val rate = _uiState.value.ratePerTenMin.toIntOrNull() ?: return ""
        val cur = _uiState.value.currency
        val p30 = rate * 3
        val p60 = rate * 6
        val p90 = rate * 9
        return "30m = $cur $p30  ·  60m = $cur $p60  ·  90m = $cur $p90"
    }

    // Step 6 — Submit
    fun submit() {
        val s = _uiState.value
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: run {
            _uiState.value = s.copy(error = "Not authenticated")
            return
        }
        val rate = s.ratePerTenMin.toIntOrNull() ?: run {
            _uiState.value = s.copy(error = "Please enter a valid rate")
            return
        }
        _uiState.value = s.copy(isLoading = true, error = null)
        val profile = TeacherProfile(
            uid = uid,
            fullName = s.fullName,
            country = s.country,
            languages = s.languages,
            bio = s.bio,
            subjects = s.subjects,
            levels = s.levels,
            teachingStyles = s.teachingStyles,
            yearsExperience = s.yearsExperience,
            education = s.education,
            ratePerTenMin = rate,
            currency = s.currency,
            availabilitySlots = s.availabilitySlots,
            trialSessionEnabled = s.trialSessionEnabled,
            trialRate = s.trialRate,
            approvalStatus = ApprovalStatus.PENDING,
            disabled = false
        )
        viewModelScope.launch {
            val result = authRepository.saveTeacherProfile(profile)
            result.fold(
                onSuccess = { _uiState.value = _uiState.value.copy(isLoading = false, isComplete = true) },
                onFailure = { e -> _uiState.value = _uiState.value.copy(isLoading = false, error = e.message) }
            )
        }
    }

    private fun toggleList(
        getter: TeacherOnboardingUiState.() -> List<String>,
        setter: TeacherOnboardingUiState.(List<String>) -> TeacherOnboardingUiState
    ) {
        // Not used directly — kept for reference
    }

    private fun toggle(list: List<String>, item: String) =
        if (list.contains(item)) list - item else list + item

    // Convenience shorthands
    private fun TeacherOnboardingViewModel.toggleList(
        getList: TeacherOnboardingUiState.() -> List<String>,
        withUpdated: TeacherOnboardingUiState.(List<String>) -> TeacherOnboardingUiState
    ) { /* no-op placeholder */ }
}