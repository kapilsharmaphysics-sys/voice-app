package com.app.voiceapp

import android.app.Application
import com.app.voiceapp.di.AppContainer
import com.app.voiceapp.service.AudioPlaybackService

/**
 * Application entry point. Holds [AppContainer] lazily so dependencies aren't created until first use.
 * Also sets up the notification channel early — before any service tries to post to it.
 */
class VoiceApp : Application() {

    val container by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        AudioPlaybackService.createNotificationChannel(this)
    }
}
