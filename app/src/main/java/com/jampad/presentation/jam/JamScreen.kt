package com.jampad.presentation.jam

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jampad.domain.model.LoopState
import com.jampad.ui.theme.BassPurple
import com.jampad.ui.theme.DrumsCyan
import com.jampad.ui.theme.GuitarAmber
import com.jampad.ui.theme.JamPadTheme
import com.jampad.ui.theme.RecordRed

@Composable
fun JamScreen(viewModel: JamViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var hasPermission by rememberSaveable {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        viewModel.onAudioPermissionResult(granted)
    }

    LaunchedEffect(hasPermission) {
        viewModel.onAudioPermissionResult(hasPermission)
    }

    JamContent(
        uiState = uiState,
        onTabSelected = viewModel::onTabSelected,
        onBpmChanged = viewModel::onBpmChanged,
        onBarCountChanged = viewModel::onBarCountChanged,
        onBigButtonClick = {
            if (!hasPermission) {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
            // Audio recording logic will be added in M2
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JamContent(
    uiState: JamUiState,
    onTabSelected: (JamTab) -> Unit,
    onBpmChanged: (Int) -> Unit,
    onBarCountChanged: (Int) -> Unit,
    onBigButtonClick: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "JamPad",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                },
                actions = {
                    Text(
                        text = "${uiState.bpm} BPM",
                        style = MaterialTheme.typography.labelLarge,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                    BarCountChips(
                        selected = uiState.barCount,
                        onSelected = onBarCountChanged,
                    )
                    IconButton(onClick = { /* Settings — M10 */ }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Waveform strip placeholder
            WaveformPlaceholder(loopState = uiState.loopState)

            Spacer(modifier = Modifier.height(12.dp))

            // Layer tabs
            LayerTabs(
                selectedTab = uiState.selectedTab,
                onTabSelected = onTabSelected,
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Contextual control area placeholder
            ContextualArea(
                tab = uiState.selectedTab,
                modifier = Modifier.weight(1f),
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Mini mixer
            MiniMixer(mixState = uiState.mixState)

            Spacer(modifier = Modifier.height(16.dp))

            // Big Button
            BigButton(
                loopState = uiState.loopState,
                onClick = onBigButtonClick,
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun BarCountChips(
    selected: Int,
    onSelected: (Int) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        listOf(4, 8, 16).forEach { count ->
            FilterChip(
                selected = selected == count,
                onClick = { onSelected(count) },
                label = {
                    Text(
                        text = "$count",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    selectedLabelColor = MaterialTheme.colorScheme.primary,
                ),
                modifier = Modifier.height(28.dp),
            )
        }
    }
}

@Composable
private fun WaveformPlaceholder(loopState: LoopState) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(
                width = 1.dp,
                color = when (loopState) {
                    LoopState.RECORDING -> RecordRed.copy(alpha = 0.6f)
                    LoopState.OVERDUBBING -> GuitarAmber.copy(alpha = 0.6f)
                    else -> MaterialTheme.colorScheme.outline
                },
                shape = RoundedCornerShape(16.dp),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = when (loopState) {
                LoopState.EMPTY -> "Record a riff to get started"
                LoopState.RECORDING -> "Recording..."
                LoopState.LOOPING -> "Looping"
                LoopState.OVERDUBBING -> "Overdubbing..."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun LayerTabs(
    selectedTab: JamTab,
    onTabSelected: (JamTab) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        JamTab.entries.forEach { tab ->
            val color = when (tab) {
                JamTab.GUITAR -> GuitarAmber
                JamTab.DRUMS -> DrumsCyan
                JamTab.BASS -> BassPurple
            }
            val label = when (tab) {
                JamTab.GUITAR -> "Guitar"
                JamTab.DRUMS -> "Drums"
                JamTab.BASS -> "Bass"
            }
            FilterChip(
                selected = selectedTab == tab,
                onClick = { onTabSelected(tab) },
                label = { Text(label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = color.copy(alpha = 0.2f),
                    selectedLabelColor = color,
                ),
            )
        }
    }
}

@Composable
private fun ContextualArea(tab: JamTab, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center,
    ) {
        val text = when (tab) {
            JamTab.GUITAR -> "Guitar controls — coming in M2"
            JamTab.DRUMS -> "Drum pad — coming in M4"
            JamTab.BASS -> "Bass generator — coming in M6"
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun MiniMixer(mixState: com.jampad.domain.model.MixState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MixerSlider(
            label = "Guitar",
            value = mixState.guitarVolume,
            color = GuitarAmber,
            muted = mixState.guitarMuted,
            onValueChange = {},
            modifier = Modifier.weight(1f),
        )
        MixerSlider(
            label = "Drums",
            value = mixState.drumsVolume,
            color = DrumsCyan,
            muted = mixState.drumsMuted,
            onValueChange = {},
            modifier = Modifier.weight(1f),
        )
        MixerSlider(
            label = "Bass",
            value = mixState.bassVolume,
            color = BassPurple,
            muted = mixState.bassMuted,
            onValueChange = {},
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun MixerSlider(
    label: String,
    value: Float,
    color: androidx.compose.ui.graphics.Color,
    muted: Boolean,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (muted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f) else color,
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            colors = SliderDefaults.colors(
                thumbColor = if (muted) color.copy(alpha = 0.4f) else color,
                activeTrackColor = if (muted) color.copy(alpha = 0.4f) else color,
                inactiveTrackColor = color.copy(alpha = 0.1f),
            ),
        )
    }
}

@Composable
private fun BigButton(
    loopState: LoopState,
    onClick: () -> Unit,
) {
    val buttonColor = when (loopState) {
        LoopState.EMPTY -> RecordRed
        LoopState.RECORDING -> RecordRed
        LoopState.LOOPING -> MaterialTheme.colorScheme.primary
        LoopState.OVERDUBBING -> GuitarAmber
    }
    val label = when (loopState) {
        LoopState.EMPTY -> "REC"
        LoopState.RECORDING -> "STOP"
        LoopState.LOOPING -> "OVERDUB"
        LoopState.OVERDUBBING -> "STOP"
    }

    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(buttonColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (loopState == LoopState.EMPTY) {
            Icon(
                imageVector = Icons.Default.FiberManualRecord,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(28.dp),
            )
        } else {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun JamContentPreview() {
    JamPadTheme {
        JamContent(
            uiState = JamUiState(),
            onTabSelected = {},
            onBpmChanged = {},
            onBarCountChanged = {},
            onBigButtonClick = {},
        )
    }
}
