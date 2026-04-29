package com.app.voiceapp.di

import android.content.Context
import com.app.voiceapp.data.local.MockAudioDataSource
import com.app.voiceapp.data.local.SampleAudioGenerator
import com.app.voiceapp.data.repository.AudioRepositoryImpl
import com.app.voiceapp.domain.repository.AudioRepository
import com.app.voiceapp.domain.usecase.GetAudioFeedUseCase
import com.app.voiceapp.domain.usecase.SaveRecordingUseCase
import com.app.voiceapp.player.AudioPlayer

/**
 * Manual DI container — no Hilt, no Dagger, just constructor injection wired at the app level.
 * [AudioRecorder] is intentionally excluded here; it's created fresh per [RecordViewModel] instance.
 */
class AppContainer(context: Context) {

    val audioPlayer: AudioPlayer = AudioPlayer(context)

    private val sampleFiles = SampleAudioGenerator.ensureSamplesExist(context)
    private val mockDataSource = MockAudioDataSource(sampleFiles)

    private val audioRepository: AudioRepository = AudioRepositoryImpl(mockDataSource)

    val getAudioFeedUseCase = GetAudioFeedUseCase(audioRepository)
    val saveRecordingUseCase = SaveRecordingUseCase(audioRepository)
}
