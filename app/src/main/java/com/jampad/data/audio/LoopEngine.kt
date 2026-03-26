package com.jampad.data.audio

import com.jampad.domain.model.LoopState
import com.jampad.domain.model.RecordingMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LoopEngine @Inject constructor(
    private val recorder: AudioRecorder,
    private val player: AudioPlayer,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _loopState = MutableStateFlow(LoopState.EMPTY)
    val loopState: StateFlow<LoopState> = _loopState.asStateFlow()

    private val _playbackProgress = MutableStateFlow(0f)
    val playbackProgress: StateFlow<Float> = _playbackProgress.asStateFlow()

    private val _waveformSamples = MutableStateFlow(floatArrayOf())
    val waveformSamples: StateFlow<FloatArray> = _waveformSamples.asStateFlow()

    private val _fullRecordingWaveform = MutableStateFlow(floatArrayOf())
    val fullRecordingWaveform: StateFlow<FloatArray> = _fullRecordingWaveform.asStateFlow()

    private var loopBuffer: ShortArray? = null
    private var fullRecording: ShortArray? = null
    private var recordingBuffer = mutableListOf<Short>()
    private var recordJob: Job? = null
    private var playJob: Job? = null

    var recordingMode: RecordingMode = RecordingMode.FREE

    private var bpm: Int = 120
    private var barCount: Int = 4

    fun updateTempo(bpm: Int, barCount: Int) {
        this.bpm = bpm
        this.barCount = barCount
    }

    fun getLoopLengthSamples(): Int {
        val beatsPerBar = 4 // 4/4 time
        val totalBeats = beatsPerBar * barCount
        val secondsPerBeat = 60.0 / bpm
        val totalSeconds = totalBeats * secondsPerBeat
        return (totalSeconds * AudioRecorder.SAMPLE_RATE).toInt()
    }

    fun getWindowLengthSamples(bpm: Int, barCount: Int): Int {
        val totalBeats = 4 * barCount
        val totalSeconds = totalBeats * 60.0 / bpm
        return (totalSeconds * AudioRecorder.SAMPLE_RATE).toInt()
    }

    fun onBigButtonPress() {
        when (_loopState.value) {
            LoopState.EMPTY -> startRecording()
            LoopState.RECORDING -> stopRecordingAndLoop()
            LoopState.LOOPING -> startOverdub()
            LoopState.OVERDUBBING -> stopOverdub()
        }
    }

    private fun startRecording() {
        if (!recorder.start()) return
        _loopState.value = LoopState.RECORDING
        recordingBuffer.clear()

        recordJob = scope.launch {
            recorder.readIntoBuffer { samples, count ->
                synchronized(recordingBuffer) {
                    for (i in 0 until count) {
                        recordingBuffer.add(samples[i])
                    }
                }
                updateLiveWaveform()
            }
        }
    }

    private fun stopRecordingAndLoop() {
        recorder.stop()
        recordJob?.cancel()
        recordJob = null

        synchronized(recordingBuffer) {
            // Always save the full recording for align-to-grid
            val raw = recordingBuffer.toShortArray()
            fullRecording = raw
            _fullRecordingWaveform.value = downsampleToFloats(raw, 400)

            loopBuffer = when (recordingMode) {
                RecordingMode.FREE -> {
                    raw.copyOf()
                }
                RecordingMode.FIXED -> {
                    val targetLength = getLoopLengthSamples()
                    if (raw.size >= targetLength) {
                        raw.copyOfRange(0, targetLength)
                    } else {
                        val padded = ShortArray(targetLength)
                        raw.copyInto(padded)
                        padded
                    }
                }
            }
        }

        updateWaveformFromLoop()
        _loopState.value = LoopState.LOOPING
        startPlayback()
    }

    private fun startPlayback() {
        if (!player.start()) return

        playJob = scope.launch {
            player.playLoop(
                getLoopSamples = { loopBuffer },
                onPositionUpdate = { position, total ->
                    if (total > 0) {
                        _playbackProgress.value = position.toFloat() / total
                    }
                }
            )
        }
    }

    private fun startOverdub() {
        if (!recorder.start()) return
        _loopState.value = LoopState.OVERDUBBING
        recordingBuffer.clear()

        recordJob = scope.launch {
            recorder.readIntoBuffer { samples, count ->
                synchronized(recordingBuffer) {
                    for (i in 0 until count) {
                        recordingBuffer.add(samples[i])
                    }
                }
            }
        }
    }

    private fun stopOverdub() {
        recorder.stop()
        recordJob?.cancel()
        recordJob = null

        val existing = loopBuffer ?: return
        synchronized(recordingBuffer) {
            val overdub = recordingBuffer.toShortArray()
            // Mix overdub into existing loop
            for (i in existing.indices) {
                if (i < overdub.size) {
                    val mixed = existing[i].toInt() + overdub[i].toInt()
                    existing[i] = mixed.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }
            }
        }

        updateWaveformFromLoop()
        _loopState.value = LoopState.LOOPING
    }

    fun setVolume(volume: Float) {
        player.volume = volume
    }

    fun getRecordedAudio(): ShortArray? = loopBuffer?.copyOf()

    fun getLoopDurationSamples(): Int = loopBuffer?.size ?: 0

    fun getFullRecordingLengthSamples(): Int = fullRecording?.size ?: 0

    fun alignLoop(offsetFraction: Float, bpm: Int, barCount: Int) {
        val full = fullRecording ?: return
        val windowLength = getWindowLengthSamples(bpm, barCount)
        if (windowLength > full.size) return

        val scrollableRange = full.size - windowLength
        val startSample = (offsetFraction * scrollableRange).toInt().coerceIn(0, scrollableRange)
        val endSample = startSample + windowLength

        // Stop playback, replace buffer, restart
        player.stop()
        playJob?.cancel()
        playJob = null

        loopBuffer = full.copyOfRange(startSample, endSample)
        updateWaveformFromLoop()
        startPlayback()
    }

    fun clearSession() {
        recorder.stop()
        player.stop()
        recordJob?.cancel()
        playJob?.cancel()
        recordJob = null
        playJob = null
        loopBuffer = null
        fullRecording = null
        recordingBuffer.clear()
        recordingMode = RecordingMode.FREE
        _loopState.value = LoopState.EMPTY
        _playbackProgress.value = 0f
        _waveformSamples.value = floatArrayOf()
        _fullRecordingWaveform.value = floatArrayOf()
    }

    private fun updateLiveWaveform() {
        synchronized(recordingBuffer) {
            _waveformSamples.value = downsampleToFloats(recordingBuffer.toShortArray(), 200)
        }
    }

    private fun updateWaveformFromLoop() {
        loopBuffer?.let {
            _waveformSamples.value = downsampleToFloats(it, 200)
        }
    }

    private fun downsampleToFloats(samples: ShortArray, targetBins: Int): FloatArray {
        if (samples.isEmpty()) return floatArrayOf()
        val binSize = (samples.size / targetBins).coerceAtLeast(1)
        val bins = minOf(targetBins, samples.size / binSize)
        return FloatArray(bins) { bin ->
            val start = bin * binSize
            val end = minOf(start + binSize, samples.size)
            var maxAbs = 0
            for (i in start until end) {
                val abs = kotlin.math.abs(samples[i].toInt())
                if (abs > maxAbs) maxAbs = abs
            }
            maxAbs.toFloat() / Short.MAX_VALUE
        }
    }

    fun destroy() {
        clearSession()
        scope.cancel()
    }
}
