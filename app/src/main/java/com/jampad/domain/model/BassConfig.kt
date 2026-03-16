package com.jampad.domain.model

data class BassConfig(
    val key: MusicalKey = MusicalKey.A,
    val pattern: BassPatternType = BassPatternType.ROOT,
    val style: MusicStyle = MusicStyle.FUNK,
    val enabled: Boolean = false,
)

enum class BassPatternType(val displayName: String) {
    ROOT("Root"),
    ROOT_FIFTH("Root-Fifth"),
    OCTAVE("Octave"),
    WALKING("Walking"),
    FUNKY("Funky"),
}
