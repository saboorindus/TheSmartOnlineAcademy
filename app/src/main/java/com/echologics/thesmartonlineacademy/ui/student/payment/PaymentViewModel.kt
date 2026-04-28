package com.echologics.thesmartonlineacademy.ui.student.payment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.echologics.thesmartonlineacademy.data.model.Booking
import com.echologics.thesmartonlineacademy.data.repository.BookingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
data class PaymentUiState(
    val booking: Booking? = null,
    val selectedMethod: PaymentMethod = PaymentMethod.EASYPAISA,
    val transactionId: String = "",
    val senderName: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSubmitted: Boolean = false
)


enum class PaymentMethod {
    EASYPAISA,
    JAZZCASH
}

class PaymentViewModel(
    private val bookingRepository: BookingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PaymentUiState())
    val uiState: StateFlow<PaymentUiState> = _uiState.asStateFlow()

    // Replace the canSubmit() function with this StateFlow
    val canSubmit: StateFlow<Boolean> = _uiState.map { s ->
        s.transactionId.isNotBlank() && s.senderName.isNotBlank()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun setBooking(booking: Booking) {
        _uiState.value = _uiState.value.copy(booking = booking)
    }


    fun onMethodSelected(method: PaymentMethod) {
        _uiState.value = _uiState.value.copy(selectedMethod = method)
    }

    fun onTransactionIdChange(v: String) {
        _uiState.value = _uiState.value.copy(transactionId = v, error = null)
    }

    fun onSenderNameChange(v: String) {
        _uiState.value = _uiState.value.copy(senderName = v, error = null)
    }

//    fun canSubmit(): Boolean {
//        val s = _uiState.value
//        return s.transactionId.isNotBlank() && s.senderName.isNotBlank()
//    }

    fun submitPayment() {
        val state = _uiState.value
        val bookingId = state.booking?.id ?: return
        if (state.transactionId.isBlank() || state.senderName.isBlank()) {
            _uiState.value = state.copy(error = "Please fill in all payment fields")
            return
        }
        _uiState.value = state.copy(isLoading = true, error = null)
        viewModelScope.launch {
            val result = bookingRepository.submitPayment(
                bookingId = bookingId,
                transactionId = state.transactionId.trim(),
                senderName = state.senderName.trim(),
                amount = state.booking.totalAmount,
                paymentMethod = state.selectedMethod.name

            )
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(isLoading = false, isSubmitted = true)
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
                }
            )
        }
    }
}