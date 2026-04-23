package com.echologics.thesmartonlineacademy.ui.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.echologics.thesmartonlineacademy.R
import com.echologics.thesmartonlineacademy.data.model.User
import com.echologics.thesmartonlineacademy.ui.common.theme.Purple
import com.echologics.thesmartonlineacademy.ui.common.theme.PurpleLight

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    role: String,
    onLoginSuccess: (User) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val webClientId = stringResource(id = R.string.default_web_client_id)

    LaunchedEffect(uiState.loggedInUser) {
        uiState.loggedInUser?.let { onLoginSuccess(it) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Role badge
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = PurpleLight
        ) {
            Text(
                text = if (role == "teacher") "Signing in as Teacher" else "Signing in as Student",
                fontSize = 13.sp,
                color = Purple,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            )
        }

        Spacer(Modifier.height(32.dp))

        Text(
            text = "Welcome to\nThe Smart Online Academy",
            fontSize = 26.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            lineHeight = 34.sp
        )

        Spacer(Modifier.height(10.dp))

        Text(
            text = if (role == "teacher")
                "Sign in to manage your bookings, sessions and profile"
            else
                "Sign in to discover teachers and book sessions",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
            textAlign = TextAlign.Center,
            lineHeight = 21.sp
        )

        Spacer(Modifier.height(48.dp))

        // Google Sign-In button
        OutlinedButton(
            onClick = {
                viewModel.signInWithGoogle(
                    context = context,
                    role = role,
                    webClientId = webClientId
                )
            },
            enabled = !uiState.isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Purple,
                    strokeWidth = 2.dp
                )
            } else {
                // Google G logo drawn with colored squares (no image asset needed)
                GoogleLogo()
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "Continue with Google",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Error message
        if (uiState.error != null) {
            Spacer(Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = uiState.error!!,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        Text(
            text = "By continuing, you agree to our Terms and Conditions",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
            textAlign = TextAlign.Center
        )
    }
}

// Simple Google G composed of 4 colored boxes — no SVG or image file needed
@Composable
private fun GoogleLogo() {
    Box(modifier = Modifier.size(20.dp)) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val s = size.width
            val h = s / 2f
            // Blue (top-left)
            drawRect(color = Color(0xFF4285F4), topLeft = androidx.compose.ui.geometry.Offset(0f, 0f), size = androidx.compose.ui.geometry.Size(h, h))
            // Red (top-right)
            drawRect(color = Color(0xFFEA4335), topLeft = androidx.compose.ui.geometry.Offset(h, 0f), size = androidx.compose.ui.geometry.Size(h, h))
            // Yellow (bottom-left)
            drawRect(color = Color(0xFFFBBC05), topLeft = androidx.compose.ui.geometry.Offset(0f, h), size = androidx.compose.ui.geometry.Size(h, h))
            // Green (bottom-right)
            drawRect(color = Color(0xFF34A853), topLeft = androidx.compose.ui.geometry.Offset(h, h), size = androidx.compose.ui.geometry.Size(h, h))
        }
    }
}