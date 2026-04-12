package com.echologics.thesmartonlineacademy.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.echologics.thesmartonlineacademy.data.repository.AuthRepository
import com.echologics.thesmartonlineacademy.ui.auth.*
import com.echologics.thesmartonlineacademy.ui.onboarding.student.*
import com.echologics.thesmartonlineacademy.ui.onboarding.teacher.*
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
}

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.RoleSelect.route
) {

    // ✅ Singleton-style repository (NO recomposition issues)
    val authRepository = remember { AuthRepository() }

    // ✅ Single factory for ALL ViewModels
    val factory = remember { AppViewModelFactory(authRepository) }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {

        // ---------------- ROLE SELECT ----------------
        composable(Screen.RoleSelect.route) {
            RoleSelectScreen(
                onTeacherSelected = {
                    navController.navigate(Screen.Login.createRoute("teacher"))
                },
                onStudentSelected = {
                    navController.navigate(Screen.Login.createRoute("student"))
                }
            )
        }

        // ---------------- LOGIN ----------------
        composable(Screen.Login.route) { backStack ->

            val role = backStack.arguments?.getString("role") ?: "student"

            val vm: LoginViewModel = viewModel(factory = factory)

            LoginScreen(
                viewModel = vm,
                role = role,
                onLoginSuccess = { user ->

                    val destination = when {
//                        !user.onboardingComplete && user.role.name.lowercase() == "teacher" ->
//                            Screen.TeacherOnboarding.route

                        user.role.name.lowercase() == "teacher" ->
                            Screen.TeacherHome.route   // bypass onboarding

                        !user.onboardingComplete && user.role.name.lowercase() == "student" ->
                            Screen.StudentOnboarding.route

                        user.role.name.lowercase() == "teacher" ->
                            Screen.TeacherHome.route

                        else ->
                            Screen.StudentHome.route
                    }

                    navController.navigate(destination) {
                        popUpTo(0)
                    }
                },
                onSignupClick = {
                    navController.navigate(Screen.Signup.createRoute(role))
                }
            )
        }

        // ---------------- SIGNUP ----------------
        composable(Screen.Signup.route) { backStack ->

            val role = backStack.arguments?.getString("role") ?: "student"

            val vm: SignupViewModel = viewModel(factory = factory)

            SignupScreen(
                viewModel = vm,
                role = role,
                onSignupSuccess = {
                    val destination = if (role == "teacher")
                        Screen.TeacherOnboarding.route
                    else
                        Screen.StudentOnboarding.route

                    navController.navigate(destination) {
                        popUpTo(0)
                    }
                },
                onLoginClick = {
                    navController.popBackStack()
                }
            )
        }

        // ---------------- TEACHER ONBOARDING ----------------
        composable(Screen.TeacherOnboarding.route) {

            val vm: TeacherOnboardingViewModel = viewModel(factory = factory)

            TeacherOnboardingScreen(
                viewModel = vm,
                onComplete = {
                    navController.navigate(Screen.TeacherHome.route) {
                        popUpTo(0)
                    }
                }
            )
        }

        // ---------------- STUDENT ONBOARDING ----------------
        composable(Screen.StudentOnboarding.route) {

            val vm: StudentOnboardingViewModel = viewModel(factory = factory)

            StudentOnboardingScreen(
                viewModel = vm,
                onComplete = {
                    navController.navigate(Screen.StudentHome.route) {
                        popUpTo(0)
                    }
                }
            )
        }

        // ---------------- HOME SCREENS ----------------
        composable(Screen.TeacherHome.route) {
            androidx.compose.material3.Text("Teacher Home — Coming soon")
        }

        composable(Screen.StudentHome.route) {
            androidx.compose.material3.Text("Student Home — Coming soon")
        }
    }
}
