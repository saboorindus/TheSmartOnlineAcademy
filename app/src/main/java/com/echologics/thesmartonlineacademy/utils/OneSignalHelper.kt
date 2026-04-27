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

    // ── Send to a specific player ID ─────────────────────────────────────────

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
            val responseBody = if (responseCode in 200..299) conn.inputStream.bufferedReader().use { it.readText() } else null

            conn.disconnect()

            if (responseCode in 200..299) {
                Log.d(TAG, "Notification sent successfully to player $playerId")
                true
            } else {
                Log.w(TAG, "OneSignal error: $responseCode $responseMessage. Body: $errorBody")
                Log.v(TAG, "Full Error Info: $errorBody")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send notification: ${e.message}")
            false
        }
    }

    // ── Notification builders ─────────────────────────────────────────────────
    // Each function returns a Pair<title, body> ready to pass to sendToPlayer

    fun bookingRequestPayload(studentName: String, subject: String) =
        Pair("New booking request", "$studentName wants to book a $subject session with you.")

    fun paymentSubmittedPayload(studentName: String, amount: String) =
        Pair("Payment submitted", "$studentName has submitted payment of $amount. Please verify and confirm.")

    fun paymentConfirmedPayload(teacherName: String, subject: String) =
        Pair("Booking confirmed!", "Your $subject session with $teacherName is confirmed. Payment verified.")

    fun newMessagePayload(senderName: String, preview: String) =
        Pair(senderName, preview)

    fun teacherApprovedPayload() =
        Pair("Profile approved!", "Congratulations! Your teacher profile has been approved. You can now receive bookings.")

    fun teacherRejectedPayload(reason: String) =
        Pair("Profile not approved", "Your profile was not approved. Reason: $reason. Please update and resubmit.")

    fun sessionReminderPayload(otherName: String, minutesUntil: Int) =
        Pair("Session starting soon", "Your session with $otherName starts in $minutesUntil minutes.")
}