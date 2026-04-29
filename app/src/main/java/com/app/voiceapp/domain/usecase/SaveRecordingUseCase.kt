package com.app.voiceapp.domain.usecase

import com.app.voiceapp.domain.model.AudioPost
import com.app.voiceapp.domain.repository.AudioRepository

/**
 * Persists a completed recording to the feed and returns the newly created [AudioPost].
 */
class SaveRecordingUseCase(private val repository: AudioRepository) {
    suspend operator fun invoke(filePath: String, durationMs: Long): AudioPost =
        repository.saveRecording(filePath, durationMs)
}
