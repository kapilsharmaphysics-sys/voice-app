package com.app.voiceapp.player

import android.content.Context
import android.media.MediaPlayer
import com.app.voiceapp.service.AudioPlaybackService
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
import java.io.IOException

/**
 * Application-scoped singleton that owns a single [MediaPlayer] at a time.
 * Tapping the same post toggles play/pause; a different post tears down the current one first.
 */
class AudioPlayer(private val context: Context) {

    private val _state = MutableStateFlow<PlaybackState>(PlaybackState.Idle)
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var mediaPlayer: MediaPlayer? = null
    private var activePostId: String? = null
    private var progressJob: Job? = null

    private val focusManager = AudioFocusManager(context) { gained ->
        if (!gained) pauseInternal(updateService = false)
    }

    /** Plays, pauses, or resumes [postId] depending on current state. Different post = hard switch. */
    fun play(postId: String, filePath: String) {
        when {
            activePostId == postId && _state.value is PlaybackState.Playing -> pauseInternal()
            activePostId == postId && _state.value is PlaybackState.Paused -> resumeInternal()
            else -> startNew(postId, filePath)
        }
    }

    /** Pauses playback and drops audio focus; called externally when recording starts. */
    fun pause() = pauseInternal()

    /** Full teardown — cancels the coroutine scope, releases [MediaPlayer], and stops the foreground service. */
    fun release() {
        progressJob?.cancel()
        mediaPlayer?.apply {
            if (isPlaying) stop()
            reset()
            release()
        }
        mediaPlayer = null
        activePostId = null
        focusManager.abandon()
        AudioPlaybackService.stop(context)
        _state.value = PlaybackState.Idle
        scope.cancel()
    }

    /** Creates a fresh [MediaPlayer] for [postId] after synchronously tearing down whatever was playing. */
    private fun startNew(postId: String, filePath: String) {
        tearDownCurrentPlayer()

        if (!focusManager.request()) return

        activePostId = postId
        _state.value = PlaybackState.Buffering(postId)

        mediaPlayer = MediaPlayer().apply {
            try {
                setDataSource(filePath)
                setOnPreparedListener { player ->
                    player.start()
                    _state.value = PlaybackState.Playing(postId, 0L, player.duration.toLong())
                    startProgressTracking(postId)
                    AudioPlaybackService.start(context)
                }
                setOnCompletionListener {
                    progressJob?.cancel()
                    _state.value = PlaybackState.Idle
                    activePostId = null
                    focusManager.abandon()
                    AudioPlaybackService.stop(context)
                }
                setOnErrorListener { _, what, extra ->
                    _state.value = PlaybackState.Error(postId, "MediaPlayer error [$what, $extra]")
                    tearDownCurrentPlayer()
                    true
                }
                prepareAsync()
            } catch (e: IOException) {
                _state.value = PlaybackState.Error(postId, e.message ?: "Failed to load audio")
                tearDownCurrentPlayer()
            }
        }
    }

    /** Re-requests audio focus before calling start() to handle cases where focus was lost while paused. */
    private fun resumeInternal() {
        if (!focusManager.request()) return
        val player = mediaPlayer ?: return
        val postId = activePostId ?: return
        player.start()
        val current = _state.value
        if (current is PlaybackState.Paused) {
            _state.value = PlaybackState.Playing(postId, current.progressMs, current.durationMs)
        }
        startProgressTracking(postId)
        AudioPlaybackService.start(context)
    }

    /** Pauses and optionally stops the foreground service; [updateService] is false when focus is lost externally. */
    private fun pauseInternal(updateService: Boolean = true) {
        val player = mediaPlayer ?: return
        val postId = activePostId ?: return
        if (!player.isPlaying) return

        player.pause()
        progressJob?.cancel()
        focusManager.abandon()
        _state.value = PlaybackState.Paused(
            postId,
            player.currentPosition.toLong(),
            player.duration.toLong()
        )
        if (updateService) AudioPlaybackService.stop(context)
    }

    /** Polls [MediaPlayer.currentPosition] every 200ms and emits [PlaybackState.Playing] updates to the UI. */
    private fun startProgressTracking(postId: String) {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (true) {
                delay(200)
                val player = mediaPlayer ?: break
                if (!player.isPlaying) break
                _state.value = PlaybackState.Playing(
                    postId,
                    player.currentPosition.toLong(),
                    player.duration.toLong()
                )
            }
        }
    }

    /** Synchronously stops and releases the active [MediaPlayer] before a new one is initialized. */
    private fun tearDownCurrentPlayer() {
        progressJob?.cancel()
        mediaPlayer?.apply {
            runCatching { if (isPlaying) stop() }
            reset()
            release()
        }
        mediaPlayer = null
        activePostId = null
        focusManager.abandon()
    }
}
