package com.app.voiceapp.usecase

import com.app.voiceapp.domain.model.AudioPost
import com.app.voiceapp.domain.repository.AudioRepository
import com.app.voiceapp.domain.usecase.GetAudioFeedUseCase
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GetAudioFeedUseCaseTest {

    private val repository: AudioRepository = mockk()
    private val useCase = GetAudioFeedUseCase(repository)

    private fun makePost(id: String) = AudioPost(
        id = id,
        authorName = "Test User",
        authorHandle = "@test",
        caption = "Test caption",
        audioFilePath = "/fake/path.wav",
        durationMs = 5_000L,
        likesCount = 10,
        commentsCount = 2,
        waveformAmplitudes = List(60) { 0.5f },
        createdAt = System.currentTimeMillis()
    )

    @Test
    fun `returns feed from repository`() = runTest {
        val posts = listOf(makePost("1"), makePost("2"))
        every { repository.getAudioFeed() } returns flowOf(posts)

        val result = useCase().first()

        assertEquals(2, result.size)
        assertEquals("1", result[0].id)
        assertEquals("2", result[1].id)
    }

    @Test
    fun `delegates directly to repository`() = runTest {
        every { repository.getAudioFeed() } returns flowOf(emptyList())

        useCase().first()

        verify(exactly = 1) { repository.getAudioFeed() }
    }

    @Test
    fun `returns empty list when repository has no posts`() = runTest {
        every { repository.getAudioFeed() } returns flowOf(emptyList())

        val result = useCase().first()

        assertEquals(0, result.size)
    }

    @Test
    fun `each call to invoke creates a fresh subscription`() = runTest {
        every { repository.getAudioFeed() } returns flowOf(emptyList())

        useCase()
        useCase()

        verify(exactly = 2) { repository.getAudioFeed() }
    }
}
