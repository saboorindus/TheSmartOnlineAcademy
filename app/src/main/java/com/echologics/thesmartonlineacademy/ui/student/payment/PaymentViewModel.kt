package com.echologics.thesmartonlineacademy.ui.student.payment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.echologics.thesmartonlineacademy.data.model.Booking
import com.echologics.thesmartonlineacademy.data.repository.BookingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PaymentUiState(
    val booking: Booking? = null,
    val transactionId: String = "",
    val senderName: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSubmitted: Boolean = false
)

class PaymentViewModel(
    private val bookingRepository: BookingRepository = BookingRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(PaymentUiState())
    val uiState: StateFlow<PaymentUiState> = _uiState.asStateFlow()

    fun setBooking(booking: Booking) {
        _uiState.value = _uiState.value.copy(booking = booking)
    }

    fun onTransactionIdChange(v: String) {
        _uiState.value = _uiState.value.copy(transactionId = v, error = null)
    }

    fun onSenderNameChange(v: String) {
        _uiState.value = _uiState.value.copy(senderName = v, error = null)
    }

    fun canSubmit(): Boolean {
        val s = _uiState.value
        return s.transactionId.isNotBlank() && s.senderName.isNotBlank()
    }

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
                senderName = state.senderName.trim()
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