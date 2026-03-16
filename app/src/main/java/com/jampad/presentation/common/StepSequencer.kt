package com.jampad.presentation.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jampad.domain.model.DrumInstrument
import com.jampad.domain.model.DrumPattern
import com.jampad.domain.model.DrumPresets
import com.jampad.ui.theme.DrumsCyan
import com.jampad.ui.theme.JamPadTheme

@Composable
fun StepSequencer(
    pattern: DrumPattern,
    currentStep: Int,
    onToggleHit: (DrumInstrument, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()

    Column(modifier = modifier) {
        // Step numbers header
        Row(modifier = Modifier.horizontalScroll(scrollState)) {
            Spacer(modifier = Modifier.width(56.dp))
            for (step in 0 until pattern.totalSteps) {
                Box(
                    modifier = Modifier.size(36.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "${step + 1}",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = if (step == currentStep) DrumsCyan
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (step == currentStep) FontWeight.Bold else FontWeight.Normal,
                    )
                }
            }
        }

        // Instrument rows
        DrumInstrument.entries.forEach { instrument ->
            Row(
                modifier = Modifier.horizontalScroll(scrollState),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Label
                Box(
                    modifier = Modifier.width(56.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text(
                        text = instrument.shortLabel(),
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // Step cells
                val hits = pattern.hits[instrument] ?: BooleanArray(pattern.totalSteps)
                for (step in 0 until pattern.totalSteps) {
                    val isActive = step < hits.size && hits[step]
                    val isCurrent = step == currentStep

                    StepCell(
                        isActive = isActive,
                        isCurrent = isCurrent,
                        onClick = { onToggleHit(instrument, step) },
                    )
                }
            }
        }
    }
}

@Composable
private fun StepCell(
    isActive: Boolean,
    isCurrent: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(4.dp)
    val bgColor = when {
        isActive -> DrumsCyan
        isCurrent -> DrumsCyan.copy(alpha = 0.15f)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val borderColor = when {
        isCurrent -> DrumsCyan.copy(alpha = 0.5f)
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    }

    Box(
        modifier = Modifier
            .size(36.dp)
            .padding(2.dp)
            .clip(shape)
            .background(bgColor)
            .border(1.dp, borderColor, shape)
            .clickable(onClick = onClick),
    )
}

private fun DrumInstrument.shortLabel(): String = when (this) {
    DrumInstrument.KICK -> "KICK"
    DrumInstrument.SNARE -> "SNR"
    DrumInstrument.HI_HAT -> "HH"
    DrumInstrument.CLAP -> "CLAP"
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun StepSequencerPreview() {
    JamPadTheme {
        StepSequencer(
            pattern = DrumPresets.funk(),
            currentStep = 2,
            onToggleHit = { _, _ -> },
            modifier = Modifier.padding(8.dp),
        )
    }
}
