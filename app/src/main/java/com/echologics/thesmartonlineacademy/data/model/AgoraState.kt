package com.echologics.thesmartonlineacademy.data.model

data class AgoraState(
    val localUid: Int = 0,
    val remoteUid: Int? = null,
    val isSessionActive: Boolean = false,
    val isRemoteVideoVisible: Boolean = false,
    val connectionState: String = "Connecting...",
    val remoteVideoKey: Int = 0,
    val error: String? = null
)