package com.jampad.domain.model

data class MixState(
    val guitarVolume: Float = 0.8f,
    val drumsVolume: Float = 0.7f,
    val bassVolume: Float = 0.7f,
    val guitarMuted: Boolean = false,
    val drumsMuted: Boolean = false,
    val bassMuted: Boolean = false,
)
