package com.jampad.presentation.jam

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jampad.data.audio.AudioRecorder
import com.jampad.data.audio.BassEngine
import com.jampad.data.audio.DrumEngine
import com.jampad.data.audio.LoopEngine
import com.jampad.data.detection.PitchDetector
import com.jampad.data.detection.TapTempoCalculator
import com.jampad.data.detection.TapTempoResult
import com.jampad.data.detection.TempoDetector
import com.jampad.domain.model.BassConfig
import com.jampad.domain.model.BassPatternType
import com.jampad.domain.model.BpmSource
import com.jampad.domain.model.DetectedKey
import com.jampad.domain.model.DrumInstrument
import com.jampad.domain.model.DrumPattern
import com.jampad.domain.model.DrumPresets
import com.jampad.domain.model.LoopState
import com.jampad.domain.model.MixState
import com.jampad.domain.model.MusicalKey
import com.jampad.domain.model.MusicStyle
import com.jampad.domain.model.RecordingMode
import com.jampad.domain.model.ScaleMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class JamViewModel @Inject constructor(
    private val loopEngine: LoopEngine,
    private val drumEngine: DrumEngine,
    private val bassEngine: BassEngine,
    private val tempoDetector: TempoDetector,
    private val pitchDetector: PitchDetector,
) : ViewModel() {

    private val _uiState = MutableStateFlow(JamUiState())
    val uiState: StateFlow<JamUiState> = _uiState.asStateFlow()

    val waveformSamples: StateFlow<FloatArray> = loopEngine.waveformSamples
    val playbackProgress: StateFlow<Float> = loopEngine.playbackProgress
    val drumCurrentStep: StateFlow<Int> = drumEngine.currentStep

    private val tapTempoCalculator = TapTempoCalculator()

    init {
        viewModelScope.launch {
            var prevState = LoopState.EMPTY
            loopEngine.loopState.collect { state ->
                _uiState.update { it.copy(loopState = state) }
                val ui = _uiState.value

                when (state) {
                    LoopState.LOOPING, LoopState.OVERDUBBING -> {
                        if (prevState == LoopState.RECORDING && ui.recordingMode == RecordingMode.FREE) {
                            // Free-record: detect BPM and key before starting engines
                            _uiState.update {
                                it.copy(loopLengthSamples = loopEngine.getLoopDurationSamples())
                            }
                            launchDetection()
                        } else {
                            startLayerEngines()
                        }
                    }
                    LoopState.EMPTY -> {
                        drumEngine.stop()
                        bassEngine.stop()
                    }
                    else -> { }
                }
                prevState = state
            }
        }
    }

    fun onTabSelected(tab: JamTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun onBpmChanged(bpm: Int) {
        val clamped = bpm.coerceIn(40, 300)
        _uiState.update { it.copy(bpm = clamped, bpmSource = BpmSource.MANUAL) }
        loopEngine.updateTempo(clamped, _uiState.value.barCount)
        if (_uiState.value.loopState == LoopState.LOOPING ||
            _uiState.value.loopState == LoopState.OVERDUBBING
        ) {
            restartEnginesWithNewBpm(clamped)
        }
    }

    fun onBarCountChanged(barCount: Int) {
        _uiState.update { it.copy(barCount = barCount) }
        loopEngine.updateTempo(_uiState.value.bpm, barCount)
    }

    fun onAudioPermissionResult(granted: Boolean) {
        _uiState.update { it.copy(hasAudioPermission = granted) }
    }

    fun onBigButtonPress() {
        val ui = _uiState.value
        loopEngine.recordingMode = ui.recordingMode
        loopEngine.updateTempo(ui.bpm, ui.barCount)
        loopEngine.onBigButtonPress()
    }

    fun onTapTempo() {
        when (val result = tapTempoCalculator.tap()) {
            is TapTempoResult.Incomplete -> { }
            is TapTempoResult.Detected -> {
                _uiState.update {
                    it.copy(
                        bpm = result.bpm,
                        bpmSource = BpmSource.TAP_TEMPO,
                        recordingMode = RecordingMode.FIXED,
                    )
                }
                loopEngine.updateTempo(result.bpm, _uiState.value.barCount)
            }
        }
    }

    fun onKeyOverride(key: MusicalKey) {
        val currentDetected = _uiState.value.detectedKey
        val newDetectedKey = if (currentDetected != null) {
            currentDetected.copy(root = key)
        } else {
            DetectedKey(key, ScaleMode.MAJOR)
        }
        _uiState.update {
            it.copy(
                detectedKey = newDetectedKey,
                bassConfig = it.bassConfig.copy(key = key),
            )
        }
        bassEngine.config = _uiState.value.bassConfig
    }

    fun onClearSession() {
        loopEngine.clearSession()
        drumEngine.stop()
        bassEngine.stop()
        tapTempoCalculator.reset()
        _uiState.update {
            it.copy(
                drumPattern = DrumPattern.empty(),
                drumPreset = null,
                bassConfig = BassConfig(),
                mixState = MixState(),
                recordingMode = RecordingMode.FREE,
                bpmSource = BpmSource.NONE,
                detectedKey = null,
                isDetecting = false,
                loopLengthSamples = 0,
            )
        }
    }

    private fun launchDetection() {
        viewModelScope.launch {
            _uiState.update { it.copy(isDetecting = true) }
            val audio = loopEngine.getRecordedAudio()
            if (audio == null || audio.isEmpty()) {
                _uiState.update { it.copy(isDetecting = false) }
                return@launch
            }

            // Detect BPM
            val tempoResult = withContext(Dispatchers.Default) {
                tempoDetector.detect(audio, AudioRecorder.SAMPLE_RATE)
            }
            if (tempoResult != null) {
                _uiState.update {
                    it.copy(
                        bpm = tempoResult.bpm,
                        bpmSource = BpmSource.AUTO_DETECTED,
                    )
                }
                loopEngine.updateTempo(tempoResult.bpm, _uiState.value.barCount)
            }

            // Detect pitch/key
            val pitchResult = withContext(Dispatchers.Default) {
                pitchDetector.detect(audio, AudioRecorder.SAMPLE_RATE)
            }
            if (pitchResult.detectedKey != null) {
                _uiState.update {
                    it.copy(
                        detectedKey = pitchResult.detectedKey,
                        bassConfig = it.bassConfig.copy(key = pitchResult.detectedKey.root),
                    )
                }
                bassEngine.config = _uiState.value.bassConfig
            }

            _uiState.update { it.copy(isDetecting = false) }

            // Start layer engines with detected BPM
            startLayerEngines()
        }
    }

    private fun startLayerEngines() {
        val ui = _uiState.value
        val mix = ui.mixState
        applyGuitarVolume(mix.guitarVolume, mix.guitarMuted)
        if (ui.drumPattern.hits.values.any { hits -> hits.any { it } }) {
            drumEngine.pattern = ui.drumPattern
            drumEngine.volume = if (mix.drumsMuted) 0f else mix.drumsVolume
            drumEngine.start(ui.bpm, ui.barCount)
        }
        if (ui.bassConfig.enabled) {
            bassEngine.config = ui.bassConfig
            bassEngine.volume = if (mix.bassMuted) 0f else mix.bassVolume
            bassEngine.start(ui.bpm)
        }
    }

    private fun restartEnginesWithNewBpm(bpm: Int) {
        drumEngine.stop()
        bassEngine.stop()
        val ui = _uiState.value
        val mix = ui.mixState
        if (ui.drumPattern.hits.values.any { hits -> hits.any { it } }) {
            drumEngine.pattern = ui.drumPattern
            drumEngine.volume = if (mix.drumsMuted) 0f else mix.drumsVolume
            drumEngine.start(bpm, ui.barCount)
        }
        if (ui.bassConfig.enabled) {
            bassEngine.config = ui.bassConfig
            bassEngine.volume = if (mix.bassMuted) 0f else mix.bassVolume
            bassEngine.start(bpm)
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
            val mix = _uiState.value.mixState
            drumEngine.volume = if (mix.drumsMuted) 0f else mix.drumsVolume
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
            val mix = _uiState.value.mixState
            drumEngine.volume = if (mix.drumsMuted) 0f else mix.drumsVolume
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
            val mix = _uiState.value.mixState
            bassEngine.volume = if (mix.bassMuted) 0f else mix.bassVolume
            bassEngine.start(_uiState.value.bpm)
        }
    }

    // Mixer controls
    fun onGuitarVolumeChange(volume: Float) {
        _uiState.update { it.copy(mixState = it.mixState.copy(guitarVolume = volume)) }
        applyGuitarVolume(volume, _uiState.value.mixState.guitarMuted)
    }

    fun onDrumsVolumeChange(volume: Float) {
        _uiState.update { it.copy(mixState = it.mixState.copy(drumsVolume = volume)) }
        applyDrumsVolume(volume, _uiState.value.mixState.drumsMuted)
    }

    fun onBassVolumeChange(volume: Float) {
        _uiState.update { it.copy(mixState = it.mixState.copy(bassVolume = volume)) }
        applyBassVolume(volume, _uiState.value.mixState.bassMuted)
    }

    fun onGuitarMuteToggle() {
        val mix = _uiState.value.mixState
        val newMuted = !mix.guitarMuted
        _uiState.update { it.copy(mixState = it.mixState.copy(guitarMuted = newMuted)) }
        applyGuitarVolume(mix.guitarVolume, newMuted)
    }

    fun onDrumsMuteToggle() {
        val mix = _uiState.value.mixState
        val newMuted = !mix.drumsMuted
        _uiState.update { it.copy(mixState = it.mixState.copy(drumsMuted = newMuted)) }
        applyDrumsVolume(mix.drumsVolume, newMuted)
    }

    fun onBassMuteToggle() {
        val mix = _uiState.value.mixState
        val newMuted = !mix.bassMuted
        _uiState.update { it.copy(mixState = it.mixState.copy(bassMuted = newMuted)) }
        applyBassVolume(mix.bassVolume, newMuted)
    }

    private fun applyGuitarVolume(volume: Float, muted: Boolean) {
        loopEngine.setVolume(if (muted) 0f else volume)
    }

    private fun applyDrumsVolume(volume: Float, muted: Boolean) {
        drumEngine.volume = if (muted) 0f else volume
    }

    private fun applyBassVolume(volume: Float, muted: Boolean) {
        bassEngine.volume = if (muted) 0f else volume
    }

    override fun onCleared() {
        super.onCleared()
        loopEngine.clearSession()
        drumEngine.stop()
        bassEngine.stop()
    }
}
