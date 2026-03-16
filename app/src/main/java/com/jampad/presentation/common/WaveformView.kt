package com.jampad.presentation.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jampad.ui.theme.GuitarAmber
import com.jampad.ui.theme.JamPadTheme

@Composable
fun WaveformView(
    samples: FloatArray,
    progress: Float,
    modifier: Modifier = Modifier,
    waveformColor: Color = GuitarAmber,
    playheadColor: Color = MaterialTheme.colorScheme.primary,
    beatCount: Int = 0,
) {
    val beatLineColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val centerY = height / 2
        val barWidth = 2.dp.toPx()
        val gap = 1.dp.toPx()
        val step = barWidth + gap

        // Beat markers
        if (beatCount > 0) {
            for (beat in 1 until beatCount) {
                val x = width * beat / beatCount
                drawLine(
                    color = beatLineColor,
                    start = Offset(x, 0f),
                    end = Offset(x, height),
                    strokeWidth = 1.dp.toPx(),
                )
            }
        }

        // Waveform bars
        if (samples.isNotEmpty()) {
            val barsCount = (width / step).toInt().coerceAtLeast(1)
            for (i in 0 until barsCount) {
                val sampleIndex = (i.toFloat() / barsCount * samples.size).toInt()
                    .coerceIn(0, samples.lastIndex)
                val amplitude = samples[sampleIndex]
                val barHeight = (amplitude * height * 0.8f).coerceAtLeast(2.dp.toPx())
                val x = i * step + barWidth / 2

                drawLine(
                    color = waveformColor,
                    start = Offset(x, centerY - barHeight / 2),
                    end = Offset(x, centerY + barHeight / 2),
                    strokeWidth = barWidth,
                    cap = StrokeCap.Round,
                )
            }
        }

        // Playhead
        if (progress > 0f) {
            val playheadX = width * progress
            drawLine(
                color = playheadColor,
                start = Offset(playheadX, 0f),
                end = Offset(playheadX, height),
                strokeWidth = 2.dp.toPx(),
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF1E1E1E)
@Composable
private fun WaveformPreview() {
    JamPadTheme {
        val fakeSamples = FloatArray(200) { index ->
            val x = index.toFloat() / 200
            (kotlin.math.sin(x * 20) * 0.5f + kotlin.math.sin(x * 7) * 0.3f).coerceIn(0f, 1f)
        }
        WaveformView(
            samples = fakeSamples,
            progress = 0.4f,
            beatCount = 16,
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
        )
    }
}
