package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Product
import com.example.data.model.ProductColor
import com.example.ui.theme.CyberCyan
import com.example.util.ProductImageHelper
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun Interactive3DViewer(
    product: Product,
    selectedColor: ProductColor,
    modifier: Modifier = Modifier
) {
    var yaw by remember { mutableFloatStateOf(0f) }
    var pitch by remember { mutableFloatStateOf(0f) }
    var isAutoSpinning by remember { mutableStateOf(true) }
    var zoomLevel by remember { mutableFloatStateOf(1.0f) }
    var showWireframe by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "spin_transition")
    val autoSpinAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spin_angle"
    )

    val activeYaw = if (isAutoSpinning) autoSpinAngle + yaw else yaw

    val colorTint = remember(selectedColor.hex) {
        try {
            Color(android.graphics.Color.parseColor(selectedColor.hex))
        } catch (e: Exception) {
            Color(0xFF16A34A)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("interactive_3d_viewer")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.15f)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF334155),
                            Color(0xFF1E293B),
                            Color(0xFF0F172A)
                        )
                    )
                )
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { isAutoSpinning = false },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            yaw = (yaw + dragAmount.x * 0.7f) % 360f
                            pitch = (pitch - dragAmount.y * 0.5f).coerceIn(-40f, 40f)
                        }
                    )
                }
        ) {
            // Background 3D Grid Pedestal
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2
                val cy = size.height * 0.82f
                val radX = size.width * 0.42f
                val radY = size.height * 0.12f

                // Elliptical pedestal shadow
                drawOval(
                    color = Color.Black.copy(alpha = 0.45f),
                    topLeft = Offset(cx - radX, cy - radY),
                    size = androidx.compose.ui.geometry.Size(radX * 2, radY * 2)
                )

                // 3D coordinate orbital ring
                drawOval(
                    color = CyberCyan.copy(alpha = 0.4f),
                    topLeft = Offset(cx - radX * 0.95f, cy - radY * 0.95f),
                    size = androidx.compose.ui.geometry.Size(radX * 1.9f, radY * 1.9f),
                    style = Stroke(width = 2.dp.toPx())
                )

                // Radial perspective lines
                for (i in 0 until 12) {
                    val angle = (i * 30 + activeYaw) * (PI / 180.0)
                    val px = cx + (radX * 0.95f * cos(angle)).toFloat()
                    val py = cy + (radY * 0.95f * sin(angle)).toFloat()
                    drawLine(
                        color = CyberCyan.copy(alpha = 0.15f),
                        start = Offset(cx, cy),
                        end = Offset(px, py),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // If Wireframe mode is enabled, draw 3D bounding geometry
                if (showWireframe) {
                    val boxW = size.width * 0.45f * zoomLevel
                    val boxH = size.height * 0.45f * zoomLevel
                    val midY = size.height * 0.46f + (pitch * 1.5f)

                    val angleRad = activeYaw * (PI / 180.0)
                    val cosA = cos(angleRad).toFloat()
                    val sinA = sin(angleRad).toFloat()

                    val p1 = Offset(cx - (boxW / 2) * cosA, midY - boxH / 2 + (boxW / 4) * sinA)
                    val p2 = Offset(cx + (boxW / 2) * cosA, midY - boxH / 2 - (boxW / 4) * sinA)
                    val p3 = Offset(cx + (boxW / 2) * cosA, midY + boxH / 2 - (boxW / 4) * sinA)
                    val p4 = Offset(cx - (boxW / 2) * cosA, midY + boxH / 2 + (boxW / 4) * sinA)

                    val path = Path().apply {
                        moveTo(p1.x, p1.y)
                        lineTo(p2.x, p2.y)
                        lineTo(p3.x, p3.y)
                        lineTo(p4.x, p4.y)
                        close()
                    }
                    drawPath(
                        path = path,
                        color = colorTint.copy(alpha = 0.8f),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }

            // Core 3D Image Layer responding to Yaw and Pitch
            val pinned3DImageRequest = ProductImageHelper.rememberImageRequest(
                imageUrl = product.imageUrl,
                category = product.category,
                title = product.title
            )
            AsyncImage(
                model = pinned3DImageRequest,
                contentDescription = "${product.title} 3D Model",
                contentScale = ContentScale.Fit,
                filterQuality = androidx.compose.ui.graphics.FilterQuality.High,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(28.dp)
                    .graphicsLayer {
                        scaleX = zoomLevel
                        scaleY = zoomLevel
                        rotationY = sin(activeYaw * (PI / 180.0)).toFloat() * 22f
                        rotationX = pitch
                        cameraDistance = 14f * density
                        shadowElevation = 16.dp.toPx()
                    }
            )

            // Dynamic Ambient Color Rim Light
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                colorTint.copy(alpha = 0.18f),
                                Color.Transparent,
                                colorTint.copy(alpha = 0.08f)
                            ),
                            start = Offset(activeYaw * 2f, 0f),
                            end = Offset(activeYaw * 2f + 400f, 600f)
                        )
                    )
            )

            // Top Status Bar in Viewport
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
                    .align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black.copy(alpha = 0.7f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ViewInAr,
                            contentDescription = null,
                            tint = CyberCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Yaw: ${activeYaw.toInt() % 360}° | Pitch: ${pitch.toInt()}°",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Selected Color Material indicator
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black.copy(alpha = 0.7f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(colorTint)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = selectedColor.name,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Bottom Tool Controls inside Viewport
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Auto spin button
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isAutoSpinning) CyberCyan else Color.Black.copy(alpha = 0.7f),
                    modifier = Modifier.clip(RoundedCornerShape(20.dp))
                ) {
                    IconButton(
                        onClick = { isAutoSpinning = !isAutoSpinning },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Autorenew,
                            contentDescription = "Auto Spin",
                            tint = if (isAutoSpinning) Color.Black else Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Wireframe mode button
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (showWireframe) colorTint else Color.Black.copy(alpha = 0.7f)
                ) {
                    IconButton(
                        onClick = { showWireframe = !showWireframe },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Wireframe Toggle",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Zoom Level Toggle
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.Black.copy(alpha = 0.7f)
                ) {
                    IconButton(
                        onClick = {
                            zoomLevel = when (zoomLevel) {
                                1.0f -> 1.35f
                                1.35f -> 1.7f
                                else -> 1.0f
                            }
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ZoomIn,
                            contentDescription = "Zoom",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Reset Orientation
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.Black.copy(alpha = 0.7f)
                ) {
                    IconButton(
                        onClick = {
                            yaw = 0f
                            pitch = 0f
                            zoomLevel = 1.0f
                            isAutoSpinning = false
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = "Reset Angle",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Tip: Drag horizontally or vertically on the 3D model to rotate 360°",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}
