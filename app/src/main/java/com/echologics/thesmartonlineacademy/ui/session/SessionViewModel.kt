package com.echologics.thesmartonlineacademy.ui.session

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.echologics.thesmartonlineacademy.data.model.Booking
import com.echologics.thesmartonlineacademy.data.model.ChatMessage
import com.echologics.thesmartonlineacademy.data.model.SessionRole
import com.echologics.thesmartonlineacademy.data.repository.SessionRepository
import com.echologics.thesmartonlineacademy.services.SessionForegroundService
import com.google.firebase.auth.FirebaseAuth
import io.agora.rtc2.ChannelMediaOptions
import io.agora.rtc2.Constants
import io.agora.rtc2.ScreenCaptureParameters
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

private const val SUPABASE_FUNCTION_URL =
    "https://vilzjwakvylaihhwitwi.supabase.co/functions/v1/generate-agora-token"
private const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InZpbHpqd2FrdnlsYWloaHdpdHdpIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzYzNDc5MjMsImV4cCI6MjA5MTkyMzkyM30.UmsdUX-f7zAHo5z431eDXAOpWU7fS4A4nsuZYXagrx4"

data class SessionUiState(
    val booking: Booking? = null,
    val role: SessionRole = SessionRole.STUDENT,
    val isMuted: Boolean = false,
    val isCameraOff: Boolean = false,
    val isSpeakerOn: Boolean = true,
    val isScreenSharing: Boolean = false,
    val isWhiteboardVisible: Boolean = false,
    val isChatVisible: Boolean = false,
    val chatMessages: List<ChatMessage> = emptyList(),
    val chatInput: String = "",
    val isEnded: Boolean = false,
    val raisedHands: List<String> = emptyList(),
    val hasRaisedHand: Boolean = false,
    val isInPipMode: Boolean = false,
    // Agora state — now comes from service
    val localUid: Int = 0,
    val remoteUid: Int? = null,
    val isSessionActive: Boolean = false,
    val isRemoteVideoVisible: Boolean = false,
    val connectionState: String = "Connecting...",
    val remoteVideoKey: Int = 0,
    val error: String? = null
)

class SessionViewModel(
    private val sessionRepository: SessionRepository = SessionRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(SessionUiState())
    val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()

    private var sessionService: SessionForegroundService? = null
    private var serviceConnected = false
    private var chatListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var handsListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var reconnectJob: Job? = null
    private var agoraStateJob: Job? = null

    private val currentUid get() = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val service = (binder as SessionForegroundService.SessionBinder).getService()
            sessionService = service
            serviceConnected = true

            // Set up callbacks
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

            // Observe Agora state from service and mirror to UI state
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
        }
    }

    // ── Session init ──────────────────────────────────────────────────────────

    fun initSession(context: Context, booking: Booking, role: SessionRole) {
        _uiState.value = _uiState.value.copy(booking = booking, role = role)

        context.getSharedPreferences("active_session", Context.MODE_PRIVATE).edit {
            putString("booking_id", booking.id)
            putString("role", role.name)
        }

        // Start and bind to service
        val intent = Intent(context, SessionForegroundService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)

        viewModelScope.launch {
            // Wait for service to connect
            while (!serviceConnected) delay(100)

            val service = sessionService ?: return@launch
            service.initAgora(context)

            val uid = currentUid.hashCode() and 0x7FFFFFFF
            val token = fetchAgoraToken(booking.agoraChannelName, uid)
            if (token == null) {
                _uiState.value = _uiState.value.copy(error = "Failed to fetch token.")
                return@launch
            }
            service.joinChannel(booking.agoraChannelName, uid, token)
            listenToChat(booking.id)
            listenToRaisedHands(booking.id)
        }
    }

    fun bindToExistingService(context: Context) {
        val intent = Intent(context, SessionForegroundService::class.java)
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    fun unbindService(context: Context) {
        if (serviceConnected) {
            context.unbindService(serviceConnection)
            serviceConnected = false
        }
    }

    // ── Token fetch ───────────────────────────────────────────────────────────

    private suspend fun fetchAgoraToken(channelName: String, uid: Int): String? =
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
            } catch (e: Exception) { null }
        }

    // ── Video controls ────────────────────────────────────────────────────────

    fun setupLocalVideo(view: android.view.SurfaceView) =
        sessionService?.setupLocalVideo(view)

    fun setupRemoteVideo(view: android.view.SurfaceView, remoteUid: Int) =
        sessionService?.setupRemoteVideo(view, remoteUid)

    fun onReturnFromBackground() {
        sessionService?.refreshVideoKey()
    }

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

    fun switchCamera() = sessionService?.switchCamera()

    fun toggleSpeaker() {
        val on = !_uiState.value.isSpeakerOn
        sessionService?.setSpeakerphone(on)
        _uiState.value = _uiState.value.copy(isSpeakerOn = on)
    }

    // ── Whiteboard + chat toggles ─────────────────────────────────────────────

    fun toggleWhiteboard() {
        _uiState.value = _uiState.value.copy(
            isWhiteboardVisible = !_uiState.value.isWhiteboardVisible,
            isChatVisible = false
        )
    }

    fun toggleChat() {
        _uiState.value = _uiState.value.copy(
            isChatVisible = !_uiState.value.isChatVisible,
            isWhiteboardVisible = false
        )
    }

    // ── Chat ──────────────────────────────────────────────────────────────────

    fun onChatInputChange(text: String) {
        _uiState.value = _uiState.value.copy(chatInput = text)
    }

    fun sendChatMessage() {
        val state = _uiState.value
        val text = state.chatInput.trim()
        if (text.isBlank()) return
        val bookingId = state.booking?.id ?: return
        val message = ChatMessage(
            senderId = currentUid,
            senderName = if (state.role == SessionRole.TEACHER)
                state.booking.teacherName else state.booking.studentName,
            text = text
        )
        _uiState.value = state.copy(chatInput = "")
        viewModelScope.launch { sessionRepository.sendChatMessage(bookingId, message) }
    }

    private fun listenToChat(bookingId: String) {
        chatListener = sessionRepository.listenToChat(bookingId) { messages ->
            _uiState.value = _uiState.value.copy(chatMessages = messages)
        }
    }

    // ── Raise hand ────────────────────────────────────────────────────────────

    fun raiseHand() {
        val bookingId = _uiState.value.booking?.id ?: return
        val raised = !_uiState.value.hasRaisedHand
        _uiState.value = _uiState.value.copy(hasRaisedHand = raised)
        viewModelScope.launch {
            if (raised) sessionRepository.raiseHand(bookingId, currentUid)
            else sessionRepository.lowerHand(bookingId, currentUid)
        }
    }

    private fun listenToRaisedHands(bookingId: String) {
        handsListener = sessionRepository.listenToRaisedHands(bookingId) { hands ->
            _uiState.value = _uiState.value.copy(raisedHands = hands)
        }
    }

    // ── PiP ───────────────────────────────────────────────────────────────────

    fun onPipModeChanged(inPip: Boolean) {
        _uiState.value = _uiState.value.copy(isInPipMode = inPip)
    }

    // ── Screen share ──────────────────────────────────────────────────────────

    fun startScreenShare(resultCode: Int, data: Intent, context: Context) {
        val projectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE)
                as android.media.projection.MediaProjectionManager
        val mediaProjection = projectionManager.getMediaProjection(resultCode, data)
        if (mediaProjection == null) {
            _uiState.value = _uiState.value.copy(error = "Screen capture not supported")
            return
        }
        sessionService?.setExternalMediaProjection(mediaProjection)
        sessionService?.startScreenCapture(ScreenCaptureParameters().apply {
            captureVideo = true
            captureAudio = false
        })
        sessionService?.updateChannelOptions(ChannelMediaOptions().apply {
            publishScreenCaptureVideo = true
            publishCameraTrack = false
            publishMicrophoneTrack = true
            autoSubscribeAudio = true
            autoSubscribeVideo = true
        })
        _uiState.value = _uiState.value.copy(
            isScreenSharing = true,
            isCameraOff = true,
            remoteVideoKey = _uiState.value.remoteVideoKey + 1
        )
    }

    fun stopScreenShare(context: Context) {
        sessionService?.stopScreenCapture()
        sessionService?.updateChannelOptions(ChannelMediaOptions().apply {
            publishScreenCaptureVideo = false
            publishCameraTrack = true
            publishMicrophoneTrack = true
            autoSubscribeAudio = true
            autoSubscribeVideo = true
        })
        _uiState.value = _uiState.value.copy(
            isScreenSharing = false,
            isCameraOff = false,
            remoteVideoKey = _uiState.value.remoteVideoKey + 1
        )
    }

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

    override fun onCleared() {
        super.onCleared()
        android.util.Log.d("SessionReturn", "SessionViewModel onCleared — service stays alive")
        reconnectJob?.cancel()
        agoraStateJob?.cancel()
        chatListener?.remove()
        handsListener?.remove()
        // Do NOT stop service or leave channel here — service keeps Agora alive
    }
}