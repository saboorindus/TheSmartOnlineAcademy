package com.echologics.thesmartonlineacademy.ui.teacher.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.echologics.thesmartonlineacademy.data.model.TeacherProfile
import com.echologics.thesmartonlineacademy.data.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class TeacherProfileEditUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null,
    // Editable fields
    val fullName: String = "",
    val country: String = "",
    val bio: String = "",
    val languages: List<String> = emptyList(),
    val subjects: List<String> = emptyList(),
    val levels: List<String> = emptyList(),
    val teachingStyles: List<String> = emptyList(),
    val trialSessionEnabled: Boolean = false,
    val trialRate: String = "",
    val availabilitySlots: Map<String, List<String>> = emptyMap(),

    val ratePerTenMin: String = "",
    val currency: String = "PKR",
)

class TeacherProfileEditViewModel(
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(TeacherProfileEditUiState())
    val uiState: StateFlow<TeacherProfileEditUiState> = _uiState.asStateFlow()

    private val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    init {
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            try {
                val doc = FirebaseFirestore.getInstance()
                    .collection("teachers").document(uid).get().await()
                val profile = doc.toObject(TeacherProfile::class.java)
                profile?.let { p ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        fullName = p.fullName,
                        country = p.country,
                        bio = p.bio,
                        languages = p.languages,
                        subjects = p.subjects,
                        levels = p.levels,
                        teachingStyles = p.teachingStyles,
                        ratePerTenMin = if (p.ratePerTenMin > 0) p.ratePerTenMin.toString() else "",
                        currency = p.currency.ifBlank { "PKR" },
                        trialSessionEnabled = p.trialSessionEnabled,
                        trialRate = p.trialRate,
                        availabilitySlots = p.availabilitySlots
                    )
                } ?: run {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    // Field update helpers
    fun onFullNameChange(v: String) = update { copy(fullName = v) }
    fun onCountryChange(v: String) = update { copy(country = v) }
    fun onBioChange(v: String) = update { copy(bio = v) }
    fun onCurrencyChange(v: String) = update { copy(currency = v) }
    fun onTrialRateChange(v: String) = update { copy(trialRate = v) }
    fun onTrialToggle(v: Boolean) = update { copy(trialSessionEnabled = v) }

    fun onRatePerTenMinChange(v: String) {
        if (v.isEmpty() || v.all { it.isDigit() }) {
            update { copy(ratePerTenMin = v) }
        }
    }

    fun toggleLanguage(lang: String) = update { copy(languages = toggle(languages, lang)) }
    fun toggleSubject(s: String) = update { copy(subjects = toggle(subjects, s)) }
    fun toggleLevel(l: String) = update { copy(levels = toggle(levels, l)) }
    fun toggleStyle(s: String) = update { copy(teachingStyles = toggle(teachingStyles, s)) }
//    fun toggleSessionLength(l: String) = update { copy(sessionLengths = toggle(sessionLengths, l)) }

    fun toggleSlot(day: String, slot: String) {
        val current = _uiState.value.availabilitySlots.toMutableMap()
        val slots = current[day]?.toMutableList() ?: mutableListOf()
        if (slots.contains(slot)) slots.remove(slot) else slots.add(slot)
        current[day] = slots
        _uiState.value = _uiState.value.copy(availabilitySlots = current)
    }

    fun previewPrices(): List<Pair<String, String>> {
        val rate = _uiState.value.ratePerTenMin.toIntOrNull() ?: return emptyList()
        val cur = _uiState.value.currency
        return listOf(10, 20, 30, 40, 50, 60, 90, 120, 150, 180).map { mins ->
            val total = rate * (mins / 10)
            Pair(formatDuration(mins), "$cur $total")
        }
    }

    fun save() {
        val s = _uiState.value
        val rate = s.ratePerTenMin.toIntOrNull()
        if (s.fullName.isBlank()) {
            update { copy(error = "Name cannot be empty") }
            return
        }
        if (rate == null || rate <= 0) {
            update { copy(error = "Please enter a valid rate per 10 minutes") }
            return
        }
        _uiState.value = s.copy(isSaving = true, error = null)
        viewModelScope.launch {
            try {
                val updates = mapOf(
                    "fullName" to s.fullName,
                    "country" to s.country,
                    "bio" to s.bio,
                    "languages" to s.languages,
                    "subjects" to s.subjects,
                    "levels" to s.levels,
                    "teachingStyles" to s.teachingStyles,
                    "ratePerTenMin" to rate,
                    "currency" to s.currency,
                    "trialSessionEnabled" to s.trialSessionEnabled,
                    "trialRate" to s.trialRate,
                    "availabilitySlots" to s.availabilitySlots
                )
                FirebaseFirestore.getInstance()
                    .collection("teachers").document(uid)
                    .update(updates).await()
                _uiState.value = _uiState.value.copy(isSaving = false, isSaved = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isSaving = false, error = e.message)
            }
        }
    }

    private fun update(block: TeacherProfileEditUiState.() -> TeacherProfileEditUiState) {
        _uiState.value = _uiState.value.block()
    }
}

private fun toggle(list: List<String>, item: String): List<String> =
    if (list.contains(item)) list - item else list + item

private fun formatDuration(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h == 0 -> "${m}m"
        m == 0 -> "${h}h"
        else -> "${h}h ${m}m"
    }
}