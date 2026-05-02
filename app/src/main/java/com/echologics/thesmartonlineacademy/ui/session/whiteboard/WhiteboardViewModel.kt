package com.echologics.thesmartonlineacademy.ui.session.whiteboard

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.echologics.thesmartonlineacademy.data.model.DrawPath
import com.echologics.thesmartonlineacademy.data.model.DrawTool
import com.echologics.thesmartonlineacademy.data.repository.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class WhiteboardUiState(
    val paths: List<DrawPath> = emptyList(),
    val currentPath: DrawPath? = null,
    val selectedTool: DrawTool = DrawTool.PEN,
    val selectedColor: Color = Color.Black,
    val strokeWidth: Float = 4f,
    val undoStack: List<DrawPath> = emptyList()
)

val toolColors = listOf(
    Color.Black,
    Color(0xFF534AB7),  // Purple
    Color(0xFF1D9E75),  // Teal
    Color(0xFFE24B4A),  // Red
    Color(0xFFBA7517),  // Amber
    Color(0xFF378ADD),  // Blue
    Color.White
)

val strokeWidths = listOf(2f, 4f, 8f, 16f)

class WhiteboardViewModel(
    private val sessionRepository: SessionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WhiteboardUiState())
    val uiState: StateFlow<WhiteboardUiState> = _uiState.asStateFlow()

    private var bookingId: String? = null
    private var myUid: String = ""
    private var strokeListener: com.google.firebase.firestore.ListenerRegistration? = null

    // Called from SessionViewModel once session is ready
    fun initSync(bookingId: String, uid: String) {
        this.bookingId = bookingId
        this.myUid = uid
        strokeListener = sessionRepository.listenToStrokes(bookingId) { remotePaths ->
            _uiState.value = _uiState.value.copy(paths = remotePaths)
        }
    }

    fun onDragStart(offset: Offset) {
        val tool = _uiState.value.selectedTool
        val color = if (tool == DrawTool.ERASER) Color.White else _uiState.value.selectedColor
        val width = if (tool == DrawTool.ERASER) 36f else _uiState.value.strokeWidth
        val alpha = if (tool == DrawTool.HIGHLIGHTER) 0.4f else 1f

        val newPath = DrawPath(
            id = UUID.randomUUID().toString(),
            points = listOf(offset),
            color = color.copy(alpha = alpha),
            strokeWidth = width,
            tool = tool,
            authorId = myUid
        )
        _uiState.value = _uiState.value.copy(currentPath = newPath)
    }

    fun onDrag(offset: Offset) {
        val current = _uiState.value.currentPath ?: return
        _uiState.value = _uiState.value.copy(
            currentPath = current.copy(points = current.points + offset)
        )
    }

    fun onDragEnd() {
        val current = _uiState.value.currentPath ?: return
        _uiState.value = _uiState.value.copy(currentPath = null)
        if (current.points.size > 1) {
            // Don't add to local paths list — the Firestore listener will push it back
            // This prevents double-rendering
            bookingId?.let { sessionRepository.sendStroke(it, current) }
            _uiState.value = _uiState.value.copy(undoStack = emptyList())
        }
    }

    fun undo() {
        val paths = _uiState.value.paths
        if (paths.isEmpty()) return
        val last = paths.last()
        _uiState.value = _uiState.value.copy(
            paths = paths.dropLast(1),
            undoStack = _uiState.value.undoStack + last
        )
    }

    fun redo() {
        val undo = _uiState.value.undoStack
        if (undo.isEmpty()) return
        val next = undo.last()
        _uiState.value = _uiState.value.copy(
            paths = _uiState.value.paths + next,
            undoStack = undo.dropLast(1)
        )
    }

    fun clearBoard() {
        _uiState.value = _uiState.value.copy(
            paths = emptyList(),
            currentPath = null,
            undoStack = emptyList()
        )
        bookingId?.let {
            viewModelScope.launch { sessionRepository.clearStrokes(it) }
        }
    }

    fun selectTool(tool: DrawTool) {
        _uiState.value = _uiState.value.copy(selectedTool = tool)
    }

    fun selectColor(color: Color) {
        _uiState.value = _uiState.value.copy(
            selectedColor = color,
            selectedTool = DrawTool.PEN
        )
    }

    fun selectStrokeWidth(width: Float) {
        _uiState.value = _uiState.value.copy(strokeWidth = width)
    }

    override fun onCleared() {
        super.onCleared()
        strokeListener?.remove()
    }
}