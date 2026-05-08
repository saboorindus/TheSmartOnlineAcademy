package com.echologics.thesmartonlineacademy.ui.session

import android.Manifest
import android.view.SurfaceView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.echologics.thesmartonlineacademy.data.model.Booking
import com.echologics.thesmartonlineacademy.data.model.SessionRole

@Composable
fun SessionScreen(
    sessionViewModel: SessionViewModel,
    booking: Booking,
    role: SessionRole,
    onSessionEnded: () -> Unit
) {
    val uiState by sessionViewModel.uiState.collectAsState()
    val context = LocalContext.current
    var permissionsGranted by remember { mutableStateOf(false) }
    var showEndConfirm by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        permissionsGranted = perms[Manifest.permission.CAMERA] == true &&
                perms[Manifest.permission.RECORD_AUDIO] == true
    }

    LaunchedEffect(Unit) {
        val camOk = androidx.core.content.ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val micOk = androidx.core.content.ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (camOk && micOk) permissionsGranted = true
        else permissionLauncher.launch(
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
        )
    }

    LaunchedEffect(permissionsGranted) {
        if (permissionsGranted && !uiState.isSessionActive) {
            sessionViewModel.initSession(context, booking, role)
        }
    }

    LaunchedEffect(uiState.isEnded) {
        if (uiState.isEnded) {
            sessionViewModel.stopSessionService(context)
            onSessionEnded()
        }
    }

    if (showEndConfirm) {
        AlertDialog(
            onDismissRequest = { showEndConfirm = false },
            title = { Text("End session?") },
            text = { Text("This will end the session for both participants.") },
            confirmButton = {
                TextButton(onClick = {
                    showEndConfirm = false
                    sessionViewModel.endSession(context)
                }) { Text("End session", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showEndConfirm = false }) { Text("Cancel") }
            }
        )
    }

    if (!permissionsGranted) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Camera and microphone access needed", fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(8.dp))
                Button(onClick = {
                    permissionLauncher.launch(
                        arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
                    )
                }) { Text("Grant permissions") }
            }
        }
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A1A))
    ) {

        // ── Remote video (full screen) ─────────────────────────────────────────
        if (uiState.isRemoteVideoVisible && uiState.remoteUid != null) {
            key(uiState.remoteVideoKey) {
                AndroidView(
                    factory = { ctx ->
                        SurfaceView(ctx).also { view ->
                            sessionViewModel.setupRemoteVideo(view, uiState.remoteUid!!)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            // Waiting placeholder
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(32.dp))
                    Spacer(Modifier.height(12.dp))
                    Text(
                        uiState.connectionState,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp
                    )
                }
            }
        }

        // ── Local video (pip top-right) ────────────────────────────────────────
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .size(width = 110.dp, height = 150.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF2A2A2A))
        ) {
            AndroidView(
                factory = { ctx ->
                    SurfaceView(ctx).also { view ->
                        sessionViewModel.setupLocalVideo(view)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // ── End button (bottom center) ─────────────────────────────────────────
        Button(
            onClick = { showEndConfirm = true },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 40.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE24B4A))
        ) {
            Text("End session")
        }
    }
}