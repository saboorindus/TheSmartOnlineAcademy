package com.echologics.thesmartonlineacademy.ui.onboarding.student

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.echologics.thesmartonlineacademy.ui.common.components.AppTextField
import com.echologics.thesmartonlineacademy.ui.common.components.PrimaryButton
import com.echologics.thesmartonlineacademy.ui.common.components.SelectableChip
import com.echologics.thesmartonlineacademy.ui.common.components.StepProgressBar
import androidx.compose.ui.text.input.ImeAction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentOnboardingScreen(
    viewModel: StudentOnboardingViewModel,
    onComplete: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isComplete) {
        if (uiState.isComplete) onComplete()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Getting started") },
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
                1 -> StudentStep1Goals(uiState = uiState, viewModel = viewModel)
                2 -> StudentStep2Preferences(uiState = uiState, viewModel = viewModel)
                3 -> StudentStep3Ready(uiState = uiState, viewModel = viewModel)
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun StudentStep1Goals(uiState: StudentOnboardingUiState, viewModel: StudentOnboardingViewModel) {
    val subjects = listOf("Mathematics", "Physics", "Chemistry", "Biology", "English", "Urdu", "History", "Computer Science", "Economics")
    val levels = listOf("Primary", "O-Level", "A-Level", "University", "Professional")
    val goals = listOf("Exam prep", "Catch up with class", "Learn something new", "Build a work skill")
    val nameFocus = remember { FocusRequester() }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {

        SectionLabel("Enter full name")
        AppTextField(
            value = uiState.fullName,
            onValueChange = viewModel::onFullNameChange,
            label = "Full name",
            isError = uiState.error != null,
            modifier = Modifier.focusRequester(nameFocus),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),

        )
        Spacer(Modifier.height(16.dp))
        Text("Your learning goals", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        Text("Help us find the right teacher for you", fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f), modifier = Modifier.padding(top = 4.dp, bottom = 20.dp))

        SectionLabel("Subject(s) you need help with")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            subjects.forEach { s ->
                SelectableChip(label = s, selected = uiState.subjects.contains(s), onClick = { viewModel.toggleSubject(s) })
            }
        }

        Spacer(Modifier.height(16.dp))
        SectionLabel("Your current level")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            levels.forEach { l ->
                SelectableChip(label = l, selected = uiState.level == l, onClick = { viewModel.onLevelChange(l) })
            }
        }

        Spacer(Modifier.height(16.dp))
        SectionLabel("Main goal")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            goals.forEach { g ->
                SelectableChip(label = g, selected = uiState.goal == g, onClick = { viewModel.onGoalChange(g) })
            }
        }

        Spacer(Modifier.height(24.dp))
        PrimaryButton(
            text = "Continue",
            onClick = viewModel::nextStep,
            enabled = uiState.subjects.isNotEmpty() && uiState.level.isNotBlank() && uiState.goal.isNotBlank() && uiState.fullName.isNotBlank()
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)

private fun StudentStep2Preferences(uiState: StudentOnboardingUiState, viewModel: StudentOnboardingViewModel) {
    val languages = listOf("English", "Urdu", "Punjabi", "Arabic", "French")
    val availPrefs = listOf("Weekday mornings", "Weekday evenings", "Weekend mornings", "Weekend afternoons", "Weekend evenings")

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Text("Your preferences", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        Text("We'll use these to filter results", fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f), modifier = Modifier.padding(top = 4.dp, bottom = 20.dp))

        SectionLabel("Preferred teaching language")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            languages.forEach { l ->
                SelectableChip(label = l, selected = uiState.preferredLanguage == l, onClick = { viewModel.onLanguageChange(l) })
            }
        }

        Spacer(Modifier.height(16.dp))
        SectionLabel("Timezone (auto-detected)")
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Text(uiState.timezone, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium)
        }

        Spacer(Modifier.height(16.dp))
        SectionLabel("When are you usually free?")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            availPrefs.forEach { p ->
                SelectableChip(label = p, selected = uiState.availabilityPrefs.contains(p), onClick = { viewModel.toggleAvailabilityPref(p) })
            }
        }

        Spacer(Modifier.height(24.dp))
        PrimaryButton(text = "Continue", onClick = viewModel::nextStep)
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun StudentStep3Ready(uiState: StudentOnboardingUiState, viewModel: StudentOnboardingViewModel) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Text("All set!", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        Text("Here's what we'll look for", fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f), modifier = Modifier.padding(top = 4.dp, bottom = 20.dp))

        ReviewRow("Subjects", uiState.subjects.joinToString(", "))
        ReviewRow("Level", uiState.level)
        ReviewRow("Goal", uiState.goal)
        ReviewRow("Language", uiState.preferredLanguage)
        ReviewRow("Timezone", uiState.timezone)
        ReviewRow("Availability", uiState.availabilityPrefs.joinToString(", ").ifBlank { "Any" })

        Spacer(Modifier.height(24.dp))
        PrimaryButton(
            text = "Find teachers",
            onClick = viewModel::submit,
            isLoading = uiState.isLoading
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SectionLabel(label: String) {
    Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun ReviewRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f), modifier = Modifier.weight(0.4f))
        Text(value.ifBlank { "—" }, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(0.6f))
    }
    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline)
}