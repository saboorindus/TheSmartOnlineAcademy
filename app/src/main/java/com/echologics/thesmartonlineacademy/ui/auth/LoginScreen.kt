package com.echologics.thesmartonlineacademy.ui.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.echologics.thesmartonlineacademy.R
import com.echologics.thesmartonlineacademy.data.model.User
import com.echologics.thesmartonlineacademy.ui.common.theme.Black
import com.echologics.thesmartonlineacademy.ui.common.theme.Purple
import com.echologics.thesmartonlineacademy.ui.common.theme.PurpleLight
import com.echologics.thesmartonlineacademy.ui.terms.TermsDialog

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    role: String,
    onLoginSuccess: (User) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val webClientId = stringResource(id = R.string.default_web_client_id)

    var showTermsDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.loggedInUser) {
        uiState.loggedInUser?.let { onLoginSuccess(it) }
    }

    // Terms dialog
    if (showTermsDialog) {
        TermsDialog(
            role = role,
            onDismiss = { showTermsDialog = false }
        )
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
            color = Black,
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

        // "By continuing..." with tappable blue "Terms and Conditions"
        val termsAnnotatedString = buildAnnotatedString {
            withStyle(
                style = SpanStyle(
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            ) {
                append("By continuing, you agree to our ")
            }
            pushStringAnnotation(tag = "TERMS", annotation = "terms")
            withStyle(
                style = SpanStyle(
                    fontSize = 12.sp,
                    color = Purple,
                    fontWeight = FontWeight.SemiBold
                )
            ) {
                append("Terms and Conditions")
            }
            pop()
        }

        ClickableText(
            text = termsAnnotatedString,
            style = TextStyle(textAlign = TextAlign.Center),
            onClick = { offset ->
                termsAnnotatedString.getStringAnnotations(
                    tag = "TERMS",
                    start = offset,
                    end = offset
                ).firstOrNull()?.let {
                    showTermsDialog = true
                }
            }
        )
    }
}

@Composable
fun GoogleLogo() {
    Icon(
        painter = painterResource(id = R.drawable.google_icon),
        contentDescription = "Google Logo",
        tint = Color.Unspecified,
        modifier = Modifier.size(20.dp)
    )
}