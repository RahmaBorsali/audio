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

    private var receiverRegistered = false
    private var selectedSong by mutableStateOf<Song?>(null)

    // état du service
    private var isServicePlaying by mutableStateOf(false)
    private var currentIndexFromService by mutableStateOf(-1)
    private var positionMs by mutableStateOf(0)
    private var durationMs by mutableStateOf(0)

    // pour resume
    private var lastSelectedSongId by mutableStateOf<Int?>(null)

    private val songs = listOf(
        Song(1, R.raw.song1, "Take", "Nassif Zaytoun", "3:45"),
        Song(2, R.raw.song2, "2odem mereytha", "Amr Diab", "3:45"),
        Song(3, R.raw.song3, "Men 2albi", "Hamaki", "3:28"),
        Song(4, R.raw.song4, "Khatafouni", "Amr Diab", "3:55"),
        Song(5, R.raw.song5, "yana ya la2", "Amr Diab", "4:20")
    )

    // ✅ chanson active (chargée dans le service), même si pause
    private val activeSongId: Int?
        get() = if (currentIndexFromService in songs.indices) songs[currentIndexFromService].id else null

    private val playerStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != AudioPlayerService.ACTION_STATE_CHANGED) return

            isServicePlaying = intent.getBooleanExtra(AudioPlayerService.EXTRA_IS_PLAYING, false)
            currentIndexFromService = intent.getIntExtra(AudioPlayerService.EXTRA_CURRENT_INDEX, -1)

            positionMs = intent.getIntExtra(AudioPlayerService.EXTRA_POSITION_MS, 0)
            durationMs = intent.getIntExtra(AudioPlayerService.EXTRA_DURATION_MS, 0)

            if (currentIndexFromService in songs.indices) {
                lastSelectedSongId = songs[currentIndexFromService].id
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
                        activeSongId = activeSongId,
                        isServicePlaying = isServicePlaying,
                        onPlayClick = { s ->
                            val sameSongPaused =
                                (activeSongId == s.id) && (!isServicePlaying) && (positionMs > 0)
                            if (sameSongPaused) resumeCurrent() else playSong(s)
                        },
                        onPauseClick = { pauseSong() },
                        onSongClick = { s -> selectedSong = s }
                    )
                } else {

                    // ✅ la chanson affichée dans Details est-elle la chanson active du service ?
                    val isThisSongActive = (activeSongId == song.id)

                    SongDetailsScreen(
                        song = song,

                        isPlaying = isServicePlaying && isThisSongActive,

                        positionMs = if (isThisSongActive) positionMs else 0,
                        durationMs = if (isThisSongActive) durationMs else 0,

                        onSeekTo = { newPosMs ->
                            if (isThisSongActive) seekTo(newPosMs)
                        },
                        onBack = { selectedSong = null },

                        onPlay = {
                            val sameSongPaused =
                                (activeSongId == song.id) && (!isServicePlaying) && (positionMs > 0)

                            if (sameSongPaused) resumeCurrent() else playSong(song)
                        },
                        onPause = {
                            if (isThisSongActive) pauseSong()
                        },

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
        // reset UI pour éviter flash d'ancien temps
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
    }

    private fun seekTo(positionMs: Int) {
        val intent = Intent(this, AudioPlayerService::class.java).apply {
            action = AudioPlayerService.ACTION_SEEK
            putExtra(AudioPlayerService.EXTRA_POSITION_MS, positionMs)
        }
        startService(intent)
    }
}
