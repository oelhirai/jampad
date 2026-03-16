package com.jampad.presentation.jam

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class JamViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(JamUiState())
    val uiState: StateFlow<JamUiState> = _uiState.asStateFlow()

    fun onTabSelected(tab: JamTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun onBpmChanged(bpm: Int) {
        _uiState.update { it.copy(bpm = bpm.coerceIn(40, 300)) }
    }

    fun onBarCountChanged(barCount: Int) {
        _uiState.update { it.copy(barCount = barCount) }
    }

    fun onAudioPermissionResult(granted: Boolean) {
        _uiState.update { it.copy(hasAudioPermission = granted) }
    }
}
