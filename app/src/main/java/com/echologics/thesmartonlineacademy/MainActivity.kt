package com.echologics.thesmartonlineacademy

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.runtime.*
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.echologics.thesmartonlineacademy.data.model.User
import com.echologics.thesmartonlineacademy.navigation.AppNavigation
import com.echologics.thesmartonlineacademy.navigation.Screen
import com.echologics.thesmartonlineacademy.notifications.NotificationHelper
import com.echologics.thesmartonlineacademy.ui.common.components.DisabledAccountDialog
import com.echologics.thesmartonlineacademy.ui.common.theme.TheSmartOnlineAcademyTheme
import com.echologics.thesmartonlineacademy.ui.session.SessionViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class MainActivity : ComponentActivity() {

    private var showDisabledDialogState: MutableState<Boolean>? = null

    private var screenShareCallback: ((Int, android.content.Intent) -> Unit)? = null

    private val screenCaptureLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            screenShareCallback?.invoke(result.resultCode, result.data!!)
        }
        screenShareCallback = null
    }

    fun requestScreenCapture(onResult: (Int, android.content.Intent) -> Unit) {
        screenShareCallback = onResult
        val mgr = getSystemService(MEDIA_PROJECTION_SERVICE) as android.media.projection.MediaProjectionManager
        screenCaptureLauncher.launch(mgr.createScreenCaptureIntent())
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        var startDestination: String? = null
        splashScreen.setKeepOnScreenCondition { startDestination == null }

        setContent {
            TheSmartOnlineAcademyTheme {
                var dest by remember { mutableStateOf<String?>(null) }
                var showDisabledDialog by remember { mutableStateOf(false) }

                // Store reference so onResume can update it directly
                showDisabledDialogState = remember { mutableStateOf(false) }
                showDisabledDialog = showDisabledDialogState!!.value

                LaunchedEffect(Unit) {
                    val (destination, isDisabled) = resolveStartDestination()
                    showDisabledDialogState?.value = isDisabled
                    dest = destination
                    startDestination = destination
                }

                if (showDisabledDialog) {
                    DisabledAccountDialog(
                        onDismiss = { showDisabledDialogState?.value = false }
                    )
                }

                dest?.let {
                    AppNavigation(startDestination = it)
                }
            }
        }

        NotificationHelper.createChannel(this)
    }

    private var isFirstResume = true

    override fun onResume() {
        super.onResume()
        if (isFirstResume) {
            isFirstResume = false
            return
        }
        checkIfUserDisabled()
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        enterPipIfSessionActive()
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: android.content.res.Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        activePipSession?.onPipModeChanged(isInPictureInPictureMode)
    }

    private fun enterPipIfSessionActive() {
        val vm = activePipSession ?: return
        if (!vm.uiState.value.isSessionActive) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val params = android.app.PictureInPictureParams.Builder()
                .setAspectRatio(android.util.Rational(16, 9))
                .build()
            enterPictureInPictureMode(params)
        }
    }

    private fun checkIfUserDisabled() {
        val firebaseUser = FirebaseAuth.getInstance().currentUser ?: return

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(firebaseUser.uid)
            .get()
            .addOnSuccessListener { doc ->
                val user = doc.toObject(User::class.java) ?: return@addOnSuccessListener
                if (user.disabled == true) {
                    FirebaseAuth.getInstance().signOut()
                    // ✅ Just show the dialog and update dest — no activity restart
                    showDisabledDialogState?.value = true
                }
            }
    }

    private suspend fun resolveStartDestination(): Pair<String, Boolean> {
        val firebaseUser = FirebaseAuth.getInstance().currentUser
            ?: return Pair(Screen.RoleSelect.route, false)

        return try {
            val doc = FirebaseFirestore.getInstance()
                .collection("users")
                .document(firebaseUser.uid)
                .get()
                .await()

            val user = doc.toObject(User::class.java)
            when {
                user == null -> Pair(Screen.RoleSelect.route, false)
                user.disabled == true -> {
                    FirebaseAuth.getInstance().signOut()
                    Pair(Screen.RoleSelect.route, true)
                }
                user.role.name.lowercase() == "admin" -> Pair(Screen.AdminPanel.route, false)
                !user.onboardingComplete && user.role.name.lowercase() == "teacher" -> Pair(Screen.TeacherOnboarding.route, false)
                !user.onboardingComplete && user.role.name.lowercase() == "student" -> Pair(Screen.StudentOnboarding.route, false)
                user.role.name.lowercase() == "teacher" -> Pair(Screen.TeacherHome.route, false)
                else -> Pair(Screen.StudentHome.route, false)
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "resolveStartDestination failed", e)
            Pair(Screen.RoleSelect.route, false)
        }
    }

    companion object {
        // SessionScreen registers the active ViewModel here so MainActivity can talk to it
        var activePipSession: SessionViewModel? = null
    }
}
