package com.echologics.thesmartonlineacademy.ui.session.whiteboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.echologics.thesmartonlineacademy.data.model.DrawTool
import com.echologics.thesmartonlineacademy.data.model.SessionRole

@Composable
fun WhiteboardCanvas(
    viewModel: WhiteboardViewModel,
    role: SessionRole,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var showClearConfirm by remember { mutableStateOf(false) }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear board?") },
            text = { Text("This will erase all strokes for both participants.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearBoard()
                    showClearConfirm = false
                }) { Text("Clear", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text("Cancel") }
            }
        )
    }

    Column(modifier = modifier.fillMaxSize()) {

        // ── Toolbar — only shown to teacher ───────────────────────────────────
        if (role == SessionRole.TEACHER) {
            WhiteboardToolbar(
                selectedTool = uiState.selectedTool,
                selectedColor = uiState.selectedColor,
                selectedStroke = uiState.strokeWidth,
                canUndo = uiState.paths.isNotEmpty(),
                canRedo = uiState.undoStack.isNotEmpty(),
                onToolSelect = viewModel::selectTool,
                onColorSelect = viewModel::selectColor,
                onStrokeSelect = viewModel::selectStrokeWidth,
                onUndo = viewModel::undo,
                onRedo = viewModel::redo,
                onClearRequest = { showClearConfirm = true }
            )
        }

        // ── Drawing canvas ────────────────────────────────────────────────────
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .then(
                    // Only teacher can draw; eraser only affects strokes inside canvas
                    if (role == SessionRole.TEACHER) {
                        Modifier.pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset -> viewModel.onDragStart(offset) },
                                onDrag = { change, _ -> viewModel.onDrag(change.position) },
                                onDragEnd = { viewModel.onDragEnd() }
                            )
                        }
                    } else Modifier
                )
        ) {
            // Committed paths
            uiState.paths.forEach { drawPath ->
                if (drawPath.points.size >= 2) {
                    val path = Path().apply {
                        moveTo(drawPath.points.first().x, drawPath.points.first().y)
                        drawPath.points.drop(1).forEach { lineTo(it.x, it.y) }
                    }
                    drawPath(
                        path = path,
                        color = drawPath.color,
                        style = Stroke(
                            width = drawPath.strokeWidth,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                } else if (drawPath.points.size == 1) {
                    drawCircle(
                        color = drawPath.color,
                        radius = drawPath.strokeWidth / 2,
                        center = drawPath.points.first()
                    )
                }
            }

            // Current in-progress stroke
            uiState.currentPath?.let { drawPath ->
                if (drawPath.points.size >= 2) {
                    val path = Path().apply {
                        moveTo(drawPath.points.first().x, drawPath.points.first().y)
                        drawPath.points.drop(1).forEach { lineTo(it.x, it.y) }
                    }
                    drawPath(
                        path = path,
                        color = drawPath.color,
                        style = Stroke(
                            width = drawPath.strokeWidth,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun WhiteboardToolbar(
    selectedTool: DrawTool,
    selectedColor: Color,
    selectedStroke: Float,
    canUndo: Boolean,
    canRedo: Boolean,
    onToolSelect: (DrawTool) -> Unit,
    onColorSelect: (Color) -> Unit,
    onStrokeSelect: (Float) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onClearRequest: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {

            // Row 1: tools + stroke sizes + undo/redo/clear
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Tool buttons
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    ToolButton(label = "Pen", selected = selectedTool == DrawTool.PEN) {
                        onToolSelect(DrawTool.PEN)
                    }
                    ToolButton(label = "Hi", selected = selectedTool == DrawTool.HIGHLIGHTER) {
                        onToolSelect(DrawTool.HIGHLIGHTER)
                    }
                    ToolButton(label = "Er", selected = selectedTool == DrawTool.ERASER) {
                        onToolSelect(DrawTool.ERASER)
                    }
                }

                // Stroke width dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    strokeWidths.forEach { width ->
                        Box(
                            modifier = Modifier
                                .size((width.coerceIn(8f, 22f)).dp)
                                .clip(CircleShape)
                                .background(
                                    if (selectedStroke == width) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                                )
                                .clickable { onStrokeSelect(width) }
                        )
                    }
                }

                // Undo / Redo / Clear
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    TextButton(onClick = onUndo, enabled = canUndo) {
                        Text("↩", fontSize = 16.sp)
                    }
                    TextButton(onClick = onRedo, enabled = canRedo) {
                        Text("↪", fontSize = 16.sp)
                    }
                    // Clear requires a confirmation — icon only, no accidental taps
                    IconButton(onClick = onClearRequest) {
                        Icon(
                            Icons.Default.DeleteForever,
                            contentDescription = "Clear board",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

            // Row 2: color palette
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                toolColors.forEach { color ->
                    val isSelected = selectedColor == color && selectedTool != DrawTool.ERASER
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                width = if (isSelected) 2.dp else 0.5.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline,
                                shape = CircleShape
                            )
                            .clickable { onColorSelect(color) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ToolButton(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .clickable { onClick() }
            .height(30.dp)
            .widthIn(min = 36.dp),
        border = androidx.compose.foundation.BorderStroke(
            0.5.dp,
            if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outline
        )
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Text(
                label,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}