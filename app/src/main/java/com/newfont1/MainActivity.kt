package com.newfont1

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { FontEditor() } }
    }
}
private data class Ink(val points: List<Offset>, val color: Color, val width: Float)

@Composable
private fun FontEditor() {
    val chars = ("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!?@#&").toList()
    var selected by remember { mutableStateOf('A') }
    var inkColor by remember { mutableStateOf(Color(0xFF202124)) }
    var brush by remember { mutableFloatStateOf(8f) }
    var drawings by remember { mutableStateOf(mapOf<Char, List<Ink>>()) }
    var current by remember { mutableStateOf(listOf<Offset>()) }
    val strokes = drawings[selected] ?: emptyList()
    val colors = listOf(Color(0xFF202124), Color(0xFF6750A4), Color(0xFF1976D2), Color(0xFFE53935), Color(0xFF00897B))

    Surface(Modifier.fillMaxSize(), color = Color(0xFFF7F5FA)) {
        Column(Modifier.fillMaxSize().padding(18.dp)) {
            Text("New Font 1", fontSize = 28.sp, color = Color(0xFF342A4D))
            Text("Draw your own letters", color = Color.Gray)
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                chars.forEach { ch ->
                    FilterChip(
                        selected = selected == ch,
                        onClick = { selected = ch; current = emptyList() },
                        label = { Text(ch.toString()) }
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Card(Modifier.fillMaxWidth().weight(1f), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Canvas(
                    Modifier.fillMaxSize().padding(8.dp).pointerInput(selected, inkColor, brush) {
                        detectDragGestures(
                            onDragStart = { p -> current = listOf(p) },
                            onDrag = { change, _ -> current = current + change.position },
                            onDragEnd = {
                                if (current.isNotEmpty()) {
                                    drawings = drawings + (selected to (strokes + Ink(current, inkColor, brush)))
                                    current = emptyList()
                                }
                            }
                        )
                    }
                ) {
                    drawLine(Color(0xFFECE8F3), Offset(size.width * .5f, 0f), Offset(size.width * .5f, size.height), 1f)
                    drawLine(Color(0xFFECE8F3), Offset(0f, size.height * .72f), Offset(size.width, size.height * .72f), 1f)
                    val visible = strokes + if (current.isNotEmpty()) listOf(Ink(current, inkColor, brush)) else emptyList()
                    visible.forEach { s ->
                        if (s.points.size == 1) drawCircle(s.color, s.width / 2, s.points.first())
                        else {
                            val path = Path().apply {
                                moveTo(s.points.first().x, s.points.first().y)
                                s.points.drop(1).forEach { lineTo(it.x, it.y) }
                            }
                            drawPath(path, s.color, style = Stroke(width = s.width, cap = StrokeCap.Round))
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Text("Selected: $selected", fontSize = 16.sp)
            Text("Brush size: ${brush.toInt()}")
            Slider(value = brush, onValueChange = { brush = it }, valueRange = 2f..24f)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                colors.forEach { c ->
                    Surface(
                        onClick = { inkColor = c },
                        modifier = Modifier.size(32.dp),
                        shape = RoundedCornerShape(50),
                        color = c,
                        border = if (inkColor == c) BorderStroke(3.dp, Color.LightGray) else null
                    ) {}
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = {
                    if (strokes.isNotEmpty()) drawings = drawings + (selected to strokes.dropLast(1))
                    current = emptyList()
                }, modifier = Modifier.weight(1f)) { Text("Undo") }
                OutlinedButton(onClick = {
                    drawings = drawings + (selected to emptyList())
                    current = emptyList()
                }, modifier = Modifier.weight(1f)) { Text("Clear") }
            }
            Spacer(Modifier.height(8.dp))
            Text("Prototype: drawings stay while this screen is open. Saving drawings and TTF/OTF font export are not implemented yet.", color = Color.Gray, fontSize = 12.sp)
        }
    }
}
