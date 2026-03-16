package com.jampad.presentation.jam

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jampad.data.audio.DrumEngine
import com.jampad.data.audio.LoopEngine
import com.jampad.domain.model.DrumInstrument
import com.jampad.domain.model.DrumPattern
import com.jampad.domain.model.DrumPresets
import com.jampad.domain.model.LoopState
import com.jampad.domain.model.MusicStyle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class JamViewModel @Inject constructor(
    private val loopEngine: LoopEngine,
    private val drumEngine: DrumEngine,
) : ViewModel() {

    private val _uiState = MutableStateFlow(JamUiState())
    val uiState: StateFlow<JamUiState> = _uiState.asStateFlow()

    val waveformSamples: StateFlow<FloatArray> = loopEngine.waveformSamples
    val playbackProgress: StateFlow<Float> = loopEngine.playbackProgress
    val drumCurrentStep: StateFlow<Int> = drumEngine.currentStep

    init {
        viewModelScope.launch {
            loopEngine.loopState.collect { state ->
                _uiState.update { it.copy(loopState = state) }
                // Start/stop drum engine in sync with loop
                when (state) {
                    LoopState.LOOPING, LoopState.OVERDUBBING -> {
                        val ui = _uiState.value
                        if (ui.drumPattern.hits.values.any { hits -> hits.any { it } }) {
                            drumEngine.pattern = ui.drumPattern
                            drumEngine.start(ui.bpm, ui.barCount)
                        }
                    }
                    LoopState.EMPTY -> drumEngine.stop()
                    else -> { }
                }
            }
        }
    }

    fun onTabSelected(tab: JamTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun onBpmChanged(bpm: Int) {
        val clamped = bpm.coerceIn(40, 300)
        _uiState.update { it.copy(bpm = clamped) }
        loopEngine.updateTempo(clamped, _uiState.value.barCount)
    }

    fun onBarCountChanged(barCount: Int) {
        _uiState.update { it.copy(barCount = barCount) }
        loopEngine.updateTempo(_uiState.value.bpm, barCount)
    }

    fun onAudioPermissionResult(granted: Boolean) {
        _uiState.update { it.copy(hasAudioPermission = granted) }
    }

    fun onBigButtonPress() {
        loopEngine.updateTempo(_uiState.value.bpm, _uiState.value.barCount)
        loopEngine.onBigButtonPress()
    }

    fun onClearSession() {
        loopEngine.clearSession()
        drumEngine.stop()
        _uiState.update {
            it.copy(
                drumPattern = DrumPattern.empty(),
                drumPreset = null,
            )
        }
    }

    // Drum controls
    fun onToggleDrumHit(instrument: DrumInstrument, step: Int) {
        val newPattern = _uiState.value.drumPattern.toggleHit(instrument, step)
        _uiState.update { it.copy(drumPattern = newPattern, drumPreset = null) }
        drumEngine.pattern = newPattern
        // Preview the sample when toggling on
        val hits = newPattern.hits[instrument]
        if (hits != null && step < hits.size && hits[step]) {
            drumEngine.previewSample(instrument)
        }
        // If loop is playing and drums weren't running, start them
        if (_uiState.value.loopState == LoopState.LOOPING) {
            drumEngine.pattern = newPattern
            if (drumEngine.currentStep.value == -1) {
                drumEngine.start(_uiState.value.bpm, _uiState.value.barCount)
            }
        }
    }

    fun onLoadDrumPreset(style: MusicStyle) {
        val stepsPerBar = _uiState.value.drumPattern.stepsPerBar
        val preset = when (style) {
            MusicStyle.FUNK -> DrumPresets.funk(stepsPerBar)
            MusicStyle.LO_FI -> DrumPresets.loFi(stepsPerBar)
            MusicStyle.ROCK -> DrumPresets.rock(stepsPerBar)
        }
        _uiState.update { it.copy(drumPattern = preset, drumPreset = style) }
        drumEngine.pattern = preset
        if (_uiState.value.loopState == LoopState.LOOPING) {
            if (drumEngine.currentStep.value == -1) {
                drumEngine.start(_uiState.value.bpm, _uiState.value.barCount)
            }
        }
    }

    fun onClearDrumPattern() {
        val cleared = _uiState.value.drumPattern.clear()
        _uiState.update { it.copy(drumPattern = cleared, drumPreset = null) }
        drumEngine.pattern = cleared
    }

    fun onDrumModeChanged(mode: DrumMode) {
        _uiState.update { it.copy(drumMode = mode) }
    }

    override fun onCleared() {
        super.onCleared()
        loopEngine.clearSession()
        drumEngine.stop()
    }
}
