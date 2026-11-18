package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentGold
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.SuccessGreen
import java.util.Locale

private fun serializeNormalizedStrokes(strokes: List<List<Offset>>): String {
    return strokes.filter { it.size >= 2 }.joinToString(";") { stroke ->
        stroke.joinToString(",") { pt ->
            val nx = String.format(Locale.US, "%.4f", pt.x.coerceIn(0f, 1f))
            val ny = String.format(Locale.US, "%.4f", pt.y.coerceIn(0f, 1f))
            "$nx:$ny"
        }
    }
}

private fun parseNormalizedStrokes(data: String): List<List<Offset>> {
    if (data.isBlank()) return emptyList()
    return data.split(";").mapNotNull { strokeStr ->
        val pts = strokeStr.split(",").mapNotNull { token ->
            val xy = token.split(":")
            if (xy.size == 2) {
                val nx = xy[0].toFloatOrNull()
                val ny = xy[1].toFloatOrNull()
                if (nx != null && ny != null) Offset(nx.coerceIn(0f, 1f), ny.coerceIn(0f, 1f)) else null
            } else null
        }
        if (pts.size >= 2) pts else null
    }
}

@Composable
fun SignaturePadBox(
    strokesData: String,
    onStrokesChanged: (String) -> Unit,
    title: String,
    subtitle: String = "Draw signature cleanly inside the white pad",
    modifier: Modifier = Modifier,
    testTagPrefix: String = "signature_pad"
) {
    val strokes = remember(strokesData) {
        mutableStateListOf<List<Offset>>().apply {
            addAll(parseNormalizedStrokes(strokesData))
        }
    }
    var activeStroke by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var canvasSize by remember { mutableStateOf(IntSize(1, 1)) }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(
                    imageVector = Icons.Default.Draw,
                    contentDescription = null,
                    tint = AccentGold,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.5.sp
                    )
                }
            }

            if (strokes.isNotEmpty() || activeStroke.isNotEmpty()) {
                OutlinedButton(
                    onClick = {
                        strokes.clear()
                        activeStroke = emptyList()
                        onStrokesChanged("")
                    },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("${testTagPrefix}_clear")
                ) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear Signature", modifier = Modifier.size(14.dp), tint = Color(0xFFEF4444))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear", fontSize = 11.sp, color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White)
                .border(
                    BorderStroke(
                        width = 1.5.dp,
                        color = if (strokes.isNotEmpty()) SuccessGreen else MaterialTheme.colorScheme.outline
                    ),
                    RoundedCornerShape(14.dp)
                )
                .onSizeChanged { if (it.width > 0 && it.height > 0) canvasSize = it }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        down.consume()
                        val w = canvasSize.width.coerceAtLeast(1).toFloat()
                        val h = canvasSize.height.coerceAtLeast(1).toFloat()
                        val currentPts = mutableListOf(
                            Offset((down.position.x / w).coerceIn(0f, 1f), (down.position.y / h).coerceIn(0f, 1f))
                        )
                        activeStroke = currentPts.toList()

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) {
                                change.consume()
                                break
                            }
                            change.consume()
                            val nx = (change.position.x / w).coerceIn(0f, 1f)
                            val ny = (change.position.y / h).coerceIn(0f, 1f)
                            currentPts.add(Offset(nx, ny))
                            activeStroke = currentPts.toList()
                        }

                        if (currentPts.size == 1) {
                            val p = currentPts.first()
                            currentPts.add(Offset((p.x + 0.005f).coerceIn(0f, 1f), p.y))
                        }
                        if (currentPts.size >= 2) {
                            strokes.add(currentPts.toList())
                            onStrokesChanged(serializeNormalizedStrokes(strokes))
                        }
                        activeStroke = emptyList()
                    }
                }
                .testTag(testTagPrefix)
        ) {
            if (strokes.isEmpty() && activeStroke.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Sign here with your finger",
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Canvas(modifier = Modifier.fillMaxSize()) {
                val allToDraw = if (activeStroke.isNotEmpty()) strokes + listOf(activeStroke) else strokes
                allToDraw.forEach { stroke ->
                    if (stroke.size >= 2) {
                        val path = Path().apply {
                            moveTo(stroke[0].x * size.width, stroke[0].y * size.height)
                            for (i in 1 until stroke.size) {
                                lineTo(stroke[i].x * size.width, stroke[i].y * size.height)
                            }
                        }
                        drawPath(
                            path = path,
                            color = Color(0xFF0F172A),
                            style = Stroke(width = 5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (strokes.isNotEmpty()) {
                Icon(Icons.Default.Verified, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Digital Signature Captured (${strokes.size} stroke(s))",
                    color = SuccessGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Text(
                    text = "Signature required before submitting",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun SignaturePreviewBox(
    strokesData: String,
    signerName: String,
    roleLabel: String,
    modifier: Modifier = Modifier
) {
    val parsedStrokes = remember(strokesData) { parseNormalizedStrokes(strokesData) }
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = 1.dp,
            color = if (parsedStrokes.isNotEmpty()) SuccessGreen.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = roleLabel,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                color = CyberCyan
            )
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(78.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White)
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (parsedStrokes.isEmpty()) {
                    Text(
                        text = "Awaiting Signature",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                } else {
                    Canvas(modifier = Modifier.fillMaxSize().padding(6.dp)) {
                        parsedStrokes.forEach { stroke ->
                            if (stroke.size >= 2) {
                                val path = Path().apply {
                                    moveTo(stroke[0].x * size.width, stroke[0].y * size.height)
                                    for (i in 1 until stroke.size) {
                                        lineTo(stroke[i].x * size.width, stroke[i].y * size.height)
                                    }
                                }
                                drawPath(
                                    path = path,
                                    color = Color(0xFF0F172A),
                                    style = Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = signerName.ifBlank { "Pending Signatory" },
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
