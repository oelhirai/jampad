package com.jampad.data.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.jampad.domain.model.DrumInstrument
import com.jampad.domain.model.DrumPattern
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DrumEngine @Inject constructor() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var playJob: Job? = null
    private var audioTrack: AudioTrack? = null

    private val samples = mutableMapOf<DrumInstrument, ShortArray>()
    private var isPlaying = false

    private val _currentStep = MutableStateFlow(-1)
    val currentStep: StateFlow<Int> = _currentStep.asStateFlow()

    @Volatile
    var pattern: DrumPattern = DrumPattern.empty()

    init {
        loadSamples()
    }

    private fun loadSamples() {
        samples[DrumInstrument.KICK] = SampleGenerator.generateKick()
        samples[DrumInstrument.SNARE] = SampleGenerator.generateSnare()
        samples[DrumInstrument.HI_HAT] = SampleGenerator.generateHiHat()
        samples[DrumInstrument.CLAP] = SampleGenerator.generateClap()
    }

    fun start(bpm: Int, barCount: Int) {
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
        val stepsPerBar = pattern.stepsPerBar
        // Each step is an 8th note at the given BPM
        // 8th note duration = 60 / bpm / 2 seconds
        val stepDurationMs = (60_000L / bpm / 2)
        val samplesPerStep = (AudioRecorder.SAMPLE_RATE * stepDurationMs / 1000).toInt()

        while (isPlaying) {
            val currentPattern = pattern
            for (step in 0 until currentPattern.totalSteps) {
                if (!isPlaying) break
                _currentStep.value = step

                // Mix all instruments hitting on this step
                val stepBuffer = ShortArray(samplesPerStep)
                for (instrument in DrumInstrument.entries) {
                    val hits = currentPattern.hits[instrument] ?: continue
                    if (step < hits.size && hits[step]) {
                        val sample = samples[instrument] ?: continue
                        for (i in 0 until minOf(sample.size, stepBuffer.size)) {
                            val mixed = stepBuffer[i].toInt() + sample[i].toInt()
                            stepBuffer[i] = mixed.coerceIn(
                                Short.MIN_VALUE.toInt(),
                                Short.MAX_VALUE.toInt()
                            ).toShort()
                        }
                    }
                }

                audioTrack?.write(stepBuffer, 0, stepBuffer.size)
            }
        }
    }

    fun stop() {
        isPlaying = false
        playJob?.cancel()
        playJob = null
        _currentStep.value = -1
        audioTrack?.let {
            try { it.stop() } catch (_: IllegalStateException) { }
            it.release()
        }
        audioTrack = null
    }

    fun previewSample(instrument: DrumInstrument) {
        val sample = samples[instrument] ?: return
        scope.launch(Dispatchers.IO) {
            val bufSize = AudioTrack.getMinBufferSize(
                AudioRecorder.SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
            ).coerceAtLeast(4096)

            val track = AudioTrack.Builder()
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
                .setBufferSizeInBytes(bufSize)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(sample, 0, sample.size)
            track.play()
            delay(300)
            track.release()
        }
    }

    fun destroy() {
        stop()
        scope.cancel()
    }
}
