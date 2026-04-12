package com.echologics.thesmartonlineacademy.ui.teacher.bookings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.echologics.thesmartonlineacademy.data.model.Booking
import com.echologics.thesmartonlineacademy.data.model.BookingStatus
import com.echologics.thesmartonlineacademy.ui.common.theme.Amber
import com.echologics.thesmartonlineacademy.ui.common.theme.AmberLight
import com.echologics.thesmartonlineacademy.ui.common.theme.Purple
import com.echologics.thesmartonlineacademy.ui.common.theme.PurpleLight
import com.echologics.thesmartonlineacademy.ui.common.theme.Teal
import com.echologics.thesmartonlineacademy.ui.common.theme.TealLight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherBookingsScreen(
    viewModel: TeacherBookingsViewModel,
    onJoinSession: (Booking) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val filtered = viewModel.filteredBookings()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("My bookings") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Tab row
            TabRow(
                selectedTabIndex = BookingTab.entries.indexOf(uiState.selectedTab),
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = Purple
            ) {
                BookingTab.entries.forEach { tab ->
                    Tab(
                        selected = uiState.selectedTab == tab,
                        onClick = { viewModel.onTabSelected(tab) },
                        text = { Text(tab.label) }
                    )
                }
            }

            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Purple)
                }
                return@Scaffold
            }

            if (filtered.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No bookings yet", fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            when (uiState.selectedTab) {
                                BookingTab.PENDING -> "New booking requests will appear here"
                                BookingTab.CONFIRMED -> "Confirmed sessions will appear here"
                                BookingTab.ALL -> "You have no bookings yet"
                            },
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        )
                    }
                }
                return@Scaffold
            }

            LazyColumn(
                contentPadding = PaddingValues(vertical = 8.dp, horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filtered) { booking ->
                    BookingCard(
                        booking = booking,
                        isConfirming = uiState.confirmingBookingId == booking.id,
                        onConfirmPayment = { viewModel.confirmPayment(booking.id) },
                        onCancel = { viewModel.cancelBooking(booking.id) },
                        onJoinSession = { onJoinSession(booking) }
                    )
                }
            }
        }
    }
}

@Composable
private fun BookingCard(
    booking: Booking,
    isConfirming: Boolean,
    onConfirmPayment: () -> Unit,
    onCancel: () -> Unit,
    onJoinSession: () -> Unit
) {
    val (bgColor, borderColor, badgeColor, badgeText) = when (booking.status) {
        BookingStatus.PAYMENT_SUBMITTED -> listOf(AmberLight, Amber, Amber, "Payment submitted")
        BookingStatus.CONFIRMED -> listOf(TealLight, Teal, Teal, "Confirmed")
        BookingStatus.PENDING_PAYMENT -> listOf(PurpleLight, Purple, Purple, "Awaiting payment")
        BookingStatus.CANCELLED -> listOf(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.error,
            MaterialTheme.colorScheme.error,
            "Cancelled"
        )
        BookingStatus.COMPLETED -> listOf(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.outline,
            MaterialTheme.colorScheme.outline,
            "Completed"
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor as androidx.compose.ui.graphics.Color),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, borderColor as androidx.compose.ui.graphics.Color)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(booking.studentName, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
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

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                InfoChip("Day", "${booking.slotDay} ${booking.slotTime}")
                if (booking.scheduledDate.isNotBlank()) InfoChip("Date", booking.scheduledDate)
                InfoChip("Amount", booking.totalAmount)
            }

            // Payment proof (only if submitted)
            if (booking.status == BookingStatus.PAYMENT_SUBMITTED) {
                Spacer(Modifier.height(10.dp))
                HorizontalDivider(thickness = 0.5.dp, color = Amber.copy(alpha = 0.3f))
                Spacer(Modifier.height(10.dp))

                Text("Payment details", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Amber)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    InfoChip("Txn ID", booking.paymentTransactionId)
                    InfoChip("Sender", booking.paymentSenderName)
                }

                Spacer(Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Confirm payment button
                    Button(
                        onClick = onConfirmPayment,
                        enabled = !isConfirming,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Teal)
                    ) {
                        if (isConfirming) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = androidx.compose.ui.graphics.Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Payment received", fontSize = 13.sp)
                        }
                    }

                    // Cancel button
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            0.5.dp,
                            MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Cancel", fontSize = 13.sp)
                    }
                }
            }

            // For confirmed sessions - show join button
            if (booking.status == BookingStatus.CONFIRMED) {
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = onJoinSession,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Teal)
                ) {
                    Text("Join session", fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun InfoChip(label: String, value: String) {
    Column {
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f))
        Text(value.ifBlank { "—" }, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}