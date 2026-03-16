package com.jampad.presentation.jam

import com.jampad.domain.model.LoopState
import com.jampad.domain.model.MixState

data class JamUiState(
    val loopState: LoopState = LoopState.EMPTY,
    val bpm: Int = 120,
    val barCount: Int = 4,
    val mixState: MixState = MixState(),
    val selectedTab: JamTab = JamTab.GUITAR,
    val hasAudioPermission: Boolean = false,
)

enum class JamTab {
    GUITAR,
    DRUMS,
    BASS,
}
