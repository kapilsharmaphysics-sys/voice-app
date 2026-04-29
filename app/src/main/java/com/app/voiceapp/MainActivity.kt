package com.app.voiceapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.app.voiceapp.presentation.navigation.AppNavigation
import com.app.voiceapp.ui.theme.VoiceAPPTheme

/**
 * Single activity. Releases the audio player only on a genuine destroy, not on rotation.
 * The [isChangingConfigurations] guard prevents cutting off audio mid-playback during a screen flip.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VoiceAPPTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!isChangingConfigurations) {
            (application as VoiceApp).container.audioPlayer.release()
        }
    }
}
