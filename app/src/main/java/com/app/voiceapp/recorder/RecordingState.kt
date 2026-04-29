package com.app.voiceapp.recorder

/**
 * Lifecycle states of a single recording session.
 * [Recording.amplitudeFraction] is normalized 0–1 from [MediaRecorder.getMaxAmplitude] and drives the live waveform.
 */
sealed interface RecordingState {
    data object Idle : RecordingState
    data class Recording(val durationMs: Long, val amplitudeFraction: Float = 0f) : RecordingState
    data class Completed(val filePath: String, val durationMs: Long) : RecordingState
    data class Error(val message: String) : RecordingState
}
