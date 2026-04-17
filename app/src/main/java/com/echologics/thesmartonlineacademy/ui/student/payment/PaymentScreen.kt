package com.echologics.thesmartonlineacademy.ui.student.payment

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.echologics.thesmartonlineacademy.data.model.Booking
import com.echologics.thesmartonlineacademy.ui.common.components.AppTextField
import com.echologics.thesmartonlineacademy.ui.common.components.PrimaryButton
import com.echologics.thesmartonlineacademy.ui.common.theme.Purple
import com.echologics.thesmartonlineacademy.ui.common.theme.PurpleLight
import com.echologics.thesmartonlineacademy.ui.common.theme.Teal
import com.echologics.thesmartonlineacademy.ui.common.theme.TealLight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(
    viewModel: PaymentViewModel,
    booking: Booking,
    onPaymentSubmitted: () -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(booking) {
        viewModel.setBooking(booking)
    }

    LaunchedEffect(uiState.isSubmitted) {
        if (uiState.isSubmitted) onPaymentSubmitted()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Complete payment") },
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
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {

            // Booking summary card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = PurpleLight)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Booking summary", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Purple)
                    Spacer(Modifier.height(10.dp))
                    SummaryRow("Teacher", booking.teacherName)
                    SummaryRow("Subject", booking.subject)
                    SummaryRow("Session", booking.sessionLength)
                    SummaryRow("Day", "${booking.slotDay} ${booking.scheduledTime}")
                    if (booking.scheduledDate.isNotBlank()) SummaryRow("Date", booking.scheduledDate)
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        thickness = 0.5.dp,
                        color = Purple.copy(alpha = 0.2f)
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total due", fontWeight = FontWeight.SemiBold, color = Purple)
                        Text(booking.totalAmount, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = Purple)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // QR Code placeholder
            Text("Scan to pay", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text(
                "Use EasyPaisa or JazzCash to send the exact amount",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                modifier = Modifier.padding(top = 2.dp, bottom = 14.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // QR grid placeholder (real app: use coil + Firebase Storage URL)
                    QrImage(
                        qrUrl = "https://vilzjwakvylaihhwitwi.supabase.co/storage/v1/object/public/qr-images/qr/admin_qr.png"
                    )

                    Spacer(Modifier.height(10.dp))
                    Text(
                        "QR Code",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                    )
                    Text(
                        "Send exactly ${booking.totalAmount}",
                        fontSize = 12.sp,
                        color = Purple,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            HorizontalDivider(thickness = 0.5.dp)

            Spacer(Modifier.height(24.dp))

            // Payment proof fields
            Text("Confirm your payment", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text(
                "After sending, enter your transaction details below",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                modifier = Modifier.padding(top = 2.dp, bottom = 14.dp)
            )

            AppTextField(
                value = uiState.transactionId,
                onValueChange = viewModel::onTransactionIdChange,
                label = "Transaction ID / Reference number",
                isError = uiState.error != null && uiState.transactionId.isBlank()
            )

            Spacer(Modifier.height(12.dp))

            AppTextField(
                value = uiState.senderName,
                onValueChange = viewModel::onSenderNameChange,
                label = "Sender name (account holder name)",
                isError = uiState.error != null && uiState.senderName.isBlank()
            )

            if (uiState.error != null) {
                Spacer(Modifier.height(10.dp))
                Text(uiState.error!!, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
            }

            Spacer(Modifier.height(16.dp))

            // Info box
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = TealLight),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = "Once you submit, your booking will be marked as pending. Your session will be confirmed once the payment is verified.",
                    fontSize = 12.sp,
                    color = Teal,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(12.dp)
                )
            }

            Spacer(Modifier.height(24.dp))

            PrimaryButton(
                text = "Submit payment",
                onClick = viewModel::submitPayment,
                enabled = viewModel.canSubmit(),
                isLoading = uiState.isLoading
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 13.sp, color = Purple.copy(alpha = 0.65f))
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Purple)
    }
}

@Composable
private fun QrImage(qrUrl: String) {
    AsyncImage(
        model = qrUrl,
        contentDescription = "Payment QR Code",
        modifier = Modifier
            .size(180.dp)
            .clip(RoundedCornerShape(12.dp)),
        contentScale = androidx.compose.ui.layout.ContentScale.Fit
    )
}
