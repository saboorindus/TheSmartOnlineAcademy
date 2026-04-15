package com.echologics.thesmartonlineacademy.ui.student.booking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.echologics.thesmartonlineacademy.data.model.Booking
import com.echologics.thesmartonlineacademy.data.model.TeacherProfile
import com.echologics.thesmartonlineacademy.data.repository.BookingRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class BookingUiState(
    val teacher: TeacherProfile? = null,
    val selectedDay: String = "",
    val selectedTimeSlot: String = "",
    val selectedSubject: String = "",
    val scheduledDate: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val createdBooking: Booking? = null,

    val durationMinutes: Int = 60,
) {
    val totalAmount: Int get() {
        val rate = teacher?.ratePerTenMin ?: 0
        return rate * (durationMinutes / 10)
    }

    val totalDisplay: String get() {
        val cur = teacher?.currency ?: "PKR"
        return "$cur $totalAmount"
    }

    val durationDisplay: String get() {
        val h = durationMinutes / 60
        val m = durationMinutes % 60
        return when {
            h == 0 -> "${m}m"
            m == 0 -> "${h}h"
            else -> "${h}h ${m}m"
        }
    }

    // Slider position (0..17 maps to 10..180 min in steps of 10)
    val sliderPosition: Float get() = ((durationMinutes / 10) - 1).toFloat()
}

class BookingViewModel(
    private val bookingRepository: BookingRepository = BookingRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(BookingUiState())
    val uiState: StateFlow<BookingUiState> = _uiState.asStateFlow()

    fun setTeacher(teacher: TeacherProfile) {
        _uiState.value = _uiState.value.copy(
            teacher = teacher,
            selectedSubject = teacher.subjects.firstOrNull() ?: "",
            durationMinutes = 60
        )
    }

    fun onDaySelected(day: String) {
        _uiState.value = _uiState.value.copy(selectedDay = day, selectedTimeSlot = "")
    }

    fun onTimeSlotSelected(slot: String) {
        _uiState.value = _uiState.value.copy(selectedTimeSlot = slot)
    }

    // Slider value 0..17 → duration 10..180 minutes in steps of 10
    fun onSliderChange(position: Float) {
        val minutes = (position.toInt() + 1) * 10
        _uiState.value = _uiState.value.copy(durationMinutes = minutes.coerceIn(10, 180))
    }

//    fun onSessionLengthSelected(length: String) {
//        _uiState.value = _uiState.value.copy(selectedSessionLength = length)
//    }

    fun onSubjectSelected(subject: String) {
        _uiState.value = _uiState.value.copy(selectedSubject = subject)
    }

    fun onScheduledDateChange(date: String) {
        _uiState.value = _uiState.value.copy(scheduledDate = date)
    }

    fun canProceed(): Boolean {
        val s = _uiState.value
        return s.selectedDay.isNotBlank() &&
                s.selectedTimeSlot.isNotBlank() &&
                s.selectedSubject.isNotBlank() &&
                s.durationMinutes >= 10
    }

    fun confirmBooking() {
        val state = _uiState.value
        val teacher = state.teacher ?: return
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: run {
            _uiState.value = state.copy(error = "Not authenticated")
            return
        }

        _uiState.value = state.copy(isLoading = true, error = null)

        viewModelScope.launch {
            // Get student name from Firestore
            val studentName = getStudentName(uid)

            val booking = Booking(
                studentId = uid,
                studentName = studentName,
                teacherId = teacher.uid,
                teacherName = teacher.fullName,
                subject = state.selectedSubject,
                durationMinutes = state.durationMinutes,
                scheduledDate = state.scheduledDate,
                scheduledTime = state.selectedTimeSlot,
                slotDay = state.selectedDay,
                slotTime = state.selectedTimeSlot,
                ratePerTenMin = teacher.ratePerTenMin,
                currency = teacher.currency,
                totalAmount = state.totalDisplay
            )
            val result = bookingRepository.createBooking(booking)
            result.fold(
                onSuccess = { created ->
                    _uiState.value = _uiState.value.copy(isLoading = false, createdBooking = created)
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
                }
            )
        }
    }

    private suspend fun getStudentName(uid: String): String {
        return try {
            val doc = FirebaseFirestore.getInstance()
                .collection("students").document(uid).get().await()
            doc.getString("fullName") ?: "Student"
        } catch (e: Exception) { "Student" }
    }

//    private fun calculateTotal(hourlyRate: String, sessionLength: String): String {
//        val rate = hourlyRate.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 0
//        val multiplier = when {
//            sessionLength.contains("30") -> 0.5
//            sessionLength.contains("90") -> 1.5
//            else -> 1.0
//        }
//        val total = (rate * multiplier).toInt()
//        val currency = if (hourlyRate.contains("PKR", ignoreCase = true)) "PKR" else ""
//        return "$currency $total".trim()
//    }
}