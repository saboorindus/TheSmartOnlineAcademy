package com.echologics.thesmartonlineacademy.ui.onboarding.teacher

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.echologics.thesmartonlineacademy.ui.common.components.StepProgressBar
import com.echologics.thesmartonlineacademy.ui.onboarding.teacher.steps.Step1PersonalInfo
import com.echologics.thesmartonlineacademy.ui.onboarding.teacher.steps.Step2TeachingDetails
import com.echologics.thesmartonlineacademy.ui.onboarding.teacher.steps.Step3Credentials
import com.echologics.thesmartonlineacademy.ui.onboarding.teacher.steps.Step4RateAndSessions
import com.echologics.thesmartonlineacademy.ui.onboarding.teacher.steps.Step5Availability
import com.echologics.thesmartonlineacademy.ui.onboarding.teacher.steps.Step6ReviewAndSubmit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherOnboardingScreen(
    viewModel: TeacherOnboardingViewModel,
    onComplete: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isComplete) {
        if (uiState.isComplete) onComplete()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Teacher profile") },
                navigationIcon = {
                    if (uiState.currentStep > 1) {
                        IconButton(onClick = viewModel::prevStep) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            StepProgressBar(
                currentStep = uiState.currentStep,
                totalSteps = uiState.totalSteps
            )

            Spacer(modifier = Modifier.height(24.dp))

            when (uiState.currentStep) {
                1 -> Step1PersonalInfo(uiState = uiState, viewModel = viewModel)
                2 -> Step2TeachingDetails(uiState = uiState, viewModel = viewModel)
                3 -> Step3Credentials(uiState = uiState, viewModel = viewModel)
                4 -> Step4RateAndSessions(uiState = uiState, viewModel = viewModel)
                5 -> Step5Availability(uiState = uiState, viewModel = viewModel)
                6 -> Step6ReviewAndSubmit(uiState = uiState, viewModel = viewModel)
            }

            if (uiState.error != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = uiState.error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}