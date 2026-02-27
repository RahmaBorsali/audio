package com.example.audio.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.audio.MainActivity

import android.net.Uri

class AudioPlayerService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var currentSongTitle: String = ""
    private var currentSongArtist: String = ""
    private var currentSongUri: String = ""

    private val playlistUris = mutableListOf<String>()
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

        const val EXTRA_SONG_URI = "EXTRA_SONG_URI"
        const val EXTRA_SONG_TITLE = "EXTRA_SONG_TITLE"
        const val EXTRA_SONG_ARTIST = "EXTRA_SONG_ARTIST"
        const val EXTRA_PLAYLIST_URIS = "EXTRA_PLAYLIST_URIS"
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
                val songUri = intent.getStringExtra(EXTRA_SONG_URI)

                val playlistUrisString = intent.getStringExtra(EXTRA_PLAYLIST_URIS)
                val playlistTitlesString = intent.getStringExtra(EXTRA_PLAYLIST_TITLES)
                val playlistArtistsString = intent.getStringExtra(EXTRA_PLAYLIST_ARTISTS)
                val songIndex = intent.getIntExtra(EXTRA_SONG_INDEX, -1)

                if (!playlistUrisString.isNullOrEmpty()
                    && !playlistTitlesString.isNullOrEmpty()
                    && !playlistArtistsString.isNullOrEmpty()
                    && songIndex >= 0
                ) {
                    playlistUris.clear()
                    playlistTitles.clear()
                    playlistArtists.clear()

                    playlistUris.addAll(playlistUrisString.split(","))
                    playlistTitles.addAll(playlistTitlesString.split("|"))
                    playlistArtists.addAll(playlistArtistsString.split("|"))
                    currentSongIndex = songIndex
                }

                if (!songUri.isNullOrEmpty()) {
                    // Si c'est une nouvelle URI, on joue.
                    // Sinon, si c'est la même et qu'on est en pause, on reprend.
                    if (songUri == currentSongUri && mediaPlayer != null && !mediaPlayer!!.isPlaying) {
                        resumeSong()
                    } else {
                        currentSongTitle = intent.getStringExtra(EXTRA_SONG_TITLE) ?: "Chanson"
                        currentSongArtist = intent.getStringExtra(EXTRA_SONG_ARTIST) ?: "Artiste"
                        currentSongUri = songUri
                        playSong(songUri)
                    }
                } else {
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

    private fun playSong(uri: String) {
        Log.d("AudioPlayerService", "Tentative de lecture de : $uri")
        try {
            stopProgressUpdates()
            mediaPlayer?.release()

            startForegroundServiceWithNotification(true)

            mediaPlayer = MediaPlayer().apply {
                setWakeMode(this@AudioPlayerService, PowerManager.PARTIAL_WAKE_LOCK)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                
                setVolume(1.0f, 1.0f)

                if (uri.startsWith("http")) {
                    setDataSource(uri)
                } else {
                    setDataSource(this@AudioPlayerService, Uri.parse(uri))
                }
                
                prepareAsync() 
                
                setOnPreparedListener { mp ->
                    Log.d("AudioPlayerService", "Média préparé, lancement de la lecture")
                    mp.start()
                    startForegroundServiceWithNotification(true)
                    broadcastState(true)
                    startProgressUpdates()
                }

                setOnCompletionListener { playNextSong() }
                
                setOnErrorListener { mp, what, extra ->
                    val errorMsg = when (what) {
                        MediaPlayer.MEDIA_ERROR_SERVER_DIED -> "Erreur serveur"
                        MediaPlayer.MEDIA_ERROR_UNKNOWN -> "Erreur inconnue"
                        else -> "Erreur de lecture"
                    }
                    Log.e("AudioPlayerService", "Erreur MediaPlayer: $errorMsg (what=$what, extra=$extra)")
                    broadcastState(false)
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    false
                }
            }

        } catch (e: Exception) {
            Log.e("AudioPlayerService", "Erreur lors de playSong", e)
            broadcastState(false)
            stopForeground(STOP_FOREGROUND_REMOVE)
        }
    }

    private fun pauseSong() {
        mediaPlayer?.let { mp ->
            if (mp.isPlaying) {
                mp.pause()
                stopProgressUpdates()
                startForegroundServiceWithNotification(false)
                broadcastState(false)
            }
        }
    }

    private fun resumeSong() {
        mediaPlayer?.let { mp ->
            if (!mp.isPlaying) {
                mp.start()
                startForegroundServiceWithNotification(true)
                broadcastState(true)
                startProgressUpdates()
            }
        }
    }

    private fun playNextSong() {
        if (playlistUris.isEmpty()) return

        currentSongIndex = (currentSongIndex + 1) % playlistUris.size
        currentSongUri = playlistUris[currentSongIndex]
        currentSongTitle = playlistTitles.getOrNull(currentSongIndex) ?: "Chanson"
        currentSongArtist = playlistArtists.getOrNull(currentSongIndex) ?: "Artiste"

        playSong(currentSongUri)
    }

    private fun playPreviousSong() {
        if (playlistUris.isEmpty()) return

        currentSongIndex = if (currentSongIndex - 1 < 0) playlistUris.size - 1 else currentSongIndex - 1
        currentSongUri = playlistUris[currentSongIndex]
        currentSongTitle = playlistTitles.getOrNull(currentSongIndex) ?: "Chanson"
        currentSongArtist = playlistArtists.getOrNull(currentSongIndex) ?: "Artiste"

        playSong(currentSongUri)
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

    private fun startForegroundServiceWithNotification(isPlaying: Boolean) {
        val notification = createNotification(isPlaying)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
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
        try {
            val position = if (mp != null && isMediaPlayerInValidState(mp)) mp.currentPosition else 0
            val duration = if (mp != null && isMediaPlayerInValidState(mp)) mp.duration else 0
            
            val intent = Intent(ACTION_STATE_CHANGED).apply {
                putExtra(EXTRA_IS_PLAYING, isPlaying)
                putExtra(EXTRA_CURRENT_INDEX, currentSongIndex)
                putExtra(EXTRA_POSITION_MS, position)
                putExtra(EXTRA_DURATION_MS, duration)
            }
            sendBroadcast(intent)
        } catch (e: Exception) {
            Log.e("AudioPlayerService", "Erreur lors du broadcastState", e)
        }
    }

    private fun isMediaPlayerInValidState(mp: MediaPlayer): Boolean {
        return try {
            mp.currentPosition
            true
        } catch (e: Exception) {
            false
        }
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
