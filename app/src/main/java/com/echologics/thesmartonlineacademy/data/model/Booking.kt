package com.echologics.thesmartonlineacademy.data.model

data class Booking(
    val id: String = "",
    val studentId: String = "",
    val studentName: String = "",
    val teacherId: String = "",
    val teacherName: String = "",
    val subject: String = "",
    val sessionLength: String = "",
    val scheduledDate: String = "",
    val scheduledTime: String = "",
    val slotDay: String = "",
    val slotTime: String = "",
    val hourlyRate: String = "",
    val totalAmount: String = "",
    val status: BookingStatus = BookingStatus.PENDING_PAYMENT,
    val paymentTransactionId: String = "",
    val paymentSenderName: String = "",
    val paymentSubmittedAt: Long = 0L,
    val paymentConfirmedAt: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val agoraChannelName: String = ""
)

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