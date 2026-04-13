package com.echologics.thesmartonlineacademy.ui.session

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.echologics.thesmartonlineacademy.data.model.Booking
import com.echologics.thesmartonlineacademy.data.model.ChatMessage
import com.echologics.thesmartonlineacademy.data.model.SessionRole
import com.echologics.thesmartonlineacademy.data.repository.SessionRepository
import com.google.firebase.auth.FirebaseAuth
import io.agora.rtc2.ChannelMediaOptions
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.RtcEngineConfig
import io.agora.rtc2.video.VideoCanvas
import io.agora.rtc2.video.VideoEncoderConfiguration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

// Replace with your Agora App ID from console.agora.io
private const val AGORA_APP_ID = "2df6491956aa4d07bebbc9e430cddcfc"

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
    val connectionState: String = "Connecting..."
)

class SessionViewModel(
    private val sessionRepository: SessionRepository = SessionRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(SessionUiState())
    val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()

    private var rtcEngine: RtcEngine? = null
    private var chatListener: com.google.firebase.firestore.ListenerRegistration? = null
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
            _uiState.value = _uiState.value.copy(
                error = "Agora error code: $err"
            )
        }
    }

    fun initSession(context: Context, booking: Booking, role: SessionRole) {
        _uiState.value = _uiState.value.copy(booking = booking, role = role)
        initAgoraEngine(context)
        joinChannel(booking.agoraChannelName)
        listenToChat(booking.id)
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

    private fun joinChannel(channelName: String) {
        val options = ChannelMediaOptions().apply {
            channelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION
            clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
            publishCameraTrack = true
            publishMicrophoneTrack = true
            autoSubscribeAudio = true
            autoSubscribeVideo = true
        }
        // Token is null for testing. In production, fetch token from your backend.
        rtcEngine?.joinChannel(null, channelName, 0, options)
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
        rtcEngine?.leaveChannel()
        RtcEngine.destroy()
        rtcEngine = null
    }
}