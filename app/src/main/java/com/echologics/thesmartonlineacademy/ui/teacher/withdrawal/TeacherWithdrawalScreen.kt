package com.echologics.thesmartonlineacademy.ui.teacher.withdrawal

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.echologics.thesmartonlineacademy.data.model.Withdrawal
import com.echologics.thesmartonlineacademy.data.model.WithdrawalStatus
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
fun TeacherWithdrawalScreen(
    viewModel: TeacherWithdrawalViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val summary = uiState.summary
    val config = uiState.config

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Withdraw earnings") },
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

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            // ── Balance summary card ───────────────────────────────────────────
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = PurpleLight),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, Purple.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Balance overview",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = Purple
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            BalanceItem(
                                label = "Available",
                                value = summary.display(summary.availableBalance),
                                color = Purple,
                                modifier = Modifier.weight(1f)
                            )
                            BalanceItem(
                                label = "Pending",
                                value = summary.display(summary.pendingWithdrawals),
                                color = Amber,
                                modifier = Modifier.weight(1f)
                            )
                            BalanceItem(
                                label = "Withdrawn",
                                value = summary.display(summary.totalWithdrawn),
                                color = Teal,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // ── Withdrawal request form ────────────────────────────────────────
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Request withdrawal",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )

                        Spacer(Modifier.height(4.dp))

                        Text(
                            "Minimum withdrawal: ${summary.currency} ${config.minimumWithdrawal}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        )

                        Spacer(Modifier.height(14.dp))

                        OutlinedTextField(
                            value = uiState.amountInput,
                            onValueChange = viewModel::onAmountChange,
                            label = { Text("Amount to withdraw") },
                            placeholder = { Text("e.g. ${config.minimumWithdrawal}") },
                            prefix = { Text("${summary.currency}  ") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth(),
                            isError = uiState.error != null,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Purple,
                                focusedLabelColor = Purple,
                                cursorColor = Purple,
                                errorBorderColor = MaterialTheme.colorScheme.error
                            )
                        )

                        // Quick amount buttons
                        val quickAmounts = listOf(
                            config.minimumWithdrawal,
                            config.minimumWithdrawal * 2,
                            config.minimumWithdrawal * 5
                        ).filter { it <= summary.availableBalance }

                        if (quickAmounts.isNotEmpty()) {
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                quickAmounts.forEach { amount ->
                                    OutlinedButton(
                                        onClick = { viewModel.onAmountChange(amount.toString()) },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        border = androidx.compose.foundation.BorderStroke(
                                            0.5.dp,
                                            if (uiState.amountInput == amount.toString()) Purple
                                            else MaterialTheme.colorScheme.outline
                                        ),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = if (uiState.amountInput == amount.toString()) Purple
                                            else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                        )
                                    ) {
                                        Text("${summary.currency} $amount", fontSize = 12.sp)
                                    }
                                }

                                // Max button
                                if (summary.availableBalance > 0) {
                                    OutlinedButton(
                                        onClick = { viewModel.onAmountChange(summary.availableBalance.toString()) },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        border = androidx.compose.foundation.BorderStroke(
                                            0.5.dp,
                                            if (uiState.amountInput == summary.availableBalance.toString()) Purple
                                            else MaterialTheme.colorScheme.outline
                                        ),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = if (uiState.amountInput == summary.availableBalance.toString()) Purple
                                            else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                        )
                                    ) {
                                        Text("Max", fontSize = 12.sp)
                                    }
                                }
                            }
                        }


                        // ── ⭐ NEW: PAYOUT METHOD ───────────────────────────────────
                        Spacer(Modifier.height(16.dp))

                        Text(
                            "Payout Method",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(Modifier.height(6.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {

                            FilterChip(
                                selected = uiState.paymentMethod == PaymentMethod.EASYPAISA,
                                onClick = { viewModel.setMethod(PaymentMethod.EASYPAISA) },
                                label = { Text("EasyPaisa") }
                            )

                            FilterChip(
                                selected = uiState.paymentMethod == PaymentMethod.JAZZCASH,
                                onClick = { viewModel.setMethod(PaymentMethod.JAZZCASH) },
                                label = { Text("JazzCash") }
                            )
                        }


                        // ── ⭐ NEW: ACCOUNT TITLE ───────────────────────────────────
                        Spacer(Modifier.height(12.dp))

                        OutlinedTextField(
                            value = uiState.accountTitle,
                            onValueChange = viewModel::onAccountTitleChange,
                            label = { Text("Account Title (Name)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // ── ⭐ NEW: ACCOUNT NUMBER ──────────────────────────────────
                        Spacer(Modifier.height(10.dp))

                        OutlinedTextField(
                            value = uiState.accountNumber,
                            onValueChange = viewModel::onAccountNumberChange,
                            label = { Text("Account Number") },
                            placeholder = { Text("03XXXXXXXXX") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )


                        // Error
                        if (uiState.error != null) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                uiState.error!!,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp
                            )
                        }

                        // Success message
                        if (uiState.successMessage != null) {
                            Spacer(Modifier.height(8.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = TealLight),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    uiState.successMessage!!,
                                    color = Teal,
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        Button(
                            onClick = viewModel::requestWithdrawal,
                            enabled = !uiState.isSubmitting &&
                                    uiState.amountInput.isNotBlank() &&
                                    summary.availableBalance > 0,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Purple)
                        ) {
                            if (uiState.isSubmitting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Submit withdrawal request", fontWeight = FontWeight.Medium)
                            }
                        }

                        if (summary.availableBalance <= 0) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "No balance available to withdraw.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f)
                            )
                        }
                    }
                }
            }

            // ── Withdrawal history header ──────────────────────────────────────
            item {
                Text(
                    "Withdrawal history",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            if (uiState.withdrawals.isEmpty()) {
                item {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No withdrawal requests yet",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f)
                        )
                    }
                }
            }

            // ── Withdrawal history rows ───────────────────────────────────────
            items(uiState.withdrawals, key = { it.id }) { withdrawal ->
                WithdrawalHistoryCard(withdrawal = withdrawal)
            }

            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun BalanceItem(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            value,
            fontSize = 15.sp,
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

@Composable
private fun WithdrawalHistoryCard(withdrawal: Withdrawal) {
    val (bgColor, borderColor, iconColor, statusLabel, icon) = when (withdrawal.status) {
        WithdrawalStatus.PAID -> WithdrawalCardStyle(
            bg = TealLight,
            border = Teal.copy(alpha = 0.4f),
            iconColor = Teal,
            label = "Payment sent",
            icon = Icons.Default.CheckCircle
        )
        WithdrawalStatus.PENDING -> WithdrawalCardStyle(
            bg = AmberLight,
            border = Amber.copy(alpha = 0.4f),
            iconColor = Amber,
            label = "Pending",
            icon = Icons.Default.HourglassTop
        )
        WithdrawalStatus.REJECTED -> WithdrawalCardStyle(
            bg = Color(0xFFFCEBEB),
            border = Color(0xFFA32D2D).copy(alpha = 0.4f),
            iconColor = Color(0xFFA32D2D),
            label = "Rejected",
            icon = Icons.Default.Close
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, borderColor)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        withdrawal.displayAmount(),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                        color = iconColor
                    )
                    Text(
                        "Requested ${formatDate(withdrawal.requestedAt)}",
                        fontSize = 12.sp,
                        color = iconColor.copy(alpha = 0.65f)
                    )
                    if (withdrawal.status == WithdrawalStatus.PAID && withdrawal.processedAt > 0) {
                        Text(
                            "Paid ${formatDate(withdrawal.processedAt)}",
                            fontSize = 12.sp,
                            color = Teal.copy(alpha = 0.8f)
                        )
                    }
                }

                // Status badge
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = iconColor.copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            icon,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            statusLabel,
                            fontSize = 11.sp,
                            color = iconColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Admin rejection note
            if (withdrawal.status == WithdrawalStatus.REJECTED &&
                withdrawal.adminNote.isNotBlank()
            ) {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = iconColor.copy(alpha = 0.2f)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Reason: ${withdrawal.adminNote}",
                    fontSize = 12.sp,
                    color = iconColor.copy(alpha = 0.8f),
                    lineHeight = 17.sp
                )
            }
        }
    }
}

private data class WithdrawalCardStyle(
    val bg: Color,
    val border: Color,
    val iconColor: Color,
    val label: String,
    val icon: ImageVector
)

// Kotlin destructuring for WithdrawalCardStyle
private operator fun WithdrawalCardStyle.component1() = bg
private operator fun WithdrawalCardStyle.component2() = border
private operator fun WithdrawalCardStyle.component3() = iconColor
private operator fun WithdrawalCardStyle.component4() = label
private operator fun WithdrawalCardStyle.component5() = icon

private fun formatDate(timestamp: Long): String {
    if (timestamp == 0L) return ""
    return SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(timestamp))
}