package com.echologics.thesmartonlineacademy.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.echologics.thesmartonlineacademy.data.model.Booking
import com.echologics.thesmartonlineacademy.data.model.TeacherProfile
import com.echologics.thesmartonlineacademy.data.repository.AuthRepository
import com.echologics.thesmartonlineacademy.ui.auth.LoginScreen
import com.echologics.thesmartonlineacademy.ui.auth.LoginViewModel
import com.echologics.thesmartonlineacademy.ui.auth.RoleSelectScreen
import com.echologics.thesmartonlineacademy.ui.auth.SignupScreen
import com.echologics.thesmartonlineacademy.ui.auth.SignupViewModel
import com.echologics.thesmartonlineacademy.ui.onboarding.student.StudentOnboardingScreen
import com.echologics.thesmartonlineacademy.ui.onboarding.student.StudentOnboardingViewModel
import com.echologics.thesmartonlineacademy.ui.onboarding.teacher.TeacherOnboardingScreen
import com.echologics.thesmartonlineacademy.ui.onboarding.teacher.TeacherOnboardingViewModel
import com.echologics.thesmartonlineacademy.ui.student.booking.BookingScreen
import com.echologics.thesmartonlineacademy.ui.student.booking.BookingViewModel
import com.echologics.thesmartonlineacademy.ui.student.discovery.DiscoveryScreen
import com.echologics.thesmartonlineacademy.ui.student.discovery.DiscoveryViewModel
import com.echologics.thesmartonlineacademy.ui.student.payment.PaymentScreen
import com.echologics.thesmartonlineacademy.ui.student.payment.PaymentViewModel
import com.echologics.thesmartonlineacademy.ui.student.teacherprofile.TeacherProfileScreen
import com.echologics.thesmartonlineacademy.ui.student.teacherprofile.TeacherProfileViewModel
import com.echologics.thesmartonlineacademy.ui.teacher.bookings.TeacherBookingsScreen
import com.echologics.thesmartonlineacademy.ui.teacher.bookings.TeacherBookingsViewModel
import com.echologics.thesmartonlineacademy.utils.AppViewModelFactory

sealed class Screen(val route: String) {
    object RoleSelect : Screen("role_select")
    object Login : Screen("login/{role}") {
        fun createRoute(role: String) = "login/$role"
    }
    object Signup : Screen("signup/{role}") {
        fun createRoute(role: String) = "signup/$role"
    }
    object TeacherOnboarding : Screen("teacher_onboarding")
    object StudentOnboarding : Screen("student_onboarding")
    object TeacherHome : Screen("teacher_home")
    object StudentHome : Screen("student_home")
    object TeacherProfile : Screen("teacher_profile/{teacherId}") {
        fun createRoute(teacherId: String) = "teacher_profile/$teacherId"
    }
    object Booking : Screen("booking")
    object Payment : Screen("payment")
    object PaymentSuccess : Screen("payment_success")
}

// Lightweight in-memory store for passing complex objects between screens
object NavArgs {
    var selectedTeacher: TeacherProfile? = null
    var createdBooking: Booking? = null
}

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.RoleSelect.route
) {

    val authRepository = remember { AuthRepository() }
    val factory = remember { AppViewModelFactory(authRepository) }

    NavHost(navController = navController, startDestination = startDestination) {

        // ── Auth ──────────────────────────────────────────────────────────────
        composable(Screen.RoleSelect.route) {
            RoleSelectScreen(
                onTeacherSelected = { navController.navigate(Screen.Login.createRoute("teacher")) },
                onStudentSelected = { navController.navigate(Screen.Login.createRoute("student")) }
            )
        }

        composable(Screen.Login.route) { backStack ->
            val role = backStack.arguments?.getString("role") ?: "student"
            val viewModel: LoginViewModel = viewModel(factory = factory)
            LoginScreen(
                viewModel = viewModel,
                role = role,
                onLoginSuccess = { user ->
                    val dest = when {
                        !user.onboardingComplete && user.role.name.equals("teacher", true) -> Screen.TeacherOnboarding.route
                        !user.onboardingComplete -> Screen.StudentOnboarding.route
                        user.role.name.equals("teacher", true) -> Screen.TeacherHome.route
                        else -> Screen.StudentHome.route
                    }
                    navController.navigate(dest) { popUpTo(0) }
                },
                onSignupClick = { navController.navigate(Screen.Signup.createRoute(role)) }
            )
        }

        composable(Screen.Signup.route) { backStack ->
            val role = backStack.arguments?.getString("role") ?: "student"
            val vm: SignupViewModel = viewModel(factory = factory)

            SignupScreen(
                viewModel = vm,
                role = role,
                onSignupSuccess = {
                    val dest = if (role == "teacher") Screen.TeacherOnboarding.route else Screen.StudentOnboarding.route
                    navController.navigate(dest) { popUpTo(0) }
                },
                onLoginClick = { navController.popBackStack() }
            )
        }

        // ── Onboarding ────────────────────────────────────────────────────────
        composable(Screen.TeacherOnboarding.route) {
            val vm: TeacherOnboardingViewModel = viewModel(factory = factory)

            TeacherOnboardingScreen(
                viewModel = vm,
                onComplete = { navController.navigate(Screen.TeacherHome.route) { popUpTo(0) } }
            )
        }

        composable(Screen.StudentOnboarding.route) {
            val vm: StudentOnboardingViewModel = viewModel(factory = factory)

            StudentOnboardingScreen(
                viewModel = vm,
                onComplete = { navController.navigate(Screen.StudentHome.route) { popUpTo(0) } }
            )
        }

        // ── Teacher home ──────────────────────────────────────────────────────
        composable(Screen.TeacherHome.route) {
            val vm: TeacherBookingsViewModel = viewModel(factory = factory)

            TeacherBookingsScreen(viewModel = vm)
        }

        // ── Student home = Discovery ──────────────────────────────────────────
        composable(Screen.StudentHome.route) {
            val vm: DiscoveryViewModel = viewModel(factory = factory)
            DiscoveryScreen(
                viewModel = vm,
                onTeacherClick = { teacherId ->
                    navController.navigate(Screen.TeacherProfile.createRoute(teacherId))
                }
            )
        }

        // ── Teacher profile view (student sees this) ──────────────────────────
        composable(Screen.TeacherProfile.route) { backStack ->
            val teacherId = backStack.arguments?.getString("teacherId") ?: return@composable
            val vm: TeacherProfileViewModel = viewModel(factory = factory)
            TeacherProfileScreen(
                viewModel = vm,
                teacherId = teacherId,
                onBookClick = { teacher ->
                    NavArgs.selectedTeacher = teacher
                    navController.navigate(Screen.Booking.route)
                },
                onBack = { navController.popBackStack() }
            )
        }

        // ── Booking slot selection ────────────────────────────────────────────
        composable(Screen.Booking.route) {
            val teacher = NavArgs.selectedTeacher ?: run {
                navController.popBackStack()
                return@composable
            }

            val vm: BookingViewModel = viewModel(factory = factory)


            BookingScreen(
                viewModel = vm,
                teacher = teacher,
                onBookingCreated = { booking ->
                    NavArgs.createdBooking = booking
                    navController.navigate(Screen.Payment.route)
                },
                onBack = { navController.popBackStack() }
            )
        }

        // ── Payment (QR + transaction ID) ─────────────────────────────────────
        composable(Screen.Payment.route) {
            val booking = NavArgs.createdBooking ?: run {
                navController.popBackStack()
                return@composable
            }
            val vm: PaymentViewModel = viewModel(factory = factory)

            PaymentScreen(
                viewModel = vm,
                booking = booking,
                onPaymentSubmitted = {
                    navController.navigate(Screen.PaymentSuccess.route) {
                        popUpTo(Screen.StudentHome.route)
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        // ── Payment success confirmation ───────────────────────────────────────
        composable(Screen.PaymentSuccess.route) {
            PaymentSuccessScreen(
                onGoHome = {
                    navController.navigate(Screen.StudentHome.route) { popUpTo(0) }
                }
            )
        }
    }
}

@Composable
private fun PaymentSuccessScreen(onGoHome: () -> Unit) {
    val booking = NavArgs.createdBooking
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Payment submitted!", fontSize = 24.sp, fontWeight = FontWeight.SemiBold)

        Spacer(Modifier.height(12.dp))

        Text(
            text = "Your booking with ${booking?.teacherName ?: "the teacher"} is pending confirmation. " +
                    "You'll be notified once the teacher verifies your payment.",
            textAlign = TextAlign.Center,
            fontSize = 14.sp,
            lineHeight = 22.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f)
        )

        Spacer(Modifier.height(8.dp))

        booking?.let {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    SuccessRow("Teacher", it.teacherName)
                    SuccessRow("Subject", it.subject)
                    SuccessRow("Session", it.sessionLength)
                    SuccessRow("Amount", it.totalAmount)
                    SuccessRow("Txn ID", it.paymentTransactionId)
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = onGoHome,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF534AB7))
        ) {
            Text("Back to home")
        }
    }
}

@Composable
private fun SuccessRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}
