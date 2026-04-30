package com.echologics.thesmartonlineacademy.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.echologics.thesmartonlineacademy.data.model.AdminStats
import com.echologics.thesmartonlineacademy.data.model.Booking
import com.echologics.thesmartonlineacademy.data.model.BookingStatus
import com.echologics.thesmartonlineacademy.data.model.PlatformConfig
import com.echologics.thesmartonlineacademy.data.model.TeacherProfile
import com.echologics.thesmartonlineacademy.data.model.User
import com.echologics.thesmartonlineacademy.data.model.Withdrawal
import com.echologics.thesmartonlineacademy.data.repository.AdminRepository
import com.echologics.thesmartonlineacademy.data.repository.BookingRepository
import com.echologics.thesmartonlineacademy.data.repository.WithdrawalRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.getOrDefault


enum class PaymentSettingUIState {
    SETTINGS,
    WITHDRAWALS,
    EARNINGS
}

// ── Shared admin state ────────────────────────────────────────────────────────

data class AdminUiState(
    val stats: AdminStats = AdminStats(),
    val pendingTeachers: List<TeacherProfile> = emptyList(),
    val allTeachers: List<TeacherProfile> = emptyList(),
    val allBookings: List<Booking> = emptyList(),
    val allUsers: List<User> = emptyList(),
    val selectedTab: AdminTab = AdminTab.DASHBOARD,
    val isSaving: Boolean = false,
    val isLoading: Boolean = true,
    val actionLoading: String? = null,
    val error: String? = null,
    val successMessage: String? = null,
    val isSaved: Boolean = false,
    val showRejectDialog: Boolean = false,
    val rejectTargetUid: String = "",
    val rejectReason: String = "",
    val bookingFilter: BookingStatus? = null,
    val confirmingBookingId: String? = null,
    val withdrawal: List<Withdrawal> = emptyList(),
    val platformConfig: PlatformConfig = PlatformConfig(),
    val paymentSettingUIState: PaymentSettingUIState = PaymentSettingUIState.SETTINGS
)

enum class AdminTab(val label: String) {
    DASHBOARD("Dashboard"),
    APPROVALS("Approvals"),
    BOOKINGS("Bookings"),
    USERS("Users"),
    QR("QR Setup"),
    PAYMENT("Payment Settings")
}

class AdminDashboardViewModel(
    val repo: AdminRepository,
    private val bookingRepository: BookingRepository,
    private val withdrawalRepository: WithdrawalRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminUiState())
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()

    init {
        loadAll()
    }

    fun onTabSelected(tab: AdminTab) {
        _uiState.value = _uiState.value.copy(selectedTab = tab, error = null, successMessage = null)
    }

    fun refresh() = loadAll()

    // ── Loaders ───────────────────────────────────────────────────────────────

    private fun loadAll() {
        _uiState.value = _uiState.value.copy(isLoading = true)
        viewModelScope.launch {
            val stats = repo.getStats().getOrDefault(AdminStats())
            val pending = repo.getPendingTeachers().getOrDefault(emptyList())
            val allTeachers = repo.getAllTeachers().getOrDefault(emptyList())
            val allBookings = repo.getAllBookings().getOrDefault(emptyList())
            val allUsers = repo.getAllUsers().getOrDefault(emptyList())
            val platformConfig = withdrawalRepository.getPlatformConfig().getOrDefault(PlatformConfig())
            val withdrawals = withdrawalRepository.getAllWithdrawals().getOrDefault(emptyList())
            _uiState.value = _uiState.value.copy(
                stats = stats,
                pendingTeachers = pending,
                allTeachers = allTeachers,
                allBookings = allBookings,
                allUsers = allUsers,
                platformConfig = platformConfig,
                isLoading = false,
                withdrawal = withdrawals
            )
        }
    }

    // ── Approval actions ──────────────────────────────────────────────────────

    fun approveTeacher(uid: String) {
        _uiState.value = _uiState.value.copy(actionLoading = uid)
        viewModelScope.launch {
            val result = repo.approveTeacher(uid)
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        actionLoading = null,
                        successMessage = "Teacher approved successfully"
                    )
                    loadAll()
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        actionLoading = null,
                        error = e.message
                    )
                }
            )
        }
    }


    fun setPlatformConfig(platformConfig: PlatformConfig) {

        _uiState.value = _uiState.value.copy(
            platformConfig = platformConfig,
            isSaving = true,
            error = null
        )

        viewModelScope.launch {
            val result = withdrawalRepository.savePlatformConfig(platformConfig)
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        successMessage = "Platform config updated",
                        isSaving = false,
                        isSaved = true
                    )
                    loadAll()
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        error = e.message,
                        isSaving = false,
                        isSaved = false
                    )
                }
            )
        }
    }

    fun confirmPayment(booking: Booking) {
        _uiState.value = _uiState.value.copy(confirmingBookingId = booking.id)
        viewModelScope.launch {
            val result = bookingRepository.confirmPayment(booking.id,booking.teacherName,booking.subject)
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(confirmingBookingId = null)
                    loadAll()
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

    fun showRejectDialog(uid: String) {
        _uiState.value = _uiState.value.copy(
            showRejectDialog = true,
            rejectTargetUid = uid,
            rejectReason = ""
        )
    }

    fun onRejectReasonChange(reason: String) {
        _uiState.value = _uiState.value.copy(rejectReason = reason)
    }

    fun dismissRejectDialog() {
        _uiState.value = _uiState.value.copy(
            showRejectDialog = false,
            rejectTargetUid = "",
            rejectReason = ""
        )
    }

    fun confirmReject() {
        val uid = _uiState.value.rejectTargetUid
        val reason = _uiState.value.rejectReason.ifBlank { "Profile does not meet our requirements" }
        _uiState.value = _uiState.value.copy(showRejectDialog = false, actionLoading = uid)
        viewModelScope.launch {
            val result = repo.rejectTeacher(uid, reason)
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        actionLoading = null,
                        successMessage = "Teacher rejected"
                    )
                    loadAll()
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(actionLoading = null, error = e.message)
                }
            )
        }
    }

    // ── Booking actions ───────────────────────────────────────────────────────

    fun setBookingFilter(status: BookingStatus?) {
        _uiState.value = _uiState.value.copy(bookingFilter = status)
    }

    fun filteredBookings(): List<Booking> {
        val filter = _uiState.value.bookingFilter ?: return _uiState.value.allBookings
        return _uiState.value.allBookings.filter { it.status == filter }
    }

    fun cancelBooking(bookingId: String) {
        _uiState.value = _uiState.value.copy(actionLoading = bookingId)
        viewModelScope.launch {
            repo.cancelBookingAdmin(bookingId)
            _uiState.value = _uiState.value.copy(
                actionLoading = null,
                successMessage = "Booking cancelled"
            )
            loadAll()
        }
    }

    // ── User actions ──────────────────────────────────────────────────────────

//    fun disableUser(uid: String) {
//        _uiState.value = _uiState.value.copy(actionLoading = uid)
//        viewModelScope.launch {
//            repo.disableUser(uid)
//            _uiState.value = _uiState.value.copy(
//                actionLoading = null,
//                successMessage = "User disabled"
//            )
//            loadAll()
//        }
//    }

    fun toggleUserStatus(user: User) {
        _uiState.value = _uiState.value.copy(actionLoading = user.uid)

        viewModelScope.launch {
            val newState = user.disabled != true

            repo.setUserDisabled(user.uid, newState)

            _uiState.value = _uiState.value.copy(
                actionLoading = null,
                successMessage = if (newState) "User disabled" else "User enabled"
            )

            loadAll()
        }
    }



    fun clearMessage() {
        _uiState.value = _uiState.value.copy(successMessage = null, error = null)
    }
}