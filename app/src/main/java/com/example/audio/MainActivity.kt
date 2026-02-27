package com.example.audio

import android.Manifest
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.*
import androidx.room.Room
import com.example.audio.data.Song
import com.example.audio.data.local.AppDatabase
import com.example.audio.data.remote.DeezerApiService
import com.example.audio.service.AudioPlayerService
import com.example.audio.ui.*
import com.example.audio.ui.navigation.Screen
import com.example.audio.ui.components.MiniPlayer
import com.example.audio.ui.theme.AudioTheme
import com.example.audio.ui.theme.MusicBackground
import com.example.audio.ui.theme.MusicPrimary
import com.example.audio.ui.theme.MusicSurface
import com.example.audio.ui.theme.MusicTextPrimary
import com.example.audio.ui.theme.MusicTextSecondary
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.Serializable
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    private var receiverRegistered = false
    private var selectedSong by mutableStateOf<Song?>(null)

    private var isServicePlaying by mutableStateOf(false)
    private var currentIndexFromService by mutableStateOf(-1)
    private var positionMs by mutableStateOf(0)
    private var durationMs by mutableStateOf(0)

    private var lastSelectedSongId by mutableStateOf<Long?>(null)
    private var isPlayerVisible by mutableStateOf(false)

    private val songs = mutableStateListOf<Song>()
    private val librarySongs = mutableStateListOf<Song>()

    private val db by lazy {
        Room.databaseBuilder(applicationContext, AppDatabase::class.java, "audio-db")
            .fallbackToDestructiveMigration()
            .build()
    }

    private val apiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.deezer.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(DeezerApiService::class.java)
    }

    private var currentPlaylist = mutableStateListOf<Song>()

    private val activeSongId: Long?
        get() {
            val index = currentIndexFromService
            val playlist = currentPlaylist
            return if (index in playlist.indices) {
                playlist[index].id
            } else {
                selectedSong?.id
            }
        }

    private val playerStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != AudioPlayerService.ACTION_STATE_CHANGED) return

            isServicePlaying = intent.getBooleanExtra(AudioPlayerService.EXTRA_IS_PLAYING, false)
            currentIndexFromService = intent.getIntExtra(AudioPlayerService.EXTRA_CURRENT_INDEX, -1)

            positionMs = intent.getIntExtra(AudioPlayerService.EXTRA_POSITION_MS, 0)
            durationMs = intent.getIntExtra(AudioPlayerService.EXTRA_DURATION_MS, 0)

            if (currentIndexFromService in currentPlaylist.indices) {
                selectedSong = currentPlaylist[currentIndexFromService]
                lastSelectedSongId = selectedSong?.id
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) {
            loadLocalSongs()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        checkPermissions()

        lifecycleScope.launch {
            db.songDao().getLibrarySongs().collectLatest { items ->
                librarySongs.clear()
                librarySongs.addAll(items)
            }
        }

        setContent {
            AudioTheme {
                val navController = rememberNavController()
                val scope = rememberCoroutineScope()
                val currentSong = selectedSong
                var songToDelete by remember { mutableStateOf<Song?>(null) }

                Box(modifier = Modifier.fillMaxSize().background(MusicBackground)) {
                    Scaffold(
                        bottomBar = {
                            Column(modifier = Modifier.background(Color.Transparent)) {
                                // MiniPlayer qui s'affiche si une musique est sélectionnée
                                currentSong?.let { song ->
                                    val isThisSongActive = (activeSongId == song.id)
                                    MiniPlayer(
                                        song = song,
                                        isPlaying = isServicePlaying && isThisSongActive,
                                        onPlayPauseClick = {
                                            if (isServicePlaying && isThisSongActive) pauseSong()
                                            else if (!isServicePlaying && isThisSongActive) resumeCurrent()
                                            else playSong(song)
                                        },
                                        onClick = { isPlayerVisible = true }
                                    )
                                }

                                NavigationBar(
                                    containerColor = Color.Black.copy(alpha = 0.95f),
                                    tonalElevation = 0.dp
                                ) {
                                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                                    val currentDestination = navBackStackEntry?.destination
                                    val screens = listOf(Screen.Home, Screen.Search, Screen.Library, Screen.LocalMusic)
                                    for (screen in screens) {
                                        NavigationBarItem(
                                            icon = {
                                                Icon(
                                                    when (screen) {
                                                        Screen.Home -> Icons.Default.Home
                                                        Screen.Search -> Icons.Default.Search
                                                        Screen.Library -> Icons.Default.LibraryMusic
                                                        Screen.LocalMusic -> Icons.Default.MusicNote
                                                    },
                                                    contentDescription = null
                                                )
                                            },
                                            label = { Text(screen.label, fontSize = 10.sp) },
                                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                                            onClick = {
                                                navController.navigate(screen.route) {
                                                    popUpTo(navController.graph.findStartDestination().id) {
                                                        saveState = true
                                                    }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = Color.White,
                                                selectedTextColor = Color.White,
                                                unselectedIconColor = MusicTextSecondary,
                                                unselectedTextColor = MusicTextSecondary,
                                                indicatorColor = Color.Transparent
                                            )
                                        )
                                    }
                                }
                            }
                        },
                        containerColor = Color.Transparent
                    ) { innerPadding ->
                        NavHost(
                            navController, 
                            startDestination = Screen.Home.route, 
                            modifier = Modifier.padding(innerPadding)
                        ) {
                            composable(Screen.Home.route) {
                                HomeScreen(
                                    recentSongs = librarySongs,
                                    activeSongId = activeSongId,
                                    isServicePlaying = isServicePlaying,
                                    onPlayClick = { s -> playSong(s, librarySongs) },
                                    onPauseClick = { pauseSong() },
                                    onSongClick = { s -> 
                                        selectedSong = s
                                        isPlayerVisible = true
                                    }
                                )
                            }
                            composable(Screen.Search.route) {
                                SearchScreen(
                                    apiService = apiService,
                                    activeSongId = activeSongId,
                                    isServicePlaying = isServicePlaying,
                                    onPlayClick = { s, list -> playSong(s, list) },
                                    onPauseClick = { pauseSong() },
                                    onSongClick = { s -> 
                                        selectedSong = s
                                        isPlayerVisible = true
                                    },
                                    onAddToLibrary = { s ->
                                        scope.launch {
                                            db.songDao().insertSong(s)
                                            Toast.makeText(this@MainActivity, "Ajouté à la bibliothèque", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )
                            }
                            composable(Screen.Library.route) {
                                LibraryScreen(
                                    librarySongs = librarySongs,
                                    activeSongId = activeSongId,
                                    isServicePlaying = isServicePlaying,
                                    onPlayClick = { s -> playSong(s, librarySongs) },
                                    onPauseClick = { pauseSong() },
                                    onSongClick = { s -> 
                                        selectedSong = s
                                        isPlayerVisible = true
                                    },
                                    onDownloadClick = { s ->
                                        downloadSong(s)
                                    },
                                    onDeleteClick = { s ->
                                        songToDelete = s
                                    },
                                    onAddClick = {
                                        navController.navigate(Screen.Search.route)
                                    }
                                )
                            }
                            composable(Screen.LocalMusic.route) {
                                AudioPlayerScreen(
                                    songs = songs,
                                    activeSongId = activeSongId,
                                    isServicePlaying = isServicePlaying,
                                    onPlayClick = { s ->
                                        val sameSongPaused = (activeSongId == s.id) && (!isServicePlaying) && (positionMs > 0)
                                        if (sameSongPaused) resumeCurrent() else playSong(s, songs)
                                    },
                                    onPauseClick = { pauseSong() },
                                    onSongClick = { s -> 
                                        selectedSong = s
                                        isPlayerVisible = true
                                    }
                                )
                            }
                        }
                    }

                    // Full Screen Player Overlay
                    if (isPlayerVisible && currentSong != null) {
                        val isThisSongActive = (activeSongId == currentSong.id)
                        SongDetailsScreen(
                            song = currentSong,
                            isPlaying = isServicePlaying && isThisSongActive,
                            positionMs = if (isThisSongActive) positionMs else 0,
                            durationMs = if (isThisSongActive) durationMs else 0,
                            onSeekTo = { newPosMs -> if (isThisSongActive) seekTo(newPosMs) },
                            onBack = { isPlayerVisible = false },
                            onPlay = {
                                val sameSongPaused = (activeSongId == currentSong.id) && (!isServicePlaying) && (positionMs > 0)
                                if (sameSongPaused) resumeCurrent() else playSong(currentSong)
                            },
                            onPause = { if (isThisSongActive) pauseSong() },
                            onNext = {
                                val i = currentPlaylist.indexOfFirst { it.id == currentSong.id }
                                if (i != -1) {
                                    val next = currentPlaylist[(i + 1) % currentPlaylist.size]
                                    playSong(next, currentPlaylist)
                                }
                            },
                            onPrevious = {
                                val i = currentPlaylist.indexOfFirst { it.id == currentSong.id }
                                if (i != -1) {
                                    val prev = if (i - 1 < 0) currentPlaylist.last() else currentPlaylist[i - 1]
                                    playSong(prev, currentPlaylist)
                                }
                            }
                        )
                    }

                    // Dialog de confirmation de suppression
                    songToDelete?.let { song ->
                        AlertDialog(
                            onDismissRequest = { songToDelete = null },
                            title = { Text("Supprimer du téléphone ?") },
                            text = { Text("Voulez-vous vraiment supprimer \"${song.title}\" de votre téléphone ? Cette action est irréversible.") },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        deleteSongPhysically(song)
                                        songToDelete = null
                                    }
                                ) {
                                    Text("Supprimer", color = Color.Red)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { songToDelete = null }) {
                                    Text("Annuler")
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (!receiverRegistered) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(
                    playerStateReceiver,
                    IntentFilter(AudioPlayerService.ACTION_STATE_CHANGED),
                    Context.RECEIVER_NOT_EXPORTED
                )
            } else {
                registerReceiver(
                    playerStateReceiver,
                    IntentFilter(AudioPlayerService.ACTION_STATE_CHANGED)
                )
            }
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

    private fun playSong(song: Song, playlist: List<Song>? = null) {
        positionMs = 0
        durationMs = 0

        // Si une playlist est fournie, on l'utilise. Sinon, si c'est une musique locale, 
        // on prend la liste locale. Sinon, on crée une playlist avec juste ce morceau.
        val finalPlaylist = playlist ?: if (songs.any { it.id == song.id }) songs else listOf(song)
        
        currentPlaylist.clear()
        currentPlaylist.addAll(finalPlaylist)

        val playlistUris = finalPlaylist.joinToString(",") { it.uri }
        val playlistTitles = finalPlaylist.joinToString("|") { it.title }
        val playlistArtists = finalPlaylist.joinToString("|") { it.artist }
        val songIndex = finalPlaylist.indexOfFirst { it.id == song.id }

        val intent = Intent(this, AudioPlayerService::class.java).apply {
            action = AudioPlayerService.ACTION_PLAY
            putExtra(AudioPlayerService.EXTRA_SONG_URI, song.uri)
            putExtra(AudioPlayerService.EXTRA_SONG_TITLE, song.title)
            putExtra(AudioPlayerService.EXTRA_SONG_ARTIST, song.artist)
            putExtra(AudioPlayerService.EXTRA_PLAYLIST_URIS, playlistUris)
            putExtra(AudioPlayerService.EXTRA_PLAYLIST_TITLES, playlistTitles)
            putExtra(AudioPlayerService.EXTRA_PLAYLIST_ARTISTS, playlistArtists)
            putExtra(AudioPlayerService.EXTRA_SONG_INDEX, songIndex)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent)
        else startService(intent)

        lastSelectedSongId = song.id
        selectedSong = song
    }

    private fun checkPermissions() {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        val notGranted = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (notGranted.isEmpty()) {
            loadLocalSongs()
        } else {
            requestPermissionLauncher.launch(notGranted.toTypedArray())
        }
    }

    private fun loadLocalSongs() {
        val songList = mutableListOf<Song>()
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"

        contentResolver.query(
            collection,
            projection,
            selection,
            null,
            "${MediaStore.Audio.Media.TITLE} ASC"
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val title = cursor.getString(titleColumn)
                val artist = cursor.getString(artistColumn)
                val durationMs = cursor.getLong(durationColumn)
                val album = cursor.getString(albumColumn)
                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    id
                ).toString()

                val duration = formatDuration(durationMs)

                songList.add(Song(id, contentUri, title, artist, duration, albumName = album))
            }
        }
        songs.clear()
        songs.addAll(songList)
    }

    private fun formatDuration(durationMs: Long): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMs) % 60
        return String.format("%d:%02d", minutes, seconds)
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

    private fun downloadSong(song: Song) {
        if (!song.uri.startsWith("http")) {
            Toast.makeText(this, "Cette musique est déjà sur votre téléphone", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val request = DownloadManager.Request(Uri.parse(song.uri))
                .setTitle(song.title)
                .setDescription("Téléchargement de ${song.artist}")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "${song.title}.mp3")
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

            val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadManager.enqueue(request)

            Toast.makeText(this, "Téléchargement commencé...", Toast.LENGTH_SHORT).show()
            
            lifecycleScope.launch {
                db.songDao().updateDownloadStatus(song.id, true)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Erreur de téléchargement : ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun deleteSongPhysically(song: Song) {
        lifecycleScope.launch {
            try {
                // 1. Supprimer de la bibliothèque (Base de données)
                db.songDao().deleteSong(song)
                
                // 2. Si c'est un fichier local (URI MediaStore)
                if (!song.uri.startsWith("http")) {
                    val uri = Uri.parse(song.uri)
                    try {
                        contentResolver.delete(uri, null, null)
                    } catch (e: SecurityException) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && e is android.app.RecoverableSecurityException) {
                            Toast.makeText(this@MainActivity, "Permission requise pour supprimer le fichier physique", Toast.LENGTH_LONG).show()
                        }
                    }
                    loadLocalSongs()
                } else if (song.isDownloaded) {
                    // 3. Si c'est un titre téléchargé via l'app (dans le dossier Downloads)
                    try {
                        val file = java.io.File(
                            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                            "${song.title}.mp3"
                        )
                        if (file.exists()) {
                            file.delete()
                        }
                    } catch (e: Exception) {
                        Log.e("MainActivity", "Erreur lors de la suppression du fichier téléchargé", e)
                    }
                }

                Toast.makeText(this@MainActivity, "\"${song.title}\" retiré de la bibliothèque", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e("MainActivity", "Erreur lors de la suppression", e)
                Toast.makeText(this@MainActivity, "Erreur : ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
