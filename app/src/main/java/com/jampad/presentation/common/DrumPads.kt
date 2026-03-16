package com.jampad.presentation.common

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jampad.domain.model.DrumInstrument
import com.jampad.ui.theme.DrumsCyan
import com.jampad.ui.theme.JamPadTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun DrumPads(
    onPadTap: (DrumInstrument) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DrumPad(
                instrument = DrumInstrument.KICK,
                label = "KICK",
                onTap = { onPadTap(DrumInstrument.KICK) },
                modifier = Modifier.weight(1f),
            )
            DrumPad(
                instrument = DrumInstrument.SNARE,
                label = "SNARE",
                onTap = { onPadTap(DrumInstrument.SNARE) },
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DrumPad(
                instrument = DrumInstrument.HI_HAT,
                label = "HI-HAT",
                onTap = { onPadTap(DrumInstrument.HI_HAT) },
                modifier = Modifier.weight(1f),
            )
            DrumPad(
                instrument = DrumInstrument.CLAP,
                label = "CLAP",
                onTap = { onPadTap(DrumInstrument.CLAP) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun DrumPad(
    instrument: DrumInstrument,
    label: String,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    var isFlashing by remember { mutableStateOf(false) }

    val bgColor = if (isFlashing) DrumsCyan else MaterialTheme.colorScheme.surfaceVariant

    Box(
        modifier = modifier
            .aspectRatio(1.6f)
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isFlashing = true
                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        onTap()
                        tryAwaitRelease()
                        scope.launch {
                            delay(80)
                            isFlashing = false
                        }
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = if (isFlashing) MaterialTheme.colorScheme.surface else DrumsCyan,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun DrumPadsPreview() {
    JamPadTheme {
        DrumPads(
            onPadTap = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
