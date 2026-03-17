package com.jampad.data.detection

import com.jampad.domain.model.DetectedKey
import com.jampad.domain.model.MusicalKey
import com.jampad.domain.model.ScaleMode
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.ln
import kotlin.math.roundToInt
import kotlin.math.sqrt

@Singleton
class KeyEstimator @Inject constructor() {

    // Krumhansl-Kessler key profiles
    private val majorProfile = doubleArrayOf(
        6.35, 2.23, 3.48, 2.33, 4.38, 4.09, 2.52, 5.19, 2.39, 3.66, 2.29, 2.88,
    )
    private val minorProfile = doubleArrayOf(
        6.33, 2.68, 3.52, 5.38, 2.60, 3.53, 2.54, 4.75, 3.98, 2.69, 3.34, 3.17,
    )

    fun estimate(frequencies: List<Float>): DetectedKey? {
        if (frequencies.size < 3) return null

        // Build pitch class histogram (0–11, where C=0)
        val histogram = DoubleArray(12)
        for (freq in frequencies) {
            val pitchClass = frequencyToPitchClass(freq)
            if (pitchClass in 0..11) {
                histogram[pitchClass] += 1.0
            }
        }

        var bestCorrelation = Double.MIN_VALUE
        var bestRoot = 0
        var bestMode = ScaleMode.MAJOR

        for (root in 0..11) {
            // Rotate histogram so root is at index 0
            val rotated = DoubleArray(12) { histogram[(it + root) % 12] }

            val majorCorr = pearsonCorrelation(rotated, majorProfile)
            if (majorCorr > bestCorrelation) {
                bestCorrelation = majorCorr
                bestRoot = root
                bestMode = ScaleMode.MAJOR
            }

            val minorCorr = pearsonCorrelation(rotated, minorProfile)
            if (minorCorr > bestCorrelation) {
                bestCorrelation = minorCorr
                bestRoot = root
                bestMode = ScaleMode.MINOR
            }
        }

        val keys = MusicalKey.entries
        val rootKey = keys[bestRoot % keys.size]
        return DetectedKey(rootKey, bestMode)
    }

    private fun frequencyToPitchClass(freq: Float): Int {
        // MIDI 69 = A4 = 440 Hz → pitch class 9 (A)
        val midi = 69.0 + 12.0 * ln(freq.toDouble() / 440.0) / ln(2.0)
        return ((midi.roundToInt() % 12) + 12) % 12
    }

    private fun pearsonCorrelation(x: DoubleArray, y: DoubleArray): Double {
        val n = x.size
        val meanX = x.average()
        val meanY = y.average()
        var num = 0.0
        var denX = 0.0
        var denY = 0.0
        for (i in 0 until n) {
            val dx = x[i] - meanX
            val dy = y[i] - meanY
            num += dx * dy
            denX += dx * dx
            denY += dy * dy
        }
        val den = sqrt(denX * denY)
        return if (den > 0.0) num / den else 0.0
    }
}
