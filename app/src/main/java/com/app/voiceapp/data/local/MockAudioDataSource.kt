package com.app.voiceapp.data.local

import com.app.voiceapp.domain.model.AudioPost
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.sin
import kotlin.random.Random

/**
 * In-memory feed store backed by [MutableStateFlow]. State resets on every app launch — no persistence.
 * New recordings are prepended so they appear at the top of the feed immediately after posting.
 */
class MockAudioDataSource(sampleFiles: List<Pair<String, Long>>) {

    private val seed = Random(42)

    private val initialPosts = listOf(
        AudioPost(
            id = "1",
            authorName = "Aarav Sharma",
            authorHandle = "@aarav_s",
            caption = "Subah ki soch — kabhi kabhi restrictions hi creativity ko push karti hain 🎙️",
            audioFilePath = sampleFiles.getOrNull(0)?.first ?: "",
            durationMs = sampleFiles.getOrNull(0)?.second ?: 8_000L,
            likesCount = 1204,
            commentsCount = 87,
            waveformAmplitudes = generateWaveform(seed),
            createdAt = System.currentTimeMillis() - 15 * 60_000
        ),
        AudioPost(
            id = "2",
            authorName = "Rohit Verma",
            authorHandle = "@rohit_v",
            caption = "2025 mein mobile development ka scene — sab kuch bohot fast change ho raha hai.",
            audioFilePath = sampleFiles.getOrNull(1)?.first ?: "",
            durationMs = sampleFiles.getOrNull(1)?.second ?: 14_000L,
            likesCount = 834,
            commentsCount = 143,
            waveformAmplitudes = generateWaveform(seed),
            createdAt = System.currentTimeMillis() - 42 * 60_000
        ),
        AudioPost(
            id = "3",
            authorName = "Priya Kapoor",
            authorHandle = "@priya_k",
            caption = "Pichle kuch din se ek thought mind mein hai… likh nahi paayi, isliye bol diya.",
            audioFilePath = sampleFiles.getOrNull(2)?.first ?: "",
            durationMs = sampleFiles.getOrNull(2)?.second ?: 6_000L,
            likesCount = 2671,
            commentsCount = 312,
            waveformAmplitudes = generateWaveform(seed),
            createdAt = System.currentTimeMillis() - 3 * 3600_000
        ),
        AudioPost(
            id = "4",
            authorName = "Kunal Mehta",
            authorHandle = "@kunal_m",
            caption = "Side project se 10k users tak ka journey — 3 important learnings share kar raha hoon.",
            audioFilePath = sampleFiles.getOrNull(3)?.first ?: "",
            durationMs = sampleFiles.getOrNull(3)?.second ?: 11_000L,
            likesCount = 507,
            commentsCount = 61,
            waveformAmplitudes = generateWaveform(seed),
            createdAt = System.currentTimeMillis() - 6 * 3600_000
        )
    )

    private val _posts = MutableStateFlow(initialPosts)
    val posts: StateFlow<List<AudioPost>> = _posts.asStateFlow()

    /** Prepends [post] to the feed so it shows up at the top right after recording. */
    fun addPost(post: AudioPost) {
        _posts.update { current -> listOf(post) + current }
    }

    /** Produces 60 amplitude values with a sine envelope so waveforms look natural, not spikey. */
    private fun generateWaveform(rng: Random): List<Float> {
        return List(60) { i ->
            val base = rng.nextFloat() * 0.6f + 0.1f
            val envelope = sin(PI_F * i / 60f)
            (base * envelope).coerceIn(0.08f, 1f)
        }
    }

    companion object {
        private const val PI_F = Math.PI.toFloat()
    }
}