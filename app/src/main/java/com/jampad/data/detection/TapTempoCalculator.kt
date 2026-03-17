package com.jampad.data.detection

sealed class TapTempoResult {
    data object Incomplete : TapTempoResult()
    data class Detected(val bpm: Int, val tapCount: Int) : TapTempoResult()
}

class TapTempoCalculator {

    private val timestamps = mutableListOf<Long>()
    private val maxTaps = 8
    private val timeoutMs = 2000L

    fun tap(): TapTempoResult {
        val now = System.currentTimeMillis()

        // Reset if too long since last tap
        if (timestamps.isNotEmpty() && now - timestamps.last() > timeoutMs) {
            timestamps.clear()
        }

        timestamps.add(now)
        if (timestamps.size > maxTaps) {
            timestamps.removeAt(0)
        }

        if (timestamps.size < 2) return TapTempoResult.Incomplete

        val intervals = (1 until timestamps.size).map { i ->
            timestamps[i] - timestamps[i - 1]
        }
        val avgIntervalMs = intervals.average()
        val bpm = (60_000.0 / avgIntervalMs).toInt().coerceIn(40, 300)

        return TapTempoResult.Detected(bpm, timestamps.size)
    }

    fun reset() {
        timestamps.clear()
    }
}
