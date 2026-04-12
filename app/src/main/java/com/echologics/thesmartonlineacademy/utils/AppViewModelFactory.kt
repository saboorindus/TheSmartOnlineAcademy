package com.echologics.thesmartonlineacademy.utils

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.echologics.thesmartonlineacademy.data.repository.AuthRepository
import com.echologics.thesmartonlineacademy.ui.auth.LoginViewModel
import com.echologics.thesmartonlineacademy.ui.auth.SignupViewModel
import com.echologics.thesmartonlineacademy.ui.onboarding.student.StudentOnboardingViewModel
import com.echologics.thesmartonlineacademy.ui.onboarding.teacher.TeacherOnboardingViewModel
import com.echologics.thesmartonlineacademy.ui.student.booking.BookingViewModel
import com.echologics.thesmartonlineacademy.ui.student.discovery.DiscoveryViewModel
import com.echologics.thesmartonlineacademy.ui.student.payment.PaymentViewModel
import com.echologics.thesmartonlineacademy.ui.student.teacherprofile.TeacherProfileViewModel
import com.echologics.thesmartonlineacademy.ui.teacher.bookings.TeacherBookingsViewModel

class AppViewModelFactory(
    private val authRepository: AuthRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {

        return when {

            // Auth
            modelClass.isAssignableFrom(LoginViewModel::class.java) ->
                LoginViewModel(authRepository) as T

            modelClass.isAssignableFrom(SignupViewModel::class.java) ->
                SignupViewModel(authRepository) as T

            // Onboarding
            modelClass.isAssignableFrom(TeacherOnboardingViewModel::class.java) ->
                TeacherOnboardingViewModel(authRepository) as T

            modelClass.isAssignableFrom(StudentOnboardingViewModel::class.java) ->
                StudentOnboardingViewModel(authRepository) as T

            // Teacher
            modelClass.isAssignableFrom(TeacherBookingsViewModel::class.java) ->
                TeacherBookingsViewModel() as T

            // Student
            modelClass.isAssignableFrom(DiscoveryViewModel::class.java) ->
                DiscoveryViewModel() as T

            modelClass.isAssignableFrom(TeacherProfileViewModel::class.java) ->
                TeacherProfileViewModel() as T

            modelClass.isAssignableFrom(BookingViewModel::class.java) ->
                BookingViewModel() as T

            modelClass.isAssignableFrom(PaymentViewModel::class.java) ->
                PaymentViewModel() as T

            else -> throw IllegalArgumentException(
                "Unknown ViewModel class: ${modelClass.name}"
            )
        }
    }
}
