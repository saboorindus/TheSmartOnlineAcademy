package com.echologics.thesmartonlineacademy.ui.teacher.earnings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.echologics.thesmartonlineacademy.data.model.Booking
import com.echologics.thesmartonlineacademy.data.model.BookingStatus
import com.echologics.thesmartonlineacademy.data.repository.BookingRepository
import com.echologics.thesmartonlineacademy.data.repository.EarningsSummary
import com.echologics.thesmartonlineacademy.data.repository.WithdrawalRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TeacherEarningsUiState(
    val summary: EarningsSummary = EarningsSummary(),
    val recentBookings: List<Booking> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

class TeacherEarningsViewModel(
    private val withdrawalRepository: WithdrawalRepository,
    private val bookingRepository: BookingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TeacherEarningsUiState())
    val uiState: StateFlow<TeacherEarningsUiState> = _uiState.asStateFlow()

    private val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    init { load() }

    fun refresh() = load()

    private fun load() {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            val summaryResult = withdrawalRepository.getEarningsSummary(uid)
            val bookingsResult = bookingRepository.getBookingsForTeacher(uid)

            val summary = summaryResult.getOrDefault(EarningsSummary())
            val allBookings = bookingsResult.getOrDefault(emptyList())

            // Show only confirmed + completed bookings sorted newest first
            val relevant = allBookings
                .filter {
                    it.status == BookingStatus.CONFIRMED ||
                            it.status == BookingStatus.COMPLETED
                }
                .take(20)

            _uiState.value = _uiState.value.copy(
                summary = summary,
                recentBookings = relevant,
                isLoading = false,
                error = summaryResult.exceptionOrNull()?.message
            )
        }
    }
}