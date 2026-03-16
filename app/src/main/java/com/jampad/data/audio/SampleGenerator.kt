package com.jampad.data.audio

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * Generates simple drum samples programmatically.
 * These are placeholder synth sounds — replace with real WAV samples later.
 */
object SampleGenerator {

    private const val SAMPLE_RATE = AudioRecorder.SAMPLE_RATE

    fun generateKick(durationMs: Int = 200): ShortArray {
        val samples = ShortArray((SAMPLE_RATE * durationMs / 1000.0).toInt())
        for (i in samples.indices) {
            val t = i.toDouble() / SAMPLE_RATE
            // Pitch sweep from 150Hz down to 50Hz
            val freq = 150.0 - 100.0 * (t / (durationMs / 1000.0))
            val envelope = exp(-t * 15.0)
            val value = sin(2.0 * PI * freq * t) * envelope * 0.9
            samples[i] = (value * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return samples
    }

    fun generateSnare(durationMs: Int = 150): ShortArray {
        val samples = ShortArray((SAMPLE_RATE * durationMs / 1000.0).toInt())
        val random = Random(42)
        for (i in samples.indices) {
            val t = i.toDouble() / SAMPLE_RATE
            val envelope = exp(-t * 20.0)
            // Mix of tone (200Hz) and noise
            val tone = sin(2.0 * PI * 200.0 * t) * 0.4
            val noise = (random.nextDouble() * 2.0 - 1.0) * 0.6
            val value = (tone + noise) * envelope * 0.8
            samples[i] = (value * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return samples
    }

    fun generateHiHat(durationMs: Int = 80): ShortArray {
        val samples = ShortArray((SAMPLE_RATE * durationMs / 1000.0).toInt())
        val random = Random(123)
        for (i in samples.indices) {
            val t = i.toDouble() / SAMPLE_RATE
            val envelope = exp(-t * 40.0)
            // High-frequency noise
            val noise = (random.nextDouble() * 2.0 - 1.0)
            val value = noise * envelope * 0.5
            samples[i] = (value * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return samples
    }

    fun generateClap(durationMs: Int = 120): ShortArray {
        val samples = ShortArray((SAMPLE_RATE * durationMs / 1000.0).toInt())
        val random = Random(777)
        for (i in samples.indices) {
            val t = i.toDouble() / SAMPLE_RATE
            // Multiple short bursts for clap texture
            val burst1 = if (t < 0.01) 1.0 else 0.0
            val burst2 = if (t in 0.015..0.025) 0.8 else 0.0
            val burst3 = if (t in 0.03..0.04) 0.6 else 0.0
            val tail = exp(-t * 25.0) * 0.5
            val burstEnvelope = burst1 + burst2 + burst3 + tail
            val noise = (random.nextDouble() * 2.0 - 1.0)
            // Bandpass-ish: add some tone
            val tone = sin(2.0 * PI * 1200.0 * t) * 0.3
            val value = (noise * 0.7 + tone) * burstEnvelope * 0.7
            samples[i] = (value * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return samples
    }
}
