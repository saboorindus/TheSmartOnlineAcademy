package com.echologics.thesmartonlineacademy.data.repository


import android.content.Context
import com.echologics.thesmartonlineacademy.data.model.Booking
import com.echologics.thesmartonlineacademy.data.model.BookingStatus
import com.echologics.thesmartonlineacademy.utils.OneSignalHelper
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.util.UUID

class BookingRepository(private val context: Context) {

    private val db = FirebaseFirestore.getInstance()
    private val bookingsCol = db.collection("bookings")
    private val authRepository = AuthRepository()

    private val oneSignalAppId: String by lazy {
        context.getString(context.resources.getIdentifier("onesignal_app_id", "string", context.packageName))
    }

    private val oneSignalRestKey: String by lazy {
        context.getString(context.resources.getIdentifier("onesignal_rest_api_key", "string", context.packageName))
    }

    suspend fun isSlotAvailable(
        teacherId: String,
        slotDay: String,
        slotTime: String,
        scheduledDate: String
    ): Boolean {
        return try {
            val snapshot = bookingsCol
                .whereEqualTo("teacherId", teacherId)
                .whereEqualTo("slotDay", slotDay)
                .whereEqualTo("slotTime", slotTime)
                .whereEqualTo("scheduledDate", scheduledDate)
                .whereIn("status", listOf(
                    BookingStatus.PENDING_PAYMENT.name,
                    BookingStatus.PAYMENT_SUBMITTED.name,
                    BookingStatus.CONFIRMED.name
                ))
                .get().await()
            snapshot.isEmpty  // true = slot is free
        } catch (e: Exception) {
            false // fail-safe: treat as unavailable on error
        }
    }

    suspend fun createBooking(booking: Booking): Result<Booking> {
        return try {
            // Check slot before creating
            val available = isSlotAvailable(
                teacherId = booking.teacherId,
                slotDay = booking.slotDay,
                slotTime = booking.slotTime,
                scheduledDate = booking.scheduledDate
            )
            if (!available) {
                return Result.failure(Exception("This time slot has already been booked. Please choose another."))
            }

            val id = UUID.randomUUID().toString()
            val agoraChannel = "session_$id"
            val newBooking = booking.copy(
                id = id,
                agoraChannelName = agoraChannel,
                status = BookingStatus.PENDING_PAYMENT
            )
            bookingsCol.document(id).set(newBooking).await()

            // Push to teacher instantly
            val teacherPlayerId = authRepository.getPlayerIdForUser(booking.teacherId)
            val (title, body) = OneSignalHelper.bookingRequestPayload(
                studentName = booking.studentName,
                subject = booking.subject
            )
            OneSignalHelper.sendToPlayer(
                restApiKey = oneSignalRestKey,
                appId = oneSignalAppId,
                playerId = teacherPlayerId,
                title = title,
                body = body,
                data = mapOf("type" to "booking_request", "bookingId" to id)
            )

            Result.success(newBooking)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Student submits payment proof
    suspend fun submitPayment(
        bookingId: String,
        transactionId: String,
        senderName: String,
        amount: String,
        paymentMethod: String
    ): Result<Unit> {
        return try {
            bookingsCol.document(bookingId).update(
                mapOf(
                    "status" to BookingStatus.PAYMENT_SUBMITTED.name,
                    "paymentTransactionId" to transactionId,
                    "paymentSenderName" to senderName,
                    "paymentSubmittedAt" to System.currentTimeMillis(),
                    "paymentMethod" to paymentMethod
                )
            ).await()

            // Get teacher ID from the booking to look up their player ID
            val bookingDoc = bookingsCol.document(bookingId).get().await()
            val teacherId = bookingDoc.getString("teacherId") ?: ""

            val teacherPlayerId = authRepository.getPlayerIdForUser(teacherId)
            val (title, body) = OneSignalHelper.paymentSubmittedPayload(
                studentName = senderName,
                amount = amount
            )
            OneSignalHelper.sendToPlayer(
                restApiKey = oneSignalRestKey,
                appId = oneSignalAppId,
                playerId = teacherPlayerId,
                title = title,
                body = body,
                data = mapOf("type" to "payment_submitted", "bookingId" to bookingId)
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Teacher confirms payment received
    suspend fun confirmPayment(
        bookingId: String,
        teacherName: String,
        subject: String
    ): Result<Unit> {
        return try {
            bookingsCol.document(bookingId).update(
                mapOf(
                    "status" to BookingStatus.CONFIRMED.name,
                    "paymentConfirmedAt" to System.currentTimeMillis()
                )
            ).await()

            // Get student ID from the booking
            val bookingDoc = bookingsCol.document(bookingId).get().await()
            val studentId = bookingDoc.getString("studentId") ?: ""

            val studentPlayerId = authRepository.getPlayerIdForUser(studentId)
            val (title, body) = OneSignalHelper.paymentConfirmedPayload(
                teacherName = teacherName,
                subject = subject
            )
            OneSignalHelper.sendToPlayer(
                restApiKey = oneSignalRestKey,
                appId = oneSignalAppId,
                playerId = studentPlayerId,
                title = title,
                body = body,
                data = mapOf("type" to "payment_confirmed", "bookingId" to bookingId)
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun cancelBooking(bookingId: String): Result<Unit> {
        return try {
            bookingsCol.document(bookingId)
                .update("status", BookingStatus.CANCELLED.name).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getBookingsForStudent(studentId: String): Result<List<Booking>> {
        return try {
            val snapshot = bookingsCol
                .whereEqualTo("studentId", studentId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get().await()
            val bookings = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Booking::class.java)
            }
            Result.success(bookings)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getBookingsForTeacher(teacherId: String): Result<List<Booking>> {
        return try {
            val snapshot = bookingsCol
                .whereEqualTo("teacherId", teacherId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get().await()
            val bookings = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Booking::class.java)
            }
            Result.success(bookings)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getBookingById(bookingId: String): Result<Booking> {
        return try {
            val doc = bookingsCol.document(bookingId).get().await()
            val booking = doc.toObject(Booking::class.java)
                ?: return Result.failure(Exception("Booking not found"))
            Result.success(booking)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}