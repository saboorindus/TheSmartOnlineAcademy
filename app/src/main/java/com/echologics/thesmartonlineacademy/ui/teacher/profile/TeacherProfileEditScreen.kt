package com.echologics.thesmartonlineacademy.ui.teacher.profile

import com.echologics.thesmartonlineacademy.ui.common.components.AppTextField
import com.echologics.thesmartonlineacademy.ui.common.components.PrimaryButton
import com.echologics.thesmartonlineacademy.ui.common.components.SelectableChip
import com.echologics.thesmartonlineacademy.ui.common.theme.Purple
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TeacherProfileEditScreen(
    viewModel: TeacherProfileEditViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onBack()
    }

    val languages = listOf("English", "Urdu", "Punjabi", "Arabic", "French", "German")
    val subjects = listOf("Mathematics", "Physics", "Chemistry", "Biology", "English", "Urdu", "History", "Computer Science", "Economics", "Accounting")
    val levels = listOf("Primary", "O-Level", "A-Level", "University", "Adult / Professional")
    val styles = listOf("Exam prep", "Conversational", "Step-by-step", "Project-based", "Problem solving")
    val sessionLengths = listOf("30 min", "60 min", "90 min")
    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val slots = listOf("Morning", "Afternoon", "Evening")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit profile") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Purple)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {

            // ── Personal ──────────────────────────────────────────────────────
            SectionHeader("Personal info")
            AppTextField(value = uiState.fullName, onValueChange = viewModel::onFullNameChange, label = "Full name")
            Spacer(Modifier.height(12.dp))
            AppTextField(value = uiState.country, onValueChange = viewModel::onCountryChange, label = "Country")
            Spacer(Modifier.height(12.dp))
            AppTextField(
                value = uiState.bio,
                onValueChange = viewModel::onBioChange,
                label = "Bio (${uiState.bio.length}/300)",
                singleLine = false,
                maxLines = 4
            )

            Spacer(Modifier.height(8.dp))
            ChipGroupLabel("Languages spoken")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                languages.forEach { lang ->
                    SelectableChip(
                        label = lang,
                        selected = uiState.languages.contains(lang),
                        onClick = {
                            val updated = if (uiState.languages.contains(lang))
                                uiState.languages - lang else uiState.languages + lang
                            viewModel.onFullNameChange(uiState.fullName) // trigger recompose trick
                            viewModel.toggleLanguage(lang)
                        }
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = MaterialTheme.colorScheme.outline)

            // ── Teaching ──────────────────────────────────────────────────────
            SectionHeader("Teaching details")
            ChipGroupLabel("Subjects")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                subjects.forEach { s ->
                    SelectableChip(label = s, selected = uiState.subjects.contains(s), onClick = { viewModel.toggleSubject(s) })
                }
            }

            Spacer(Modifier.height(12.dp))
            ChipGroupLabel("Levels")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                levels.forEach { l ->
                    SelectableChip(label = l, selected = uiState.levels.contains(l), onClick = { viewModel.toggleLevel(l) })
                }
            }

            Spacer(Modifier.height(12.dp))
            ChipGroupLabel("Teaching style")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                styles.forEach { s ->
                    SelectableChip(label = s, selected = uiState.teachingStyles.contains(s), onClick = { viewModel.toggleStyle(s) })
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = MaterialTheme.colorScheme.outline)

            // ── Pricing ───────────────────────────────────────────────────────
            SectionHeader("Pricing & sessions")
            AppTextField(value = uiState.hourlyRate, onValueChange = viewModel::onHourlyRateChange, label = "Hourly rate (e.g. PKR 2500)")
            Spacer(Modifier.height(12.dp))

            ChipGroupLabel("Session lengths")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                sessionLengths.forEach { l ->
                    SelectableChip(label = l, selected = uiState.sessionLengths.contains(l), onClick = { viewModel.toggleSessionLength(l) })
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("Offer trial session", modifier = Modifier.weight(1f))
                Switch(checked = uiState.trialSessionEnabled, onCheckedChange = viewModel::onTrialToggle)
            }
            if (uiState.trialSessionEnabled) {
                Spacer(Modifier.height(8.dp))
                AppTextField(value = uiState.trialRate, onValueChange = viewModel::onTrialRateChange, label = "Trial session rate")
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = MaterialTheme.colorScheme.outline)

            // ── Availability ──────────────────────────────────────────────────
            SectionHeader("Availability")
            days.forEach { day ->
                Text(day, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    slots.forEach { slot ->
                        val selected = uiState.availabilitySlots[day]?.contains(slot) == true
                        SelectableChip(
                            label = slot,
                            selected = selected,
                            onClick = { viewModel.toggleSlot(day, slot) }
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
            }

            // Error
            if (uiState.error != null) {
                Spacer(Modifier.height(8.dp))
                Text(uiState.error!!, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
            }

            Spacer(Modifier.height(24.dp))

            PrimaryButton(
                text = "Save changes",
                onClick = viewModel::save,
                isLoading = uiState.isSaving
            )

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(bottom = 12.dp)
    )
}

@Composable
private fun ChipGroupLabel(label: String) {
    Text(
        label,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
        modifier = Modifier.padding(bottom = 8.dp)
    )
}