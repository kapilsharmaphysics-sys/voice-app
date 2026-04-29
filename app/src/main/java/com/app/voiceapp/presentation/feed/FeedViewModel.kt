package com.app.voiceapp.presentation.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.app.voiceapp.VoiceApp
import com.app.voiceapp.domain.model.AudioPost
import com.app.voiceapp.domain.usecase.GetAudioFeedUseCase
import com.app.voiceapp.player.AudioPlayer
import com.app.voiceapp.player.PlaybackState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** Merged UI state for the feed — post list and playback state in one place so the screen has a single source of truth. */
data class FeedUiState(
    val posts: List<AudioPost> = emptyList(),
    val playbackState: PlaybackState = PlaybackState.Idle,
    val isLoading: Boolean = true
)

/**
 * Combines the post list and audio player state into a single [FeedUiState] the screen observes.
 * All playback decisions are delegated to [AudioPlayer] — this ViewModel just forwards the tap.
 */
class FeedViewModel(
    private val getAudioFeedUseCase: GetAudioFeedUseCase,
    private val audioPlayer: AudioPlayer
) : ViewModel() {

    val uiState: StateFlow<FeedUiState> = combine(
        getAudioFeedUseCase(),
        audioPlayer.state
    ) { posts, playbackState ->
        FeedUiState(posts = posts, playbackState = playbackState, isLoading = false)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = FeedUiState()
    )

    /** Tells the player to play or pause [post]; the player decides which based on its current state. */
    fun onPostTapped(post: AudioPost) {
        audioPlayer.play(post.id, post.audioFilePath)
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val app = extras[APPLICATION_KEY] as VoiceApp
                return FeedViewModel(
                    getAudioFeedUseCase = app.container.getAudioFeedUseCase,
                    audioPlayer = app.container.audioPlayer
                ) as T
            }
        }
    }
}
