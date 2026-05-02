package com.echologics.thesmartonlineacademy.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.echologics.thesmartonlineacademy.data.model.Booking
import com.echologics.thesmartonlineacademy.data.model.BookingStatus
import com.echologics.thesmartonlineacademy.data.model.TeacherProfile
import com.echologics.thesmartonlineacademy.data.model.User
import com.echologics.thesmartonlineacademy.data.model.Withdrawal
import com.echologics.thesmartonlineacademy.data.model.WithdrawalStatus
import com.echologics.thesmartonlineacademy.ui.common.components.AppTextField
import com.echologics.thesmartonlineacademy.ui.common.components.PrimaryButton
import com.echologics.thesmartonlineacademy.ui.common.components.WithdrawalCard
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

            // Success / error banner — only show outside PaymentsConfig tab to avoid duplication
            if (uiState.selectedTab != AdminTab.PAYMENT) {
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
                AdminTab.BOOKINGS  -> BookingsTab(uiState, viewModel)
                AdminTab.USERS     -> UsersTab(uiState, viewModel)
                AdminTab.QR        -> QrTab(viewModel)
                AdminTab.PAYMENT   -> PaymentsConfig(uiState, viewModel)
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
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard("Teachers", uiState.stats.totalTeachers.toString(), Icons.Default.School, Purple, modifier = Modifier.weight(1f))
                    StatCard("Students", uiState.stats.totalStudents.toString(), Icons.Default.People, Teal, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard("Bookings", uiState.stats.totalBookings.toString(), Icons.Default.CalendarMonth, Amber, modifier = Modifier.weight(1f))
                    StatCard("Revenue", uiState.stats.totalRevenue.toString(), Icons.Default.Payments, Color(0xFF639922), modifier = Modifier.weight(1f))
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
                Column(horizontalAlignment = Alignment.End) {
                    Text(teacher.displayRate(), fontWeight = FontWeight.SemiBold, color = Purple, fontSize = 13.sp)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                teacher.subjects.take(4).joinToString(" · "),
                fontSize = 12.sp, color = Purple.copy(alpha = 0.75f),
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Text(teacher.levels.joinToString(", "), fontSize = 12.sp, color = Purple.copy(alpha = 0.6f))
            Spacer(Modifier.height(6.dp))
            if (teacher.bio.isNotBlank()) {
                Text(
                    teacher.bio, fontSize = 12.sp, maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = Purple.copy(alpha = 0.65f), lineHeight = 17.sp
                )
                Spacer(Modifier.height(6.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                if (teacher.yearsExperience.isNotBlank())
                    Text("${teacher.yearsExperience} yrs exp", fontSize = 12.sp, color = Purple.copy(alpha = 0.6f))
                if (teacher.education.isNotBlank())
                    Text(teacher.education, fontSize = 12.sp, color = Purple.copy(alpha = 0.6f),
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            if (isProcessing) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Purple, strokeWidth = 2.dp)
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onApprove, modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Teal)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Approve", fontSize = 13.sp)
                    }
                    OutlinedButton(
                        onClick = onReject, modifier = Modifier.weight(1f),
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
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(selected = uiState.bookingFilter == null,
                    onClick = { viewModel.setBookingFilter(null) }, label = { Text("All") })
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
                Text("${filtered.size} booking${if (filtered.size != 1) "s" else ""}",
                    fontSize = 13.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
            }
            items(filtered, key = { it.id }) { booking ->
                AdminBookingCard(
                    booking = booking,
                    isCancelling = uiState.actionLoading == booking.id,       // ← computed here
                    isConfirming = uiState.confirmingBookingId == booking.id, // ← computed here
                    onCancel = { viewModel.cancelBooking(booking.id) },
                    onConfirmPayment = { viewModel.confirmPayment(booking) }
                )
            }
        }
    }
}

@Composable
private fun AdminBookingCard(
    booking: Booking,
    isCancelling: Boolean,   // ← replaces uiState.actionLoading == booking.id
    isConfirming: Boolean,   // ← replaces uiState.confirmingBookingId == booking.id
    onCancel: () -> Unit,
    onConfirmPayment: () -> Unit
) {
    val statusColor = when (booking.status) {
        BookingStatus.CONFIRMED         -> Teal
        BookingStatus.PAYMENT_SUBMITTED -> Amber
        BookingStatus.COMPLETED         -> Color(0xFF639922)
        BookingStatus.CANCELLED         -> MaterialTheme.colorScheme.error
        BookingStatus.PENDING_PAYMENT   -> Purple
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("${booking.studentName} → ${booking.teacherName}", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    Text("${booking.subject} · ${booking.sessionLength}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                }
                Surface(shape = RoundedCornerShape(20.dp), color = statusColor.copy(alpha = 0.12f)) {
                    Text(booking.status.name.replace("_", " "), fontSize = 10.sp, color = statusColor,
                        fontWeight = FontWeight.Medium, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Amount: ${booking.totalAmount}", fontSize = 12.sp)
                if (booking.paymentTransactionId.isNotBlank())
                    Text("Txn: ${booking.paymentTransactionId}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f))
            }
            if (booking.status != BookingStatus.CANCELLED && booking.status != BookingStatus.COMPLETED) {
                Spacer(Modifier.height(8.dp))
                if (isCancelling) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Purple, strokeWidth = 2.dp)
                } else {
                    TextButton(onClick = onCancel, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                        Text("Cancel booking", fontSize = 12.sp)
                    }
                }
            }

            if (booking.status == BookingStatus.PAYMENT_SUBMITTED) {
                Spacer(Modifier.height(8.dp))
                if (isConfirming) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Purple, strokeWidth = 2.dp)
                } else {
                    Button(onClick = onConfirmPayment, colors = ButtonDefaults.buttonColors(containerColor = Teal)) {
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
            AdminUserCard(user = user, isProcessing = uiState.actionLoading == user.uid, onDisable = { viewModel.toggleUserStatus(user) })
        }
    }
}

@Composable
private fun AdminUserCard(user: User, isProcessing: Boolean, onDisable: () -> Unit) {
    val roleColor = when (user.role.name) {
        "TEACHER" -> Purple
        "ADMIN"   -> MaterialTheme.colorScheme.error
        else      -> Teal
    }
    val isDisabled = user.disabled == true

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f)) {
                Text(user.email, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                    Surface(shape = RoundedCornerShape(20.dp), color = roleColor.copy(alpha = 0.12f)) {
                        Text(user.role.name, fontSize = 10.sp, color = roleColor, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                    if (user.onboardingComplete) Text("Onboarded", fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f))
                    if (user.disabled == true) Text("Disabled", fontSize = 11.sp, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 2.dp))
                }
            }
            if (user.role.name != "ADMIN") {
                if (isProcessing) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Purple, strokeWidth = 2.dp)
                } else {
                    TextButton(
                        onClick = onDisable,
                        colors = ButtonDefaults.textButtonColors(contentColor = if (isDisabled) Teal else MaterialTheme.colorScheme.error)
                    ) {
                        Text(if (isDisabled) "Enable" else "Disable", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

// ── QR Tab ────────────────────────────────────────────────────────────────────

@Composable
private fun QrTab(viewModel: AdminDashboardViewModel) {
    val factory = remember {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return AdminQrViewModel(viewModel.repo) as T
            }
        }
    }
    AdminQrUploadSection(factory = factory)
}

// ── Payment Settings Tab ──────────────────────────────────────────────────────

@Composable
private fun PaymentsConfig(
    uiState: AdminUiState,
    viewModel: AdminDashboardViewModel,
) {
    var minWithdrawal by remember(uiState.platformConfig.minimumWithdrawal) {
        mutableStateOf(uiState.platformConfig.minimumWithdrawal.toString())
    }
    var feePercent by remember(uiState.platformConfig.platformFeePercent) {
        mutableStateOf(uiState.platformConfig.platformFeePercent.toString())
    }
    var selectedWithdrawal by remember { mutableStateOf<Withdrawal?>(null) }
    var transactionIdInput by remember { mutableStateOf("") }
    var adminNoteInput by remember { mutableStateOf("") }
    var showWithdrawalDialog by remember { mutableStateOf(false) }

    LaunchedEffect(selectedWithdrawal) {
        transactionIdInput = selectedWithdrawal?.transactionId ?: ""
        adminNoteInput = selectedWithdrawal?.adminNote ?: ""
    }

    Column(modifier = Modifier.fillMaxSize()) {

        // ── Feedback banners (only inside Payment tab) ────────────────────────
        uiState.successMessage?.let { msg ->
            LaunchedEffect(msg) { viewModel.clearMessage() }
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                colors = CardDefaults.cardColors(containerColor = TealLight),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Teal, modifier = Modifier.size(16.dp))
                    Text(msg, color = Teal, fontSize = 13.sp)
                }
            }
        }
        uiState.error?.let { err ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    Text(err, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                }
            }
        }

        // ── Sub-tab chips ─────────────────────────────────────────────────────
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(label = { Text("Settings") },
                    selected = uiState.paymentSettingUIState == PaymentSettingUIState.SETTINGS,
                    onClick = { viewModel.setPaymentUIState(PaymentSettingUIState.SETTINGS) })
            }
            item {
                FilterChip(label = { Text("Withdrawals") },
                    selected = uiState.paymentSettingUIState == PaymentSettingUIState.WITHDRAWALS,
                    onClick = { viewModel.setPaymentUIState(PaymentSettingUIState.WITHDRAWALS) })
            }
            item {
                FilterChip(label = { Text("Earnings") },
                    selected = uiState.paymentSettingUIState == PaymentSettingUIState.EARNINGS,
                    onClick = { viewModel.setPaymentUIState(PaymentSettingUIState.EARNINGS) })
            }
        }

        // ── Content ───────────────────────────────────────────────────────────
        when (uiState.paymentSettingUIState) {

            // ── SETTINGS ─────────────────────────────────────────────────────
            PaymentSettingUIState.SETTINGS -> {
                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = PurpleLight), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("Payment Configuration", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Purple)
                            Spacer(Modifier.height(4.dp))
                            Text("Control how teachers withdraw earnings and platform fee structure.", fontSize = 12.sp, color = Purple.copy(alpha = 0.7f))
                        }
                    }

                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("Current Settings", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                            Spacer(Modifier.height(8.dp))
                            Text("Minimum Withdrawal: PKR ${uiState.platformConfig.minimumWithdrawal}", fontSize = 13.sp)
                            Text("Platform Fee: ${uiState.platformConfig.platformFeePercent}%", fontSize = 13.sp)
                        }
                    }

                    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = TealLight), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Edit Settings", fontWeight = FontWeight.SemiBold, color = Teal)
                            AppTextField(value = minWithdrawal, label = "Minimum Withdrawal (PKR)", onValueChange = { minWithdrawal = it })
                            AppTextField(value = feePercent, label = "Platform Fee (%)", onValueChange = { feePercent = it })
                            PrimaryButton(
                                text = if (uiState.isSaving) "Saving..." else "Save Changes",
                                onClick = {
                                    val min = minWithdrawal.toIntOrNull()
                                    val fee = feePercent.toIntOrNull()
                                    if (min != null && fee != null) {
                                        viewModel.setPlatformConfig(uiState.platformConfig.copy(minimumWithdrawal = min, platformFeePercent = fee))
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // ── WITHDRAWALS ───────────────────────────────────────────────────
            PaymentSettingUIState.WITHDRAWALS -> {
                val withdrawals = uiState.withdrawal
                if (withdrawals.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Payments, contentDescription = null, tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f), modifier = Modifier.size(40.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("No withdrawals yet", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                        }
                    }
                } else {
                    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(withdrawals, key = { it.id }) { w ->
                            WithdrawalCard(
                                withdrawal = w,
                                onClick = {
                                    // BUG FIX: only open dialog for PENDING withdrawals
                                    if (w.status == WithdrawalStatus.PENDING) {
                                        selectedWithdrawal = w
                                        transactionIdInput = w.transactionId
                                        adminNoteInput = w.adminNote
                                        showWithdrawalDialog = true
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // ── EARNINGS ──────────────────────────────────────────────────────
            PaymentSettingUIState.EARNINGS -> {
                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item { Text("Earnings Overview", fontSize = 18.sp, fontWeight = FontWeight.SemiBold) }

                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                StatCard("Total Revenue", "PKR ${uiState.totalRevenue}", Icons.Default.Payments, Purple, modifier = Modifier.weight(1f))
                                StatCard("Platform Earnings", "PKR ${uiState.platformEarnings}", Icons.Default.AccountBalance, Teal, modifier = Modifier.weight(1f))
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                StatCard("Teacher Payouts", "PKR ${uiState.teacherPayouts}", Icons.Default.School, Amber, modifier = Modifier.weight(1f))
                                StatCard("Pending Withdrawals", "PKR ${uiState.pendingPayouts}", Icons.Default.HourglassTop, MaterialTheme.colorScheme.error, modifier = Modifier.weight(1f))
                            }
                        }
                    }

                    item {
                        Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("Revenue Insights", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                Spacer(Modifier.height(10.dp))
                                RevenueInsightRow("Today", uiState.todayRevenue)
                                RevenueInsightRow("This Week", uiState.weeklyRevenue)
                                RevenueInsightRow("This Month", uiState.monthlyRevenue)
                            }
                        }
                    }

                    item {
                        Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = PurpleLight), modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("Payout Management", fontWeight = FontWeight.SemiBold, color = Purple)
                                Spacer(Modifier.height(6.dp))
                                Text("Review teacher withdrawals and process pending payouts.", fontSize = 12.sp, color = Purple.copy(alpha = 0.7f))
                                Spacer(Modifier.height(10.dp))
                                PrimaryButton(text = "Go to Withdrawals", onClick = { viewModel.setPaymentUIState(PaymentSettingUIState.WITHDRAWALS) })
                            }
                        }
                    }
                }
            }
        }
    }

    // ── Withdrawal Action Dialog ───────────────────────────────────────────────
    if (showWithdrawalDialog && selectedWithdrawal != null) {
        val w = selectedWithdrawal!!
        AlertDialog(
            onDismissRequest = {
                showWithdrawalDialog = false
                selectedWithdrawal = null
            },
            shape = RoundedCornerShape(16.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Payments, contentDescription = null, tint = Purple, modifier = Modifier.size(20.dp))
                    Text("Withdrawal Request", fontWeight = FontWeight.SemiBold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {

                    // ── Teacher & amount summary card ─────────────────────────
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = PurpleLight),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Teacher", fontSize = 12.sp, color = Purple.copy(alpha = 0.6f))
                                Text(w.teacherName, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Purple)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Amount", fontSize = 12.sp, color = Purple.copy(alpha = 0.6f))
                                Text(w.displayAmount(), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Purple)
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // ── Payment method details ────────────────────────────────
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Payment Details", fontSize = 12.sp, fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                            HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                            DialogDetailRow(label = "Method", value = w.paymentMethod)
                            if (w.accountTitle.isNotBlank()) DialogDetailRow(label = "Account Name", value = w.accountTitle)
                            if (w.accountNumber.isNotBlank()) DialogDetailRow(label = "Account No.", value = w.accountNumber)
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // ── Admin input fields ────────────────────────────────────
                    OutlinedTextField(
                        value = transactionIdInput,
                        onValueChange = { transactionIdInput = it },
                        label = { Text("Transaction ID (optional)") },
                        leadingIcon = { Icon(Icons.Default.Tag, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = adminNoteInput,
                        onValueChange = { adminNoteInput = it },
                        label = { Text("Admin Note (optional)") },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.Notes, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                // ── Action buttons row ────────────────────────────────────────
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            viewModel.markWithdrawalPaid(
                                w.copy(transactionId = transactionIdInput, adminNote = adminNoteInput)
                            )
                            showWithdrawalDialog = false
                            selectedWithdrawal = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Teal)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Mark as Paid")
                    }
                    OutlinedButton(
                        onClick = {
                            viewModel.rejectWithdrawal(w.id)
                            showWithdrawalDialog = false
                            selectedWithdrawal = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Reject")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showWithdrawalDialog = false; selectedWithdrawal = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// ── Small helper composables ──────────────────────────────────────────────────

@Composable
private fun DialogDetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 16.dp))
    }
}

@Composable
private fun RevenueInsightRow(label: String, amount: Int) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
        Text("PKR $amount", fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}