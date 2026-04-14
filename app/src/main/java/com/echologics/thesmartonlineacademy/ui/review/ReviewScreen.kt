package com.echologics.thesmartonlineacademy.ui.review

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.echologics.thesmartonlineacademy.data.model.Booking
import com.echologics.thesmartonlineacademy.ui.common.components.PrimaryButton
import com.echologics.thesmartonlineacademy.ui.common.theme.Amber
import com.echologics.thesmartonlineacademy.ui.common.theme.Purple
import com.echologics.thesmartonlineacademy.ui.common.theme.PurpleLight
import com.echologics.thesmartonlineacademy.ui.common.theme.Teal
import com.echologics.thesmartonlineacademy.ui.common.theme.TealLight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(
    viewModel: ReviewViewModel,
    booking: Booking,
    onSubmitted: () -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(booking) {
        viewModel.setBooking(booking)
    }

    LaunchedEffect(uiState.isSubmitted) {
        if (uiState.isSubmitted) onSubmitted()
    }

    val starLabels = listOf("", "Poor", "Fair", "Good", "Great", "Excellent")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Leave a review") },
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
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Already reviewed state
            if (uiState.alreadyReviewed) {
                Spacer(Modifier.height(48.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = TealLight),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Review already submitted", fontWeight = FontWeight.SemiBold, color = Teal)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "You've already reviewed this session with ${booking.teacherName}.",
                            fontSize = 13.sp, color = Teal.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
                return@Scaffold
            }

            Spacer(Modifier.height(16.dp))

            // Teacher summary
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = PurpleLight),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(booking.teacherName, fontWeight = FontWeight.SemiBold, color = Purple, fontSize = 16.sp)
                    Text(
                        "${booking.subject} · ${booking.sessionLength}",
                        fontSize = 13.sp,
                        color = Purple.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    if (booking.scheduledDate.isNotBlank()) {
                        Text(
                            booking.scheduledDate,
                            fontSize = 12.sp,
                            color = Purple.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            // Rating prompt
            Text(
                "How was your session?",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Your review helps other students find great teachers",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(24.dp))

            // Star rating row
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                (1..5).forEach { star ->
                    Icon(
                        imageVector = if (star <= uiState.rating)
                            Icons.Filled.Star else Icons.Outlined.StarOutline,
                        contentDescription = "Star $star",
                        tint = if (star <= uiState.rating) Amber
                        else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.25f),
                        modifier = Modifier
                            .size(44.dp)
                            .clickable { viewModel.onRatingChange(star) }
                    )
                }
            }

            // Rating label
            if (uiState.rating > 0) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = starLabels[uiState.rating],
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = Amber
                )
            }

            Spacer(Modifier.height(28.dp))

            // Comment field
            OutlinedTextField(
                value = uiState.comment,
                onValueChange = viewModel::onCommentChange,
                label = { Text("Write a review (optional)") },
                placeholder = { Text("What did you like? What could be better?") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                singleLine = false,
                maxLines = 6,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Purple,
                    focusedLabelColor = Purple,
                    cursorColor = Purple
                )
            )

            // Character counter
            Text(
                text = "${uiState.comment.length}/500",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = 4.dp)
            )

            if (uiState.error != null) {
                Spacer(Modifier.height(10.dp))
                Text(
                    uiState.error!!,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp
                )
            }

            Spacer(Modifier.height(24.dp))

            PrimaryButton(
                text = "Submit review",
                onClick = viewModel::submit,
                enabled = viewModel.canSubmit(),
                isLoading = uiState.isLoading
            )

            Spacer(Modifier.height(12.dp))

            TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text("Skip for now", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}