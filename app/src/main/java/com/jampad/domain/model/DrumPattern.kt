package com.jampad.domain.model

data class DrumPattern(
    val hits: Map<DrumInstrument, BooleanArray>,
    val stepsPerBar: Int = 8,
) {
    val totalSteps: Int get() = stepsPerBar

    fun toggleHit(instrument: DrumInstrument, step: Int): DrumPattern {
        val newHits = hits.toMutableMap()
        val instrumentHits = newHits[instrument]?.copyOf() ?: BooleanArray(totalSteps)
        if (step in instrumentHits.indices) {
            instrumentHits[step] = !instrumentHits[step]
        }
        newHits[instrument] = instrumentHits
        return copy(hits = newHits)
    }

    fun clear(): DrumPattern {
        val newHits = DrumInstrument.entries.associateWith { BooleanArray(totalSteps) }
        return copy(hits = newHits)
    }

    companion object {
        fun empty(stepsPerBar: Int = 8): DrumPattern {
            val hits = DrumInstrument.entries.associateWith { BooleanArray(stepsPerBar) }
            return DrumPattern(hits = hits, stepsPerBar = stepsPerBar)
        }
    }
}

object DrumPresets {
    fun funk(stepsPerBar: Int = 8): DrumPattern {
        val steps = stepsPerBar
        return DrumPattern(
            stepsPerBar = steps,
            hits = mapOf(
                DrumInstrument.KICK to BooleanArray(steps).apply {
                    this[0] = true; this[3] = true; this[6] = true
                },
                DrumInstrument.SNARE to BooleanArray(steps).apply {
                    this[2] = true; this[6] = true
                },
                DrumInstrument.HI_HAT to BooleanArray(steps).apply {
                    for (i in indices) this[i] = true
                },
                DrumInstrument.CLAP to BooleanArray(steps).apply {
                    this[4] = true
                },
            ),
        )
    }

    fun loFi(stepsPerBar: Int = 8): DrumPattern {
        val steps = stepsPerBar
        return DrumPattern(
            stepsPerBar = steps,
            hits = mapOf(
                DrumInstrument.KICK to BooleanArray(steps).apply {
                    this[0] = true; this[4] = true
                },
                DrumInstrument.SNARE to BooleanArray(steps).apply {
                    this[2] = true; this[6] = true
                },
                DrumInstrument.HI_HAT to BooleanArray(steps).apply {
                    this[0] = true; this[2] = true; this[4] = true; this[6] = true
                },
                DrumInstrument.CLAP to BooleanArray(steps),
            ),
        )
    }

    fun rock(stepsPerBar: Int = 8): DrumPattern {
        val steps = stepsPerBar
        return DrumPattern(
            stepsPerBar = steps,
            hits = mapOf(
                DrumInstrument.KICK to BooleanArray(steps).apply {
                    this[0] = true; this[4] = true
                },
                DrumInstrument.SNARE to BooleanArray(steps).apply {
                    this[2] = true; this[6] = true
                },
                DrumInstrument.HI_HAT to BooleanArray(steps).apply {
                    for (i in indices) this[i] = true
                },
                DrumInstrument.CLAP to BooleanArray(steps),
            ),
        )
    }
}
