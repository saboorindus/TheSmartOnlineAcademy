package com.echologics.thesmartonlineacademy.data.model

data class Booking(
    val id: String = "",
    val studentId: String = "",
    val studentName: String = "",
    val teacherId: String = "",
    val teacherName: String = "",
    val subject: String = "",
    val scheduledDate: String = "",
    val scheduledTime: String = "",
    val slotDay: String = "",
    val slotTime: String = "",
    val ratePerTenMin: Int = 0,
    val currency: String = "PKR",
    val totalAmount: Int = 0,
    val platformFeePercent: Int = 0,
    val platformFee: Int = 0,       // e.g. "PKR 150"
    val teacherEarning: Int = 0,
    val status: BookingStatus = BookingStatus.PENDING_PAYMENT,
    val paymentTransactionId: String = "",
    val paymentSenderName: String = "",
    val paymentSubmittedAt: Long = 0L,
    val paymentConfirmedAt: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val agoraChannelName: String = "",

    val durationMinutes: Int = 60,
) {
    // Friendly display e.g. "1h 30m"
    fun durationDisplay(): String {
        val h = durationMinutes / 60
        val m = durationMinutes % 60
        return when {
            h == 0 -> "${m}m"
            m == 0 -> "${h}h"
            else -> "${h}h ${m}m"
        }
    }

    // Legacy accessor so existing UI that reads sessionLength still compiles
    val sessionLength: String get() = durationDisplay()

}
enum class BookingStatus {
    PENDING_PAYMENT,    // Student booked, hasn't paid yet
    PAYMENT_SUBMITTED,  // Student uploaded transaction ID, awaiting teacher confirm
    CONFIRMED,          // Teacher confirmed payment received
    CANCELLED,          // Cancelled by either side
    COMPLETED           // Session done
}

data class Review(
    val id: String = "",
    val bookingId: String = "",
    val teacherId: String = "",
    val studentId: String = "",
    val studentName: String = "",
    val rating: Float = 0f,
    val comment: String = "",
    val createdAt: Long = System.currentTimeMillis()
)