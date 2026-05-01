package com.echologics.thesmartonlineacademy.data.repository

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.echologics.thesmartonlineacademy.data.model.PlatformConfig
import com.echologics.thesmartonlineacademy.data.model.Withdrawal
import com.echologics.thesmartonlineacademy.data.model.WithdrawalStatus
import com.echologics.thesmartonlineacademy.utils.OneSignalHelper
import kotlinx.coroutines.tasks.await
import java.util.UUID

class WithdrawalRepository(private val context: Context? = null) {

    private val db = FirebaseFirestore.getInstance()
    private val withdrawalsCol = db.collection("withdrawals")
    private val configCol = db.collection("config")
    private val authRepository = AuthRepository()

    private val oneSignalAppId: String? by lazy {
        context?.getString(context.resources.getIdentifier("onesignal_app_id", "string", context.packageName))
    }
    private val oneSignalRestKey: String? by lazy {
        context?.getString(context.resources.getIdentifier("onesignal_rest_api_key", "string", context.packageName))
    }

    // ── Platform config ───────────────────────────────────────────────────────

    suspend fun getPlatformConfig(): Result<PlatformConfig> {
        return try {
            val doc = configCol.document("platform").get().await()
            val config = if (doc.exists()) {
                PlatformConfig(
                    platformFeePercent = doc.getLong("platformFeePercent")?.toInt() ?: 10,
                    minimumWithdrawal = doc.getLong("minimumWithdrawal")?.toInt() ?: 500
                )
            } else {
                PlatformConfig() // defaults
            }
            Result.success(config)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun savePlatformConfig(config: PlatformConfig): Result<Unit> {
        return try {
            configCol.document("platform").set(
                mapOf(
                    "platformFeePercent" to config.platformFeePercent,
                    "minimumWithdrawal" to config.minimumWithdrawal
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Teacher withdrawals ───────────────────────────────────────────────────

    suspend fun requestWithdrawal(
        withdrawal: Withdrawal
    ): Result<Unit> {
        return try {
            val id = UUID.randomUUID().toString()

            val data = withdrawal.copy(
                id = id,
                status = WithdrawalStatus.PENDING,
                requestedAt = System.currentTimeMillis()
            )

            withdrawalsCol.document(id).set(data).await()

            val (currency,amount) = OneSignalHelper.withdrawalRequestedPayload(
                currency = withdrawal.currency,
                amount = withdrawal.amount
            )

            OneSignalHelper.sendToPlayer(
                restApiKey = oneSignalRestKey ?: "",
                appId = oneSignalAppId ?: "",
                playerId = withdrawal.teacherId,
                title = "Withdrawal request submitted",
                body = "Your withdrawal of $currency $amount is under review"
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    suspend fun getWithdrawalsForTeacher(teacherId: String): Result<List<Withdrawal>> {
        return try {
            val snapshot = withdrawalsCol
                .whereEqualTo("teacherId", teacherId)
                .orderBy("requestedAt", Query.Direction.DESCENDING)
                .get().await()
            Result.success(snapshot.documents.mapNotNull { it.toObject(Withdrawal::class.java) })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllWithdrawals(): Result<List<Withdrawal>> {
        return try {
            val snapshot = withdrawalsCol
                .orderBy("requestedAt", Query.Direction.DESCENDING)
                .get().await()
            Result.success(snapshot.documents.mapNotNull { it.toObject(Withdrawal::class.java) })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Admin marks withdrawal as paid → notifies teacher ────────────────────

    suspend fun markWithdrawalPaid(withdrawalId: String, teacherId: String, amount: Int, currency: String): Result<Unit> {
        return try {
            withdrawalsCol.document(withdrawalId).update(
                mapOf(
                    "status" to WithdrawalStatus.PAID.name,
                    "processedAt" to System.currentTimeMillis()
                )
            ).await()

            // Push notification to teacher
            if (oneSignalAppId != null && oneSignalRestKey != null) {
                val teacherPlayerId = authRepository.getPlayerIdForUser(teacherId)
                OneSignalHelper.sendToPlayer(
                    restApiKey = oneSignalRestKey!!,
                    appId = oneSignalAppId!!,
                    playerId = teacherPlayerId,
                    title = "Withdrawal paid!",
                    body = "Your withdrawal of $currency $amount has been sent to your account.",
                    data = mapOf("type" to "withdrawal_paid", "withdrawalId" to withdrawalId)
                )
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun rejectWithdrawal(withdrawalId: String, note: String): Result<Unit> {
        return try {
            withdrawalsCol.document(withdrawalId).update(
                mapOf(
                    "status" to WithdrawalStatus.REJECTED.name,
                    "adminNote" to note
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Teacher earnings summary ──────────────────────────────────────────────
    // Reads confirmed bookings and withdrawal history to compute metrics

    suspend fun getEarningsSummary(teacherId: String): Result<EarningsSummary> {
        return try {
            // Get all confirmed+completed bookings for this teacher
            val bookingsSnapshot = db.collection("bookings")
                .whereEqualTo("teacherId", teacherId)
                .get().await()

            val bookings = bookingsSnapshot.documents.mapNotNull {
                it.toObject(com.echologics.thesmartonlineacademy.data.model.Booking::class.java)
            }.filter {
                it.status == com.echologics.thesmartonlineacademy.data.model.BookingStatus.CONFIRMED ||
                        it.status == com.echologics.thesmartonlineacademy.data.model.BookingStatus.COMPLETED
            }

            // Total earned = sum of teacherEarning across all confirmed/completed bookings
            val currency = bookings.firstOrNull()?.currency ?: "PKR"
            val totalEarned = bookings.sumOf { booking ->
                booking.teacherNetEarning
            }

            // Total withdrawn = sum of PAID withdrawals
            val withdrawals = getWithdrawalsForTeacher(teacherId).getOrDefault(emptyList())
            val totalWithdrawn = withdrawals
                .filter { it.status == WithdrawalStatus.PAID }
                .sumOf { it.amount }

            val pendingWithdrawals = withdrawals
                .filter { it.status == WithdrawalStatus.PENDING }
                .sumOf { it.amount }

            val availableBalance = totalEarned - totalWithdrawn - pendingWithdrawals

            Result.success(
                EarningsSummary(
                    currency = currency,
                    totalEarned = totalEarned,
                    totalWithdrawn = totalWithdrawn,
                    pendingWithdrawals = pendingWithdrawals,
                    availableBalance = availableBalance.coerceAtLeast(0),
                    totalSessions = bookings.size,
                    completedSessions = bookings.count {
                        it.status == com.echologics.thesmartonlineacademy.data.model.BookingStatus.COMPLETED
                    }
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

data class EarningsSummary(
    val currency: String = "PKR",
    val totalEarned: Int = 0,
    val totalWithdrawn: Int = 0,
    val pendingWithdrawals: Int = 0,
    val availableBalance: Int = 0,
    val totalSessions: Int = 0,
    val completedSessions: Int = 0
) {
    fun display(amount: Int) = "$currency $amount"
}