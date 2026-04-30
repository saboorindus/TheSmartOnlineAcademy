package com.echologics.thesmartonlineacademy.ui.teacher.earnings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Pending
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherEarningsScreen(
    viewModel: TeacherEarningsViewModel,
    onWithdrawClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val summary = uiState.summary

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Earnings") },
                actions = {
                    IconButton(onClick = viewModel::refresh) {
                        Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = "Refresh")
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

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // ── Available balance hero card ────────────────────────────────────
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Purple)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Available balance",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.75f)
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            summary.display(summary.availableBalance),
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = onWithdrawClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Purple
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Request withdrawal", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // ── Metrics grid ──────────────────────────────────────────────────
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        MetricCard(
                            label = "Total earned",
                            value = summary.display(summary.totalEarned),
                            icon = Icons.AutoMirrored.Filled.TrendingUp,
                            color = Teal,
                            background = TealLight,
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            label = "Total withdrawn",
                            value = summary.display(summary.totalWithdrawn),
                            icon = Icons.Default.CheckCircle,
                            color = Color(0xFF639922),
                            background = Color(0xFFEDF5DA),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        MetricCard(
                            label = "Pending requests",
                            value = summary.display(summary.pendingWithdrawals),
                            icon = Icons.Default.Pending,
                            color = Amber,
                            background = AmberLight,
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            label = "Total sessions",
                            value = "${summary.totalSessions}",
                            icon = Icons.Default.School,
                            color = Purple,
                            background = PurpleLight,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // ── Completed sessions count ──────────────────────────────────────
            if (summary.completedSessions > 0) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = TealLight),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, Teal.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Teal, modifier = Modifier.size(20.dp))
                            Text(
                                "${summary.completedSessions} session${if (summary.completedSessions != 1) "s" else ""} completed",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Teal
                            )
                        }
                    }
                }
            }

            // ── Recent earnings header ────────────────────────────────────────
            item {
                Text(
                    "Recent earnings",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            if (uiState.recentBookings.isEmpty()) {
                item {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No earnings yet. Confirmed bookings will appear here.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            // ── Recent booking rows ───────────────────────────────────────────
            items(uiState.recentBookings, key = { it.id }) { booking ->
                EarningRow(booking = booking)
            }

            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun MetricCard(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color,
    background: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = background),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, color.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                value,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = color
            )
            Text(
                label,
                fontSize = 11.sp,
                color = color.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun EarningRow(booking: Booking) {
    val isCompleted = booking.status == BookingStatus.COMPLETED
    val earning = if (booking.teacherEarning == 0) {
        booking.totalAmount
    } else {
        booking.teacherEarning
    }
    val feeText = if (booking.platformFeePercent > 0)
        " (after ${booking.platformFeePercent}% fee)" else ""

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted)
                MaterialTheme.colorScheme.surfaceVariant
            else
                PurpleLight.copy(alpha = 0.5f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            0.5.dp,
            if (isCompleted) MaterialTheme.colorScheme.outline
            else Purple.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    booking.studentName,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )
                Text(
                    "${booking.subject} · ${booking.durationDisplay()}$feeText",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
                )
                if (booking.scheduledDate.isNotBlank()) {
                    Text(
                        booking.scheduledDate,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    earning.toString(),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = if (isCompleted) Teal else Purple
                )
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isCompleted) Teal.copy(alpha = 0.12f)
                    else Purple.copy(alpha = 0.12f)
                ) {
                    Text(
                        if (isCompleted) "Completed" else "Upcoming",
                        fontSize = 10.sp,
                        color = if (isCompleted) Teal else Purple,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}