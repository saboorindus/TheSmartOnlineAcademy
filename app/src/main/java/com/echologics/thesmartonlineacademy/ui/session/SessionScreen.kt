package com.echologics.thesmartonlineacademy.ui.session

import android.Manifest
import android.view.SurfaceView
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.echologics.thesmartonlineacademy.MainActivity
import com.echologics.thesmartonlineacademy.data.model.Booking
import com.echologics.thesmartonlineacademy.data.model.SessionRole
import com.echologics.thesmartonlineacademy.ui.session.whiteboard.WhiteboardViewModel

@Composable
fun SessionScreen(
    sessionViewModel: SessionViewModel,
    whiteboardViewModel: WhiteboardViewModel,
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

    // Register this ViewModel as the active PiP session
    DisposableEffect(Unit) {
        MainActivity.activePipSession = sessionViewModel
        onDispose {
            MainActivity.activePipSession = null
            sessionViewModel.unbindService(context)
        }
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
            text = { Text("This will end the session for both participants and mark it as completed.") },
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

        // ── Remote video — always visible including in PiP ─────────────────────
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

        // ── Everything below is hidden in PiP ─────────────────────────────────
        if (!uiState.isInPipMode) {

            // Local video pip
            if (!uiState.isCameraOff) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 72.dp, end = 16.dp)
                        .size(width = 110.dp, height = 150.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF2A2A2A))
                ) {
                    key(uiState.isCameraOff) {
                        AndroidView(
                            factory = { ctx ->
                                SurfaceView(ctx).also { view ->
                                    sessionViewModel.setupLocalVideo(view)
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            // Top bar
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = if (role == SessionRole.TEACHER) booking.studentName
                        else booking.teacherName,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "${booking.subject} · ${booking.sessionLength}",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp
                    )
                }
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (uiState.isSessionActive) Color(0xFFE24B4A) else Color.DarkGray
                ) {
                    Text(
                        text = if (uiState.isSessionActive) "LIVE" else uiState.connectionState,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // Controls bar
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .background(Color(0xCC000000))
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ControlButton(
                    icon = if (uiState.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                    label = if (uiState.isMuted) "Unmute" else "Mute",
                    active = !uiState.isMuted,
                    onClick = sessionViewModel::toggleMute
                )
                ControlButton(
                    icon = if (uiState.isCameraOff) Icons.Default.VideocamOff else Icons.Default.Videocam,
                    label = if (uiState.isCameraOff) "Cam off" else "Cam on",
                    active = !uiState.isCameraOff,
                    onClick = sessionViewModel::toggleCamera
                )
                ControlButton(
                    icon = if (uiState.isSpeakerOn) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                    label = if (uiState.isSpeakerOn) "Speaker" else "Earpiece",
                    active = uiState.isSpeakerOn,
                    onClick = sessionViewModel::toggleSpeaker
                )
                ControlButton(
                    icon = Icons.Default.Cameraswitch,
                    label = "Flip",
                    active = false,
                    onClick = sessionViewModel::switchCamera
                )
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE24B4A))
                        .clickable(interactionSource = null, indication = null) {
                            showEndConfirm = true
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.CallEnd,
                        contentDescription = "End session",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

        } // end isInPipMode check
    }
}

@Composable
private fun ControlButton(
    icon: ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(interactionSource = null, indication = null) { onClick() }
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(if (active) Color(0xFF2A2A2A) else Color(0xFF3A3A3A)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (active) Color.White else Color(0xFFAAAAAA),
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(Modifier.height(3.dp))
        Text(label, color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp)
    }
}