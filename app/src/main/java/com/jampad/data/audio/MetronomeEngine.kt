package com.jampad.data.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
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
import kotlin.math.sin

@Singleton
class MetronomeEngine @Inject constructor() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var audioTrack: AudioTrack? = null
    private var clickJob: Job? = null
    private var isRunning = false

    private val downbeatClick: ShortArray = generateClick(frequency = 1500.0, durationMs = 15)
    private val upbeatClick: ShortArray = generateClick(frequency = 1000.0, durationMs = 10)

    fun start(bpm: Int) {
        stop()
        val sampleRate = AudioRecorder.SAMPLE_RATE

        val bufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        ).coerceAtLeast(4096)

        audioTrack = try {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
                .also { it.play() }
        } catch (e: Exception) {
            return
        }

        isRunning = true
        clickJob = scope.launch(Dispatchers.IO) {
            val samplesPerBeat = (sampleRate * 60.0 / bpm).toInt()
            var beatIndex = 0
            while (isActive && isRunning) {
                val click = if (beatIndex % 4 == 0) downbeatClick else upbeatClick
                audioTrack?.write(click, 0, click.size)

                val silenceCount = (samplesPerBeat - click.size).coerceAtLeast(0)
                if (silenceCount > 0) {
                    // Write silence in chunks to stay responsive to cancellation
                    val silence = ShortArray(minOf(silenceCount, 4096))
                    var written = 0
                    while (isActive && isRunning && written < silenceCount) {
                        val toWrite = minOf(silence.size, silenceCount - written)
                        audioTrack?.write(silence, 0, toWrite)
                        written += toWrite
                    }
                }
                beatIndex++
            }
        }
    }

    fun stop() {
        isRunning = false
        clickJob?.cancel()
        clickJob = null
        audioTrack?.let {
            try {
                it.stop()
            } catch (_: IllegalStateException) { }
            it.release()
        }
        audioTrack = null
    }

    fun destroy() {
        stop()
        scope.cancel()
    }

    companion object {
        private fun generateClick(frequency: Double, durationMs: Int): ShortArray {
            val sampleRate = AudioRecorder.SAMPLE_RATE
            val numSamples = sampleRate * durationMs / 1000
            return ShortArray(numSamples) { i ->
                val t = i.toDouble() / sampleRate
                val envelope = 1.0 - (i.toDouble() / numSamples) // linear decay
                val sample = sin(2 * PI * frequency * t) * envelope * 0.7
                (sample * Short.MAX_VALUE).toInt()
                    .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                    .toShort()
            }
        }
    }
}
