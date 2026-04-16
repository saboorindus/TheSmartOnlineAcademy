package com.echologics.thesmartonlineacademy.ui.admin

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.echologics.thesmartonlineacademy.data.model.AdminQrConfig
import com.echologics.thesmartonlineacademy.data.repository.AdminRepository
import com.echologics.thesmartonlineacademy.ui.common.theme.Purple
import com.echologics.thesmartonlineacademy.ui.common.theme.Teal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.echologics.thesmartonlineacademy.ui.common.components.AppTextField
import com.echologics.thesmartonlineacademy.ui.common.components.PrimaryButton
import com.echologics.thesmartonlineacademy.ui.common.theme.TealLight

data class QrUiState(
    val qrImageUrl: String = "",
    val accountTitle: String = "",
    val accountNumber: String = "",
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null
)

class AdminQrViewModel(
    private val repo: AdminRepository = AdminRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(QrUiState())
    val uiState: StateFlow<QrUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            val result = repo.getQrConfig()
            result.fold(
                onSuccess = { config ->
                    _uiState.value = _uiState.value.copy(
                        qrImageUrl = config.qrImageUrl,
                        accountTitle = config.accountTitle,
                        accountNumber = config.accountNumber,
                        isLoading = false
                    )
                },
                onFailure = {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
            )
        }
    }

    fun uploadQr(bytes: ByteArray) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null)

            val result = repo.uploadQrToSupabase(
                bytes = bytes,
                fileName = "qr/admin_qr.png"
            )

            result.fold(
                onSuccess = { url ->
                    _uiState.value = _uiState.value.copy(
                        qrImageUrl = url,
                        isSaving = false,
                        isSaved = true
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        error = e.message
                    )
                }
            )
        }
    }

//    fun onUrlChange(v: String) { _uiState.value = _uiState.value.copy(qrImageUrl = v, isSaved = false) }
    fun onTitleChange(v: String) { _uiState.value = _uiState.value.copy(accountTitle = v, isSaved = false) }
    fun onNumberChange(v: String) { _uiState.value = _uiState.value.copy(accountNumber = v, isSaved = false) }

    fun save() {
        val s = _uiState.value
        if (s.accountTitle.isBlank() || s.accountNumber.isBlank()) {
            _uiState.value = s.copy(error = "Account title and number are required")
            return
        }
        _uiState.value = s.copy(isSaving = true, error = null)
        viewModelScope.launch {
            val config = AdminQrConfig(
                qrImageUrl = s.qrImageUrl.trim(),
                accountTitle = s.accountTitle.trim(),
                accountNumber = s.accountNumber.trim()
            )
            val result = repo.saveQrConfig(config)
            result.fold(
                onSuccess = { _uiState.value = _uiState.value.copy(isSaving = false, isSaved = true) },
                onFailure = { e -> _uiState.value = _uiState.value.copy(isSaving = false, error = e.message) }
            )
        }
    }
}

@Composable
fun AdminQrUploadSection(viewModel: AdminDashboardViewModel) {
    // QR section uses its own lightweight vm via remember
    val qrVm = remember { AdminQrViewModel() }
    val uiState by qrVm.uiState.collectAsState()

    val context = LocalContext.current

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val inputStream = context.contentResolver.openInputStream(uri)
            val bytes = inputStream?.readBytes()

            if (bytes != null) {
                qrVm.uploadQr(bytes)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text("Payment QR setup", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        Text(
            "Students will see this QR code on the payment screen. Update it whenever your account details change.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            lineHeight = 19.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
        )

        if (uiState.isLoading) {
            CircularProgressIndicator(color = Purple, modifier = Modifier.align(Alignment.CenterHorizontally))
            return
        }

        // QR preview
        Box(
            modifier = Modifier
                .size(160.dp)
                .align(Alignment.CenterHorizontally)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (uiState.qrImageUrl.isNotBlank()) {

                AsyncImage(
                    model = uiState.qrImageUrl,
                    contentDescription = "Payment QR",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No QR set", fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f))
                    Text("Upload QR image", fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f))
                }
            }

        }

        Spacer(Modifier.height(24.dp))

        PrimaryButton(
            text = "Upload QR Image",
            onClick = {
                imagePicker.launch("image/*")
            },
            isLoading = uiState.isSaving
        )

        Spacer(Modifier.height(12.dp))

        AppTextField(
            value = uiState.accountTitle,
            onValueChange = qrVm::onTitleChange,
            label = "Account holder name"
        )

        Spacer(Modifier.height(12.dp))

        AppTextField(
            value = uiState.accountNumber,
            onValueChange = qrVm::onNumberChange,
            label = "Account / phone number"
        )

        if (uiState.error != null) {
            Spacer(Modifier.height(10.dp))
            Text(uiState.error!!, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
        }

        if (uiState.isSaved) {
            Spacer(Modifier.height(10.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = TealLight),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("QR configuration saved successfully", color = Teal, modifier = Modifier.padding(12.dp), fontSize = 13.sp)
            }
        }

        Spacer(Modifier.height(24.dp))

        PrimaryButton(
            text = "Save QR configuration",
            onClick = qrVm::save,
            isLoading = uiState.isSaving
        )

        Spacer(Modifier.height(16.dp))

        // Info box
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(10.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("How to use", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Spacer(Modifier.height(6.dp))
                Text(
                    "1. Upload your EasyPaisa or JazzCash QR image to Firebase Storage\n" +
                            "2. Copy the download URL and paste it above\n" +
                            "3. Add your account details\n" +
                            "4. Save — students will see this immediately on the payment screen",
                    fontSize = 12.sp,
                    lineHeight = 19.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f)
                )
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}