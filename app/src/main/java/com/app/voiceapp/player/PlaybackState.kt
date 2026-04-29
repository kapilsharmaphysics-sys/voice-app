package com.app.voiceapp.player

/**
 * All possible states of the audio player at any given moment.
 * Only one post can be active at a time — [Idle] means nothing is loaded or playing.
 */
sealed interface PlaybackState {
    data object Idle : PlaybackState
    data class Buffering(val postId: String) : PlaybackState
    data class Playing(val postId: String, val progressMs: Long, val durationMs: Long) : PlaybackState
    data class Paused(val postId: String, val progressMs: Long, val durationMs: Long) : PlaybackState
    data class Error(val postId: String, val cause: String) : PlaybackState
}

/** Returns the ID of whichever post currently owns the player, or null when [Idle]. */
val PlaybackState.activePostId: String?
    get() = when (this) {
        is PlaybackState.Playing -> postId
        is PlaybackState.Paused -> postId
        is PlaybackState.Buffering -> postId
        is PlaybackState.Error -> postId
        PlaybackState.Idle -> null
    }

/** True only when audio is actively streaming — false during buffering and pause. */
val PlaybackState.isPlayingPost: Boolean
    get() = this is PlaybackState.Playing

/** Progress as a 0–1 fraction for waveform fill; returns 0f for states that have no position info. */
fun PlaybackState.progressFraction(): Float = when (this) {
    is PlaybackState.Playing -> if (durationMs > 0) progressMs / durationMs.toFloat() else 0f
    is PlaybackState.Paused -> if (durationMs > 0) progressMs / durationMs.toFloat() else 0f
    else -> 0f
}
