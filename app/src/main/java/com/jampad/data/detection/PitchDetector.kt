package com.jampad.data.detection

import com.jampad.domain.model.DetectedKey
import javax.inject.Inject
import javax.inject.Singleton

data class DetectedPitch(val frequency: Float, val probability: Float)

data class PitchResult(
    val pitches: List<DetectedPitch>,
    val detectedKey: DetectedKey?,
)

@Singleton
class PitchDetector @Inject constructor(
    private val keyEstimator: KeyEstimator,
) {

    companion object {
        private const val WINDOW_SIZE = 2048
        private const val HOP_SIZE = 1024
        private const val YIN_THRESHOLD = 0.15f
        private const val MIN_FREQ = 80f
        private const val MAX_FREQ = 1200f
        private const val MIN_PROBABILITY = 0.7f
    }

    fun detect(samples: ShortArray, sampleRate: Int): PitchResult {
        val floatSamples = FloatArray(samples.size) { samples[it].toFloat() / Short.MAX_VALUE }
        val pitches = mutableListOf<DetectedPitch>()

        var offset = 0
        while (offset + WINDOW_SIZE <= floatSamples.size) {
            val window = floatSamples.copyOfRange(offset, offset + WINDOW_SIZE)
            val result = yinDetect(window, sampleRate)
            if (result != null &&
                result.frequency in MIN_FREQ..MAX_FREQ &&
                result.probability >= MIN_PROBABILITY
            ) {
                pitches.add(result)
            }
            offset += HOP_SIZE
        }

        val detectedKey = if (pitches.size >= 3) {
            keyEstimator.estimate(pitches.map { it.frequency })
        } else {
            null
        }

        return PitchResult(pitches, detectedKey)
    }

    /**
     * YIN algorithm for fundamental frequency estimation.
     * Based on de Cheveigné & Kawahara (2002).
     */
    private fun yinDetect(buffer: FloatArray, sampleRate: Int): DetectedPitch? {
        val halfSize = buffer.size / 2

        // Step 2: Difference function
        val diff = FloatArray(halfSize)
        for (tau in 1 until halfSize) {
            var sum = 0f
            for (j in 0 until halfSize) {
                val d = buffer[j] - buffer[j + tau]
                sum += d * d
            }
            diff[tau] = sum
        }

        // Step 3: Cumulative mean normalized difference function
        val cmndf = FloatArray(halfSize)
        cmndf[0] = 1f
        var runningSum = 0f
        for (tau in 1 until halfSize) {
            runningSum += diff[tau]
            cmndf[tau] = if (runningSum > 0f) diff[tau] * tau / runningSum else 1f
        }

        // Step 4: Absolute threshold — find first dip below threshold
        var tauEstimate = -1
        for (tau in 2 until halfSize) {
            if (cmndf[tau] < YIN_THRESHOLD) {
                // Walk to the local minimum in this dip
                var minTau = tau
                while (minTau + 1 < halfSize && cmndf[minTau + 1] < cmndf[minTau]) {
                    minTau++
                }
                tauEstimate = minTau
                break
            }
        }

        if (tauEstimate == -1) return null

        // Step 5: Parabolic interpolation
        val betterTau = if (tauEstimate > 0 && tauEstimate < halfSize - 1) {
            val s0 = cmndf[tauEstimate - 1]
            val s1 = cmndf[tauEstimate]
            val s2 = cmndf[tauEstimate + 1]
            val adjustment = (s2 - s0) / (2f * (2f * s1 - s2 - s0))
            if (adjustment.isFinite()) tauEstimate + adjustment else tauEstimate.toFloat()
        } else {
            tauEstimate.toFloat()
        }

        val frequency = sampleRate.toFloat() / betterTau
        val probability = 1f - cmndf[tauEstimate]

        return if (frequency in MIN_FREQ..MAX_FREQ) {
            DetectedPitch(frequency, probability.coerceIn(0f, 1f))
        } else {
            null
        }
    }
}
