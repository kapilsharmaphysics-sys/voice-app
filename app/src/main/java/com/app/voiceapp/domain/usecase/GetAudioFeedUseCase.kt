package com.app.voiceapp.domain.usecase

import com.app.voiceapp.domain.model.AudioPost
import com.app.voiceapp.domain.repository.AudioRepository
import kotlinx.coroutines.flow.Flow

/**
 * Fetches the audio feed as a live [Flow].
 * Thin wrapper — keeps the ViewModel from depending on the repository interface directly.
 */
class GetAudioFeedUseCase(private val repository: AudioRepository) {
    operator fun invoke(): Flow<List<AudioPost>> = repository.getAudioFeed()
}
