package com.echologics.thesmartonlineacademy.ui.student.discovery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.echologics.thesmartonlineacademy.data.model.TeacherProfile
import com.echologics.thesmartonlineacademy.data.repository.TeacherRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class DiscoveryUiState(
    val teachers: List<TeacherProfile> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val selectedSubject: String = "",
    val selectedLevel: String = "",
    val maxRate: String = ""
)

@OptIn(FlowPreview::class)
class DiscoveryViewModel(
    private val teacherRepository: TeacherRepository = TeacherRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(DiscoveryUiState())
    val uiState: StateFlow<DiscoveryUiState> = _uiState.asStateFlow()

    // Debounced search trigger
    private val _searchTrigger = MutableStateFlow(0)

    init {
        viewModelScope.launch {
            _searchTrigger
                .debounce(400)
                .collectLatest { loadTeachers() }
        }
        loadTeachers()
    }

    fun onSearchQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        _searchTrigger.value++
    }

    fun onSubjectChange(subject: String) {
        _uiState.value = _uiState.value.copy(
            selectedSubject = if (_uiState.value.selectedSubject == subject) "" else subject
        )
        loadTeachers()
    }

    fun onLevelChange(level: String) {
        _uiState.value = _uiState.value.copy(
            selectedLevel = if (_uiState.value.selectedLevel == level) "" else level
        )
        loadTeachers()
    }

    fun onMaxRateChange(rate: String) {
        _uiState.value = _uiState.value.copy(maxRate = rate)
        _searchTrigger.value++
    }

    fun clearFilters() {
        _uiState.value = _uiState.value.copy(
            searchQuery = "",
            selectedSubject = "",
            selectedLevel = "",
            maxRate = ""
        )
        loadTeachers()
    }

    fun refresh() = loadTeachers()

    private fun loadTeachers() {
        val state = _uiState.value
        _uiState.value = state.copy(isLoading = true, error = null)
        viewModelScope.launch {
            val subject = state.selectedSubject.ifBlank {
                state.searchQuery.ifBlank { null }
            }
            val result = teacherRepository.getApprovedTeachers(
                subject = subject,
                level = state.selectedLevel.ifBlank { null },
                maxRate = state.maxRate.toIntOrNull()
            )
            result.fold(
                onSuccess = { teachers ->
                    _uiState.value = _uiState.value.copy(
                        teachers = teachers,
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