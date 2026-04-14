package com.echologics.thesmartonlineacademy.utils

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.echologics.thesmartonlineacademy.data.repository.AdminRepository
import com.echologics.thesmartonlineacademy.data.repository.AuthRepository
import com.echologics.thesmartonlineacademy.data.repository.BookingRepository
import com.echologics.thesmartonlineacademy.data.repository.MessagingRepository
import com.echologics.thesmartonlineacademy.data.repository.ReviewRepository
import com.echologics.thesmartonlineacademy.ui.admin.AdminDashboardViewModel
import com.echologics.thesmartonlineacademy.ui.auth.LoginViewModel
import com.echologics.thesmartonlineacademy.ui.auth.SignupViewModel
import com.echologics.thesmartonlineacademy.ui.messaging.ChatViewModel
import com.echologics.thesmartonlineacademy.ui.messaging.ConversationListViewModel
import com.echologics.thesmartonlineacademy.ui.onboarding.student.StudentOnboardingViewModel
import com.echologics.thesmartonlineacademy.ui.onboarding.teacher.TeacherOnboardingViewModel
import com.echologics.thesmartonlineacademy.ui.review.ReviewViewModel
import com.echologics.thesmartonlineacademy.ui.session.SessionViewModel
import com.echologics.thesmartonlineacademy.ui.session.whiteboard.WhiteboardViewModel
import com.echologics.thesmartonlineacademy.ui.student.booking.BookingViewModel
import com.echologics.thesmartonlineacademy.ui.student.bookinghistory.BookingHistoryViewModel
import com.echologics.thesmartonlineacademy.ui.student.discovery.DiscoveryViewModel
import com.echologics.thesmartonlineacademy.ui.student.payment.PaymentViewModel
import com.echologics.thesmartonlineacademy.ui.student.teacherprofile.TeacherProfileViewModel
import com.echologics.thesmartonlineacademy.ui.teacher.bookings.TeacherBookingsViewModel

class AppViewModelFactory(
    private val authRepository: AuthRepository,
    private val messagingRepository: MessagingRepository,
    private val adminRepository: AdminRepository,
    private val bookingRepository: BookingRepository,
    private val reviewRepository: ReviewRepository
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

            // Session
            modelClass.isAssignableFrom(SessionViewModel::class.java) ->
                SessionViewModel() as T

            modelClass.isAssignableFrom(WhiteboardViewModel::class.java) ->
                WhiteboardViewModel() as T

            modelClass.isAssignableFrom(ConversationListViewModel::class.java) ->
                ConversationListViewModel(messagingRepository) as T

            modelClass.isAssignableFrom(ChatViewModel::class.java) ->
                ChatViewModel(messagingRepository) as T

            modelClass.isAssignableFrom(AdminDashboardViewModel::class.java) ->
                AdminDashboardViewModel(adminRepository) as T

            modelClass.isAssignableFrom(BookingHistoryViewModel::class.java) ->
                BookingHistoryViewModel(bookingRepository,reviewRepository) as T

            modelClass.isAssignableFrom(ReviewViewModel::class.java) ->
                ReviewViewModel(reviewRepository) as T

            else -> throw IllegalArgumentException(
                "Unknown ViewModel class: ${modelClass.name}"
            )
        }
    }
}
