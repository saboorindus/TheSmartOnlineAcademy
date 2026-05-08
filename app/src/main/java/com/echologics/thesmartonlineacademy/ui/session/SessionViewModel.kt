package com.echologics.thesmartonlineacademy.ui.session

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.echologics.thesmartonlineacademy.data.repository.SessionRepository
import com.echologics.thesmartonlineacademy.data.model.SessionRole
import com.echologics.thesmartonlineacademy.services.SessionForegroundService
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import androidx.core.content.edit
import com.echologics.thesmartonlineacademy.data.model.Booking

private const val SUPABASE_FUNCTION_URL =
    "https://vilzjwakvylaihhwitwi.supabase.co/functions/v1/generate-agora-token"
private const val SUPABASE_ANON_KEY =
    "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InZpbHpqd2FrdnlsYWloaHdpdHdpIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzYzNDc5MjMsImV4cCI6MjA5MTkyMzkyM30.UmsdUX-f7zAHo5z431eDXAOpWU7fS4A4nsuZYXagrx4"

data class SessionUiState(
    val booking: Booking? = null,
    val role: SessionRole = SessionRole.STUDENT,
    // controls
    val isMuted: Boolean = false,
    val isCameraOff: Boolean = false,
    val isSpeakerOn: Boolean = true,
    // pip
    val isInPipMode: Boolean = false,
    // mirrors from AgoraState
    val localUid: Int = 0,
    val remoteUid: Int? = null,
    val isSessionActive: Boolean = false,
    val isRemoteVideoVisible: Boolean = false,
    val connectionState: String = "Connecting...",
    val remoteVideoKey: Int = 0,
    val error: String? = null,
    val isEnded: Boolean = false
)

class SessionViewModel(
    private val sessionRepository: SessionRepository = SessionRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(SessionUiState())
    val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()

    private var sessionService: SessionForegroundService? = null
    private var serviceConnected = false
    private var agoraStateJob: Job? = null
    private var reconnectJob: Job? = null

    private val currentUid get() = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    // ── Service connection ────────────────────────────────────────────────────

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val service = (binder as SessionForegroundService.SessionBinder).getService()
            sessionService = service
            serviceConnected = true
            Log.d(TAG, "service connected")

            service.onUserOfflineCallback = {
                reconnectJob?.cancel()
                reconnectJob = viewModelScope.launch {
                    delay(10_000)
                    service.markRemoteOffline()
                }
            }
            service.onUserJoinedCallback = {
                reconnectJob?.cancel()
            }

            agoraStateJob = viewModelScope.launch {
                service.agoraState.collect { agora ->
                    _uiState.value = _uiState.value.copy(
                        localUid = agora.localUid,
                        remoteUid = agora.remoteUid,
                        isSessionActive = agora.isSessionActive,
                        isRemoteVideoVisible = agora.isRemoteVideoVisible,
                        connectionState = agora.connectionState,
                        remoteVideoKey = agora.remoteVideoKey,
                        error = agora.error
                    )
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            sessionService = null
            serviceConnected = false
            Log.d(TAG, "service disconnected")
        }
    }

    // ── Session init ──────────────────────────────────────────────────────────

    fun initSession(context: Context, booking: Booking, role: SessionRole) {
        _uiState.value = _uiState.value.copy(booking = booking, role = role)

        context.getSharedPreferences("active_session", Context.MODE_PRIVATE).edit {
            putString("booking_id", booking.id)
            putString("role", role.name)
        }

        val intent = Intent(context, SessionForegroundService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)

        viewModelScope.launch {
            // Wait for service to bind
            while (!serviceConnected) delay(50)

            val service = sessionService ?: return@launch
            service.initAgora(context)

            val uid = currentUid.hashCode() and 0x7FFFFFFF
            val token = fetchToken(booking.agoraChannelName, uid)
            if (token == null) {
                _uiState.value = _uiState.value.copy(error = "Failed to fetch token")
                return@launch
            }
            service.joinChannel(booking.agoraChannelName, uid, token)
        }
    }

    fun bindToExistingService(context: Context) {
        if (serviceConnected) return
        val intent = Intent(context, SessionForegroundService::class.java)
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    fun unbindService(context: Context) {
        if (serviceConnected) {
            agoraStateJob?.cancel()
            context.unbindService(serviceConnection)
            serviceConnected = false
        }
    }

    // ── Token ─────────────────────────────────────────────────────────────────

    private suspend fun fetchToken(channelName: String, uid: Int): String? =
        withContext(Dispatchers.IO) {
            try {
                val url = URL(SUPABASE_FUNCTION_URL)
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Authorization", "Bearer $SUPABASE_ANON_KEY")
                    setRequestProperty("apikey", SUPABASE_ANON_KEY)
                    doOutput = true
                    connectTimeout = 10_000
                    readTimeout = 10_000
                }
                val body = JSONObject().apply {
                    put("channelName", channelName)
                    put("uid", uid)
                }.toString()
                conn.outputStream.use { it.write(body.toByteArray()) }
                val response = conn.inputStream.bufferedReader().use { it.readText() }
                conn.disconnect()
                JSONObject(response).getString("token")
            } catch (e: Exception) {
                Log.e(TAG, "fetchToken failed", e)
                null
            }
        }

    // ── Controls ──────────────────────────────────────────────────────────────

    fun toggleMute() {
        val muted = !_uiState.value.isMuted
        sessionService?.muteLocalAudio(muted)
        _uiState.value = _uiState.value.copy(isMuted = muted)
    }

    fun toggleCamera() {
        val off = !_uiState.value.isCameraOff
        sessionService?.muteLocalVideo(off)
        _uiState.value = _uiState.value.copy(isCameraOff = off)
    }

    fun toggleSpeaker() {
        val on = !_uiState.value.isSpeakerOn
        sessionService?.setSpeakerphone(on)
        _uiState.value = _uiState.value.copy(isSpeakerOn = on)
    }

    fun switchCamera() = sessionService?.switchCamera()

    // ── PiP ───────────────────────────────────────────────────────────────────

    fun onPipModeChanged(inPip: Boolean) {
        _uiState.value = _uiState.value.copy(isInPipMode = inPip)
    }

    fun onReturnFromBackground() =
        sessionService?.refreshVideoKey()

    // ── Video ─────────────────────────────────────────────────────────────────

    fun setupLocalVideo(view: android.view.SurfaceView) =
        sessionService?.setupLocalVideo(view)

    fun setupRemoteVideo(view: android.view.SurfaceView, remoteUid: Int) =
        sessionService?.setupRemoteVideo(view, remoteUid)

    // ── End session ───────────────────────────────────────────────────────────

    fun endSession(context: Context) {
        val bookingId = _uiState.value.booking?.id ?: return
        sessionService?.leaveChannel()
        unbindService(context)
        context.stopService(Intent(context, SessionForegroundService::class.java))
        context.getSharedPreferences("active_session", Context.MODE_PRIVATE).edit { clear() }
        viewModelScope.launch {
            sessionRepository.markSessionCompleted(bookingId)
            _uiState.value = _uiState.value.copy(isEnded = true)
        }
    }

    fun stopSessionService(context: Context) {
        unbindService(context)
        context.stopService(Intent(context, SessionForegroundService::class.java))
    }

    // ── Cleanup ───────────────────────────────────────────────────────────────

    override fun onCleared() {
        super.onCleared()
        reconnectJob?.cancel()
        agoraStateJob?.cancel()
        Log.d(TAG, "onCleared — service stays alive")
    }

    companion object {
        const val TAG = "SessionViewModel"
    }
}