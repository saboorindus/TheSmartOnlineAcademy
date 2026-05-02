package com.echologics.thesmartonlineacademy.data.repository

import android.content.Context
import com.echologics.thesmartonlineacademy.R
import com.echologics.thesmartonlineacademy.utils.SupabaseClient
import com.echologics.thesmartonlineacademy.data.model.AdminQrConfig
import com.echologics.thesmartonlineacademy.data.model.AdminStats
import com.echologics.thesmartonlineacademy.data.model.ApprovalStatus
import com.echologics.thesmartonlineacademy.data.model.Booking
import com.echologics.thesmartonlineacademy.data.model.BookingStatus
import com.echologics.thesmartonlineacademy.data.model.TeacherProfile
import com.echologics.thesmartonlineacademy.data.model.User
import com.echologics.thesmartonlineacademy.utils.OneSignalHelper
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import io.github.jan.supabase.storage.storage


class AdminRepository(private val context: Context) {

    private val db = FirebaseFirestore.getInstance()
    private val teachersCol = db.collection("teachers")
    private val studentsCol = db.collection("students")
    private val bookingsCol = db.collection("bookings")
    private val usersCol = db.collection("users")
    private val configCol = db.collection("config")
    private val authRepository = AuthRepository()


    private val oneSignalAppId: String by lazy {
        context.getString(R.string.onesignal_app_id)
    }

    private val oneSignalRestKey: String by lazy {
        context.getString(R.string.onesignal_rest_api_key)
    }

    // ── Stats ─────────────────────────────────────────────────────────────────

    suspend fun getStats(): Result<AdminStats> {
        return try {
            val teachers = teachersCol.get().await()
            val students = studentsCol.get().await()
            val bookings = bookingsCol.get().await()

            val allBookings = bookings.documents.mapNotNull { it.toObject(Booking::class.java) }
            val allTeachers = teachers.documents.mapNotNull { it.toObject(TeacherProfile::class.java) }

            val pendingApprovals = allTeachers.count { it.approvalStatus == ApprovalStatus.PENDING }
            val confirmedBookings = allBookings.count { it.status == BookingStatus.CONFIRMED }
            val pendingPayments = allBookings.count { it.status == BookingStatus.PAYMENT_SUBMITTED }
            val completedSessions = allBookings.count { it.status == BookingStatus.COMPLETED }

            // Calculate total revenue from completed bookings
            val totalRevenue = allBookings
                .filter { it.status == BookingStatus.COMPLETED }
                .sumOf { booking ->
                    booking.totalAmount
                }

            val stats = AdminStats(
                totalTeachers = teachers.size(),
                pendingApprovals = pendingApprovals,
                totalStudents = students.size(),
                totalBookings = bookings.size(),
                confirmedBookings = confirmedBookings,
                pendingPayments = pendingPayments,
                completedSessions = completedSessions,
                totalRevenue = totalRevenue
            )
            Result.success(stats)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Teacher approval ──────────────────────────────────────────────────────

    suspend fun getPendingTeachers(): Result<List<TeacherProfile>> {
        return try {
            val snapshot = teachersCol
                .whereEqualTo("approvalStatus", ApprovalStatus.PENDING.name)
                .get().await()
            val teachers = snapshot.documents.mapNotNull { it.toObject(TeacherProfile::class.java) }
            Result.success(teachers)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllTeachers(): Result<List<TeacherProfile>> {
        return try {
            val snapshot = teachersCol.get().await()
            val teachers = snapshot.documents.mapNotNull { it.toObject(TeacherProfile::class.java) }
            Result.success(teachers)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun approveTeacher(uid: String): Result<Unit> {
        return try {
            val batch = db.batch()
            batch.update(
                teachersCol.document(uid),
                mapOf(
                    "approvalStatus" to ApprovalStatus.APPROVED.name,
                    "isVerified" to true
                )
            )
            // Write approval notification to user's notifications subcollection
            val notifRef = usersCol.document(uid)
                .collection("notifications")
                .document()
            batch.set(
                notifRef,
                mapOf(
                    "type" to "APPROVAL",
                    "status" to "APPROVED",
                    "message" to "Congratulations! Your teacher profile has been approved. You can now receive bookings.",
                    "timestamp" to System.currentTimeMillis(),
                    "isRead" to false
                )
            )
            batch.commit().await()

            val teacherPlayerId = authRepository.getPlayerIdForUser(uid)
            val (title, body) = OneSignalHelper.teacherApprovedPayload()
            OneSignalHelper.sendToPlayer(
                restApiKey = oneSignalRestKey,
                appId = oneSignalAppId,
                playerId = teacherPlayerId,
                title = title,
                body = body,
                data = mapOf("type" to "profile_approved")
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun rejectTeacher(uid: String, reason: String): Result<Unit> {
        return try {
            val batch = db.batch()
            batch.update(
                teachersCol.document(uid),
                mapOf(
                    "approvalStatus" to ApprovalStatus.REJECTED.name,
                    "rejectionReason" to reason
                )
            )
            val notifRef = usersCol.document(uid)
                .collection("notifications")
                .document()
            batch.set(
                notifRef,
                mapOf(
                    "type" to "APPROVAL",
                    "status" to "REJECTED",
                    "message" to "Your profile was not approved. Reason: $reason. Please update your profile and resubmit.",
                    "timestamp" to System.currentTimeMillis(),
                    "isRead" to false
                )
            )
            batch.commit().await()

            val teacherPlayerId = authRepository.getPlayerIdForUser(uid)
            val (title, body) = OneSignalHelper.teacherRejectedPayload(reason)
            OneSignalHelper.sendToPlayer(
                restApiKey = oneSignalRestKey,
                appId = oneSignalAppId,
                playerId = teacherPlayerId,
                title = title,
                body = body,
                data = mapOf("type" to "profile_rejected")
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Bookings ──────────────────────────────────────────────────────────────

    suspend fun getAllBookings(): Result<List<Booking>> {
        return try {
            val snapshot = bookingsCol
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get().await()
            val bookings = snapshot.documents.mapNotNull { it.toObject(Booking::class.java) }
            Result.success(bookings)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun cancelBookingAdmin(bookingId: String): Result<Unit> {
        return try {
            val snap = bookingsCol.document(bookingId).get().await()
            val booking = snap.toObject(Booking::class.java)

            bookingsCol.document(bookingId).update("status", BookingStatus.CANCELLED.name).await()

            booking?.let { b ->
                // ── Notify teacher ────────────────────────────────────────────
                val teacherPlayerId = runCatching {
                    authRepository.getPlayerIdForUser(b.teacherId)
                }.getOrDefault("")

                val (teacherTitle, teacherBody) = OneSignalHelper.bookingCancelledForTeacherPayload(
                    studentName = b.studentName,
                    subject = b.subject
                )
                OneSignalHelper.sendToPlayer(
                    restApiKey = oneSignalRestKey,
                    appId = oneSignalAppId,
                    playerId = teacherPlayerId,
                    title = teacherTitle,
                    body = teacherBody,
                    data = mapOf("type" to "booking_cancelled", "bookingId" to bookingId)
                )

                // ── Notify student ────────────────────────────────────────────
                val studentPlayerId = runCatching {
                    authRepository.getPlayerIdForUser(b.studentId)
                }.getOrDefault("")

                val (studentTitle, studentBody) = OneSignalHelper.bookingCancelledForStudentPayload(
                    teacherName = b.teacherName,
                    subject = b.subject
                )
                OneSignalHelper.sendToPlayer(
                    restApiKey = oneSignalRestKey,
                    appId = oneSignalAppId,
                    playerId = studentPlayerId,
                    title = studentTitle,
                    body = studentBody,
                    data = mapOf("type" to "booking_cancelled", "bookingId" to bookingId)
                )
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Users ─────────────────────────────────────────────────────────────────

    suspend fun getAllUsers(): Result<List<User>> {
        return try {
            val snapshot = usersCol.get().await()
            val users = snapshot.documents.mapNotNull { it.toObject(User::class.java) }
            Result.success(users)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

//    suspend fun disableUser(uid: String): Result<Unit> {
//        return try {
//            usersCol.document(uid).update("isDisabled", true).await()
//            Result.success(Unit)
//        } catch (e: Exception) {
//            Result.failure(e)
//        }
//    }

    suspend fun setUserDisabled(uid: String, disabled: Boolean): Result<Unit> {
        return try {
            usersCol.document(uid)
                .update("disabled", disabled)
                .await()

            // 🔥 IMPORTANT: also update teacher collection
            teachersCol.document(uid)
                .update("disabled", disabled)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }



    // ── QR Config ─────────────────────────────────────────────────────────────

    suspend fun getQrConfig(): Result<AdminQrConfig> {
        return try {
            val doc = configCol.document("payment_qr").get().await()
            val config = doc.toObject(AdminQrConfig::class.java) ?: AdminQrConfig()
            Result.success(config)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveQrConfig(config: AdminQrConfig): Result<Unit> {
        return try {
            configCol.document("payment_qr")
                .set(config.copy(updatedAt = System.currentTimeMillis()))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Teacher approval status (for teacher-side banner) ─────────────────────

    suspend fun getApprovalNotification(uid: String): Map<String, Any>? {
        return try {
            val snapshot = usersCol.document(uid)
                .collection("notifications")
                .whereEqualTo("type", "APPROVAL")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(1)
                .get().await()
            snapshot.documents.firstOrNull()?.data
        } catch (_: Exception) {
            null
        }
    }


    suspend fun uploadQrToSupabase(
        bytes: ByteArray,
        fileName: String = "qr/admin_qr.png"
    ): Result<String> {
        return try {
            val bucket = SupabaseClient.supabase.storage.from("qr-images")

            bucket.upload(fileName, bytes) {
                upsert = true
            }


            val publicUrl = bucket.publicUrl(fileName)

            Result.success(publicUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

}