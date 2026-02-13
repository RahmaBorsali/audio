package com.example.audio.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.audio.MainActivity

class AudioPlayerService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var currentSongTitle: String = ""
    private var currentSongArtist: String = ""
    private var currentResourceId: Int = -1

    private val playlist = mutableListOf<Int>()
    private val playlistTitles = mutableListOf<String>()
    private val playlistArtists = mutableListOf<String>()
    private var currentSongIndex: Int = 0

    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private var progressRunnable: Runnable? = null

    companion object {
        const val ACTION_PLAY = "ACTION_PLAY"
        const val ACTION_PAUSE = "ACTION_PAUSE"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_NEXT = "ACTION_NEXT"
        const val ACTION_PREVIOUS = "ACTION_PREVIOUS"
        const val ACTION_SEEK = "ACTION_SEEK"

        const val EXTRA_SONG_RESOURCE_ID = "EXTRA_SONG_RESOURCE_ID"
        const val EXTRA_SONG_TITLE = "EXTRA_SONG_TITLE"
        const val EXTRA_SONG_ARTIST = "EXTRA_SONG_ARTIST"
        const val EXTRA_PLAYLIST_IDS = "EXTRA_PLAYLIST_IDS"
        const val EXTRA_PLAYLIST_TITLES = "EXTRA_PLAYLIST_TITLES"
        const val EXTRA_PLAYLIST_ARTISTS = "EXTRA_PLAYLIST_ARTISTS"
        const val EXTRA_SONG_INDEX = "EXTRA_SONG_INDEX"

        const val ACTION_STATE_CHANGED = "com.example.audio.STATE_CHANGED"
        const val EXTRA_IS_PLAYING = "EXTRA_IS_PLAYING"
        const val EXTRA_CURRENT_INDEX = "EXTRA_CURRENT_INDEX"
        const val EXTRA_POSITION_MS = "EXTRA_POSITION_MS"
        const val EXTRA_DURATION_MS = "EXTRA_DURATION_MS"

        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "audio_player_channel"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {

            ACTION_PLAY -> {
                val resourceId = intent.getIntExtra(EXTRA_SONG_RESOURCE_ID, -1)

                // playlist si fournie
                val playlistIdsString = intent.getStringExtra(EXTRA_PLAYLIST_IDS)
                val playlistTitlesString = intent.getStringExtra(EXTRA_PLAYLIST_TITLES)
                val playlistArtistsString = intent.getStringExtra(EXTRA_PLAYLIST_ARTISTS)
                val songIndex = intent.getIntExtra(EXTRA_SONG_INDEX, -1)

                if (!playlistIdsString.isNullOrEmpty()
                    && !playlistTitlesString.isNullOrEmpty()
                    && !playlistArtistsString.isNullOrEmpty()
                    && songIndex >= 0
                ) {
                    playlist.clear()
                    playlistTitles.clear()
                    playlistArtists.clear()

                    playlist.addAll(playlistIdsString.split(",").mapNotNull { it.toIntOrNull() })
                    playlistTitles.addAll(playlistTitlesString.split("|"))
                    playlistArtists.addAll(playlistArtistsString.split("|"))
                    currentSongIndex = songIndex
                }

                // ✅ si resourceId fourni => nouvelle chanson
                if (resourceId != -1 && resourceId != 0) {
                    currentSongTitle = intent.getStringExtra(EXTRA_SONG_TITLE) ?: "Chanson"
                    currentSongArtist = intent.getStringExtra(EXTRA_SONG_ARTIST) ?: "Artiste"
                    currentResourceId = resourceId
                    playSong(resourceId)
                } else {
                    // ✅ sinon => resume
                    resumeSong()
                }
            }

            ACTION_PAUSE -> pauseSong()

            ACTION_STOP -> {
                stopSong()
                stopSelf()
            }

            ACTION_NEXT -> playNextSong()

            ACTION_PREVIOUS -> playPreviousSong()

            ACTION_SEEK -> {
                val pos = intent.getIntExtra(EXTRA_POSITION_MS, 0)
                mediaPlayer?.let { mp ->
                    mp.seekTo(pos.coerceIn(0, mp.duration.coerceAtLeast(0)))
                    broadcastState(mp.isPlaying)
                }
            }
        }

        return START_STICKY
    }

    private fun playSong(resourceId: Int) {
        try {
            stopProgressUpdates()
            mediaPlayer?.release()

            mediaPlayer = MediaPlayer.create(this, resourceId).apply {
                setOnCompletionListener { stopSong() }
                start()
            }

            startForeground(NOTIFICATION_ID, createNotification(true))
            broadcastState(true)
            startProgressUpdates()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun pauseSong() {
        mediaPlayer?.let { mp ->
            if (mp.isPlaying) {
                mp.pause()
                stopProgressUpdates()
                startForeground(NOTIFICATION_ID, createNotification(false))
                broadcastState(false) // position fixe
            }
        }
    }

    private fun resumeSong() {
        mediaPlayer?.let { mp ->
            if (!mp.isPlaying) {
                mp.start()
                startForeground(NOTIFICATION_ID, createNotification(true))
                broadcastState(true)
                startProgressUpdates()
            }
        }
    }

    private fun playNextSong() {
        if (playlist.isEmpty()) return

        currentSongIndex = (currentSongIndex + 1) % playlist.size
        currentResourceId = playlist[currentSongIndex]
        currentSongTitle = playlistTitles.getOrNull(currentSongIndex) ?: "Chanson"
        currentSongArtist = playlistArtists.getOrNull(currentSongIndex) ?: "Artiste"

        playSong(currentResourceId)
    }

    private fun playPreviousSong() {
        if (playlist.isEmpty()) return

        currentSongIndex = if (currentSongIndex - 1 < 0) playlist.size - 1 else currentSongIndex - 1
        currentResourceId = playlist[currentSongIndex]
        currentSongTitle = playlistTitles.getOrNull(currentSongIndex) ?: "Chanson"
        currentSongArtist = playlistArtists.getOrNull(currentSongIndex) ?: "Artiste"

        playSong(currentResourceId)
    }

    private fun stopSong() {
        stopProgressUpdates()
        mediaPlayer?.let { mp ->
            try {
                if (mp.isPlaying) mp.stop()
            } catch (_: Exception) {}
            mp.release()
        }
        mediaPlayer = null
        broadcastState(false)
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Lecteur Audio",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Contrôles de lecture audio"
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun createNotification(isPlaying: Boolean): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val previousPendingIntent = PendingIntent.getService(
            this, 1,
            Intent(this, AudioPlayerService::class.java).apply { action = ACTION_PREVIOUS },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val playPausePendingIntent = PendingIntent.getService(
            this, 2,
            Intent(this, AudioPlayerService::class.java).apply { action = if (isPlaying) ACTION_PAUSE else ACTION_PLAY },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val nextPendingIntent = PendingIntent.getService(
            this, 3,
            Intent(this, AudioPlayerService::class.java).apply { action = ACTION_NEXT },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopPendingIntent = PendingIntent.getService(
            this, 4,
            Intent(this, AudioPlayerService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(currentSongTitle)
            .setContentText(currentSongArtist)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(pendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(isPlaying)
            .setShowWhen(false)
            .addAction(android.R.drawable.ic_media_previous, "", previousPendingIntent)
            .addAction(
                if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                "",
                playPausePendingIntent
            )
            .addAction(android.R.drawable.ic_media_next, "", nextPendingIntent)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setShowActionsInCompactView(0, 1, 2)
                    .setShowCancelButton(true)
                    .setCancelButtonIntent(stopPendingIntent)
            )
            .build()
    }

    private fun broadcastState(isPlaying: Boolean) {
        val mp = mediaPlayer
        val intent = Intent(ACTION_STATE_CHANGED).apply {
            putExtra(EXTRA_IS_PLAYING, isPlaying)
            putExtra(EXTRA_CURRENT_INDEX, currentSongIndex)
            putExtra(EXTRA_POSITION_MS, mp?.currentPosition ?: 0)
            putExtra(EXTRA_DURATION_MS, mp?.duration ?: 0)
        }
        sendBroadcast(intent)
    }

    private fun startProgressUpdates() {
        stopProgressUpdates()
        progressRunnable = object : Runnable {
            override fun run() {
                val mp = mediaPlayer ?: return
                broadcastState(mp.isPlaying)
                if (mp.isPlaying) handler.postDelayed(this, 500)
            }
        }
        handler.post(progressRunnable!!)
    }

    private fun stopProgressUpdates() {
        progressRunnable?.let { handler.removeCallbacks(it) }
        progressRunnable = null
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        stopSong()
    }
}
