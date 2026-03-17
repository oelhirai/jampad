package com.jampad.presentation.jam

import com.jampad.domain.model.BassConfig
import com.jampad.domain.model.BpmSource
import com.jampad.domain.model.DetectedKey
import com.jampad.domain.model.DrumPattern
import com.jampad.domain.model.LoopState
import com.jampad.domain.model.MixState
import com.jampad.domain.model.MusicStyle
import com.jampad.domain.model.RecordingMode

data class JamUiState(
    val loopState: LoopState = LoopState.EMPTY,
    val bpm: Int = 120,
    val barCount: Int = 4,
    val mixState: MixState = MixState(),
    val selectedTab: JamTab = JamTab.GUITAR,
    val hasAudioPermission: Boolean = false,
    val drumPattern: DrumPattern = DrumPattern.empty(),
    val drumPreset: MusicStyle? = null,
    val drumMode: DrumMode = DrumMode.SEQUENCER,
    val bassConfig: BassConfig = BassConfig(),
    val recordingMode: RecordingMode = RecordingMode.FREE,
    val bpmSource: BpmSource = BpmSource.NONE,
    val detectedKey: DetectedKey? = null,
    val isDetecting: Boolean = false,
    val loopLengthSamples: Int = 0,
) {
    val beatCount: Int
        get() = when {
            bpmSource == BpmSource.NONE -> 0
            recordingMode == RecordingMode.FIXED -> barCount * 4
            loopLengthSamples > 0 -> {
                val durationSec = loopLengthSamples.toDouble() / 44100
                (durationSec * bpm / 60.0).toInt().coerceAtLeast(1)
            }
            else -> barCount * 4
        }
}

enum class JamTab {
    GUITAR,
    DRUMS,
    BASS,
}

enum class DrumMode {
    SEQUENCER,
    PADS,
}
