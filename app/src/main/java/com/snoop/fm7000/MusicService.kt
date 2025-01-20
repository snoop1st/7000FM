import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.LifecycleService
import androidx.media.app.NotificationCompat.MediaStyle
import androidx.media.session.MediaButtonReceiver
import com.snoop.fm7000.MainActivity
import com.snoop.fm7000.R

class MusicService : LifecycleService() {

    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var audioManager: AudioManager
    private lateinit var audioFocusRequest: AudioFocusRequest
    private var isPlaying = false

    override fun onCreate() {
        super.onCreate()

        // Initialize MediaPlayer
        initializeMediaPlayer()

        // Initialize MediaSession
        initializeMediaSession()

        // Initialize AudioManager
        initializeAudioManager()
    }

    private fun initializeMediaPlayer() {
        mediaPlayer = MediaPlayer().apply {
            setDataSource("https://stream.7000fm.gr/radio/8000/radio.mp3")
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            prepareAsync()
            setOnPreparedListener {
                // Start playback when prepared
                start()
                this@MusicService.isPlaying = true
                showNotification()
            }
        }

        // Create notification channel
        createNotificationChannel()
    }

    private fun initializeMediaSession() {
        mediaSession = MediaSessionCompat(this, "MusicService").apply {
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() {
                    super.onPlay()
                    if (requestAudioFocus()) {
                        mediaPlayer.start()
                        updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
                        showNotification()
                    }
                }

                override fun onPause() {
                    super.onPause()
                    mediaPlayer.pause()
                    updatePlaybackState(PlaybackStateCompat.STATE_PAUSED)
                    showNotification()
                }

                override fun onStop() {
                    super.onStop()
                    mediaPlayer.stop()
                    updatePlaybackState(PlaybackStateCompat.STATE_STOPPED)
                    stopSelf()
                }
            })
            setSessionActivity(PendingIntent.getActivity(this@MusicService, 0, Intent(this@MusicService, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE))
            isActive = true
        }
    }

    private fun initializeAudioManager() {
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                .setOnAudioFocusChangeListener { focusChange ->
                    when (focusChange) {
                        AudioManager.AUDIOFOCUS_LOSS -> {
                            mediaPlayer.pause()
                            updatePlaybackState(PlaybackStateCompat.STATE_PAUSED)
                        }
                        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                            mediaPlayer.pause()
                            updatePlaybackState(PlaybackStateCompat.STATE_PAUSED)
                        }
                        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                            mediaPlayer.setVolume(0.2f, 0.2f)
                        }
                        AudioManager.AUDIOFOCUS_GAIN -> {
                            mediaPlayer.setVolume(1.0f, 1.0f)
                            if (!mediaPlayer.isPlaying) {
                                mediaPlayer.start()
                                updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
                            }
                        }
                    }
                }
                .build()
        }
    }

    private fun requestAudioFocus(): Boolean {
        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioManager.requestAudioFocus(audioFocusRequest)
        } else {
            audioManager.requestAudioFocus(
                { focusChange ->
                    when (focusChange) {
                        AudioManager.AUDIOFOCUS_LOSS -> {
                            mediaPlayer.pause()
                            updatePlaybackState(PlaybackStateCompat.STATE_PAUSED)
                        }
                        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                            mediaPlayer.pause()
                            updatePlaybackState(PlaybackStateCompat.STATE_PAUSED)
                        }
                        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                            mediaPlayer.setVolume(0.2f, 0.2f)
                        }
                        AudioManager.AUDIOFOCUS_GAIN -> {
                            mediaPlayer.setVolume(1.0f, 1.0f)
                            if (!mediaPlayer.isPlaying) {
                                mediaPlayer.start()
                                updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
                            }
                        }
                    }
                },
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    private fun updatePlaybackState(state: Int) {
        val playbackState = PlaybackStateCompat.Builder()
            .setActions(PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_PAUSE or PlaybackStateCompat.ACTION_STOP)
            .setState(state, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 0f)
            .build()
        mediaSession.setPlaybackState(playbackState)
    }

    private fun showNotification() {
        val controller = mediaSession.controller
        val mediaMetadata = controller.metadata
        val description = mediaMetadata.description

        val playPauseAction = if (isPlaying) {
            createPauseAction()
        } else {
            createPlayAction()
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(description.title)
            .setContentText(description.subtitle)
            .setSubText(description.description)
            .setLargeIcon(description.iconBitmap)
            .setContentIntent(controller.sessionActivity)
            .setDeleteIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_STOP))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSmallIcon(R.drawable.ic_music_note)
            .addAction(playPauseAction)
            .addAction(createStopAction())
            .setStyle(MediaStyle()
                .setMediaSession(mediaSession.sessionToken)
                .setShowActionsInCompactView(0, 1))
            .setOngoing(isPlaying)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(1, notification)
    }

    private fun createPlayAction(): NotificationCompat.Action {
        val playIntent = MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_PLAY)
        return NotificationCompat.Action(R.drawable.ic_play_arrow, "Play", playIntent)
    }

    private fun createPauseAction(): NotificationCompat.Action {
        val pauseIntent = MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_PAUSE)
        return NotificationCompat.Action(R.drawable.ic_pause, "Pause", pauseIntent)
    }

    private fun createStopAction(): NotificationCompat.Action {
        val stopIntent = MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_STOP)
        return NotificationCompat.Action(R.drawable.ic_stop, "Stop", stopIntent)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Music Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.release()
        mediaSession.release()
        stopForeground(true)
        NotificationManagerCompat.from(this).cancel(1)
    }

    companion object {
        const val CHANNEL_ID = "music_channel"
        const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1

        fun startService(context: Context) {
            val serviceIntent = Intent(context, MusicService::class.java)
            context.startService(serviceIntent)
        }
    }
}