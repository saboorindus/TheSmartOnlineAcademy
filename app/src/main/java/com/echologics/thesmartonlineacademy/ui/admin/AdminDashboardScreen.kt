package com.echologics.thesmartonlineacademy.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.echologics.thesmartonlineacademy.data.model.Booking
import com.echologics.thesmartonlineacademy.data.model.BookingStatus
import com.echologics.thesmartonlineacademy.data.model.TeacherProfile
import com.echologics.thesmartonlineacademy.data.model.User
import com.echologics.thesmartonlineacademy.ui.common.theme.Amber
import com.echologics.thesmartonlineacademy.ui.common.theme.AmberLight
import com.echologics.thesmartonlineacademy.ui.common.theme.Purple
import com.echologics.thesmartonlineacademy.ui.common.theme.PurpleLight
import com.echologics.thesmartonlineacademy.ui.common.theme.Teal
import com.echologics.thesmartonlineacademy.ui.common.theme.TealLight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(viewModel: AdminDashboardViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    // Reject dialog
    if (uiState.showRejectDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissRejectDialog,
            title = { Text("Reject teacher") },
            text = {
                Column {
                    Text(
                        "Provide a reason that will be sent to the teacher:",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = uiState.rejectReason,
                        onValueChange = viewModel::onRejectReasonChange,
                        label = { Text("Reason") },
                        singleLine = false,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmReject) {
                    Text("Reject", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissRejectDialog) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin panel") },
                actions = {
                    IconButton(onClick = viewModel::refresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            // Tab row
            ScrollableTabRow(
                selectedTabIndex = AdminTab.entries.indexOf(uiState.selectedTab),
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = Purple,
                edgePadding = 8.dp
            ) {
                AdminTab.entries.forEach { tab ->
                    Tab(
                        selected = uiState.selectedTab == tab,
                        onClick = { viewModel.onTabSelected(tab) },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(tab.label)
                                if (tab == AdminTab.APPROVALS && uiState.pendingTeachers.isNotEmpty()) {
                                    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.error) {
                                        Text(
                                            "${uiState.pendingTeachers.size}",
                                            fontSize = 10.sp,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                        }
                    )
                }
            }

            // Success / error snackbar
            uiState.successMessage?.let { msg ->
                LaunchedEffect(msg) { viewModel.clearMessage() }
                Card(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    colors = CardDefaults.cardColors(containerColor = TealLight),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(msg, color = Teal, modifier = Modifier.padding(12.dp), fontSize = 13.sp)
                }
            }
            uiState.error?.let { err ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(err, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(12.dp), fontSize = 13.sp)
                }
            }

            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Purple)
                }
                return@Scaffold
            }

            when (uiState.selectedTab) {
                AdminTab.DASHBOARD -> DashboardTab(uiState)
                AdminTab.APPROVALS -> ApprovalsTab(uiState, viewModel)
                AdminTab.BOOKINGS -> BookingsTab(uiState, viewModel)
                AdminTab.USERS -> UsersTab(uiState, viewModel)
                AdminTab.QR -> QrTab(viewModel)
            }
        }
    }
}

// ── Dashboard Tab ─────────────────────────────────────────────────────────────

@Composable
private fun DashboardTab(uiState: AdminUiState) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Platform overview", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, modifier = Modifier.padding(bottom = 4.dp))
        }
        item {
            // 2x2 stat grid
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard("Teachers", uiState.stats.totalTeachers.toString(), Icons.Default.School, Purple, modifier = Modifier.weight(1f))
                    StatCard("Students", uiState.stats.totalStudents.toString(), Icons.Default.People, Teal, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard("Bookings", uiState.stats.totalBookings.toString(), Icons.Default.CalendarMonth, Amber, modifier = Modifier.weight(1f))
                    StatCard("Revenue", uiState.stats.totalRevenue, Icons.Default.Payments, Color(0xFF639922), modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard("Completed", uiState.stats.completedSessions.toString(), Icons.Default.CheckCircle, Teal, modifier = Modifier.weight(1f))
                    StatCard("Pending pay", uiState.stats.pendingPayments.toString(), Icons.Default.PendingActions, Amber, modifier = Modifier.weight(1f))
                }
            }
        }
        if (uiState.pendingTeachers.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = AmberLight),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, Amber)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Amber)
                        Text(
                            "${uiState.pendingTeachers.size} teacher${if (uiState.pendingTeachers.size > 1) "s" else ""} awaiting approval",
                            color = Amber,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f)),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, color.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(8.dp))
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = color)
            Text(label, fontSize = 12.sp, color = color.copy(alpha = 0.7f))
        }
    }
}

// ── Approvals Tab ─────────────────────────────────────────────────────────────

@Composable
private fun ApprovalsTab(uiState: AdminUiState, viewModel: AdminDashboardViewModel) {
    if (uiState.pendingTeachers.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Teal, modifier = Modifier.size(40.dp))
                Spacer(Modifier.height(8.dp))
                Text("All caught up!", fontWeight = FontWeight.Medium)
                Text("No pending teacher approvals", fontSize = 13.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
            }
        }
        return
    }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text(
                "${uiState.pendingTeachers.size} pending approval${if (uiState.pendingTeachers.size > 1) "s" else ""}",
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
        }
        items(uiState.pendingTeachers, key = { it.uid }) { teacher ->
            TeacherApprovalCard(
                teacher = teacher,
                isProcessing = uiState.actionLoading == teacher.uid,
                onApprove = { viewModel.approveTeacher(teacher.uid) },
                onReject = { viewModel.showRejectDialog(teacher.uid) }
            )
        }
    }
}

@Composable
private fun TeacherApprovalCard(
    teacher: TeacherProfile,
    isProcessing: Boolean,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = PurpleLight),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, Purple.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(teacher.fullName, fontWeight = FontWeight.SemiBold, color = Purple)
                    Text(teacher.country, fontSize = 12.sp, color = Purple.copy(alpha = 0.6f))
                }
                Text(teacher.hourlyRate, fontWeight = FontWeight.SemiBold, color = Purple, fontSize = 13.sp)
            }

            Spacer(Modifier.height(8.dp))

            // Subjects + levels
            Text(
                teacher.subjects.take(4).joinToString(" · "),
                fontSize = 12.sp,
                color = Purple.copy(alpha = 0.75f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                teacher.levels.joinToString(", "),
                fontSize = 12.sp,
                color = Purple.copy(alpha = 0.6f)
            )

            Spacer(Modifier.height(6.dp))

            // Bio preview
            if (teacher.bio.isNotBlank()) {
                Text(
                    teacher.bio,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = Purple.copy(alpha = 0.65f),
                    lineHeight = 17.sp
                )
                Spacer(Modifier.height(6.dp))
            }

            // Experience + education
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                if (teacher.yearsExperience.isNotBlank()) {
                    Text("${teacher.yearsExperience} yrs exp", fontSize = 12.sp, color = Purple.copy(alpha = 0.6f))
                }
                if (teacher.education.isNotBlank()) {
                    Text(teacher.education, fontSize = 12.sp, color = Purple.copy(alpha = 0.6f), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                }
            }

            Spacer(Modifier.height(12.dp))

            if (isProcessing) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Purple, strokeWidth = 2.dp)
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onApprove,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Teal)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Approve", fontSize = 13.sp)
                    }
                    OutlinedButton(
                        onClick = onReject,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Reject", fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

// ── Bookings Tab ──────────────────────────────────────────────────────────────

@Composable
private fun BookingsTab(uiState: AdminUiState, viewModel: AdminDashboardViewModel) {
    Column {
        // Filter chips
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = uiState.bookingFilter == null,
                    onClick = { viewModel.setBookingFilter(null) },
                    label = { Text("All") }
                )
            }
            items(BookingStatus.entries) { status ->
                FilterChip(
                    selected = uiState.bookingFilter == status,
                    onClick = { viewModel.setBookingFilter(status) },
                    label = { Text(status.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }) }
                )
            }
        }

        val filtered = viewModel.filteredBookings()

        if (filtered.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No bookings found", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
            }
            return
        }

        LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Text("${filtered.size} booking${if (filtered.size != 1) "s" else ""}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
            }
            items(filtered, key = { it.id }) { booking ->
                AdminBookingCard(
                    booking = booking,
                    isProcessing = uiState.actionLoading == booking.id
                            || uiState.confirmingBookingId == booking.id,
                    onCancel = { viewModel.cancelBooking(booking.id) },
                    onConfirmPayment = { viewModel.confirmPayment(booking.id) }
                )

            }
        }
    }
}

@Composable
private fun AdminBookingCard(booking: Booking, isProcessing: Boolean, onCancel: () -> Unit, onConfirmPayment: () -> Unit) {
    val statusColor = when (booking.status) {
        BookingStatus.CONFIRMED -> Teal
        BookingStatus.PAYMENT_SUBMITTED -> Amber
        BookingStatus.COMPLETED -> Color(0xFF639922)
        BookingStatus.CANCELLED -> MaterialTheme.colorScheme.error
        BookingStatus.PENDING_PAYMENT -> Purple
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("${booking.studentName} → ${booking.teacherName}", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    Text("${booking.subject} · ${booking.sessionLength}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                }
                Surface(shape = RoundedCornerShape(20.dp), color = statusColor.copy(alpha = 0.12f)) {
                    Text(
                        booking.status.name.replace("_", " "),
                        fontSize = 10.sp,
                        color = statusColor,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Amount: ${booking.totalAmount}", fontSize = 12.sp)
                if (booking.paymentTransactionId.isNotBlank()) {
                    Text("Txn: ${booking.paymentTransactionId}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f))
                }
            }
            if (booking.status != BookingStatus.CANCELLED && booking.status != BookingStatus.COMPLETED) {
                Spacer(Modifier.height(8.dp))
                if (isProcessing) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Purple, strokeWidth = 2.dp)
                } else {
                    TextButton(
                        onClick = onCancel,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Cancel booking", fontSize = 12.sp)
                    }
                }
            }

            if (booking.status == BookingStatus.PAYMENT_SUBMITTED) {
                Spacer(Modifier.height(8.dp))

                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Purple,
                        strokeWidth = 2.dp
                    )
                } else {
                    Button(
                        onClick = onConfirmPayment,
                        colors = ButtonDefaults.buttonColors(containerColor = Teal)
                    ) {
                        Text("Confirm payment", fontSize = 12.sp)
                    }
                }
            }

        }
    }
}

// ── Users Tab ─────────────────────────────────────────────────────────────────

@Composable
private fun UsersTab(uiState: AdminUiState, viewModel: AdminDashboardViewModel) {
    if (uiState.allUsers.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No users found", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
        }
        return
    }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Text("${uiState.allUsers.size} registered users", fontSize = 13.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
        }
        items(uiState.allUsers, key = { it.uid }) { user ->
            AdminUserCard(
                user = user,
                isProcessing = uiState.actionLoading == user.uid,
                onDisable = { viewModel.disableUser(user.uid) }
            )
        }
    }
}

@Composable
private fun AdminUserCard(user: User, isProcessing: Boolean, onDisable: () -> Unit) {
    val roleColor = when (user.role.name) {
        "TEACHER" -> Purple
        "ADMIN" -> MaterialTheme.colorScheme.error
        else -> Teal
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(user.email, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                    Surface(shape = RoundedCornerShape(20.dp), color = roleColor.copy(alpha = 0.12f)) {
                        Text(user.role.name, fontSize = 10.sp, color = roleColor, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                    if (user.onboardingComplete) {
                        Text("Onboarded", fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f))
                    }
                }
            }
            if (user.role.name != "ADMIN") {
                if (isProcessing) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Purple, strokeWidth = 2.dp)
                } else {
                    TextButton(
                        onClick = onDisable,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Disable", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

// ── QR Tab ────────────────────────────────────────────────────────────────────

@Composable
private fun QrTab(viewModel: AdminDashboardViewModel) {
    AdminQrUploadSection(viewModel = viewModel)
}