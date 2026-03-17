package com.jampad.domain.model

enum class MusicalKey(val displayName: String) {
    C("C"),
    C_SHARP("C#"),
    D("D"),
    D_SHARP("D#"),
    E("E"),
    F("F"),
    F_SHARP("F#"),
    G("G"),
    G_SHARP("G#"),
    A("A"),
    A_SHARP("A#"),
    B("B"),
}

enum class ScaleMode(val displayName: String) {
    MAJOR("Major"),
    MINOR("Minor"),
}

data class DetectedKey(
    val root: MusicalKey,
    val mode: ScaleMode,
) {
    val displayName: String
        get() = "${root.displayName} ${mode.displayName}"
}
