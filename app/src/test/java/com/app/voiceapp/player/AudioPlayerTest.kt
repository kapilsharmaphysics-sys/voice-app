package com.app.voiceapp.player

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlaybackStateTest {

    @Test
    fun `idle state has no active post id`() {
        assertNull(PlaybackState.Idle.activePostId)
    }

    @Test
    fun `playing state exposes correct post id`() {
        val state = PlaybackState.Playing("post_1", 0L, 10_000L)
        assertEquals("post_1", state.activePostId)
    }

    @Test
    fun `paused state exposes correct post id`() {
        val state = PlaybackState.Paused("post_2", 3_000L, 10_000L)
        assertEquals("post_2", state.activePostId)
    }

    @Test
    fun `progress fraction is zero for idle`() {
        assertEquals(0f, PlaybackState.Idle.progressFraction())
    }

    @Test
    fun `progress fraction is calculated correctly for playing state`() {
        val state = PlaybackState.Playing("post_1", 5_000L, 10_000L)
        assertEquals(0.5f, state.progressFraction(), 0.001f)
    }

    @Test
    fun `progress fraction is calculated correctly for paused state`() {
        val state = PlaybackState.Paused("post_1", 2_500L, 10_000L)
        assertEquals(0.25f, state.progressFraction(), 0.001f)
    }

    @Test
    fun `progress fraction is zero when duration is zero`() {
        val state = PlaybackState.Playing("post_1", 100L, 0L)
        assertEquals(0f, state.progressFraction())
    }

    @Test
    fun `isPlayingPost is true only for Playing state`() {
        assertEquals(true, PlaybackState.Playing("id", 0L, 1000L).isPlayingPost)
        assertEquals(false, PlaybackState.Paused("id", 0L, 1000L).isPlayingPost)
        assertEquals(false, PlaybackState.Idle.isPlayingPost)
        assertEquals(false, PlaybackState.Buffering("id").isPlayingPost)
    }

    @Test
    fun `buffering state exposes its post id`() {
        val state = PlaybackState.Buffering("post_3")
        assertEquals("post_3", state.activePostId)
    }

    @Test
    fun `error state exposes its post id`() {
        val state = PlaybackState.Error("post_4", "some error")
        assertEquals("post_4", state.activePostId)
    }
}
