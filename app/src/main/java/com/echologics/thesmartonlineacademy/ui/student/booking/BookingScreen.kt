package com.echologics.thesmartonlineacademy.ui.student.booking

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
import com.echologics.thesmartonlineacademy.data.model.Booking
import com.echologics.thesmartonlineacademy.data.model.TeacherProfile
import com.echologics.thesmartonlineacademy.ui.common.components.PrimaryButton
import com.echologics.thesmartonlineacademy.ui.common.components.SelectableChip
import com.echologics.thesmartonlineacademy.ui.common.theme.Purple
import com.echologics.thesmartonlineacademy.ui.common.theme.PurpleLight

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun BookingScreen(
    viewModel: BookingViewModel,
    teacher: TeacherProfile,
    onBookingCreated: (Booking) -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(teacher) {
        viewModel.setTeacher(teacher)
    }

    LaunchedEffect(uiState.createdBooking) {
        uiState.createdBooking?.let { onBookingCreated(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Book a session") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Teacher summary
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = PurpleLight)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(teacher.fullName, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Purple)
                        Text(teacher.subjects.take(2).joinToString(", "), fontSize = 12.sp, color = Purple.copy(alpha = 0.7f))
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(teacher.displayRate(), fontWeight = FontWeight.SemiBold, color = Purple, fontSize = 13.sp)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Subject selection
            SectionTitle("Subject")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                teacher.subjects.forEach { subject ->
                    SelectableChip(
                        label = subject,
                        selected = uiState.selectedSubject == subject,
                        onClick = { viewModel.onSubjectSelected(subject) }
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // Session length
            SectionTitle("Session duration")
//            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
//                teacher.sessionLengths.forEach { length ->
//                    SelectableChip(
//                        label = length,
//                        selected = uiState.selectedSessionLength == length,
//                        onClick = { viewModel.onSessionLengthSelected(length) }
//                    )
//                }
//            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("10m", fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                Text(
                    uiState.durationDisplay,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Purple
                )
                Text("3h", fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
            }

            Slider(
                value = uiState.sliderPosition,
                onValueChange = viewModel::onSliderChange,
                valueRange = 0f..17f,
                steps = 16,   // 18 positions total (10, 20, ..., 180) → 16 steps between them
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = Purple,
                    activeTrackColor = Purple,
                    inactiveTrackColor = Purple.copy(alpha = 0.2f)
                )
            )

            // Quick select buttons for common durations
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(30, 60, 90, 120).forEach { mins ->
                    OutlinedButton(
                        onClick = { viewModel.onSliderChange(((mins / 10) - 1).toFloat()) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (uiState.durationMinutes == mins) 1.5.dp else 0.5.dp,
                            color = if (uiState.durationMinutes == mins) Purple
                            else MaterialTheme.colorScheme.outline
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (uiState.durationMinutes == mins) Purple
                            else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    ) {
                        Text(
                            formatDuration(mins),
                            fontSize = 12.sp,
                            fontWeight = if (uiState.durationMinutes == mins) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }

            // Live price card
            Spacer(Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = PurpleLight),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Session total", fontSize = 13.sp, color = Purple.copy(alpha = 0.7f))
                        Text(
                            "${uiState.durationDisplay} · ${teacher.ratePerTenMin} ${teacher.currency}/10min",
                            fontSize = 11.sp,
                            color = Purple.copy(alpha = 0.5f)
                        )
                    }
                    Text(
                        uiState.totalDisplay,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 20.sp,
                        color = Purple
                    )
                }
            }


            Spacer(Modifier.height(20.dp))

            // Day picker
            SectionTitle("Choose a day")
            val availableDays = teacher.availabilitySlots.filter { it.value.isNotEmpty() }.keys.toList()
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                availableDays.forEach { day ->
                    SelectableChip(
                        label = day,
                        selected = uiState.selectedDay == day,
                        onClick = { viewModel.onDaySelected(day) }
                    )
                }
            }

            // Time slot picker (shows after day is selected)
            if (uiState.selectedDay.isNotBlank()) {
                Spacer(Modifier.height(20.dp))
                SectionTitle("Choose a time slot")
                val slots = teacher.availabilitySlots[uiState.selectedDay] ?: emptyList()
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    slots.forEach { slot ->
                        SelectableChip(
                            label = slot,
                            selected = uiState.selectedTimeSlot == slot,
                            onClick = { viewModel.onTimeSlotSelected(slot) }
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Specific date (optional text input)
            OutlinedTextField(
                value = uiState.scheduledDate,
                onValueChange = viewModel::onScheduledDateChange,
                label = { Text("Preferred date (e.g. 15 Jan 2026)") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Purple,
                    focusedLabelColor = Purple,
                    cursorColor = Purple
                )
            )

//            // Price summary
//            if (uiState.selectedSessionLength.isNotBlank()) {
//                Spacer(Modifier.height(20.dp))
//                Card(
//                    modifier = Modifier.fillMaxWidth(),
//                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
//                    shape = RoundedCornerShape(10.dp)
//                ) {
//                    Row(
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .padding(14.dp),
//                        horizontalArrangement = Arrangement.SpaceBetween
//                    ) {
//                        Column {
//                            Text("Session total", fontSize = 13.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
//                            Text(
//                                "${uiState.selectedSessionLength} · ${uiState.selectedSubject}",
//                                fontSize = 12.sp,
//                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
//                            )
//                        }
//                        Text(
//                            text = computeTotal(teacher.hourlyRate, uiState.selectedSessionLength),
//                            fontWeight = FontWeight.SemiBold,
//                            fontSize = 18.sp,
//                            color = Purple
//                        )
//                    }
//                }
//            }


            if (uiState.error != null) {
                Spacer(Modifier.height(12.dp))
                Text(uiState.error!!, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
            }

            Spacer(Modifier.height(24.dp))

            PrimaryButton(
                text = "Confirm booking",
                onClick = viewModel::confirmBooking,
                enabled = viewModel.canProceed(),
                isLoading = uiState.isLoading
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
        modifier = Modifier.padding(bottom = 10.dp)
    )
}


private fun formatDuration(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h == 0 -> "${m}m"
        m == 0 -> "${h}h"
        else -> "${h}h ${m}m"
    }
}

//private fun computeTotal(hourlyRate: String, sessionLength: String): String {
//    val rate = hourlyRate.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 0
//    val multiplier = when {
//        sessionLength.contains("30") -> 0.5
//        sessionLength.contains("90") -> 1.5
//        else -> 1.0
//    }
//    val total = (rate * multiplier).toInt()
//    val currency = if (hourlyRate.contains("PKR", ignoreCase = true)) "PKR" else ""
//    return "$currency $total".trim()
//}