package com.echologics.thesmartonlineacademy

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.runtime.*
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.echologics.thesmartonlineacademy.data.model.User
import com.echologics.thesmartonlineacademy.navigation.AppNavigation
import com.echologics.thesmartonlineacademy.navigation.NavArgs
import com.echologics.thesmartonlineacademy.navigation.Screen
import com.echologics.thesmartonlineacademy.notifications.NotificationHelper
import com.echologics.thesmartonlineacademy.ui.common.components.DisabledAccountDialog
import com.echologics.thesmartonlineacademy.ui.common.theme.TheSmartOnlineAcademyTheme
import com.echologics.thesmartonlineacademy.ui.session.SessionViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import androidx.core.content.edit
import androidx.core.net.toUri

class MainActivity : ComponentActivity() {

    private var showDisabledDialogState: MutableState<Boolean>? = null

    private var screenShareCallback: ((Int, Intent) -> Unit)? = null

    private val screenCaptureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            screenShareCallback?.invoke(result.resultCode, result.data!!)
        }
        screenShareCallback = null
    }


    fun requestScreenCapture(onResult: (Int, Intent) -> Unit) {
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
                val navController = androidx.navigation.compose.rememberNavController()
                var dest by remember { mutableStateOf<String?>(null) }
                var showDisabledDialog by remember { mutableStateOf(false) }

                showDisabledDialogState = remember { mutableStateOf(false) }
                showDisabledDialog = showDisabledDialogState!!.value

                LaunchedEffect(Unit) {
                    val (destination, isDisabled) = resolveStartDestination()
                    showDisabledDialogState?.value = isDisabled
                    dest = destination
                    startDestination = destination
                }

                LaunchedEffect(dest) {
                    if (dest != null) {
                        val prefs = getSharedPreferences("active_session", MODE_PRIVATE)
                        val bookingId = prefs.getString("booking_id", null)
                        val roleStr = prefs.getString("role", null)
                        if (bookingId != null && roleStr != null) {
                            try {
                                val doc = FirebaseFirestore.getInstance()
                                    .collection("bookings")
                                    .document(bookingId)
                                    .get()
                                    .await()
                                val booking = doc.toObject(com.echologics.thesmartonlineacademy.data.model.Booking::class.java)
                                if (booking != null && booking.status.name != "COMPLETED") {
                                    NavArgs.sessionBooking = booking
                                    navController.navigate(Screen.Session.createRoute(roleStr.lowercase()))
                                } else {
                                    prefs.edit { clear() }
                                }
                            } catch (_: Exception) {
                                prefs.edit { clear() }
                            }
                        }
                    }
                }

                if (showDisabledDialog) {
                    DisabledAccountDialog(
                        onDismiss = { showDisabledDialogState?.value = false }
                    )
                }

                dest?.let {
                    AppNavigation(navController = navController, startDestination = it)
                }
            }
        }

        NotificationHelper.createChannel(this)
        requestIgnoreBatteryOptimization()
    }
    @SuppressLint("BatteryLife")
    private fun requestIgnoreBatteryOptimization() {
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            val intent = Intent(
                android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
            ).apply {
                data = "package:$packageName".toUri()
            }
            startActivity(intent)
        }
    }


    private var isFirstResume = true

    override fun onResume() {
        super.onResume()
        if (isFirstResume) {
            isFirstResume = false
            return
        }
        activePipSession?.onReturnFromBackground()
        Log.d("SessionReturn", "onResume — activePipSession=${activePipSession != null}")
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
        if (vm.uiState.value.isScreenSharing) return   // ← add this
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
