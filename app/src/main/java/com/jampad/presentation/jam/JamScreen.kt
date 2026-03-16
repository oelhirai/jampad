package com.jampad.presentation.jam

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Remove
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jampad.domain.model.BassPatternType
import com.jampad.domain.model.DrumInstrument
import com.jampad.domain.model.LoopState
import com.jampad.domain.model.MusicalKey
import com.jampad.domain.model.MusicStyle
import com.jampad.presentation.common.DrumPads
import com.jampad.presentation.common.StepSequencer
import com.jampad.presentation.common.WaveformView
import com.jampad.ui.theme.BassPurple
import com.jampad.ui.theme.DrumsCyan
import com.jampad.ui.theme.GuitarAmber
import com.jampad.ui.theme.JamPadTheme
import com.jampad.ui.theme.RecordRed

@Composable
fun JamScreen(viewModel: JamViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val waveform by viewModel.waveformSamples.collectAsStateWithLifecycle()
    val progress by viewModel.playbackProgress.collectAsStateWithLifecycle()
    val drumStep by viewModel.drumCurrentStep.collectAsStateWithLifecycle()
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
        waveformSamples = waveform,
        playbackProgress = progress,
        drumCurrentStep = drumStep,
        onTabSelected = viewModel::onTabSelected,
        onBpmChanged = viewModel::onBpmChanged,
        onBarCountChanged = viewModel::onBarCountChanged,
        onBigButtonClick = {
            if (!hasPermission && uiState.loopState == LoopState.EMPTY) {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            } else {
                viewModel.onBigButtonPress()
            }
        },
        onToggleDrumHit = viewModel::onToggleDrumHit,
        onLoadDrumPreset = viewModel::onLoadDrumPreset,
        onClearDrumPattern = viewModel::onClearDrumPattern,
        onDrumModeChanged = viewModel::onDrumModeChanged,
        onDrumPadTap = viewModel::onDrumPadTap,
        onBassKeyChanged = viewModel::onBassKeyChanged,
        onBassPatternChanged = viewModel::onBassPatternChanged,
        onBassStyleChanged = viewModel::onBassStyleChanged,
        onBassToggle = viewModel::onBassToggle,
        onClearSession = viewModel::onClearSession,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JamContent(
    uiState: JamUiState,
    waveformSamples: FloatArray,
    playbackProgress: Float,
    drumCurrentStep: Int,
    onTabSelected: (JamTab) -> Unit,
    onBpmChanged: (Int) -> Unit,
    onBarCountChanged: (Int) -> Unit,
    onBigButtonClick: () -> Unit,
    onToggleDrumHit: (DrumInstrument, Int) -> Unit,
    onLoadDrumPreset: (MusicStyle) -> Unit,
    onClearDrumPattern: () -> Unit,
    onDrumModeChanged: (DrumMode) -> Unit,
    onDrumPadTap: (DrumInstrument) -> Unit,
    onBassKeyChanged: (MusicalKey) -> Unit,
    onBassPatternChanged: (BassPatternType) -> Unit,
    onBassStyleChanged: (MusicStyle) -> Unit,
    onBassToggle: () -> Unit,
    onClearSession: () -> Unit,
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
                    BpmControl(
                        bpm = uiState.bpm,
                        onBpmChanged = onBpmChanged,
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
            // Waveform strip
            WaveformStrip(
                loopState = uiState.loopState,
                waveformSamples = waveformSamples,
                playbackProgress = playbackProgress,
                beatCount = uiState.barCount * 4,
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Layer tabs
            LayerTabs(
                selectedTab = uiState.selectedTab,
                onTabSelected = onTabSelected,
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Contextual control area
            ContextualArea(
                tab = uiState.selectedTab,
                loopState = uiState.loopState,
                drumPattern = uiState.drumPattern,
                drumCurrentStep = drumCurrentStep,
                drumPreset = uiState.drumPreset,
                drumMode = uiState.drumMode,
                bassConfig = uiState.bassConfig,
                onToggleDrumHit = onToggleDrumHit,
                onLoadDrumPreset = onLoadDrumPreset,
                onClearDrumPattern = onClearDrumPattern,
                onDrumModeChanged = onDrumModeChanged,
                onDrumPadTap = onDrumPadTap,
                onBassKeyChanged = onBassKeyChanged,
                onBassPatternChanged = onBassPatternChanged,
                onBassStyleChanged = onBassStyleChanged,
                onBassToggle = onBassToggle,
                modifier = Modifier.weight(1f),
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Mini mixer
            MiniMixer(mixState = uiState.mixState)

            Spacer(modifier = Modifier.height(16.dp))

            // Big Button + Clear
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                // Clear session button — only visible when a loop exists
                if (uiState.loopState != LoopState.EMPTY) {
                    IconButton(
                        onClick = onClearSession,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Clear session",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(40.dp))
                }

                BigButton(
                    loopState = uiState.loopState,
                    onClick = onBigButtonClick,
                )

                // Spacer for symmetry
                Spacer(modifier = Modifier.size(40.dp))
            }

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
private fun BpmControl(
    bpm: Int,
    onBpmChanged: (Int) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        IconButton(
            onClick = { onBpmChanged(bpm - 5) },
            modifier = Modifier.size(28.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Remove,
                contentDescription = "Decrease BPM",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = "$bpm",
            style = MaterialTheme.typography.labelLarge,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        IconButton(
            onClick = { onBpmChanged(bpm + 5) },
            modifier = Modifier.size(28.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Increase BPM",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun WaveformStrip(
    loopState: LoopState,
    waveformSamples: FloatArray,
    playbackProgress: Float,
    beatCount: Int,
) {
    val borderColor = when (loopState) {
        LoopState.RECORDING -> RecordRed.copy(alpha = 0.6f)
        LoopState.OVERDUBBING -> GuitarAmber.copy(alpha = 0.6f)
        else -> MaterialTheme.colorScheme.outline
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center,
    ) {
        if (waveformSamples.isEmpty()) {
            Text(
                text = when (loopState) {
                    LoopState.EMPTY -> "Record a riff to get started"
                    LoopState.RECORDING -> "Recording..."
                    else -> ""
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            WaveformView(
                samples = waveformSamples,
                progress = if (loopState == LoopState.LOOPING || loopState == LoopState.OVERDUBBING) playbackProgress else 0f,
                beatCount = beatCount,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
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
private fun ContextualArea(
    tab: JamTab,
    loopState: LoopState,
    drumPattern: com.jampad.domain.model.DrumPattern,
    drumCurrentStep: Int,
    drumPreset: MusicStyle?,
    drumMode: DrumMode,
    bassConfig: com.jampad.domain.model.BassConfig,
    onToggleDrumHit: (DrumInstrument, Int) -> Unit,
    onLoadDrumPreset: (MusicStyle) -> Unit,
    onClearDrumPattern: () -> Unit,
    onDrumModeChanged: (DrumMode) -> Unit,
    onDrumPadTap: (DrumInstrument) -> Unit,
    onBassKeyChanged: (MusicalKey) -> Unit,
    onBassPatternChanged: (BassPatternType) -> Unit,
    onBassStyleChanged: (MusicStyle) -> Unit,
    onBassToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface),
    ) {
        when (tab) {
            JamTab.GUITAR -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = when (loopState) {
                            LoopState.EMPTY -> "Set BPM and bar count, then hit REC"
                            LoopState.RECORDING -> "Play your riff..."
                            LoopState.LOOPING -> "Tap OVERDUB to layer more guitar"
                            LoopState.OVERDUBBING -> "Playing over the loop..."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            JamTab.DRUMS -> {
                DrumControlArea(
                    pattern = drumPattern,
                    currentStep = drumCurrentStep,
                    selectedPreset = drumPreset,
                    mode = drumMode,
                    onToggleHit = onToggleDrumHit,
                    onLoadPreset = onLoadDrumPreset,
                    onClear = onClearDrumPattern,
                    onModeChanged = onDrumModeChanged,
                    onPadTap = onDrumPadTap,
                )
            }
            JamTab.BASS -> {
                BassControlArea(
                    config = bassConfig,
                    onKeyChanged = onBassKeyChanged,
                    onPatternChanged = onBassPatternChanged,
                    onStyleChanged = onBassStyleChanged,
                    onToggle = onBassToggle,
                )
            }
        }
    }
}

@Composable
private fun DrumControlArea(
    pattern: com.jampad.domain.model.DrumPattern,
    currentStep: Int,
    selectedPreset: MusicStyle?,
    mode: DrumMode,
    onToggleHit: (DrumInstrument, Int) -> Unit,
    onLoadPreset: (MusicStyle) -> Unit,
    onClear: () -> Unit,
    onModeChanged: (DrumMode) -> Unit,
    onPadTap: (DrumInstrument) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
    ) {
        // Mode toggle
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DrumMode.entries.forEach { m ->
                FilterChip(
                    selected = mode == m,
                    onClick = { onModeChanged(m) },
                    label = {
                        Text(
                            text = when (m) {
                                DrumMode.SEQUENCER -> "Sequencer"
                                DrumMode.PADS -> "Pads"
                            },
                            style = MaterialTheme.typography.labelSmall,
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = DrumsCyan.copy(alpha = 0.2f),
                        selectedLabelColor = DrumsCyan,
                    ),
                    modifier = Modifier.height(28.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        when (mode) {
            DrumMode.SEQUENCER -> {
                StepSequencer(
                    pattern = pattern,
                    currentStep = currentStep,
                    onToggleHit = onToggleHit,
                    modifier = Modifier.weight(1f),
                )
            }
            DrumMode.PADS -> {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    DrumPads(
                        onPadTap = onPadTap,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Preset chips
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Preset:",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            MusicStyle.entries.forEach { style ->
                FilterChip(
                    selected = selectedPreset == style,
                    onClick = { onLoadPreset(style) },
                    label = {
                        Text(
                            text = when (style) {
                                MusicStyle.FUNK -> "Funk"
                                MusicStyle.LO_FI -> "Lo-fi"
                                MusicStyle.ROCK -> "Rock"
                            },
                            style = MaterialTheme.typography.labelSmall,
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = DrumsCyan.copy(alpha = 0.2f),
                        selectedLabelColor = DrumsCyan,
                    ),
                    modifier = Modifier.height(28.dp),
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            FilterChip(
                selected = false,
                onClick = onClear,
                label = {
                    Text(
                        text = "Clear",
                        style = MaterialTheme.typography.labelSmall,
                    )
                },
                modifier = Modifier.height(28.dp),
            )
        }
    }
}

@Composable
private fun BassControlArea(
    config: com.jampad.domain.model.BassConfig,
    onKeyChanged: (MusicalKey) -> Unit,
    onPatternChanged: (BassPatternType) -> Unit,
    onStyleChanged: (MusicStyle) -> Unit,
    onToggle: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Enable toggle
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Bass",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = BassPurple,
            )
            Spacer(modifier = Modifier.weight(1f))
            FilterChip(
                selected = config.enabled,
                onClick = onToggle,
                label = {
                    Text(
                        text = if (config.enabled) "ON" else "OFF",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = BassPurple.copy(alpha = 0.2f),
                    selectedLabelColor = BassPurple,
                ),
                modifier = Modifier.height(28.dp),
            )
        }

        // Key selector
        Column {
            Text(
                text = "Key",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                MusicalKey.entries.forEach { key ->
                    FilterChip(
                        selected = config.key == key,
                        onClick = { onKeyChanged(key) },
                        label = {
                            Text(
                                text = key.displayName,
                                style = MaterialTheme.typography.labelSmall,
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BassPurple.copy(alpha = 0.2f),
                            selectedLabelColor = BassPurple,
                        ),
                        modifier = Modifier.height(28.dp),
                    )
                }
            }
        }

        // Pattern type
        Column {
            Text(
                text = "Pattern",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                BassPatternType.entries.forEach { pattern ->
                    FilterChip(
                        selected = config.pattern == pattern,
                        onClick = { onPatternChanged(pattern) },
                        label = {
                            Text(
                                text = pattern.displayName,
                                style = MaterialTheme.typography.labelSmall,
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BassPurple.copy(alpha = 0.2f),
                            selectedLabelColor = BassPurple,
                        ),
                        modifier = Modifier.height(28.dp),
                    )
                }
            }
        }

        // Style
        Column {
            Text(
                text = "Style",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                MusicStyle.entries.forEach { style ->
                    FilterChip(
                        selected = config.style == style,
                        onClick = { onStyleChanged(style) },
                        label = {
                            Text(
                                text = when (style) {
                                    MusicStyle.FUNK -> "Funk"
                                    MusicStyle.LO_FI -> "Lo-fi"
                                    MusicStyle.ROCK -> "Rock"
                                },
                                style = MaterialTheme.typography.labelSmall,
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BassPurple.copy(alpha = 0.2f),
                            selectedLabelColor = BassPurple,
                        ),
                        modifier = Modifier.height(28.dp),
                    )
                }
            }
        }
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
    color: Color,
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
            waveformSamples = floatArrayOf(),
            playbackProgress = 0f,
            drumCurrentStep = -1,
            onTabSelected = {},
            onBpmChanged = {},
            onBarCountChanged = {},
            onBigButtonClick = {},
            onToggleDrumHit = { _, _ -> },
            onLoadDrumPreset = {},
            onClearDrumPattern = {},
            onDrumModeChanged = {},
            onDrumPadTap = {},
            onBassKeyChanged = {},
            onBassPatternChanged = {},
            onBassStyleChanged = {},
            onBassToggle = {},
            onClearSession = {},
        )
    }
}
