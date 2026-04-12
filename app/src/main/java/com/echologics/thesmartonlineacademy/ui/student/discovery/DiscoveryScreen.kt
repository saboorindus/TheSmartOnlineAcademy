package com.echologics.thesmartonlineacademy.ui.student.discovery

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.echologics.thesmartonlineacademy.data.model.TeacherProfile
import com.echologics.thesmartonlineacademy.ui.common.components.SelectableChip
import com.echologics.thesmartonlineacademy.ui.common.theme.Purple
import com.echologics.thesmartonlineacademy.ui.common.theme.PurpleLight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoveryScreen(
    viewModel: DiscoveryViewModel,
    onTeacherClick: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    val subjectFilters = listOf("Mathematics", "Physics", "Chemistry", "English", "Biology", "Computer Science")
    val levelFilters = listOf("O-Level", "A-Level", "Primary", "University")

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Find a teacher") })
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // Search bar
            item {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = viewModel::onSearchQueryChange,
                    placeholder = { Text("Search by subject or name") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Purple,
                        focusedLabelColor = Purple,
                        cursorColor = Purple
                    )
                )
            }

            // Subject filter chips
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(subjectFilters) { subject ->
                        SelectableChip(
                            label = subject,
                            selected = uiState.selectedSubject == subject,
                            onClick = { viewModel.onSubjectChange(subject) }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            // Level filter chips
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(levelFilters) { level ->
                        SelectableChip(
                            label = level,
                            selected = uiState.selectedLevel == level,
                            onClick = { viewModel.onLevelChange(level) }
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
            }

            // Active filter + clear button
            if (uiState.selectedSubject.isNotBlank() || uiState.selectedLevel.isNotBlank() || uiState.maxRate.isNotBlank()) {
                item {
                    TextButton(
                        onClick = viewModel::clearFilters,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    ) {
                        Text("Clear filters", color = Purple)
                    }
                }
            }

            // Loading
            if (uiState.isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Purple)
                    }
                }
            }

            // Error
            uiState.error?.let { err ->
                item {
                    Text(
                        text = err,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            // Empty state
            if (!uiState.isLoading && uiState.teachers.isEmpty() && uiState.error == null) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("No teachers found", fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Try adjusting your filters",
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // Result count
            if (!uiState.isLoading && uiState.teachers.isNotEmpty()) {
                item {
                    Text(
                        text = "${uiState.teachers.size} teacher${if (uiState.teachers.size != 1) "s" else ""} available",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }

            // Teacher cards
            items(uiState.teachers) { teacher ->
                TeacherCard(
                    teacher = teacher,
                    onClick = { onTeacherClick(teacher.uid) }
                )
            }
        }
    }
}

@Composable
fun TeacherCard(teacher: TeacherProfile, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = teacher.fullName,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = teacher.subjects.take(3).joinToString(" · "),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = teacher.hourlyRate,
                        fontWeight = FontWeight.SemiBold,
                        color = Purple,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "per hour",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = teacher.bio,
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                lineHeight = 18.sp
            )

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Levels
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    teacher.levels.take(2).forEach { level ->
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = PurpleLight
                        ) {
                            Text(
                                text = level,
                                fontSize = 11.sp,
                                color = Purple,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (teacher.levels.size > 2) {
                        Text(
                            text = "+${teacher.levels.size - 2}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                        )
                    }
                }

                // Verified badge
                if (teacher.isVerified) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = "Verified",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Experience
            if (teacher.yearsExperience.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "${teacher.yearsExperience} years experience · ${teacher.country}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f)
                )
            }
        }
    }
}