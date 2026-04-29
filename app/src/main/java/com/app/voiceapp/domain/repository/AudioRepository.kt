package com.app.voiceapp.domain.repository

import com.app.voiceapp.domain.model.AudioPost
import kotlinx.coroutines.flow.Flow

/**
 * Contract between the domain and data layers. The feed is a hot [Flow] so the UI always gets live updates.
 */
interface AudioRepository {

    /** Returns the live feed — any new post added via [saveRecording] shows up immediately. */
    fun getAudioFeed(): Flow<List<AudioPost>>

    /** Persists the recording at [filePath] to the feed and returns the created [AudioPost]. */
    suspend fun saveRecording(filePath: String, durationMs: Long): AudioPost
}
