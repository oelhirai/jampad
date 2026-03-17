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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import com.jampad.domain.model.BpmSource
import com.jampad.domain.model.DetectedKey
import com.jampad.domain.model.DrumInstrument
import com.jampad.domain.model.LoopState
import com.jampad.domain.model.MusicalKey
import com.jampad.domain.model.MusicStyle
import com.jampad.domain.model.RecordingMode
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
        onTapTempo = viewModel::onTapTempo,
        onKeyOverride = viewModel::onKeyOverride,
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
        onGuitarVolumeChange = viewModel::onGuitarVolumeChange,
        onDrumsVolumeChange = viewModel::onDrumsVolumeChange,
        onBassVolumeChange = viewModel::onBassVolumeChange,
        onGuitarMuteToggle = viewModel::onGuitarMuteToggle,
        onDrumsMuteToggle = viewModel::onDrumsMuteToggle,
        onBassMuteToggle = viewModel::onBassMuteToggle,
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
    onTapTempo: () -> Unit,
    onKeyOverride: (MusicalKey) -> Unit,
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
    onGuitarVolumeChange: (Float) -> Unit,
    onDrumsVolumeChange: (Float) -> Unit,
    onBassVolumeChange: (Float) -> Unit,
    onGuitarMuteToggle: () -> Unit,
    onDrumsMuteToggle: () -> Unit,
    onBassMuteToggle: () -> Unit,
    onClearSession: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "JamPad",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                },
                actions = {
                    BpmControl(
                        bpm = uiState.bpm,
                        bpmSource = uiState.bpmSource,
                        onBpmChanged = onBpmChanged,
                        onTapTempo = onTapTempo,
                    )
                    if (uiState.recordingMode == RecordingMode.FIXED) {
                        BarCountChips(
                            selected = uiState.barCount,
                            onSelected = onBarCountChanged,
                        )
                    }
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
                beatCount = uiState.beatCount,
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
                recordingMode = uiState.recordingMode,
                bpm = uiState.bpm,
                bpmSource = uiState.bpmSource,
                detectedKey = uiState.detectedKey,
                isDetecting = uiState.isDetecting,
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
                onKeyOverride = onKeyOverride,
                modifier = Modifier.weight(1f),
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Mini mixer
            MiniMixer(
                mixState = uiState.mixState,
                onGuitarVolumeChange = onGuitarVolumeChange,
                onDrumsVolumeChange = onDrumsVolumeChange,
                onBassVolumeChange = onBassVolumeChange,
                onGuitarMuteToggle = onGuitarMuteToggle,
                onDrumsMuteToggle = onDrumsMuteToggle,
                onBassMuteToggle = onBassMuteToggle,
            )

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
    bpmSource: BpmSource,
    onBpmChanged: (Int) -> Unit,
    onTapTempo: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        // TAP button
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(onClick = onTapTempo)
                .padding(horizontal = 6.dp, vertical = 4.dp),
        ) {
            Text(
                text = "TAP",
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(modifier = Modifier.width(6.dp))

        IconButton(
            onClick = { onBpmChanged(bpm - 1) },
            modifier = Modifier.size(28.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Remove,
                contentDescription = "Decrease BPM",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = if (bpmSource == BpmSource.NONE) "\u2014" else "$bpm",
                style = MaterialTheme.typography.labelLarge,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = MaterialTheme.typography.labelLarge.lineHeight,
            )
            Text(
                text = when (bpmSource) {
                    BpmSource.NONE -> "BPM"
                    BpmSource.AUTO_DETECTED -> "detected"
                    BpmSource.TAP_TEMPO -> "tap"
                    BpmSource.MANUAL -> "manual"
                },
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            )
        }
        IconButton(
            onClick = { onBpmChanged(bpm + 1) },
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
    recordingMode: RecordingMode,
    bpm: Int,
    bpmSource: BpmSource,
    detectedKey: DetectedKey?,
    isDetecting: Boolean,
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
    onKeyOverride: (MusicalKey) -> Unit,
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
                GuitarTabContent(
                    loopState = loopState,
                    recordingMode = recordingMode,
                    bpm = bpm,
                    bpmSource = bpmSource,
                    detectedKey = detectedKey,
                    isDetecting = isDetecting,
                    onKeyOverride = onKeyOverride,
                )
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
private fun GuitarTabContent(
    loopState: LoopState,
    recordingMode: RecordingMode,
    bpm: Int,
    bpmSource: BpmSource,
    detectedKey: DetectedKey?,
    isDetecting: Boolean,
    onKeyOverride: (MusicalKey) -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        when {
            isDetecting -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = GuitarAmber,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Detecting tempo & key...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            loopState == LoopState.EMPTY -> {
                Text(
                    text = if (recordingMode == RecordingMode.FREE) {
                        "Just hit REC and play"
                    } else {
                        "Set tempo with TAP, then REC"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            loopState == LoopState.RECORDING -> {
                Text(
                    text = "Playing your riff...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            detectedKey != null -> {
                GuitarDetectionInfo(
                    detectedKey = detectedKey,
                    bpm = bpm,
                    bpmSource = bpmSource,
                    onKeyOverride = onKeyOverride,
                )
            }
            else -> {
                Text(
                    text = when (loopState) {
                        LoopState.LOOPING -> "Tap OVERDUB to layer more guitar"
                        LoopState.OVERDUBBING -> "Playing over the loop..."
                        else -> ""
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun GuitarDetectionInfo(
    detectedKey: DetectedKey,
    bpm: Int,
    bpmSource: BpmSource,
    onKeyOverride: (MusicalKey) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Key display with edit
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Key: ",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            var keyExpanded by remember { mutableStateOf(false) }
            Box {
                Row(
                    modifier = Modifier.clickable { keyExpanded = true },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = detectedKey.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = GuitarAmber,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit key",
                        modifier = Modifier.size(14.dp),
                        tint = GuitarAmber.copy(alpha = 0.6f),
                    )
                }
                DropdownMenu(
                    expanded = keyExpanded,
                    onDismissRequest = { keyExpanded = false },
                ) {
                    MusicalKey.entries.forEach { key ->
                        DropdownMenuItem(
                            text = { Text(key.displayName) },
                            onClick = {
                                onKeyOverride(key)
                                keyExpanded = false
                            },
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // BPM with source label
        val sourceLabel = when (bpmSource) {
            BpmSource.AUTO_DETECTED -> "(detected)"
            BpmSource.TAP_TEMPO -> "(tap)"
            BpmSource.MANUAL -> "(manual)"
            BpmSource.NONE -> ""
        }
        Text(
            text = "$bpm BPM $sourceLabel",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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
            val keyScrollState = rememberScrollState()
            val selectedKeyIndex = remember(config.key) {
                MusicalKey.entries.indexOf(config.key)
            }
            LaunchedEffect(selectedKeyIndex) {
                // Each chip is ~60dp wide + 4dp gap; scroll to center the selected key
                val chipWidth = 64
                val scrollTo = (selectedKeyIndex * chipWidth - 100).coerceAtLeast(0)
                keyScrollState.animateScrollTo(scrollTo)
            }
            Row(
                modifier = Modifier.horizontalScroll(keyScrollState),
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
private fun MiniMixer(
    mixState: com.jampad.domain.model.MixState,
    onGuitarVolumeChange: (Float) -> Unit,
    onDrumsVolumeChange: (Float) -> Unit,
    onBassVolumeChange: (Float) -> Unit,
    onGuitarMuteToggle: () -> Unit,
    onDrumsMuteToggle: () -> Unit,
    onBassMuteToggle: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MixerSlider(
            label = "Guitar",
            value = mixState.guitarVolume,
            color = GuitarAmber,
            muted = mixState.guitarMuted,
            onValueChange = onGuitarVolumeChange,
            onMuteToggle = onGuitarMuteToggle,
            modifier = Modifier.weight(1f),
        )
        MixerSlider(
            label = "Drums",
            value = mixState.drumsVolume,
            color = DrumsCyan,
            muted = mixState.drumsMuted,
            onValueChange = onDrumsVolumeChange,
            onMuteToggle = onDrumsMuteToggle,
            modifier = Modifier.weight(1f),
        )
        MixerSlider(
            label = "Bass",
            value = mixState.bassVolume,
            color = BassPurple,
            muted = mixState.bassMuted,
            onValueChange = onBassVolumeChange,
            onMuteToggle = onBassMuteToggle,
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
    onMuteToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.alpha(if (muted) 0.4f else 1f),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.clickable(onClick = onMuteToggle),
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            colors = SliderDefaults.colors(
                thumbColor = color,
                activeTrackColor = color,
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
            onTapTempo = {},
            onKeyOverride = {},
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
            onGuitarVolumeChange = {},
            onDrumsVolumeChange = {},
            onBassVolumeChange = {},
            onGuitarMuteToggle = {},
            onDrumsMuteToggle = {},
            onBassMuteToggle = {},
            onClearSession = {},
        )
    }
}
