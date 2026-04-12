package com.echologics.thesmartonlineacademy.data.model

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color

data class DrawPath(
    val id: String = "",
    val points: List<Offset> = emptyList(),
    val color: Color = Color.Black,
    val strokeWidth: Float = 4f,
    val tool: DrawTool = DrawTool.PEN
)

enum class DrawTool {
    PEN, HIGHLIGHTER, ERASER, LINE, RECTANGLE, TEXT
}

data class SessionParticipant(
    val uid: String = "",
    val name: String = "",
    val isHost: Boolean = false,
    val isMuted: Boolean = false,
    val isCameraOff: Boolean = false
)

data class ChatMessage(
    val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

enum class SessionRole { TEACHER, STUDENT }