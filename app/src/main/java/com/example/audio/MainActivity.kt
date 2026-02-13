package com.example.audio

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.example.audio.data.Song
import com.example.audio.service.AudioPlayerService
import com.example.audio.ui.AudioPlayerScreen
import com.example.audio.ui.SongDetailsScreen
import com.example.audio.ui.theme.AudioTheme

class MainActivity : ComponentActivity() {

    private var currentPlayingSongId by mutableStateOf<Int?>(null)
    private var receiverRegistered = false

    private var selectedSong by mutableStateOf<Song?>(null)
    private var positionMs by mutableStateOf(0)
    private var durationMs by mutableStateOf(0)

    private var lastSelectedSongId by mutableStateOf<Int?>(null)

    private val songs = listOf(
        Song(1, R.raw.song1, "Take", "Nassif Zaytoun", "3:45"),
        Song(2, R.raw.song2, "2odem mereytha", "Amr Diab", "3:45"),
        Song(3, R.raw.song3, "Men 2albi", "Hamaki", "3:28"),
        Song(4, R.raw.song4, "Khatafouni", "Amr Diab", "3:55"),
        Song(5, R.raw.song5, "yana ya la2", "Amr Diab", "4:20")
    )

    private val playerStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != AudioPlayerService.ACTION_STATE_CHANGED) return

            val isPlaying = intent.getBooleanExtra(AudioPlayerService.EXTRA_IS_PLAYING, false)
            val index = intent.getIntExtra(AudioPlayerService.EXTRA_CURRENT_INDEX, -1)

            positionMs = intent.getIntExtra(AudioPlayerService.EXTRA_POSITION_MS, 0)
            durationMs = intent.getIntExtra(AudioPlayerService.EXTRA_DURATION_MS, 0)

            currentPlayingSongId =
                if (isPlaying && index in songs.indices) songs[index].id else null

            if (index in songs.indices) {
                lastSelectedSongId = songs[index].id
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            AudioTheme {
                val song = selectedSong

                if (song == null) {
                    AudioPlayerScreen(
                        songs = songs,
                        currentPlayingSongId = currentPlayingSongId,
                        onPlayClick = { s ->
                            val sameSongPaused =
                                (lastSelectedSongId == s.id) &&
                                        (currentPlayingSongId == null) &&
                                        (positionMs > 0)

                            if (sameSongPaused) resumeCurrent() else playSong(s)
                        },
                        onPauseClick = { pauseSong() },
                        onSongClick = { s ->
                            selectedSong = s

                            // ✅ reset affichage si ce n’est pas la chanson en cours
                            if (currentPlayingSongId != s.id) {
                                positionMs = 0
                                durationMs = 0
                            }
                        }
                    )
                } else {
                    SongDetailsScreen(
                        song = song,
                        isPlaying = (currentPlayingSongId == song.id),
                        positionMs = positionMs,
                        durationMs = durationMs,
                        onSeekTo = { newPosMs -> seekTo(newPosMs) },
                        onBack = { selectedSong = null },

                        onPlay = {
                            val sameSongPaused =
                                (lastSelectedSongId == song.id) &&
                                        (currentPlayingSongId == null) &&
                                        (positionMs > 0)

                            if (sameSongPaused) resumeCurrent() else playSong(song)
                        },
                        onPause = { pauseSong() },

                        onNext = {
                            val i = songs.indexOfFirst { it.id == song.id }
                            val next = songs[(i + 1) % songs.size]
                            selectedSong = next
                            playSong(next)
                        },
                        onPrevious = {
                            val i = songs.indexOfFirst { it.id == song.id }
                            val prev = if (i - 1 < 0) songs.last() else songs[i - 1]
                            selectedSong = prev
                            playSong(prev)
                        }
                    )
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onStart() {
        super.onStart()
        if (!receiverRegistered) {
            registerReceiver(
                playerStateReceiver,
                IntentFilter(AudioPlayerService.ACTION_STATE_CHANGED),
                RECEIVER_NOT_EXPORTED
            )
            receiverRegistered = true
        }
    }

    override fun onStop() {
        super.onStop()
        if (receiverRegistered) {
            unregisterReceiver(playerStateReceiver)
            receiverRegistered = false
        }
    }

    private fun playSong(song: Song) {
        // ✅ reset pour éviter d’afficher le time de l’ancienne chanson
        positionMs = 0
        durationMs = 0

        val playlistIds = songs.joinToString(",") { it.resourceId.toString() }
        val playlistTitles = songs.joinToString("|") { it.title }
        val playlistArtists = songs.joinToString("|") { it.artist }
        val songIndex = songs.indexOfFirst { it.id == song.id }

        val intent = Intent(this, AudioPlayerService::class.java).apply {
            action = AudioPlayerService.ACTION_PLAY
            putExtra(AudioPlayerService.EXTRA_SONG_RESOURCE_ID, song.resourceId)
            putExtra(AudioPlayerService.EXTRA_SONG_TITLE, song.title)
            putExtra(AudioPlayerService.EXTRA_SONG_ARTIST, song.artist)
            putExtra(AudioPlayerService.EXTRA_PLAYLIST_IDS, playlistIds)
            putExtra(AudioPlayerService.EXTRA_PLAYLIST_TITLES, playlistTitles)
            putExtra(AudioPlayerService.EXTRA_PLAYLIST_ARTISTS, playlistArtists)
            putExtra(AudioPlayerService.EXTRA_SONG_INDEX, songIndex)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent)
        else startService(intent)

        currentPlayingSongId = song.id
        lastSelectedSongId = song.id
    }

    private fun resumeCurrent() {
        val intent = Intent(this, AudioPlayerService::class.java).apply {
            action = AudioPlayerService.ACTION_PLAY
        }
        startService(intent)
    }

    private fun pauseSong() {
        val intent = Intent(this, AudioPlayerService::class.java).apply {
            action = AudioPlayerService.ACTION_PAUSE
        }
        startService(intent)
        currentPlayingSongId = null
    }

    private fun seekTo(positionMs: Int) {
        val intent = Intent(this, AudioPlayerService::class.java).apply {
            action = AudioPlayerService.ACTION_SEEK
            putExtra(AudioPlayerService.EXTRA_POSITION_MS, positionMs)
        }
        startService(intent)
    }
}
