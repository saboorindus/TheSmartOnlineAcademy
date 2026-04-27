package com.echologics.thesmartonlineacademy.ui.teacher.bookings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.echologics.thesmartonlineacademy.data.model.Booking
import com.echologics.thesmartonlineacademy.data.model.BookingStatus
import com.echologics.thesmartonlineacademy.data.repository.AdminRepository
import com.echologics.thesmartonlineacademy.data.repository.BookingRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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
    private val bookingRepository: BookingRepository,
    val adminRepository: AdminRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TeacherBookingsUiState())
    val uiState: StateFlow<TeacherBookingsUiState> = _uiState.asStateFlow()

    init { loadBookings() }

    fun onTabSelected(tab: BookingTab) {
        _uiState.value = _uiState.value.copy(selectedTab = tab)
    }

    val filteredBookings: StateFlow<List<Booking>> =
        uiState.map { state ->
            when (state.selectedTab) {
                BookingTab.PENDING -> state.bookings.filter {
                    it.status == BookingStatus.PENDING_PAYMENT ||
                            it.status == BookingStatus.PAYMENT_SUBMITTED
                }

                BookingTab.CONFIRMED -> state.bookings.filter {
                    it.status == BookingStatus.CONFIRMED
                }

                BookingTab.ALL -> state.bookings
            }
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )


    fun confirmPayment(booking: Booking) {
        _uiState.value = _uiState.value.copy(confirmingBookingId = booking.id)
        viewModelScope.launch {
            val result = bookingRepository.confirmPayment(booking.id, booking.teacherName, booking.subject)
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