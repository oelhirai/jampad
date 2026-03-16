package com.jampad.presentation.jam

import com.jampad.domain.model.DrumPattern
import com.jampad.domain.model.LoopState
import com.jampad.domain.model.MixState
import com.jampad.domain.model.MusicStyle

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
)

enum class JamTab {
    GUITAR,
    DRUMS,
    BASS,
}

enum class DrumMode {
    SEQUENCER,
    PADS,
}
