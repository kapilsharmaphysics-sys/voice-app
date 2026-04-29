package com.app.voiceapp.domain.model

/**
 * Core domain model for a single voice post in the feed.
 * [waveformAmplitudes] is a pre-computed list of 60 floats (0–1) used to draw the visual waveform bars.
 */
data class AudioPost(
    val id: String,
    val authorName: String,
    val authorHandle: String,
    val caption: String,
    val audioFilePath: String,
    val durationMs: Long,
    val likesCount: Int,
    val commentsCount: Int,
    val waveformAmplitudes: List<Float>,
    val createdAt: Long
)
