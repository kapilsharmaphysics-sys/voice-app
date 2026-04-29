package com.app.voiceapp.data.repository

import com.app.voiceapp.data.local.MockAudioDataSource
import com.app.voiceapp.domain.model.AudioPost
import com.app.voiceapp.domain.repository.AudioRepository
import kotlinx.coroutines.flow.Flow
import kotlin.math.sin
import kotlin.random.Random

/**
 * Wires [MockAudioDataSource] to the [AudioRepository] contract.
 * New recordings get a seeded-random waveform so every post looks visually distinct even before playback.
 */
class AudioRepositoryImpl(private val dataSource: MockAudioDataSource) : AudioRepository {

    override fun getAudioFeed(): Flow<List<AudioPost>> = dataSource.posts

    override suspend fun saveRecording(filePath: String, durationMs: Long): AudioPost {
        val post = AudioPost(
            id = System.currentTimeMillis().toString(),
            authorName = "You",
            authorHandle = "@me",
            caption = "My voice note",
            audioFilePath = filePath,
            durationMs = durationMs,
            likesCount = 0,
            commentsCount = 0,
            waveformAmplitudes = generateWaveformFromDuration(durationMs),
            createdAt = System.currentTimeMillis()
        )
        dataSource.addPost(post)
        return post
    }

    /** Seeds the RNG from [durationMs] so the same recording always produces the same waveform shape. */
    private fun generateWaveformFromDuration(durationMs: Long): List<Float> {
        val barCount = 60
        val rng = Random(durationMs)
        return List(barCount) { i ->
            val base = rng.nextFloat() * 0.7f + 0.15f
            val envelope = sin(Math.PI * i / barCount).toFloat()
            (base * envelope).coerceIn(0.08f, 1f)
        }
    }
}
