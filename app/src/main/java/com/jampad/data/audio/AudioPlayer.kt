package com.jampad.data.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioPlayer @Inject constructor() {

    private var audioTrack: AudioTrack? = null
    private var isPlaying = false

    var volume: Float = 0.8f
        set(value) {
            field = value
            audioTrack?.setVolume(value)
        }

    fun start(): Boolean {
        if (isPlaying) return false
        return try {
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
                .also {
                    it.play()
                    it.setVolume(volume)
                }

            isPlaying = true
            true
        } catch (e: Exception) {
            false
        }
    }

    fun stop() {
        isPlaying = false
        audioTrack?.let {
            try {
                it.stop()
            } catch (_: IllegalStateException) { }
            it.release()
        }
        audioTrack = null
    }

    fun writeSamples(samples: ShortArray, offset: Int = 0, count: Int = samples.size) {
        audioTrack?.write(samples, offset, count)
    }

    suspend fun playLoop(
        getLoopSamples: () -> ShortArray?,
        onPositionUpdate: (Int, Int) -> Unit,
    ) = withContext(Dispatchers.IO) {
        val chunkSize = 1024
        while (isActive && isPlaying) {
            val loopData = getLoopSamples() ?: continue
            if (loopData.isEmpty()) continue

            var position = 0
            while (isActive && isPlaying && position < loopData.size) {
                val remaining = loopData.size - position
                val toWrite = minOf(chunkSize, remaining)
                val chunk = loopData.copyOfRange(position, position + toWrite)
                audioTrack?.write(chunk, 0, chunk.size)
                position += toWrite
                onPositionUpdate(position, loopData.size)
            }
        }
    }

    fun isPlaying(): Boolean = isPlaying
}
