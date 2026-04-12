package com.echologics.thesmartonlineacademy.ui.auth

import com.echologics.thesmartonlineacademy.data.repository.AuthRepository

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.echologics.thesmartonlineacademy.data.model.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SignupUiState(
    val fullName: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val signupComplete: Boolean = false
)

class SignupViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(SignupUiState())
    val uiState: StateFlow<SignupUiState> = _uiState.asStateFlow()

    fun onFullNameChange(v: String) { _uiState.value = _uiState.value.copy(fullName = v, error = null) }
    fun onEmailChange(v: String) { _uiState.value = _uiState.value.copy(email = v, error = null) }
    fun onPasswordChange(v: String) { _uiState.value = _uiState.value.copy(password = v, error = null) }
    fun onConfirmPasswordChange(v: String) { _uiState.value = _uiState.value.copy(confirmPassword = v, error = null) }

    fun signup(role: String) {
        val state = _uiState.value
        when {
            state.fullName.isBlank() -> { _uiState.value = state.copy(error = "Full name is required"); return }
            state.email.isBlank() -> { _uiState.value = state.copy(error = "Email is required"); return }
            state.password.length < 6 -> { _uiState.value = state.copy(error = "Password must be at least 6 characters"); return }
            state.password != state.confirmPassword -> { _uiState.value = state.copy(error = "Passwords do not match"); return }
        }
        val userRole = if (role == "teacher") UserRole.TEACHER else UserRole.STUDENT
        _uiState.value = state.copy(isLoading = true, error = null)
        viewModelScope.launch {
            val result = authRepository.signUp(state.email.trim(), state.password, userRole)
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(isLoading = false, signupComplete = true)
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Signup failed"
                    )
                }
            )
        }
    }
}