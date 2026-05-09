package com.example.trackit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.dp

@Composable
fun GuidedTourBubble(
    text: String,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    targetCoordinates: LayoutCoordinates?,
    isLastStep: Boolean = false
) {
    // If we have target coordinates, we show the darkened background and bubble.
    // If coordinates aren't attached yet, we wait to avoid flickering.
    if (targetCoordinates != null && !targetCoordinates.isAttached) return

    Box(modifier = Modifier.fillMaxSize()) {
        // Dimmed background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
        )

        Surface(
            modifier = Modifier
                .padding(32.dp)
                .align(Alignment.Center),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.size(8.dp))
                Row(modifier = Modifier.align(Alignment.End)) {
                    TextButton(onClick = onSkip) {
                        Text("Skip")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = onNext) {
                        Text(if (isLastStep) "Finish" else "Next")
                    }
                }
            }
        }
    }
}
