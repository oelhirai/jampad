package com.jampad.presentation.jam

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jampad.data.audio.LoopEngine
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
) : ViewModel() {

    private val _uiState = MutableStateFlow(JamUiState())
    val uiState: StateFlow<JamUiState> = _uiState.asStateFlow()

    val waveformSamples: StateFlow<FloatArray> = loopEngine.waveformSamples
    val playbackProgress: StateFlow<Float> = loopEngine.playbackProgress

    init {
        viewModelScope.launch {
            loopEngine.loopState.collect { state ->
                _uiState.update { it.copy(loopState = state) }
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
    }

    override fun onCleared() {
        super.onCleared()
        loopEngine.clearSession()
    }
}
