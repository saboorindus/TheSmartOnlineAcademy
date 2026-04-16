package com.echologics.thesmartonlineacademy.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.echologics.thesmartonlineacademy.ui.common.components.AppTextField
import com.echologics.thesmartonlineacademy.ui.common.components.ErrorBanner
import com.echologics.thesmartonlineacademy.ui.common.components.OutlinedPrimaryButton
import com.echologics.thesmartonlineacademy.ui.common.components.PrimaryButton

@Composable
fun SignupScreen(
    viewModel: SignupViewModel,
    role: String,
    onSignupSuccess: () -> Unit,
    onLoginClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current

    val nameFocus = remember { FocusRequester() }
    val emailFocus = remember { FocusRequester() }
    val passwordFocus = remember { FocusRequester() }
    val confirmPasswordFocus = remember { FocusRequester() }

    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.signupComplete) {
        if (uiState.signupComplete) onSignupSuccess()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 40.dp)
    ) {

        Text(
            text = "Create account",
            fontSize = 28.sp,
            fontWeight = FontWeight.SemiBold
        )

        Text(
            text = "Sign up as ${if (role == "teacher") "Teacher" else "Student"}",
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
            modifier = Modifier.padding(top = 4.dp, bottom = 32.dp)
        )

//        AppTextField(
//            value = uiState.fullName,
//            onValueChange = viewModel::onFullNameChange,
//            label = "Full name",
//            isError = uiState.error != null,
//            modifier = Modifier.focusRequester(nameFocus),
//            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
//            keyboardActions = KeyboardActions(
//                onNext = { emailFocus.requestFocus() }
//            )
//        )

        Spacer(modifier = Modifier.height(12.dp))

        AppTextField(
            value = uiState.email,
            onValueChange = viewModel::onEmailChange,
            label = "Email address",
            isError = uiState.error != null,
            modifier = Modifier.focusRequester(emailFocus),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(
                onNext = { passwordFocus.requestFocus() }
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        AppTextField(
            value = uiState.password,
            onValueChange = viewModel::onPasswordChange,
            label = "Password",
            isPassword = true,
            isError = uiState.error != null,
            modifier = Modifier.focusRequester(passwordFocus),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(
                onNext = { confirmPasswordFocus.requestFocus() }
            ),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible)
                            Icons.Default.VisibilityOff
                        else Icons.Default.Visibility,
                        contentDescription = null
                    )
                }
            },
            visualTransformation = if (passwordVisible)
                VisualTransformation.None
            else PasswordVisualTransformation()
        )

        Spacer(modifier = Modifier.height(12.dp))

        AppTextField(
            value = uiState.confirmPassword,
            onValueChange = viewModel::onConfirmPasswordChange,
            label = "Confirm password",
            isPassword = true,
            isError = uiState.error != null,
            modifier = Modifier.focusRequester(confirmPasswordFocus),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = { focusManager.clearFocus() }
            ),
            trailingIcon = {
                IconButton(onClick = {
                    confirmPasswordVisible = !confirmPasswordVisible
                }) {
                    Icon(
                        imageVector = if (confirmPasswordVisible)
                            Icons.Default.VisibilityOff
                        else Icons.Default.Visibility,
                        contentDescription = null
                    )
                }
            },
            visualTransformation = if (confirmPasswordVisible)
                VisualTransformation.None
            else PasswordVisualTransformation()
        )

        if (uiState.error != null) {
            Spacer(modifier = Modifier.height(12.dp))
            ErrorBanner(message = uiState.error!!)
        }

        Spacer(modifier = Modifier.height(24.dp))

        PrimaryButton(
            text = "Create account",
            onClick = { viewModel.signup(role) },
            isLoading = uiState.isLoading
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedPrimaryButton(
            text = "Already have an account? Log in",
            onClick = onLoginClick
        )
    }
}
