package com.echologics.thesmartonlineacademy.ui.session

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.echologics.thesmartonlineacademy.data.model.Booking
import com.echologics.thesmartonlineacademy.data.model.SessionRole
import com.google.firebase.auth.FirebaseAuth

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

    // Check permissions on entry
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

    // Start session once permissions are granted and session not already active
    LaunchedEffect(permissionsGranted) {
        if (permissionsGranted && !uiState.isSessionActive) {
            sessionViewModel.initSession(context, booking, role)
        }
    }

    // Navigate away when session ends
    LaunchedEffect(uiState.isEnded) {
        if (uiState.isEnded) {
            sessionViewModel.stopSessionService(context)
            onSessionEnded()
        }
    }

    // End confirm dialog
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

    // Permission gate
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

    // ── Main UI ───────────────────────────────────────────────────────────────

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A1A)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            // Error
            uiState.error?.let {
                Text(it, color = Color.Red, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
            }

            // Connection state
            Text(
                text = uiState.connectionState,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (uiState.isSessionActive) "Local UID: ${uiState.localUid}" else "",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 12.sp
            )
            Text(
                text = if (uiState.remoteUid != null) "Remote UID: ${uiState.remoteUid}" else "Waiting for other participant...",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 12.sp
            )

            Spacer(Modifier.height(40.dp))

            Button(
                onClick = { showEndConfirm = true },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE24B4A))
            ) {
                Text("End session")
            }
        }
    }
}