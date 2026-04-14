package com.echologics.thesmartonlineacademy.ui.student.bookinghistory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.echologics.thesmartonlineacademy.data.model.Booking
import com.echologics.thesmartonlineacademy.data.model.BookingStatus
import com.echologics.thesmartonlineacademy.data.repository.BookingRepository
import com.echologics.thesmartonlineacademy.data.repository.ReviewRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class HistoryTab(val label: String) {
    UPCOMING("Upcoming"),
    PAST("Past"),
    ALL("All")
}

data class BookingHistoryUiState(
    val bookings: List<Booking> = emptyList(),
    val reviewedBookingIds: Set<String> = emptySet(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val selectedTab: HistoryTab = HistoryTab.UPCOMING
)

class BookingHistoryViewModel(
    private val bookingRepository: BookingRepository = BookingRepository(),
    private val reviewRepository: ReviewRepository = ReviewRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(BookingHistoryUiState())
    val uiState: StateFlow<BookingHistoryUiState> = _uiState.asStateFlow()

    private val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    init {
        load()
    }

    fun onTabSelected(tab: HistoryTab) {
        _uiState.value = _uiState.value.copy(selectedTab = tab)
    }

    fun refresh() = load()

    fun filteredBookings(): List<Booking> {
        val all = _uiState.value.bookings
        return when (_uiState.value.selectedTab) {
            HistoryTab.UPCOMING -> all.filter {
                it.status == BookingStatus.CONFIRMED || it.status == BookingStatus.PAYMENT_SUBMITTED
            }
            HistoryTab.PAST -> all.filter {
                it.status == BookingStatus.COMPLETED || it.status == BookingStatus.CANCELLED
            }
            HistoryTab.ALL -> all
        }
    }

    fun canReview(booking: Booking): Boolean {
        return booking.status == BookingStatus.COMPLETED &&
                !_uiState.value.reviewedBookingIds.contains(booking.id)
    }

    private fun load() {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            val bookingsResult = bookingRepository.getBookingsForStudent(currentUid)
            bookingsResult.fold(
                onSuccess = { bookings ->
                    // Check which completed bookings have reviews
                    val completedIds = bookings
                        .filter { it.status == BookingStatus.COMPLETED }
                        .map { it.id }
                    val reviewedIds = completedIds.filter { bookingId ->
                        reviewRepository.hasReviewed(bookingId)
                    }.toSet()

                    _uiState.value = _uiState.value.copy(
                        bookings = bookings,
                        reviewedBookingIds = reviewedIds,
                        isLoading = false
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
            )
        }
    }
}