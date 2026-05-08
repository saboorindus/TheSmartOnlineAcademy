package com.echologics.thesmartonlineacademy.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.echologics.thesmartonlineacademy.MainActivity
import com.echologics.thesmartonlineacademy.R
import com.echologics.thesmartonlineacademy.data.model.AgoraState
import io.agora.rtc2.ChannelMediaOptions
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.RtcEngineConfig
import io.agora.rtc2.video.VideoEncoderConfiguration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val AGORA_APP_ID = "0bcd1a1d17b44aeeba473215676773fa"

class SessionForegroundService : Service() {

    // ── Binder ────────────────────────────────────────────────────────────────

    inner class SessionBinder : Binder() {
        fun getService(): SessionForegroundService = this@SessionForegroundService
    }

    private val binder = SessionBinder()
    override fun onBind(intent: Intent?): IBinder = binder

    // ── Agora ─────────────────────────────────────────────────────────────────

    private var rtcEngine: RtcEngine? = null

    private val _agoraState = MutableStateFlow(AgoraState())
    val agoraState: StateFlow<AgoraState> = _agoraState.asStateFlow()

    var onUserJoinedCallback: (() -> Unit)? = null
    var onUserOfflineCallback: (() -> Unit)? = null

    private val eventHandler = object : IRtcEngineEventHandler() {

        override fun onJoinChannelSuccess(channel: String, uid: Int, elapsed: Int) {
            Log.d(TAG, "onJoinChannelSuccess uid=$uid")
            _agoraState.value = _agoraState.value.copy(
                localUid = uid,
                isSessionActive = true,
                connectionState = "Connected"
            )
        }

        override fun onUserJoined(uid: Int, elapsed: Int) {
            Log.d(TAG, "onUserJoined uid=$uid")
            _agoraState.value = _agoraState.value.copy(
                remoteUid = uid,
                isRemoteVideoVisible = true
            )
            onUserJoinedCallback?.invoke()
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            Log.d(TAG, "onUserOffline uid=$uid reason=$reason")
            onUserOfflineCallback?.invoke()
        }

        override fun onConnectionStateChanged(state: Int, reason: Int) {
            val label = when (state) {
                Constants.CONNECTION_STATE_CONNECTING -> "Connecting..."
                Constants.CONNECTION_STATE_CONNECTED -> "Connected"
                Constants.CONNECTION_STATE_RECONNECTING -> "Reconnecting..."
                Constants.CONNECTION_STATE_FAILED -> "Connection failed"
                else -> return
            }
            _agoraState.value = _agoraState.value.copy(connectionState = label)
        }

        override fun onError(err: Int) {
            Log.e(TAG, "Agora error $err")
            _agoraState.value = _agoraState.value.copy(error = "Agora error: $err")
        }
    }

    fun initAgora(context: Context) {
        if (rtcEngine != null) {
            Log.d(TAG, "initAgora — already initialised, skipping")
            return
        }
        try {
            val config = RtcEngineConfig().apply {
                mContext = context.applicationContext
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
            Log.d(TAG, "initAgora — success")
        } catch (e: Exception) {
            Log.e(TAG, "initAgora failed", e)
            _agoraState.value = _agoraState.value.copy(error = "Failed to init Agora: ${e.message}")
        }
    }

    fun joinChannel(channelName: String, uid: Int, token: String) {
        val options = ChannelMediaOptions().apply {
            channelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION
            clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
            publishCameraTrack = true
            publishMicrophoneTrack = true
            autoSubscribeAudio = true
            autoSubscribeVideo = true
        }
        rtcEngine?.joinChannel(token, channelName, uid, options)
        Log.d(TAG, "joinChannel called — channel=$channelName uid=$uid")
    }

    fun muteLocalAudio(muted: Boolean) = rtcEngine?.muteLocalAudioStream(muted)
    fun muteLocalVideo(muted: Boolean) = rtcEngine?.muteLocalVideoStream(muted)
    fun switchCamera() = rtcEngine?.switchCamera()
    fun setSpeakerphone(on: Boolean) = rtcEngine?.setEnableSpeakerphone(on)

    fun setupLocalVideo(view: android.view.SurfaceView) {
        val canvas = io.agora.rtc2.video.VideoCanvas(
            view, io.agora.rtc2.video.VideoCanvas.RENDER_MODE_HIDDEN, 0
        )
        rtcEngine?.setupLocalVideo(canvas)
        rtcEngine?.startPreview()
    }

    fun setupRemoteVideo(view: android.view.SurfaceView, remoteUid: Int) {
        val canvas = io.agora.rtc2.video.VideoCanvas(
            view, io.agora.rtc2.video.VideoCanvas.RENDER_MODE_HIDDEN, remoteUid
        )
        rtcEngine?.setupRemoteVideo(canvas)
    }

    fun refreshVideoKey() {
        _agoraState.value = _agoraState.value.copy(
            remoteVideoKey = _agoraState.value.remoteVideoKey + 1,
            isRemoteVideoVisible = _agoraState.value.remoteUid != null
        )
    }

    fun markRemoteOffline() {
        _agoraState.value = _agoraState.value.copy(
            remoteUid = null,
            isRemoteVideoVisible = false,
            connectionState = "Other participant left"
        )
    }

    fun leaveChannel() {
        rtcEngine?.leaveChannel()
        rtcEngine?.let { RtcEngine.destroy() }
        rtcEngine = null
        _agoraState.value = AgoraState()
        Log.d(TAG, "leaveChannel — engine destroyed")
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")
        ensureNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand")
        showNotification()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        leaveChannel()
        Log.d(TAG, "onDestroy")
    }

    // ── Notification ──────────────────────────────────────────────────────────

    @android.annotation.SuppressLint("InlinedApi")
    private fun showNotification() {
        val returnIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, returnIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Session in progress")
            .setContentText("Tap to return to your session")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setSilent(true)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA or
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                manager.createNotificationChannel(
                    NotificationChannel(
                        CHANNEL_ID,
                        "Active Session",
                        NotificationManager.IMPORTANCE_LOW
                    ).apply { setShowBadge(false) }
                )
            }
        }
    }

    companion object {
        const val TAG = "SessionService"
        const val CHANNEL_ID = "session_active_channel"
        const val NOTIFICATION_ID = 102
    }
}