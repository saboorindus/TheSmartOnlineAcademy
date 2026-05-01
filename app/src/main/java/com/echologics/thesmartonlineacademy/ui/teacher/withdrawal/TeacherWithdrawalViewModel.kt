package com.echologics.thesmartonlineacademy.ui.teacher.withdrawal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.echologics.thesmartonlineacademy.data.model.PlatformConfig
import com.echologics.thesmartonlineacademy.data.model.Withdrawal
import com.echologics.thesmartonlineacademy.data.repository.EarningsSummary
import com.echologics.thesmartonlineacademy.data.repository.WithdrawalRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class TeacherWithdrawalUiState(
    val summary: EarningsSummary = EarningsSummary(),
    val config: PlatformConfig = PlatformConfig(),
    val withdrawals: List<Withdrawal> = emptyList(),
    val amountInput: String = "",
    val isLoading: Boolean = true,
    val isSubmitting: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,

    val paymentMethod: PaymentMethod = PaymentMethod.EASYPAISA,

    val accountTitle: String = "",    // name on account
    val accountNumber: String = "",   // wallet number

)

enum class PaymentMethod {
    EASYPAISA,
    JAZZCASH
}



class TeacherWithdrawalViewModel(
    private val withdrawalRepository: WithdrawalRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TeacherWithdrawalUiState())
    val uiState: StateFlow<TeacherWithdrawalUiState> = _uiState.asStateFlow()

    private val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    private var teacherName = ""

    init { load() }

    fun onAccountTitleChange(value: String) {
        _uiState.value = _uiState.value.copy(accountTitle = value)
    }

    fun onAccountNumberChange(value: String) {
        if (value.all { it.isDigit() }) {
            _uiState.value = _uiState.value.copy(accountNumber = value)
        }
    }



    fun onAmountChange(v: String) {
        if (v.isEmpty() || v.all { it.isDigit() }) {
            _uiState.value = _uiState.value.copy(amountInput = v, error = null)
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(error = null, successMessage = null)
    }

    fun setMethod(method: PaymentMethod) {
        _uiState.value = _uiState.value.copy(paymentMethod = method)
    }


    fun requestWithdrawal() {
        val state = _uiState.value
        val amount = state.amountInput.toIntOrNull() ?: run {
            _uiState.value = state.copy(error = "Please enter a valid amount")
            return
        }


        if (state.accountTitle.isBlank()) {
            _uiState.value = state.copy(error = "Enter account title")
            return
        }

        if (state.accountNumber.isBlank()) {
            _uiState.value = state.copy(error = "Enter account number")
            return
        }


        when {

            amount <= 0 -> {
                _uiState.value = state.copy(error = "Amount must be greater than zero")
                return
            }
            amount < state.config.minimumWithdrawal -> {
                _uiState.value = state.copy(
                    error = "Minimum withdrawal is ${state.summary.currency} ${state.config.minimumWithdrawal}"
                )
                return
            }
            amount > state.summary.availableBalance -> {
                _uiState.value = state.copy(
                    error = "Amount exceeds your available balance of ${state.summary.display(state.summary.availableBalance)}"
                )
                return
            }
        }

        _uiState.value = state.copy(isSubmitting = true, error = null)
        viewModelScope.launch {
            val result = withdrawalRepository.requestWithdrawal(
                withdrawal = Withdrawal(
                    teacherId = uid,
                    teacherName = teacherName,
                    amount = amount,
                    currency = state.summary.currency,

                    paymentMethod = state.paymentMethod.name,
                    accountTitle = state.accountTitle,
                    accountNumber = state.accountNumber
                )
            )

            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        amountInput = "",
                        successMessage = "Withdrawal request of ${state.summary.currency} $amount submitted. Admin will process it shortly."
                    )
                    load()
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        error = e.message ?: "Failed to submit withdrawal"
                    )
                }
            )
        }
    }

    fun refresh() = load()

    private fun load() {
        viewModelScope.launch {
            // Get teacher name
            try {
                val doc = FirebaseFirestore.getInstance()
                    .collection("teachers").document(uid).get().await()
                teacherName = doc.getString("fullName") ?: ""
            } catch (_: Exception) {}

            val summary = withdrawalRepository.getEarningsSummary(uid).getOrDefault(EarningsSummary())
            val config = withdrawalRepository.getPlatformConfig().getOrDefault(PlatformConfig())
            val withdrawals = withdrawalRepository.getWithdrawalsForTeacher(uid).getOrDefault(emptyList())

            _uiState.value = _uiState.value.copy(
                summary = summary,
                config = config,
                withdrawals = withdrawals,
                isLoading = false
            )
        }
    }
}