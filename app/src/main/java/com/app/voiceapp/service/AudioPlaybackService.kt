package com.app.voiceapp.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.app.voiceapp.MainActivity
import com.app.voiceapp.R

/**
 * Keeps the process alive while audio plays in the background via a foreground notification.
 * Started and stopped directly by [AudioPlayer] — nothing else should interact with this service.
 */
class AudioPlaybackService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startForeground(NOTIFICATION_ID, buildNotification())
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    /** Builds the persistent playback notification with a tap-to-return [PendingIntent] to [MainActivity]. */
    private fun buildNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("VoiceApp")
            .setContentText("Playing audio")
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val ACTION_START = "action_playback_start"
        private const val ACTION_STOP = "action_playback_stop"
        const val CHANNEL_ID = "voice_app_playback"
        private const val NOTIFICATION_ID = 1001

        /** Sends ACTION_START; uses [startForegroundService] on O+ to satisfy the API requirement. */
        fun start(context: Context) {
            val intent = Intent(context, AudioPlaybackService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        /** Sends ACTION_STOP so the service removes the notification and calls stopSelf. */
        fun stop(context: Context) {
            context.startService(Intent(context, AudioPlaybackService::class.java).apply {
                action = ACTION_STOP
            })
        }

        /** Registers the low-importance playback channel on O+; idempotent — safe to call more than once. */
        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Audio Playback",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Shows when audio is playing in the background"
                    setShowBadge(false)
                }
                context.getSystemService(NotificationManager::class.java)
                    .createNotificationChannel(channel)
            }
        }
    }
}
