package com.echologics.thesmartonlineacademy.ui.onboarding.teacher.steps

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.echologics.thesmartonlineacademy.ui.common.components.AppTextField
import com.echologics.thesmartonlineacademy.ui.common.components.PrimaryButton
import com.echologics.thesmartonlineacademy.ui.common.components.SelectableChip
import com.echologics.thesmartonlineacademy.ui.onboarding.teacher.TeacherOnboardingUiState
import com.echologics.thesmartonlineacademy.ui.onboarding.teacher.TeacherOnboardingViewModel

// ─── Step 1: Personal Info ───────────────────────────────────────────────────

@Composable
@OptIn(ExperimentalLayoutApi::class)

fun Step1PersonalInfo(uiState: TeacherOnboardingUiState, viewModel: TeacherOnboardingViewModel) {
    val languages = listOf("English", "Urdu", "Punjabi", "Arabic", "French", "German")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        StepHeader("Personal info", "Tell students who you are")

        AppTextField(value = uiState.fullName, onValueChange = viewModel::onFullNameChange, label = "Full name")
        Spacer(Modifier.height(12.dp))
        AppTextField(value = uiState.country, onValueChange = viewModel::onCountryChange, label = "Country")
        Spacer(Modifier.height(16.dp))

        Text("Languages spoken", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            languages.forEach { lang ->
                SelectableChip(label = lang, selected = uiState.languages.contains(lang), onClick = { viewModel.toggleLanguage(lang) })
            }
        }

        Spacer(Modifier.height(16.dp))
        AppTextField(
            value = uiState.bio,
            onValueChange = viewModel::onBioChange,
            label = "Bio (${uiState.bio.length}/300)",
            singleLine = false,
            maxLines = 4
        )

        Spacer(Modifier.height(24.dp))
        PrimaryButton(
            text = "Continue",
            onClick = viewModel::nextStep,
            enabled = uiState.fullName.isNotBlank() && uiState.country.isNotBlank() && uiState.languages.isNotEmpty()
        )
        Spacer(Modifier.height(24.dp))
    }
}

// ─── Step 2: Teaching Details ─────────────────────────────────────────────────

@Composable
@OptIn(ExperimentalLayoutApi::class)

fun Step2TeachingDetails(uiState: TeacherOnboardingUiState, viewModel: TeacherOnboardingViewModel) {
    val subjects = listOf("Mathematics", "Physics", "Chemistry", "Biology", "English", "Urdu", "History", "Computer Science", "Economics", "Accounting")
    val levels = listOf("Primary", "O-Level", "A-Level", "University", "Adult / Professional")
    val styles = listOf("Exam prep", "Conversational", "Step-by-step", "Project-based", "Problem solving")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        StepHeader("Teaching details", "Help students find you by subject")

        SectionLabel("Subjects you teach")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            subjects.forEach { s ->
                SelectableChip(label = s, selected = uiState.subjects.contains(s), onClick = { viewModel.toggleSubject(s) })
            }
        }

        Spacer(Modifier.height(16.dp))
        SectionLabel("Student levels")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            levels.forEach { l ->
                SelectableChip(label = l, selected = uiState.levels.contains(l), onClick = { viewModel.toggleLevel(l) })
            }
        }

        Spacer(Modifier.height(16.dp))
        SectionLabel("Teaching style")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            styles.forEach { s ->
                SelectableChip(label = s, selected = uiState.teachingStyles.contains(s), onClick = { viewModel.toggleTeachingStyle(s) })
            }
        }

        Spacer(Modifier.height(24.dp))
        PrimaryButton(
            text = "Continue",
            onClick = viewModel::nextStep,
            enabled = uiState.subjects.isNotEmpty() && uiState.levels.isNotEmpty()
        )
        Spacer(Modifier.height(24.dp))
    }
}

// ─── Step 3: Credentials ─────────────────────────────────────────────────────

@Composable
fun Step3Credentials(uiState: TeacherOnboardingUiState, viewModel: TeacherOnboardingViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        StepHeader("Credentials", "Build student trust with your background")

        AppTextField(value = uiState.yearsExperience, onValueChange = viewModel::onYearsExperienceChange, label = "Years of teaching experience")
        Spacer(Modifier.height(12.dp))
        AppTextField(
            value = uiState.education,
            onValueChange = viewModel::onEducationChange,
            label = "Highest education (e.g. MSc Physics, UoK)",
            singleLine = false,
            maxLines = 3
        )

        Spacer(Modifier.height(16.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Text(
                text = "Tip: Uploading degree certificates or teaching credentials will earn you a Verified badge on your profile.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(12.dp),
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }

        Spacer(Modifier.height(24.dp))
        PrimaryButton(
            text = "Continue",
            onClick = viewModel::nextStep,
            enabled = uiState.yearsExperience.isNotBlank() && uiState.education.isNotBlank()
        )
        Spacer(Modifier.height(24.dp))
    }
}

// ─── Step 4: Rate & Session Lengths ──────────────────────────────────────────

@Composable
@OptIn(ExperimentalLayoutApi::class)

fun Step4RateAndSessions(uiState: TeacherOnboardingUiState, viewModel: TeacherOnboardingViewModel) {
    val lengths = listOf("30 min", "60 min", "90 min")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        StepHeader("Rate & sessions", "Set your pricing")

        AppTextField(value = uiState.hourlyRate, onValueChange = viewModel::onHourlyRateChange, label = "Hourly rate (e.g. PKR 2500)")
        Spacer(Modifier.height(16.dp))

        SectionLabel("Session lengths offered")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            lengths.forEach { l ->
                SelectableChip(label = l, selected = uiState.sessionLengths.contains(l), onClick = { viewModel.toggleSessionLength(l) })
            }
        }

        Spacer(Modifier.height(16.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Offer a trial session", modifier = Modifier.weight(1f))
            Switch(checked = uiState.trialSessionEnabled, onCheckedChange = viewModel::onTrialSessionToggle)
        }

        if (uiState.trialSessionEnabled) {
            Spacer(Modifier.height(12.dp))
            AppTextField(value = uiState.trialRate, onValueChange = viewModel::onTrialRateChange, label = "Trial session rate (e.g. PKR 1000 / 30 min)")
        }

        Spacer(Modifier.height(24.dp))
        PrimaryButton(
            text = "Continue",
            onClick = viewModel::nextStep,
            enabled = uiState.hourlyRate.isNotBlank() && uiState.sessionLengths.isNotEmpty()
        )
        Spacer(Modifier.height(24.dp))
    }
}

// ─── Step 5: Availability ─────────────────────────────────────────────────────

@Composable
fun Step5Availability(uiState: TeacherOnboardingUiState, viewModel: TeacherOnboardingViewModel) {
    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val slots = listOf("Morning", "Afternoon", "Evening")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        StepHeader("Availability", "When are you free to teach?")

        days.forEach { day ->
            Text(day, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                slots.forEach { slot ->
                    val selected = uiState.availabilitySlots[day]?.contains(slot) == true
                    SelectableChip(label = slot, selected = selected, onClick = { viewModel.toggleSlot(day, slot) })
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        Spacer(Modifier.height(8.dp))
        PrimaryButton(
            text = "Continue",
            onClick = viewModel::nextStep,
            enabled = uiState.availabilitySlots.values.any { it.isNotEmpty() }
        )
        Spacer(Modifier.height(24.dp))
    }
}

// ─── Step 6: Review & Submit ──────────────────────────────────────────────────

@Composable
fun Step6ReviewAndSubmit(uiState: TeacherOnboardingUiState, viewModel: TeacherOnboardingViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        StepHeader("Review & submit", "Check your profile before submitting")

        ReviewRow("Name", uiState.fullName)
        ReviewRow("Country", uiState.country)
        ReviewRow("Languages", uiState.languages.joinToString(", "))
        ReviewRow("Subjects", uiState.subjects.joinToString(", "))
        ReviewRow("Levels", uiState.levels.joinToString(", "))
        ReviewRow("Experience", "${uiState.yearsExperience} years")
        ReviewRow("Education", uiState.education)
        ReviewRow("Rate", uiState.hourlyRate)
        ReviewRow("Sessions", uiState.sessionLengths.joinToString(", "))

        Spacer(Modifier.height(8.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Text(
                text = "Your profile will be reviewed by our team within 24–48 hours. You will be notified by email once approved.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(12.dp),
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        Spacer(Modifier.height(24.dp))
        PrimaryButton(
            text = "Submit for review",
            onClick = viewModel::submit,
            isLoading = uiState.isLoading
        )
        Spacer(Modifier.height(24.dp))
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

@Composable
private fun StepHeader(title: String, subtitle: String) {
    Text(title, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
    Text(subtitle, fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f), modifier = Modifier.padding(top = 4.dp, bottom = 20.dp))
}

@Composable
private fun SectionLabel(label: String) {
    Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun ReviewRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f), modifier = Modifier.weight(0.4f))
        Text(value.ifBlank { "—" }, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(0.6f))
    }
    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline)
}
