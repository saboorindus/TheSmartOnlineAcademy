package com.echologics.thesmartonlineacademy.ui.student.bookinghistory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.echologics.thesmartonlineacademy.data.model.Booking
import com.echologics.thesmartonlineacademy.data.model.BookingStatus
import com.echologics.thesmartonlineacademy.data.model.Conversation
import com.echologics.thesmartonlineacademy.ui.common.theme.Amber
import com.echologics.thesmartonlineacademy.ui.common.theme.AmberLight
import com.echologics.thesmartonlineacademy.ui.common.theme.Purple
import com.echologics.thesmartonlineacademy.ui.common.theme.PurpleLight
import com.echologics.thesmartonlineacademy.ui.common.theme.Teal
import com.echologics.thesmartonlineacademy.ui.common.theme.TealLight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingHistoryScreen(
    viewModel: BookingHistoryViewModel,
    onJoinSession: (Booking) -> Unit,
    onReview: (Booking) -> Unit,
    onChatClick: (Conversation, String, String) -> Unit,
    onConfirmPayment: (Booking) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val filtered by viewModel.filteredBookings.collectAsState()

    LaunchedEffect(uiState.conversationReady) {
        uiState.conversationReady?.let { convo ->
            onChatClick(convo, uiState.chatOtherName, uiState.chatOtherId)
            viewModel.onChatNavigated()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("My sessions") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Tab row
            TabRow(
                selectedTabIndex = HistoryTab.entries.indexOf(uiState.selectedTab),
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = Purple
            ) {
                HistoryTab.entries.forEach { tab ->
                    Tab(
                        selected = uiState.selectedTab == tab,
                        onClick = { viewModel.onTabSelected(tab) },
                        text = { Text(tab.label) }
                    )
                }
            }

            when {
                uiState.isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Purple)
                    }
                }

                filtered.isEmpty() -> {
                    Box(
                        Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                when (uiState.selectedTab) {
                                    HistoryTab.UPCOMING -> "No upcoming sessions"
                                    HistoryTab.PAST -> "No past sessions"
                                    HistoryTab.ALL -> "No sessions yet"
                                },
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Book a session from the Discover tab",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                            )
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filtered, key = { it.id }) { booking ->
                            StudentBookingCard(
                                booking = booking,
                                canReview = viewModel.canReview(booking),
                                isChatLoading = uiState.chatLoadingBookingId == booking.id,
                                onJoinSession = { onJoinSession(booking) },
                                onReview = { onReview(booking) },
                                onMessage = { viewModel.startChat(booking) },
                                onConfirmPayment = { onConfirmPayment(booking) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StudentBookingCard(
    booking: Booking,
    canReview: Boolean,
    isChatLoading: Boolean,
    onJoinSession: () -> Unit,
    onReview: () -> Unit,
    onMessage: () -> Unit,
    onConfirmPayment: () -> Unit

) {
    val (bgColor, borderColor, badgeColor, badgeText) = when (booking.status) {
        BookingStatus.CONFIRMED -> listOf(TealLight, Teal, Teal, "Confirmed")
        BookingStatus.PAYMENT_SUBMITTED -> listOf(AmberLight, Amber, Amber, "Awaiting confirmation")
        BookingStatus.PENDING_PAYMENT -> listOf(PurpleLight, Purple, Purple, "Payment pending")
        BookingStatus.COMPLETED -> listOf(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.outline,
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            "Completed"
        )
        BookingStatus.CANCELLED -> listOf(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.error,
            MaterialTheme.colorScheme.error,
            "Cancelled"
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = bgColor as androidx.compose.ui.graphics.Color
        ),
        border = androidx.compose.foundation.BorderStroke(
            0.5.dp,
            borderColor as androidx.compose.ui.graphics.Color
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {

            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        booking.teacherName,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                    Text(
                        "${booking.subject} · ${booking.sessionLength}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f)
                    )
                }
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = (badgeColor as androidx.compose.ui.graphics.Color).copy(alpha = 0.15f)
                ) {
                    Text(
                        badgeText as String,
                        fontSize = 11.sp,
                        color = badgeColor,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Info row
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                InfoItem("Day", "${booking.slotDay} ${booking.slotTime}")
                if (booking.scheduledDate.isNotBlank()) {
                    InfoItem("Date", booking.scheduledDate)
                }
                InfoItem("Amount", booking.totalAmount)
            }

            // Payment pending instructions
            // Payment pending instructions + action
            if (booking.status == BookingStatus.PENDING_PAYMENT) {
                Spacer(Modifier.height(10.dp))

                Text(
                    "Please complete payment to confirm this session.",
                    fontSize = 12.sp,
                    color = Purple.copy(alpha = 0.75f)
                )

                Spacer(Modifier.height(10.dp))

                Button(
                    onClick = onConfirmPayment, // 👈 add this callback
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Purple
                    )
                ) {
                    Text("Confirm payment", fontSize = 13.sp)
                }
            }


            // Action buttons
            val showJoin = booking.status == BookingStatus.CONFIRMED

            if (showJoin || canReview) {
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (showJoin) {
                        Button(
                            onClick = onJoinSession,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Teal)
                        ) {
                            Text("Join session", fontSize = 13.sp)
                        }
                    }
                    if (canReview) {
                        OutlinedButton(
                            onClick = onReview,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Purple),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Purple)
                        ) {
                            Text("Leave review", fontSize = 13.sp)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onMessage, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), enabled = !isChatLoading,
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
                ) {
                    if (isChatLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Purple, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Message ${booking.teacherName}", fontSize = 13.sp)
                    }
                }
            }

            // Already reviewed badge
            if (booking.status == BookingStatus.COMPLETED && !canReview) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Review submitted",
                    fontSize = 12.sp,
                    color = Teal,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun InfoItem(label: String, value: String) {
    Column {
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f))
        Text(value.ifBlank { "—" }, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}