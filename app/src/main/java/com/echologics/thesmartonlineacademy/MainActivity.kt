package com.echologics.thesmartonlineacademy

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.runtime.*
import com.echologics.thesmartonlineacademy.data.model.User
import com.echologics.thesmartonlineacademy.navigation.AppNavigation
import com.echologics.thesmartonlineacademy.navigation.Screen
import com.echologics.thesmartonlineacademy.notifications.NotificationHelper
import com.echologics.thesmartonlineacademy.ui.common.theme.TheSmartOnlineAcademyTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class MainActivity : ComponentActivity() {

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TheSmartOnlineAcademyTheme {
                var startDestination by remember { mutableStateOf<String?>(null) }

                LaunchedEffect(Unit) {
                    startDestination = resolveStartDestination()
                }

                startDestination?.let { dest ->
                    AppNavigation(startDestination = dest)
                }
            }
        }

        NotificationHelper.createChannels(this)
    }

    private suspend fun resolveStartDestination(): String {
        val firebaseUser = FirebaseAuth.getInstance().currentUser
            ?: return Screen.RoleSelect.route

        return try {
            val doc = FirebaseFirestore.getInstance()
                .collection("users")
                .document(firebaseUser.uid)
                .get()
                .await()

            val user = doc.toObject(User::class.java)
            when {
                user == null -> Screen.RoleSelect.route
                !user.onboardingComplete && user.role.name.lowercase() == "teacher" -> Screen.TeacherOnboarding.route
                !user.onboardingComplete && user.role.name.lowercase() == "student" -> Screen.StudentOnboarding.route
                user.role.name.lowercase() == "teacher" -> Screen.TeacherHome.route
                else -> Screen.StudentHome.route
            }
        } catch (_: Exception) {
            Screen.RoleSelect.route
        }
    }
}

