package com.echologics.thesmartonlineacademy.utils

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * OneSignalHelper
 *
 * Sends push notifications directly from the client to OneSignal REST API.
 * No server required. No Firebase Blaze required.
 *
 * Setup:
 * 1. Create a free account at onesignal.com
 * 2. Create a new App → select Android → follow their FCM setup guide
 * 3. Copy your App ID and REST API Key into strings.xml:
 *      <string name="onesignal_app_id">YOUR_APP_ID</string>
 *      <string name="onesignal_rest_api_key">YOUR_REST_API_KEY</string>
 * 4. Each user's OneSignal Player ID is saved to Firestore on first app open
 *    (done in MainActivity) and retrieved when sending notifications.
 */
object OneSignalHelper {

    private const val TAG = "OneSignalHelper"
    private const val ONESIGNAL_API = "https://onesignal.com/api/v1/notifications"

    // ── Send to a specific player ID ──────────────────────────────────────────

    suspend fun sendToPlayer(
        restApiKey: String,
        appId: String,
        playerId: String,
        title: String,
        body: String,
        data: Map<String, String> = emptyMap()
    ): Boolean = withContext(Dispatchers.IO) {
        if (playerId.isBlank()) {
            Log.w(TAG, "sendToPlayer: playerId is blank, skipping")
            return@withContext false
        }
        Log.d(TAG, "Attempting to send notification to player: $playerId, Title: $title")

        try {
            Log.v(TAG, "Step 1: Building JSON payload...")
            val payload = JSONObject().apply {
                put("app_id", appId)
                put("include_player_ids", JSONArray().put(playerId))
                put("headings", JSONObject().put("en", title))
                put("contents", JSONObject().put("en", body))
                put("priority", 10)
                if (data.isNotEmpty()) {
                    val dataObj = JSONObject()
                    data.forEach { (k, v) -> dataObj.put(k, v) }
                    put("data", dataObj)
                }
            }

            Log.d(TAG, "Payload: $payload")

            Log.v(TAG, "Step 2: Opening connection to OneSignal...")
            val url = URL(ONESIGNAL_API)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                setRequestProperty("Authorization", "Basic $restApiKey")
                doOutput = true
                connectTimeout = 10_000
                readTimeout = 10_000
            }

            Log.v(TAG, "Step 3: Writing payload to output stream...")
            OutputStreamWriter(conn.outputStream, "UTF-8").use { it.write(payload.toString()) }

            Log.v(TAG, "Step 4: Waiting for response...")
            val responseCode = conn.responseCode
            val responseMessage = conn.responseMessage

            Log.v(TAG, "Step 5: Response received - Code: $responseCode")
            val errorBody = if (responseCode !in 200..299) {
                conn.errorStream?.bufferedReader()?.use { it.readText() }
            } else null
            val responseBody = if (responseCode in 200..299) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else null

            conn.disconnect()

            if (responseCode in 200..299) {
                Log.d(TAG, "Notification sent successfully to player $playerId. Response: $responseBody")
                true
            } else {
                Log.w(TAG, "OneSignal error: $responseCode $responseMessage. Body: $errorBody")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send notification: ${e.message}")
            false
        }
    }

    // ── Notification payload builders ─────────────────────────────────────────
    // Each returns Pair<title, body> ready to pass into sendToPlayer

    // ── Booking ───────────────────────────────────────────────────────────────

    /** Teacher receives a new booking request from a student */
    fun bookingRequestPayload(studentName: String, subject: String) =
        Pair(
            "New booking request",
            "$studentName wants to book a $subject session with you."
        )

    /** Teacher receives notice that student has submitted payment */
    fun paymentSubmittedPayload(studentName: String, amount: Int) =
        Pair(
            "Payment submitted",
            "$studentName has submitted payment of $$amount. Our team will verify and confirm shortly."
        )

    /** Student receives notice that admin confirmed their payment → booking active */
    fun paymentConfirmedPayload(teacherName: String, subject: String) =
        Pair(
            "Booking confirmed!",
            "Your $subject session with $teacherName is confirmed. Payment verified."
        )

    /** Teacher receives notice that admin confirmed a student's payment for their session */
    fun bookingConfirmedForTeacherPayload(studentName: String, subject: String) =
        Pair(
            "Session confirmed!",
            "Payment for your $subject session with $studentName has been verified. The session is now active."
        )

    /** Teacher receives notice that admin cancelled their session */
    fun bookingCancelledForTeacherPayload(studentName: String, subject: String) =
        Pair(
            "Session cancelled",
            "Your $subject session with $studentName has been cancelled by admin."
        )

    /** Student receives notice that admin cancelled their booking */
    fun bookingCancelledForStudentPayload(teacherName: String, subject: String) =
        Pair(
            "Booking cancelled",
            "Your $subject booking with $teacherName has been cancelled by admin."
        )

    // ── Withdrawals ───────────────────────────────────────────────────────────

    /** Teacher receives confirmation that their withdrawal request was received */
    fun withdrawalRequestedPayload(currency: String, amount: Int) =
        Pair(
            "Withdrawal request submitted",
            "Your withdrawal request of $currency $amount is under review. We'll notify you once it's processed."
        )

    /** Admin receives notice that a teacher has submitted a withdrawal request */
    fun withdrawalRequestedAdminPayload(teacherName: String, currency: String, amount: Int, method: String) =
        Pair(
            "New withdrawal request",
            "$teacherName requested $currency $amount via $method — review needed."
        )

    /** Teacher receives notice that their withdrawal was paid */
    fun withdrawalPaidPayload(currency: String, amount: Int) =
        Pair(
            "Withdrawal paid 🎉",
            "Your $currency $amount withdrawal has been processed and sent to your account."
        )

    /** Teacher receives notice that their withdrawal was rejected */
    fun withdrawalRejectedPayload(currency: String, amount: Int, reason: String) =
        Pair(
            "Withdrawal rejected",
            "Your $currency $amount withdrawal request was not approved. Reason: $reason. Your balance has been restored."
        )

    // ── Messaging ─────────────────────────────────────────────────────────────

    /** Generic new message notification */
    fun newMessagePayload(senderName: String, preview: String) =
        Pair(senderName, preview)

    // ── Teacher profile ───────────────────────────────────────────────────────

    /** Teacher profile approved by admin */
    fun teacherApprovedPayload() =
        Pair(
            "Profile approved!",
            "Congratulations! Your teacher profile has been approved. You can now receive bookings."
        )

    /** Teacher profile rejected by admin */
    fun teacherRejectedPayload(reason: String) =
        Pair(
            "Profile not approved",
            "Your profile was not approved. Reason: $reason. Please update and resubmit."
        )

    // ── Session reminders ─────────────────────────────────────────────────────

    /** Reminder sent to either party before a session starts */
    fun sessionReminderPayload(otherName: String, minutesUntil: Int) =
        Pair(
            "Session starting soon",
            "Your session with $otherName starts in $minutesUntil minutes."
        )
}