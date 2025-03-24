package com.snoop.fm7000

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.media.MediaBrowserServiceCompat
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.SimpleExoPlayer

class MediaService : MediaBrowserServiceCompat() {

    private lateinit var player: SimpleExoPlayer
    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var stateBuilder: PlaybackStateCompat.Builder

    override fun onCreate() {
        super.onCreate()

        // ✅ Initialize ExoPlayer
        player = SimpleExoPlayer.Builder(this).build().apply {
            setMediaItem(MediaItem.fromUri("https://stream.7000fm.gr/radio/8000/radio.mp3"))
            prepare()
        }

        // ✅ Create MediaSession
        mediaSession = MediaSessionCompat(this, "MediaService").apply {
            setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                        MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
            )

            stateBuilder = PlaybackStateCompat.Builder()
                .setActions(
                    PlaybackStateCompat.ACTION_PLAY or
                            PlaybackStateCompat.ACTION_PAUSE or
                            PlaybackStateCompat.ACTION_STOP
                )

            setPlaybackState(stateBuilder.build())
            setCallback(mediaSessionCallback)
            isActive = true
        }

        sessionToken = mediaSession.sessionToken
        startForegroundService()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "ACTION_PLAY" -> handlePlay()
            "ACTION_PAUSE" -> handlePause()
        }
        return START_STICKY
    }

    // ✅ Handle Play Action
    private fun handlePlay() {
        player.play()

        val albumArtBitmap = BitmapFactory.decodeResource(resources, R.drawable.fm7000_og)

        val metadata = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, "7000FM")
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, "")
            .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, albumArtBitmap) // ✅ This sets the artwork
            .build()

        mediaSession.setMetadata(metadata) // ✅ Apply metadata

        updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
        updateNotification()
        sendPlaybackStateBroadcast(true)
        Log.d("MediaService", "ACTION_PLAY processed with custom album art")
    }


    // ✅ Handle Pause Action
    private fun handlePause() {
        player.pause()

        val metadata = mediaSession.controller.metadata?.let {
            MediaMetadataCompat.Builder(it)
                .build()
        }
        mediaSession.setMetadata(metadata) // ✅ Keep the last metadata

        updatePlaybackState(PlaybackStateCompat.STATE_PAUSED)
        updateNotification()
        sendPlaybackStateBroadcast(false)
        Log.d("MediaService", "ACTION_PAUSE processed")
    }


    // ✅ Update Playback State
    private fun updatePlaybackState(state: Int) {
        val playbackState = stateBuilder.setState(state, player.currentPosition, 1f).build()
        mediaSession.setPlaybackState(playbackState)
        sendPlaybackStateBroadcast(state == PlaybackStateCompat.STATE_PLAYING)
    }

    // ✅ Broadcast Playback State
    private fun sendPlaybackStateBroadcast(isPlaying: Boolean) {
        val intent = Intent("com.snoop.fm7000.PLAYBACK_STATE_CHANGED").apply {
            putExtra("isPlaying", isPlaying)
        }
        sendBroadcast(intent)
        Log.d("MediaService", "Broadcast sent: isPlaying = $isPlaying")
    }

    override fun onDestroy() {
        player.release()
        mediaSession.release()
        stopForeground(true)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot? = null

    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {}

    // ✅ MediaSession Callbacks
    private val mediaSessionCallback = object : MediaSessionCompat.Callback() {
        override fun onPlay() = handlePlay()
        override fun onPause() = handlePause()
        override fun onStop() {
            player.stop()
            stopForeground(true)
            stopSelf()
        }
    }

    // ✅ Update Notification
    private fun updateNotification() {
        val notification = createMediaNotification()
        startForeground(1, notification)
    }

    // ✅ Create Media Notification
    private fun createMediaNotification(): Notification {
        val metadata = mediaSession.controller.metadata
        val albumArtBitmap = metadata?.getBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART)
            ?: BitmapFactory.decodeResource(resources, R.drawable.fm7000_og) // ✅ Default image

        val channelId = "MEDIA_PLAYBACK_CHANNEL"
        val channel = NotificationChannel(
            channelId, "Media Playback", NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle(metadata?.getString(MediaMetadataCompat.METADATA_KEY_TITLE) ?: "7000FM")
            .setContentText(metadata?.getString(MediaMetadataCompat.METADATA_KEY_ARTIST) ?: "")
            .setSmallIcon(R.drawable.ic_music)
            .setLargeIcon(albumArtBitmap) // ✅ This sets the Quick Settings/Lock Screen image
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
            )
            .setOngoing(true)
            .build()
    }



    // ✅ Start Foreground Service
    private fun startForegroundService() {
        startForeground(1, createMediaNotification())
    }
}
