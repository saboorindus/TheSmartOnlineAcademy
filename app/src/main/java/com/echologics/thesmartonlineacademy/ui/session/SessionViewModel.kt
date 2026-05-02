package com.echologics.thesmartonlineacademy.ui.session

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.echologics.thesmartonlineacademy.data.model.Booking
import com.echologics.thesmartonlineacademy.data.model.ChatMessage
import com.echologics.thesmartonlineacademy.data.model.SessionRole
import com.echologics.thesmartonlineacademy.data.repository.SessionRepository
import com.echologics.thesmartonlineacademy.services.ScreenCaptureService
import com.google.firebase.auth.FirebaseAuth
import io.agora.rtc2.ChannelMediaOptions
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.RtcEngineConfig
import io.agora.rtc2.ScreenCaptureParameters
import io.agora.rtc2.video.VideoCanvas
import io.agora.rtc2.video.VideoEncoderConfiguration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private const val AGORA_APP_ID = "0bcd1a1d17b44aeeba473215676773fa"
private const val SUPABASE_FUNCTION_URL =
    "https://vilzjwakvylaihhwitwi.supabase.co/functions/v1/generate-agora-token"
private const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InZpbHpqd2FrdnlsYWloaHdpdHdpIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzYzNDc5MjMsImV4cCI6MjA5MTkyMzkyM30.UmsdUX-f7zAHo5z431eDXAOpWU7fS4A4nsuZYXagrx4" // ← paste your anon key here

data class SessionUiState(
    val booking: Booking? = null,
    val role: SessionRole = SessionRole.STUDENT,
    val localUid: Int = 0,
    val remoteUid: Int? = null,
    val isMuted: Boolean = false,
    val isCameraOff: Boolean = false,
    val isSpeakerOn: Boolean = true,
    val isScreenSharing: Boolean = false,
    val isWhiteboardVisible: Boolean = false,
    val isChatVisible: Boolean = false,
    val chatMessages: List<ChatMessage> = emptyList(),
    val chatInput: String = "",
    val isSessionActive: Boolean = false,
    val isEnded: Boolean = false,
    val error: String? = null,
    val isRemoteVideoVisible: Boolean = false,
    val connectionState: String = "Connecting...",
    val raisedHands: List<String> = emptyList(),
    val hasRaisedHand: Boolean = false,
    val isInPipMode: Boolean = false
)

class SessionViewModel(
    private val sessionRepository: SessionRepository = SessionRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(SessionUiState())
    val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()

    private var rtcEngine: RtcEngine? = null
    private var chatListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var handsListener: com.google.firebase.firestore.ListenerRegistration? = null  // ← add

    private val currentUid get() = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    private val eventHandler = object : IRtcEngineEventHandler() {

        override fun onJoinChannelSuccess(channel: String, uid: Int, elapsed: Int) {
            _uiState.value = _uiState.value.copy(
                localUid = uid,
                isSessionActive = true,
                connectionState = "Connected"
            )
        }

        override fun onUserJoined(uid: Int, elapsed: Int) {
            _uiState.value = _uiState.value.copy(
                remoteUid = uid,
                isRemoteVideoVisible = true
            )
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            _uiState.value = _uiState.value.copy(
                remoteUid = null,
                isRemoteVideoVisible = false,
                connectionState = "Other participant left"
            )
        }

        override fun onConnectionStateChanged(state: Int, reason: Int) {
            val label = when (state) {
                Constants.CONNECTION_STATE_CONNECTING -> "Connecting..."
                Constants.CONNECTION_STATE_CONNECTED -> "Connected"
                Constants.CONNECTION_STATE_RECONNECTING -> "Reconnecting..."
                Constants.CONNECTION_STATE_FAILED -> "Connection failed"
                else -> ""
            }
            if (label.isNotEmpty()) {
                _uiState.value = _uiState.value.copy(connectionState = label)
            }
        }

        override fun onError(err: Int) {
            _uiState.value = _uiState.value.copy(error = "Agora error code: $err")
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
            } catch (e: Exception) {
                null
            }
        }

    // ── Session init ──────────────────────────────────────────────────────────

    fun initSession(context: Context, booking: Booking, role: SessionRole) {
        _uiState.value = _uiState.value.copy(booking = booking, role = role)
        initAgoraEngine(context)
        viewModelScope.launch {
            val uid = currentUid.hashCode() and 0x7FFFFFFF // convert uid string to positive int
            val token = fetchAgoraToken(booking.agoraChannelName, uid)
            if (token == null) {
                _uiState.value = _uiState.value.copy(error = "Failed to fetch token. Check your connection.")
                return@launch
            }
            joinChannel(booking.agoraChannelName, uid, token)
            listenToChat(booking.id)
            listenToRaisedHands(booking.id)
        }
    }

    private fun initAgoraEngine(context: Context) {
        try {
            val config = RtcEngineConfig().apply {
                mContext = context
                mAppId = AGORA_APP_ID
                mEventHandler = eventHandler
            }
            rtcEngine = RtcEngine.create(config).apply {
                enableVideo()
                setVideoEncoderConfiguration(
                    VideoEncoderConfiguration(
                        VideoEncoderConfiguration.VD_640x360,
                        VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_15,
                        VideoEncoderConfiguration.STANDARD_BITRATE,
                        VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_ADAPTIVE
                    )
                )
                setDefaultAudioRoutetoSpeakerphone(true)
            }
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(error = "Failed to init Agora: ${e.message}")
        }
    }

    private fun joinChannel(channelName: String, uid: Int, token: String) {
        val options = ChannelMediaOptions().apply {
            channelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION
            clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
            publishCameraTrack = true
            publishMicrophoneTrack = true
            autoSubscribeAudio = true
            autoSubscribeVideo = true
        }
        rtcEngine?.joinChannel(token, channelName, uid, options)
    }

    // ── Video controls ────────────────────────────────────────────────────────

    fun setupLocalVideo(view: android.view.SurfaceView) {
        val canvas = VideoCanvas(view, VideoCanvas.RENDER_MODE_HIDDEN, 0)
        rtcEngine?.setupLocalVideo(canvas)
        rtcEngine?.startPreview()
    }

    fun setupRemoteVideo(view: android.view.SurfaceView, remoteUid: Int) {
        val canvas = VideoCanvas(view, VideoCanvas.RENDER_MODE_HIDDEN, remoteUid)
        rtcEngine?.setupRemoteVideo(canvas)
    }

    fun toggleMute() {
        val muted = !_uiState.value.isMuted
        rtcEngine?.muteLocalAudioStream(muted)
        _uiState.value = _uiState.value.copy(isMuted = muted)
    }

    fun toggleCamera() {
        val off = !_uiState.value.isCameraOff
        rtcEngine?.muteLocalVideoStream(off)
        _uiState.value = _uiState.value.copy(isCameraOff = off)
    }

    fun switchCamera() {
        rtcEngine?.switchCamera()
    }

    fun toggleSpeaker() {
        val on = !_uiState.value.isSpeakerOn
        rtcEngine?.setEnableSpeakerphone(on)
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
        viewModelScope.launch {
            sessionRepository.sendChatMessage(bookingId, message)
        }
    }

    private fun listenToChat(bookingId: String) {
        chatListener = sessionRepository.listenToChat(bookingId) { messages ->
            _uiState.value = _uiState.value.copy(chatMessages = messages)
        }
    }

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

    fun onPipModeChanged(inPip: Boolean) {
        _uiState.value = _uiState.value.copy(isInPipMode = inPip)
    }


    fun startScreenShare(resultCode: Int, data: android.content.Intent, context: Context) {
        // startForegroundService requires API 26+, use startService on API 24-25
        val serviceIntent = Intent(context, ScreenCaptureService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }

        val parameters = ScreenCaptureParameters().apply {
            captureVideo = true
            captureAudio = false
        }
        rtcEngine?.startScreenCapture(parameters)

        val options = ChannelMediaOptions().apply {
            publishScreenCaptureVideo = true
            publishCameraTrack = false
            publishMicrophoneTrack = true
            autoSubscribeAudio = true
            autoSubscribeVideo = true
        }
        rtcEngine?.updateChannelMediaOptions(options)

        _uiState.value = _uiState.value.copy(isScreenSharing = true, isCameraOff = true)
    }

    fun stopScreenShare(context: Context) {
        rtcEngine?.stopScreenCapture()

        // Switch back to camera
        val options = ChannelMediaOptions().apply {
            publishScreenCaptureVideo = false
            publishCameraTrack = true
            publishMicrophoneTrack = true
            autoSubscribeAudio = true
            autoSubscribeVideo = true
        }
        rtcEngine?.updateChannelMediaOptions(options)

        // Stop the foreground service
        context.stopService(android.content.Intent(context, ScreenCaptureService::class.java))

        _uiState.value = _uiState.value.copy(isScreenSharing = false, isCameraOff = false)
    }

    // ── End session ───────────────────────────────────────────────────────────

    fun endSession() {
        val bookingId = _uiState.value.booking?.id ?: return
        rtcEngine?.leaveChannel()
        viewModelScope.launch {
            sessionRepository.markSessionCompleted(bookingId)
            _uiState.value = _uiState.value.copy(isEnded = true)
        }
    }

    override fun onCleared() {
        super.onCleared()
        chatListener?.remove()
        handsListener?.remove()
        rtcEngine?.leaveChannel()
        RtcEngine.destroy()
        rtcEngine = null
    }
}