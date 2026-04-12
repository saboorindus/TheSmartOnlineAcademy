package com.echologics.thesmartonlineacademy.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.echologics.thesmartonlineacademy.data.model.User
import com.echologics.thesmartonlineacademy.ui.common.components.AppTextField
import com.echologics.thesmartonlineacademy.ui.common.components.ErrorBanner
import com.echologics.thesmartonlineacademy.ui.common.components.OutlinedPrimaryButton
import com.echologics.thesmartonlineacademy.ui.common.components.PrimaryButton

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    role: String,
    onLoginSuccess: (User) -> Unit,
    onSignupClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.loggedInUser) {
        uiState.loggedInUser?.let { onLoginSuccess(it) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Welcome back",
            fontSize = 28.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "Log in as ${if (role == "teacher") "Teacher" else "Student"}",
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
            modifier = Modifier.padding(top = 4.dp, bottom = 32.dp)
        )

        AppTextField(
            value = uiState.email,
            onValueChange = viewModel::onEmailChange,
            label = "Email address",
            isError = uiState.error != null
        )

        Spacer(modifier = Modifier.height(12.dp))

        AppTextField(
            value = uiState.password,
            onValueChange = viewModel::onPasswordChange,
            label = "Password",
            isPassword = true,
            isError = uiState.error != null
        )

        if (uiState.error != null) {
            Spacer(modifier = Modifier.height(12.dp))
            ErrorBanner(message = uiState.error!!)
        }

        Spacer(modifier = Modifier.height(24.dp))

        PrimaryButton(
            text = "Log in",
            onClick = viewModel::login,
            isLoading = uiState.isLoading
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedPrimaryButton(
            text = "Create an account",
            onClick = onSignupClick
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(
            onClick = { /* TODO: forgot password */ },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text(
                text = "Forgot password?",
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}