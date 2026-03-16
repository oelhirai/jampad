package com.jampad.presentation.jam

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jampad.data.audio.BassEngine
import com.jampad.data.audio.DrumEngine
import com.jampad.data.audio.LoopEngine
import com.jampad.domain.model.BassConfig
import com.jampad.domain.model.BassPatternType
import com.jampad.domain.model.DrumInstrument
import com.jampad.domain.model.DrumPattern
import com.jampad.domain.model.DrumPresets
import com.jampad.domain.model.LoopState
import com.jampad.domain.model.MusicalKey
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
    private val bassEngine: BassEngine,
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
                val ui = _uiState.value
                when (state) {
                    LoopState.LOOPING, LoopState.OVERDUBBING -> {
                        if (ui.drumPattern.hits.values.any { hits -> hits.any { it } }) {
                            drumEngine.pattern = ui.drumPattern
                            drumEngine.start(ui.bpm, ui.barCount)
                        }
                        if (ui.bassConfig.enabled) {
                            bassEngine.config = ui.bassConfig
                            bassEngine.start(ui.bpm)
                        }
                    }
                    LoopState.EMPTY -> {
                        drumEngine.stop()
                        bassEngine.stop()
                    }
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
        bassEngine.stop()
        _uiState.update {
            it.copy(
                drumPattern = DrumPattern.empty(),
                drumPreset = null,
                bassConfig = BassConfig(),
            )
        }
    }

    // Drum controls
    fun onToggleDrumHit(instrument: DrumInstrument, step: Int) {
        val newPattern = _uiState.value.drumPattern.toggleHit(instrument, step)
        _uiState.update { it.copy(drumPattern = newPattern, drumPreset = null) }
        drumEngine.pattern = newPattern
        val hits = newPattern.hits[instrument]
        if (hits != null && step < hits.size && hits[step]) {
            drumEngine.previewSample(instrument)
        }
        if (_uiState.value.loopState == LoopState.LOOPING && drumEngine.currentStep.value == -1) {
            drumEngine.start(_uiState.value.bpm, _uiState.value.barCount)
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
        if (_uiState.value.loopState == LoopState.LOOPING && drumEngine.currentStep.value == -1) {
            drumEngine.start(_uiState.value.bpm, _uiState.value.barCount)
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

    fun onDrumPadTap(instrument: DrumInstrument) {
        drumEngine.previewSample(instrument)
        // If looping, record the hit at the current step position
        if (_uiState.value.loopState == LoopState.LOOPING || _uiState.value.loopState == LoopState.OVERDUBBING) {
            val currentStep = drumEngine.currentStep.value
            if (currentStep >= 0) {
                val pattern = _uiState.value.drumPattern
                val hits = pattern.hits[instrument]?.copyOf() ?: BooleanArray(pattern.totalSteps)
                if (currentStep < hits.size) {
                    hits[currentStep] = true
                }
                val newHits = pattern.hits.toMutableMap()
                newHits[instrument] = hits
                val newPattern = pattern.copy(hits = newHits)
                _uiState.update { it.copy(drumPattern = newPattern, drumPreset = null) }
                drumEngine.pattern = newPattern
            }
        }
    }

    // Bass controls
    fun onBassKeyChanged(key: MusicalKey) {
        val newConfig = _uiState.value.bassConfig.copy(key = key)
        _uiState.update { it.copy(bassConfig = newConfig) }
        bassEngine.config = newConfig
    }

    fun onBassPatternChanged(pattern: BassPatternType) {
        val newConfig = _uiState.value.bassConfig.copy(pattern = pattern, enabled = true)
        _uiState.update { it.copy(bassConfig = newConfig) }
        bassEngine.config = newConfig
        if (_uiState.value.loopState == LoopState.LOOPING && !bassEngine.config.enabled) {
            bassEngine.start(_uiState.value.bpm)
        }
    }

    fun onBassStyleChanged(style: MusicStyle) {
        val newConfig = _uiState.value.bassConfig.copy(style = style, enabled = true)
        _uiState.update { it.copy(bassConfig = newConfig) }
        bassEngine.config = newConfig
    }

    fun onBassToggle() {
        val current = _uiState.value.bassConfig
        val newConfig = current.copy(enabled = !current.enabled)
        _uiState.update { it.copy(bassConfig = newConfig) }
        bassEngine.config = newConfig
        if (!newConfig.enabled) {
            bassEngine.stop()
        } else if (_uiState.value.loopState == LoopState.LOOPING) {
            bassEngine.start(_uiState.value.bpm)
        }
    }

    override fun onCleared() {
        super.onCleared()
        loopEngine.clearSession()
        drumEngine.stop()
        bassEngine.stop()
    }
}
