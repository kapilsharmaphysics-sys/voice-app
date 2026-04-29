package com.app.voiceapp.recorder

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

/**
 * Thin [MediaRecorder] wrapper managing a single recording lifecycle per ViewModel instance.
 * Do not share across ViewModels — [release] cancels the internal scope and makes this instance unusable.
 */
class AudioRecorder(private val context: Context) {

    private val _state = MutableStateFlow<RecordingState>(RecordingState.Idle)
    val state: StateFlow<RecordingState> = _state.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var recorder: MediaRecorder? = null
    private var pollingJob: Job? = null
    private var startTime = 0L
    private var outputFile: File? = null

    /** Creates the output file, configures [MediaRecorder], and starts the 80ms amplitude polling loop. */
    fun start() {
        val file = createOutputFile() ?: return
        outputFile = file
        startTime = System.currentTimeMillis()

        recorder = buildMediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioSamplingRate(44100)
            setAudioEncodingBitRate(128_000)
            setOutputFile(file.absolutePath)
            setMaxDuration(MAX_DURATION_MS)
            setOnInfoListener { _, what, _ ->
                if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                    stopAndFinalize()
                }
            }
            try {
                prepare()
                start()
                _state.value = RecordingState.Recording(0L)
                startPolling()
            } catch (e: Exception) {
                _state.value = RecordingState.Error(e.message ?: "Failed to start recording")
                reset()
            }
        }
    }

    /** Stops recording and transitions to [RecordingState.Completed] with the saved file path. */
    fun stop() = stopAndFinalize()

    /** Discards the current recording and returns to [RecordingState.Idle] without saving anything. */
    fun reset() {
        pollingJob?.cancel()
        recorder?.runCatching { stop() }
        recorder?.release()
        recorder = null
        outputFile = null
        _state.value = RecordingState.Idle
    }

    /** Stops [MediaRecorder], measures final duration, and emits [RecordingState.Completed] or an error. */
    private fun stopAndFinalize() {
        pollingJob?.cancel()
        val file = outputFile ?: return
        val duration = System.currentTimeMillis() - startTime

        try {
            recorder?.stop()
        } catch (e: RuntimeException) {
            file.delete()
            _state.value = RecordingState.Error("Recording too short")
            recorder?.release()
            recorder = null
            return
        }

        recorder?.release()
        recorder = null
        _state.value = RecordingState.Completed(file.absolutePath, duration)
    }

    /** Reads [MediaRecorder.maxAmplitude] every 80ms and updates recording state with the normalized value. */
    private fun startPolling() {
        pollingJob = scope.launch {
            while (true) {
                delay(80)
                val elapsed = System.currentTimeMillis() - startTime
                val maxAmp = recorder?.maxAmplitude ?: 0
                val amplitude = (maxAmp / MAX_AMPLITUDE.toFloat()).coerceIn(0f, 1f)
                _state.value = RecordingState.Recording(elapsed, amplitude)
            }
        }
    }

    /** Prefers external app storage for the .m4a file; falls back to internal if external isn't available. */
    private fun createOutputFile(): File? {
        return runCatching {
            val dir = context.getExternalFilesDir(null) ?: context.filesDir
            File(dir, "rec_${System.currentTimeMillis()}.m4a")
        }.onFailure {
            _state.value = RecordingState.Error("Cannot create output file")
        }.getOrNull()
    }

    /** Uses the context-aware constructor on API 31+ and the deprecated no-arg form on older devices. */
    private fun buildMediaRecorder(): MediaRecorder =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

    /** Resets the recorder and cancels the coroutine scope; call this only from [ViewModel.onCleared]. */
    fun release() {
        reset()
        scope.cancel()
    }

    companion object {
        private const val MAX_DURATION_MS = 30_000
        private const val MAX_AMPLITUDE = 32767
    }
}
