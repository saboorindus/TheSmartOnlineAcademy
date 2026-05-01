package com.echologics.thesmartonlineacademy.ui.common.components


import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.echologics.thesmartonlineacademy.ui.common.theme.Gray100
import com.echologics.thesmartonlineacademy.ui.common.theme.Purple
import com.echologics.thesmartonlineacademy.ui.common.theme.PurpleLight
import com.echologics.thesmartonlineacademy.R
import androidx.core.net.toUri
import com.echologics.thesmartonlineacademy.data.model.Withdrawal
import com.echologics.thesmartonlineacademy.data.model.WithdrawalStatus
import com.echologics.thesmartonlineacademy.ui.admin.AdminDashboardViewModel
import com.echologics.thesmartonlineacademy.ui.admin.AdminUiState
import com.echologics.thesmartonlineacademy.ui.common.theme.Amber
import com.echologics.thesmartonlineacademy.ui.common.theme.Teal


@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false
) {
    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Purple)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp
            )
        } else {
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun OutlinedPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Purple),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Purple)
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun CountryPickerField(
    value: String,
    countries: List<String>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Country"
) {
    Box(modifier = modifier) {

        AppTextField(
            value = value,
            onValueChange = {},
            label = label,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // full area click to open dropdown
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { onExpandedChange(true) }
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) }
        ) {
            countries.forEach { country ->
                DropdownMenuItem(
                    text = { Text(country) },
                    onClick = {
                        onValueChange(country)
                        onExpandedChange(false)
                    }
                )
            }
        }
    }
}

@Composable
fun PaymentMethodCard(
    label: String,
    isSelected: Boolean,
    accentColor: Color,
    lightColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderWidth = if (isSelected) 2.dp else 0.5.dp
    val borderColor = if (isSelected) accentColor else MaterialTheme.colorScheme.outline

    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(borderWidth, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(lightColor),
                contentAlignment = Alignment.Center
            ) {

                Image(
                    painter = painterResource(
                        id = if (label == "EasyPaisa") R.drawable.easypaisa
                        else R.drawable.jazzcash
                    ),
                    contentDescription = "$label logo",
                    modifier = Modifier
                        .size(26.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    contentScale = ContentScale.Fit
                )

            }
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            if (isSelected) {
                Surface(shape = RoundedCornerShape(4.dp), color = lightColor) {
                    Text(
                        "Selected",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = accentColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun DisabledAccountDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.Block,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                "Account Disabled",
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Text(
                "Your account has been disabled by an administrator. " +
                        "Please contact support if you believe this is a mistake.",
                textAlign = TextAlign.Center,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = "mailto:".toUri()
                        putExtra(Intent.EXTRA_EMAIL, arrayOf("contact@algostack.com"))
                        putExtra(Intent.EXTRA_SUBJECT, "Account Disabled - Appeal Request")
                        putExtra(
                            Intent.EXTRA_TEXT,
                            "Hello,\n\nMy account has been disabled and I would like to appeal this decision.\n\nPlease look into my account and assist me.\n\nThank you."
                        )
                    }
                    context.startActivity(Intent.createChooser(intent, "Contact Support"))
                },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Purple)
            ) {
                Text("Contact Support")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}


@Composable
fun WithdrawalsSection(
    uiState: AdminUiState,
    viewModel: AdminDashboardViewModel
) {
    val withdrawals = uiState.withdrawal

    if (withdrawals.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No withdrawals yet", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
        }
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(withdrawals, key = { it.id }) { w ->
            WithdrawalCard(w, viewModel)
        }
    }
}


@Composable
fun WithdrawalCard(
    withdrawal: Withdrawal,
    viewModel: AdminDashboardViewModel
) {
    val statusColor = when (withdrawal.status) {
        WithdrawalStatus.PENDING -> Amber
        WithdrawalStatus.PAID -> Teal
        WithdrawalStatus.REJECTED -> MaterialTheme.colorScheme.error
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(Modifier.padding(12.dp)) {

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(withdrawal.teacherName, fontWeight = FontWeight.Medium)
//                    Text("ID: ${withdrawal.teacherId}", fontSize = 11.sp)
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = statusColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        withdrawal.status.name,
                        color = statusColor,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            Text(
                "Amount: ${withdrawal.displayAmount()}",
                fontWeight = FontWeight.SemiBold
            )

            if (withdrawal.method.isNotBlank()) {
                Text("Method: ${withdrawal.method}", fontSize = 12.sp)
            }

            if (withdrawal.transactionId.isNotBlank()) {
                Text("Txn: ${withdrawal.transactionId}", fontSize = 12.sp)
            }

            if (withdrawal.adminNote.isNotBlank()) {
                Text("Note: ${withdrawal.adminNote}", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
            }

            // ACTIONS
            if (withdrawal.status == WithdrawalStatus.PENDING) {
                Spacer(Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {

                    Button(
                        onClick = {
                            viewModel.markWithdrawalPaid(withdrawal)
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Teal)
                    ) {
                        Text("Mark Paid", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.rejectWithdrawal(withdrawal.id)
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Reject", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}




@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    isError: Boolean = false,
    errorMessage: String? = null,
    singleLine: Boolean = true,
    maxLines: Int = 1,
    trailingIcon: @Composable (() -> Unit)? = null,

    // ✅ ADD THESE 3 (THIS FIXES YOUR ERRORS)
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation =
        if (isPassword) PasswordVisualTransformation()
        else VisualTransformation.None
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            isError = isError,
            singleLine = singleLine,
            maxLines = maxLines,
            visualTransformation = visualTransformation,
            trailingIcon = trailingIcon,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Purple,
                focusedLabelColor = Purple,
                cursorColor = Purple
            )
        )

        if (isError && errorMessage != null) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )
        }
    }
}


@Composable
fun SelectableChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.bodyMedium) },
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = PurpleLight,
            selectedLabelColor = Purple
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            selectedBorderColor = Purple,
            borderColor = Color(0xFFD3D1C7)
        )
    )
}

@Composable
fun StepProgressBar(
    currentStep: Int,
    totalSteps: Int,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Step $currentStep of $totalSteps",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.padding(bottom = 6.dp)
        )
        LinearProgressIndicator(
            progress = { currentStep.toFloat() / totalSteps.toFloat() },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp),
            color = Purple,
            trackColor = Gray100
        )
    }
}

@Composable
fun ErrorBanner(message: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFCEBEB)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = message,
            color = Color(0xFFA32D2D),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(12.dp)
        )
    }
}