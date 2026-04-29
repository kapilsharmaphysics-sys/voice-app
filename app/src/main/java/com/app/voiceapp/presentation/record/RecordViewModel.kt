package com.app.voiceapp.presentation.record

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.app.voiceapp.VoiceApp
import com.app.voiceapp.domain.usecase.SaveRecordingUseCase
import com.app.voiceapp.player.AudioPlayer
import com.app.voiceapp.recorder.AudioRecorder
import com.app.voiceapp.recorder.RecordingState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Drives the recording screen by bridging [AudioRecorder] and [SaveRecordingUseCase].
 * Pauses any active playback before recording starts so the mic and speaker don't overlap.
 */
class RecordViewModel(
    private val recorder: AudioRecorder,
    private val audioPlayer: AudioPlayer,
    private val saveRecordingUseCase: SaveRecordingUseCase
) : ViewModel() {

    val recordingState: StateFlow<RecordingState> = recorder.state
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RecordingState.Idle)

    /** Pauses active playback first, then starts the recorder so the two audio sources don't collide. */
    fun startRecording() {
        audioPlayer.pause()
        recorder.start()
    }

    /** Stops recording and moves to the review state so the user can post or discard. */
    fun stopRecording() = recorder.stop()

    /** Throws away the recorded file and resets back to idle without saving anything. */
    fun discardRecording() = recorder.reset()

    /** Saves the recording via the use case, resets the recorder, then calls [onSuccess] on completion. */
    fun postRecording(onSuccess: () -> Unit) {
        val state = recordingState.value
        if (state !is RecordingState.Completed) return
        viewModelScope.launch {
            saveRecordingUseCase(state.filePath, state.durationMs)
            recorder.reset()
            onSuccess()
        }
    }

    override fun onCleared() {
        super.onCleared()
        recorder.release()
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val app = extras[APPLICATION_KEY] as VoiceApp
                return RecordViewModel(
                    recorder = AudioRecorder(app),
                    audioPlayer = app.container.audioPlayer,
                    saveRecordingUseCase = app.container.saveRecordingUseCase
                ) as T
            }
        }
    }
}
