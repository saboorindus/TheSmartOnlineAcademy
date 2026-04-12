package com.echologics.thesmartonlineacademy.ui.teacher.bookings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.echologics.thesmartonlineacademy.data.model.Booking
import com.echologics.thesmartonlineacademy.data.model.BookingStatus
import com.echologics.thesmartonlineacademy.data.repository.BookingRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TeacherBookingsUiState(
    val bookings: List<Booking> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedTab: BookingTab = BookingTab.PENDING,
    val confirmingBookingId: String? = null
)

enum class BookingTab(val label: String) {
    PENDING("Pending"),
    CONFIRMED("Confirmed"),
    ALL("All")
}

class TeacherBookingsViewModel(
    private val bookingRepository: BookingRepository = BookingRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(TeacherBookingsUiState())
    val uiState: StateFlow<TeacherBookingsUiState> = _uiState.asStateFlow()

    init { loadBookings() }

    fun onTabSelected(tab: BookingTab) {
        _uiState.value = _uiState.value.copy(selectedTab = tab)
    }

    fun filteredBookings(): List<Booking> {
        val all = _uiState.value.bookings
        return when (_uiState.value.selectedTab) {
            BookingTab.PENDING -> all.filter {
                it.status == BookingStatus.PENDING_PAYMENT || it.status == BookingStatus.PAYMENT_SUBMITTED
            }
            BookingTab.CONFIRMED -> all.filter { it.status == BookingStatus.CONFIRMED }
            BookingTab.ALL -> all
        }
    }

    fun confirmPayment(bookingId: String) {
        _uiState.value = _uiState.value.copy(confirmingBookingId = bookingId)
        viewModelScope.launch {
            val result = bookingRepository.confirmPayment(bookingId)
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(confirmingBookingId = null)
                    loadBookings()
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        confirmingBookingId = null,
                        error = e.message
                    )
                }
            )
        }
    }

    fun cancelBooking(bookingId: String) {
        viewModelScope.launch {
            bookingRepository.cancelBooking(bookingId)
            loadBookings()
        }
    }

    fun refresh() = loadBookings()

    private fun loadBookings() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            val result = bookingRepository.getBookingsForTeacher(uid)
            result.fold(
                onSuccess = { bookings ->
                    _uiState.value = _uiState.value.copy(bookings = bookings, isLoading = false)
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
                }
            )
        }
    }
}