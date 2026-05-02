package com.echologics.thesmartonlineacademy.data.model

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color


data class DrawPath(
    val id: String = "",
    val points: List<Offset> = emptyList(),
    val color: Color = Color.Black,
    val strokeWidth: Float = 4f,
    val tool: DrawTool = DrawTool.PEN,
    val authorId: String = ""
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

fun DrawPath.toMap(): Map<String, Any> = mapOf(
    "id" to id,
    "points" to points.map { mapOf("x" to it.x.toDouble(), "y" to it.y.toDouble()) },
    "color" to color.toHex(),
    "strokeWidth" to strokeWidth.toDouble(),
    "tool" to tool.name,
    "authorId" to authorId,
    "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
)

fun Color.toHex(): String {
    val r = (red * 255).toInt()
    val g = (green * 255).toInt()
    val b = (blue * 255).toInt()
    return "#%02X%02X%02X".format(r, g, b)
}

@Suppress("UNCHECKED_CAST")
fun Map<String, Any>.toDrawPath(): DrawPath {
    val pts = (this["points"] as List<Map<String, Double>>)
        .map { Offset(it["x"]!!.toFloat(), it["y"]!!.toFloat()) }
    val color = Color(android.graphics.Color.parseColor(this["color"] as String))
    return DrawPath(
        id = this["id"] as String,
        points = pts,
        color = color,
        strokeWidth = (this["strokeWidth"] as Double).toFloat(),
        tool = DrawTool.valueOf(this["tool"] as String),
        authorId = this["authorId"] as? String ?: ""
    )
}