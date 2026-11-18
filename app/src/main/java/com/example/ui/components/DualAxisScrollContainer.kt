package com.example.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Bounded vertical-only viewport wrapper.
 * Provides finite maxWidth/maxHeight constraints to nested LazyVerticalGrid/LazyColumn
 * without horizontal screen panning or gesture jitter.
 */
@Composable
fun DualAxisScrollContainer(
    wideMode: Boolean = false,
    minWideWidth: Dp = 680.dp,
    modifier: Modifier = Modifier,
    horizontalScrollState: ScrollState = rememberScrollState(),
    contentAlignment: Alignment = Alignment.TopCenter,
    content: @Composable BoxScope.(canvasWidth: Dp, viewportHeight: Dp) -> Unit
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val viewportWidth = maxWidth
        val viewportHeight = maxHeight
        Box(
            modifier = Modifier
                .width(viewportWidth)
                .height(viewportHeight),
            contentAlignment = contentAlignment
        ) {
            content(viewportWidth, viewportHeight)
        }
    }
}
