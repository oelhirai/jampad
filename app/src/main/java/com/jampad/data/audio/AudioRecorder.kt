package com.jampad.data.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioRecorder @Inject constructor() {

    companion object {
        const val SAMPLE_RATE = 44100
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT

        val BUFFER_SIZE: Int = AudioRecord.getMinBufferSize(
            SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT
        ).coerceAtLeast(4096)
    }

    private var audioRecord: AudioRecord? = null
    private var isRecording = false

    @SuppressLint("MissingPermission")
    fun start(): Boolean {
        if (isRecording) return false
        return try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                BUFFER_SIZE,
            ).also {
                if (it.state != AudioRecord.STATE_INITIALIZED) {
                    it.release()
                    return false
                }
                it.startRecording()
            }
            isRecording = true
            true
        } catch (e: Exception) {
            false
        }
    }

    fun stop() {
        isRecording = false
        audioRecord?.let {
            try {
                it.stop()
            } catch (_: IllegalStateException) { }
            it.release()
        }
        audioRecord = null
    }

    suspend fun readIntoBuffer(
        onSamples: (ShortArray, Int) -> Unit,
    ) = withContext(Dispatchers.IO) {
        val buffer = ShortArray(BUFFER_SIZE / 2)
        while (isActive && isRecording) {
            val read = audioRecord?.read(buffer, 0, buffer.size) ?: -1
            if (read > 0) {
                onSamples(buffer, read)
            }
        }
    }

    fun isRecording(): Boolean = isRecording
}
