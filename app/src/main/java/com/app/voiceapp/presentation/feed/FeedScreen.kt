package com.app.voiceapp.presentation.feed

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.app.voiceapp.domain.model.AudioPost
import com.app.voiceapp.player.PlaybackState
import com.app.voiceapp.player.activePostId
import com.app.voiceapp.player.isPlayingPost
import com.app.voiceapp.player.progressFraction
import java.util.concurrent.TimeUnit
import kotlin.math.absoluteValue

/**
 * Root feed screen — a [LazyColumn] of audio posts with a floating record button at the bottom.
 * Items use [key = { it.id }] so waveform animations don't reset when the list updates during scroll.
 */
@Composable
fun FeedScreen(
    onRecordTapped: () -> Unit,
    viewModel: FeedViewModel = viewModel(factory = FeedViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        if (uiState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                item {
                    FeedHeader()
                }
                items(items = uiState.posts, key = { it.id }) { post ->
                    AudioPostCard(
                        post = post,
                        playbackState = uiState.playbackState,
                        onTap = { viewModel.onPostTapped(post) }
                    )
                }
                item { Spacer(modifier = Modifier.height(88.dp)) }
            }
        }

        RecordFab(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp),
            onClick = onRecordTapped
        )
    }
}

@Composable
private fun FeedHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "VoiceApp",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

/** Single feed card — author info, caption, waveform playback row, and engagement counts. */
@Composable
private fun AudioPostCard(
    post: AudioPost,
    playbackState: PlaybackState,
    onTap: () -> Unit
) {
    val isThisPostActive = playbackState.activePostId == post.id
    val isPlaying = isThisPostActive && playbackState.isPlayingPost
    val isBuffering = isThisPostActive && playbackState is PlaybackState.Buffering
    val progress = if (isThisPostActive) playbackState.progressFraction() else 0f

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            PostAuthorRow(post)

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = post.caption,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            PlaybackRow(
                post = post,
                isPlaying = isPlaying,
                isBuffering = isBuffering,
                progress = progress,
                onTap = onTap
            )

            Spacer(modifier = Modifier.height(14.dp))

            EngagementRow(post)
        }
    }
}

@Composable
private fun PostAuthorRow(post: AudioPost) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        AuthorAvatar(name = post.authorName)
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = post.authorName,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${post.authorHandle} · ${post.createdAt.toRelativeTime()}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Initials-based avatar; background color is deterministic so the same author always gets the same color. */
@Composable
private fun AuthorAvatar(name: String) {
    val initials = name.split(" ").take(2).mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")
    val avatarColor = name.hashColorFromString()

    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(avatarColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
private fun PlaybackRow(
    post: AudioPost,
    isPlaying: Boolean,
    isBuffering: Boolean,
    progress: Float,
    onTap: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        PlayPauseButton(isPlaying = isPlaying, isBuffering = isBuffering, onClick = onTap)

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            AudioWaveform(
                amplitudes = post.waveformAmplitudes,
                progress = progress,
                isActive = isPlaying || isBuffering,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (progress > 0f) (post.durationMs * progress).toLong().formatDuration()
                    else "0:00",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = post.durationMs.formatDuration(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Circle button that switches between play icon, pause icon, and a spinner while buffering. */
@Composable
private fun PlayPauseButton(isPlaying: Boolean, isBuffering: Boolean, onClick: () -> Unit) {
    val containerColor by animateColorAsState(
        targetValue = if (isPlaying || isBuffering)
            MaterialTheme.colorScheme.primary
        else
            MaterialTheme.colorScheme.primaryContainer,
        label = "btn_color"
    )

    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(containerColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        when {
            isBuffering -> CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary
            )
            isPlaying -> Icon(
                imageVector = Icons.Default.Pause,
                contentDescription = "Pause",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(22.dp)
            )
            else -> Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Play",
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

/**
 * Draws 60 amplitude bars on [Canvas]; played bars pulse when active, unplayed bars stay muted grey.
 * Progress animates with a 180ms tween so it tracks playback without feeling laggy.
 */
@Composable
private fun AudioWaveform(
    amplitudes: List<Float>,
    progress: Float,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(180, easing = LinearEasing),
        label = "waveform_progress"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Canvas(modifier = modifier) {
        if (amplitudes.isEmpty()) return@Canvas

        val count = amplitudes.size
        val spacing = 2.dp.toPx()
        val barWidth = ((size.width - spacing * (count - 1)) / count).coerceAtLeast(2f)
        val centerY = size.height / 2f

        amplitudes.forEachIndexed { i, amp ->
            val fraction = i.toFloat() / count
            val isPlayed = fraction <= animatedProgress
            val scale = if (isActive && isPlayed) pulseScale else 1f
            val barHeight = (amp * size.height * scale).coerceAtLeast(4.dp.toPx())
            val x = i * (barWidth + spacing)

            drawRoundRect(
                color = if (isPlayed) primaryColor else surfaceVariant,
                topLeft = Offset(x, centerY - barHeight / 2),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2)
            )
        }
    }
}

@Composable
private fun EngagementRow(post: AudioPost) {
    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.FavoriteBorder,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = post.likesCount.formatCount(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.ChatBubbleOutline,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = post.commentsCount.formatCount(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Pill-shaped FAB anchored to the bottom center; navigates to the record screen on tap. */
@Composable
private fun RecordFab(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.primary)
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "Record",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

/** Formats milliseconds to m:ss — used for waveform duration labels. */
private fun Long.formatDuration(): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(this)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(this) % 60
    return "%d:%02d".format(minutes, seconds)
}

/** Returns "now", "5m", "2h", or "3d" depending on how old the timestamp is. */
private fun Long.toRelativeTime(): String {
    val diffMs = System.currentTimeMillis() - this
    val minutes = TimeUnit.MILLISECONDS.toMinutes(diffMs)
    val hours = TimeUnit.MILLISECONDS.toHours(diffMs)
    return when {
        minutes < 1 -> "now"
        minutes < 60 -> "${minutes}m"
        hours < 24 -> "${hours}h"
        else -> "${TimeUnit.MILLISECONDS.toDays(diffMs)}d"
    }
}

/** Formats counts above 1000 as "1.2k" to save space in the engagement row. */
private fun Int.formatCount(): String = when {
    this >= 1000 -> "%.1fk".format(this / 1000f)
    else -> toString()
}

private val avatarColors = listOf(
    Color(0xFF6C63FF), Color(0xFF43BCCD), Color(0xFFFF6584),
    Color(0xFFFF9A3C), Color(0xFF52B788), Color(0xFFE07BE0)
)

/** Picks a consistent avatar color from the palette using the string's hash — same name always same color. */
private fun String.hashColorFromString(): Color {
    return avatarColors[(hashCode().absoluteValue) % avatarColors.size]
}
