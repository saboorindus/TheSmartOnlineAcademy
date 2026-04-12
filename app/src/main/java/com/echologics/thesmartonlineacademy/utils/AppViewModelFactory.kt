package com.echologics.thesmartonlineacademy.utils

import com.echologics.thesmartonlineacademy.data.repository.AuthRepository
import com.echologics.thesmartonlineacademy.ui.auth.LoginViewModel
import com.echologics.thesmartonlineacademy.ui.auth.SignupViewModel
import com.echologics.thesmartonlineacademy.ui.onboarding.student.StudentOnboardingViewModel
import com.echologics.thesmartonlineacademy.ui.onboarding.teacher.TeacherOnboardingViewModel

class AppViewModelFactory(
    private val authRepository: AuthRepository
) : androidx.lifecycle.ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {

        return when {

            modelClass.isAssignableFrom(LoginViewModel::class.java) ->
                LoginViewModel(authRepository) as T

            modelClass.isAssignableFrom(SignupViewModel::class.java) ->
                SignupViewModel(authRepository) as T

            modelClass.isAssignableFrom(TeacherOnboardingViewModel::class.java) ->
                TeacherOnboardingViewModel(authRepository) as T

            modelClass.isAssignableFrom(StudentOnboardingViewModel::class.java) ->
                StudentOnboardingViewModel(authRepository) as T

            else -> throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
