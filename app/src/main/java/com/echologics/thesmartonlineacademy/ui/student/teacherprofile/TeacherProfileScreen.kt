package com.echologics.thesmartonlineacademy.ui.student.teacherprofile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.echologics.thesmartonlineacademy.data.model.Conversation
import com.echologics.thesmartonlineacademy.data.model.Review
import com.echologics.thesmartonlineacademy.data.model.TeacherProfile
import com.echologics.thesmartonlineacademy.ui.common.theme.Amber
import com.echologics.thesmartonlineacademy.ui.common.theme.Purple
import com.echologics.thesmartonlineacademy.ui.common.theme.PurpleLight
import com.echologics.thesmartonlineacademy.ui.common.theme.Teal
import com.echologics.thesmartonlineacademy.ui.common.theme.TealLight
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherProfileScreen(
    viewModel: TeacherProfileViewModel,
    teacherId: String,
    onBookClick: (TeacherProfile) -> Unit,
    onChatClick: (Conversation, String, String) -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(teacherId) {
        viewModel.loadTeacher(teacherId)
    }

    // Navigate to chat when conversation is ready
    LaunchedEffect(uiState.conversationReady) {
        uiState.conversationReady?.let { convo ->
            onChatClick(convo, uiState.chatOtherName, uiState.chatOtherId)
            viewModel.onChatNavigated()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Teacher profile") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            uiState.teacher?.let { teacher ->
                Surface(
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Message button
                        OutlinedButton(
                            onClick = { viewModel.startChat() },
                            modifier = Modifier.weight(1f).height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Purple),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Purple),
                            enabled = !uiState.isChatLoading
                        ) {
                            if (uiState.isChatLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = Purple,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    Icons.AutoMirrored.Filled.Chat,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text("Message", style = MaterialTheme.typography.labelLarge)
                            }
                        }

                        // Book button
                        Button(
                            onClick = { onBookClick(teacher) },
                            modifier = Modifier.weight(1f).height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Purple)
                        ) {
                            Text("Book a session", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
    ) { padding ->
        when {
            uiState.isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Purple)
                }
            }
            uiState.error != null -> {
                Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(uiState.error!!, color = MaterialTheme.colorScheme.error)
                }
            }
            uiState.teacher != null -> {
                TeacherProfileContent(
                    teacher = uiState.teacher!!,
                    reviews = uiState.reviews,
                    averageRating = uiState.averageRating,
                    modifier = Modifier.padding(padding)
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)

private fun TeacherProfileContent(
    teacher: TeacherProfile,
    reviews: List<Review>,
    averageRating: Float,
    modifier: Modifier = Modifier
) {
    val bottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            bottom = bottomPadding + 120.dp // 120dp = your bottom bar height buffer
        )
    ) {
        // Header
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Avatar initials
                Surface(
                    shape = CircleShape,
                    color = PurpleLight,
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = teacher.fullName.split(" ")
                                .take(2)
                                .joinToString("") { it.first().uppercaseChar().toString() },
                            fontSize = 24.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Purple
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                Text(teacher.fullName, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    if (averageRating > 0f) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = Amber, modifier = Modifier.size(16.dp))
                        Text(String.format(Locale.US, "%.1f", averageRating), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text("(${reviews.size} reviews)", fontSize = 13.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                    } else {
                        Text("No reviews yet", fontSize = 13.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                    }
                }

                Text(
                    text = "${teacher.yearsExperience} yrs exp · ${teacher.country}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    modifier = Modifier.padding(top = 2.dp)
                )

                if (teacher.isVerified) {
                    Spacer(Modifier.height(6.dp))
                    Surface(shape = RoundedCornerShape(20.dp), color = TealLight) {
                        Text(
                            "Verified teacher",
                            fontSize = 12.sp,
                            color = Teal,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline)
        }

        // Rate & session info
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                PricePill(
                    currency = teacher.currency,
                    amount = teacher.ratePerTenMin.toString(),
                    duration = "/10 min"
                )


//                InfoPill(label = "Sessions", value = teacher.sessionLengths.joinToString(", "))
                if (teacher.trialSessionEnabled) {
                    HorizontalInfoPill(label = "Trial", value = teacher.trialRate)
                }
            }
            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline)
        }

        // Bio
        item {
            ProfileSection(title = "About") {
                Text(
                    text = teacher.bio,
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f)
                )
            }
        }

        // Subjects
        item {
            ProfileSection(title = "Subjects") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    teacher.subjects.forEach { subject ->
                        Surface(shape = RoundedCornerShape(6.dp), color = PurpleLight) {
                            Text(subject, fontSize = 12.sp, color = Purple, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                        }
                    }
                }
            }
        }

        // Levels
        item {
            ProfileSection(title = "Student levels") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    teacher.levels.forEach { level ->
                        Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                            Text(level, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                        }
                    }
                }
            }
        }

        // Teaching styles
        item {
            ProfileSection(title = "Teaching style") {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    teacher.teachingStyles.forEach { style ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = style,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }


        // Availability
        item {
            ProfileSection(title = "Availability") {
                teacher.availabilitySlots.forEach { (day, slots) ->
                    if (slots.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(day, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.width(48.dp))
                            Text(
                                slots.joinToString(", "),
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f)
                            )
                        }
                    }
                }
            }
        }

        // Reviews
        item {
            ProfileSection(title = "Reviews (${reviews.size})") {
                if (reviews.isEmpty()) {
                    Text("No reviews yet", fontSize = 13.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f))
                }
            }
        }

        items(reviews.take(5)) { review ->
            ReviewCard(review = review)
        }
    }
}

@Composable
private fun ProfileSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
        Text(title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        Spacer(Modifier.height(10.dp))
        content()
    }
    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline)
}

@Composable
private fun InfoPill(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Purple)
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
    }
}

@Composable
private fun HorizontalInfoPill(label: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp) // 👈 important
    ) {
        Text(
            label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
        )

        Text(
            value,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            color = Purple
        )
    }
}

@Composable
fun PricePill(currency: String, amount: String, duration: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            currency,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
        )
        Text(
            amount,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            color = Purple
        )
        Text(
            duration,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
        )
    }
}




@Composable
private fun ReviewCard(review: Review) {
    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = PurpleLight, modifier = Modifier.size(32.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        review.studentName.firstOrNull()?.uppercaseChar()?.toString() ?: "S",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Purple
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Column {
                Text(review.studentName, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    repeat(5) { index ->
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = if (index < review.rating) Amber else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }
        if (review.comment.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(
                review.comment,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
        }
        Spacer(Modifier.height(8.dp))
        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline)
    }
}