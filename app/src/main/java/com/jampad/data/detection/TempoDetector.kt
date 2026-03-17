package com.jampad.data.detection

import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

data class TempoResult(val bpm: Int, val confidence: Float)

@Singleton
class TempoDetector @Inject constructor() {

    companion object {
        private const val WINDOW_SIZE = 1024
        private const val HOP_SIZE = 512
        private const val MIN_BPM = 40
        private const val MAX_BPM = 300
    }

    fun detect(samples: ShortArray, sampleRate: Int): TempoResult? {
        if (samples.size < WINDOW_SIZE * 4) return null

        // Step 1: Compute RMS energy in windows
        val energies = computeEnergies(samples)
        if (energies.size < 4) return null

        // Step 2: Half-wave rectify first derivative (onset signal)
        val onsetSignal = computeOnsetSignal(energies)
        if (onsetSignal.size < 4) return null

        // Step 3: Autocorrelate
        val autocorrelation = autocorrelate(onsetSignal)

        // Step 4: Find peak in BPM range
        val framesPerSecond = sampleRate.toDouble() / HOP_SIZE
        val minLag = (framesPerSecond * 60.0 / MAX_BPM).toInt().coerceAtLeast(1)
        val maxLag = (framesPerSecond * 60.0 / MIN_BPM).toInt().coerceAtMost(autocorrelation.size - 1)

        if (minLag >= maxLag || maxLag >= autocorrelation.size) return null

        var bestLag = minLag
        var bestVal = autocorrelation[minLag]
        for (lag in minLag..maxLag) {
            if (autocorrelation[lag] > bestVal) {
                bestVal = autocorrelation[lag]
                bestLag = lag
            }
        }

        val bpm = (framesPerSecond * 60.0 / bestLag).toInt()

        // Confidence: ratio of peak to average
        val avgCorr = autocorrelation.slice(minLag..maxLag).average().toFloat()
        val confidence = if (avgCorr > 0f) (bestVal / avgCorr).coerceIn(0f, 1f) else 0f

        // Handle octave errors: check double-tempo (half lag)
        var finalBpm = bpm
        val doubleLag = bestLag / 2
        if (doubleLag in minLag..maxLag && autocorrelation[doubleLag] > bestVal * 0.8f) {
            val doubleBpm = (framesPerSecond * 60.0 / doubleLag).toInt()
            if (doubleBpm in MIN_BPM..MAX_BPM) {
                finalBpm = doubleBpm
            }
        }

        return if (finalBpm in MIN_BPM..MAX_BPM) {
            TempoResult(finalBpm, confidence)
        } else {
            null
        }
    }

    private fun computeEnergies(samples: ShortArray): FloatArray {
        val numWindows = (samples.size - WINDOW_SIZE) / HOP_SIZE + 1
        if (numWindows <= 0) return floatArrayOf()

        return FloatArray(numWindows) { w ->
            val start = w * HOP_SIZE
            var sum = 0.0
            for (i in start until (start + WINDOW_SIZE).coerceAtMost(samples.size)) {
                val s = samples[i].toDouble() / Short.MAX_VALUE
                sum += s * s
            }
            sqrt(sum / WINDOW_SIZE).toFloat()
        }
    }

    private fun computeOnsetSignal(energies: FloatArray): FloatArray {
        if (energies.size < 2) return floatArrayOf()
        return FloatArray(energies.size - 1) { i ->
            val diff = energies[i + 1] - energies[i]
            if (diff > 0f) diff else 0f
        }
    }

    private fun autocorrelate(signal: FloatArray): FloatArray {
        val n = signal.size
        val result = FloatArray(n)
        for (lag in 0 until n) {
            var sum = 0f
            for (i in 0 until n - lag) {
                sum += signal[i] * signal[i + lag]
            }
            result[lag] = sum
        }
        if (result[0] > 0f) {
            val norm = result[0]
            for (i in result.indices) {
                result[i] /= norm
            }
        }
        return result
    }
}
