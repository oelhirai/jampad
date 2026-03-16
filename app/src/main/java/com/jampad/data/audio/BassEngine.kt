package com.jampad.data.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.jampad.domain.model.BassConfig
import com.jampad.domain.model.BassPatternType
import com.jampad.domain.model.MusicalKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

@Singleton
class BassEngine @Inject constructor() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var playJob: Job? = null
    private var audioTrack: AudioTrack? = null
    private var isPlaying = false

    @Volatile
    var config: BassConfig = BassConfig()

    fun start(bpm: Int) {
        if (isPlaying) return
        isPlaying = true

        val bufferSize = AudioTrack.getMinBufferSize(
            AudioRecorder.SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        ).coerceAtLeast(4096)

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(AudioRecorder.SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
            .also { it.play() }

        playJob = scope.launch {
            playLoop(bpm)
        }
    }

    private suspend fun playLoop(bpm: Int) {
        while (isPlaying) {
            val currentConfig = config
            if (!currentConfig.enabled) {
                // Write silence
                val silenceDuration = (AudioRecorder.SAMPLE_RATE * 0.1).toInt()
                audioTrack?.write(ShortArray(silenceDuration), 0, silenceDuration)
                continue
            }

            val pattern = generatePattern(currentConfig, bpm)
            for (noteSamples in pattern) {
                if (!isPlaying) break
                audioTrack?.write(noteSamples, 0, noteSamples.size)
            }
        }
    }

    private fun generatePattern(config: BassConfig, bpm: Int): List<ShortArray> {
        val baseFreq = getFrequency(config.key)
        val eighthNoteDuration = 60.0 / bpm / 2.0
        val samplesPerEighth = (AudioRecorder.SAMPLE_RATE * eighthNoteDuration).toInt()

        val intervals = when (config.pattern) {
            BassPatternType.ROOT -> listOf(0, -1, 0, -1, 0, -1, 0, -1)
            BassPatternType.ROOT_FIFTH -> listOf(0, -1, 7, -1, 0, -1, 7, -1)
            BassPatternType.OCTAVE -> listOf(0, -1, 12, -1, 0, -1, 12, -1)
            BassPatternType.WALKING -> listOf(0, 2, 4, 5, 7, 5, 4, 2)
            BassPatternType.FUNKY -> listOf(0, 0, -1, 7, -1, 5, 0, -1)
        }

        return intervals.map { interval ->
            if (interval == -1) {
                ShortArray(samplesPerEighth) // silence / rest
            } else {
                generateBassNote(baseFreq * semitoneRatio(interval), samplesPerEighth)
            }
        }
    }

    private fun generateBassNote(freq: Double, numSamples: Int): ShortArray {
        val samples = ShortArray(numSamples)
        for (i in samples.indices) {
            val t = i.toDouble() / AudioRecorder.SAMPLE_RATE
            // Envelope: attack + sustain + release
            val attack = if (t < 0.01) t / 0.01 else 1.0
            val release = if (i > numSamples - 500) (numSamples - i).toDouble() / 500 else 1.0
            val envelope = attack * release

            // Bass tone: fundamental + slight sub-octave
            val fundamental = sin(2.0 * PI * freq * t)
            val sub = sin(2.0 * PI * freq * 0.5 * t) * 0.3
            val value = (fundamental + sub) * envelope * 0.6

            samples[i] = (value * Short.MAX_VALUE)
                .toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                .toShort()
        }
        return samples
    }

    private fun getFrequency(key: MusicalKey): Double {
        // Bass frequencies (octave 2)
        val a2 = 110.0 // A2
        val semitones = when (key) {
            MusicalKey.C -> -9
            MusicalKey.C_SHARP -> -8
            MusicalKey.D -> -7
            MusicalKey.D_SHARP -> -6
            MusicalKey.E -> -5
            MusicalKey.F -> -4
            MusicalKey.F_SHARP -> -3
            MusicalKey.G -> -2
            MusicalKey.G_SHARP -> -1
            MusicalKey.A -> 0
            MusicalKey.A_SHARP -> 1
            MusicalKey.B -> 2
        }
        return a2 * semitoneRatio(semitones)
    }

    private fun semitoneRatio(semitones: Int): Double {
        return Math.pow(2.0, semitones.toDouble() / 12.0)
    }

    fun stop() {
        isPlaying = false
        playJob?.cancel()
        playJob = null
        audioTrack?.let {
            try { it.stop() } catch (_: IllegalStateException) { }
            it.release()
        }
        audioTrack = null
    }

    fun destroy() {
        stop()
        scope.cancel()
    }
}
