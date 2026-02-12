package com.example.audio

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import com.example.audio.data.Song
import com.example.audio.service.AudioPlayerService
import com.example.audio.ui.AudioPlayerScreen
import com.example.audio.ui.theme.AudioTheme

/**
 * Activité principale de l'application
 * Gère l'interface utilisateur et la communication avec le service audio
 */
class MainActivity : ComponentActivity() {

    // ID de la chanson actuellement en cours de lecture
    private var currentPlayingSongId by mutableStateOf<Int?>(null)

    // Gestionnaire de permission pour les notifications (Android 13+)
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // On peut continuer même si la permission est refusée
        // Les notifications ne s'afficheront simplement pas
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Demander la permission pour les notifications sur Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Liste des chansons de démonstration
        // NOTE: Vous devez ajouter vos propres fichiers MP3 dans res/raw/
        val songs = listOf(
            Song(
                id = 1,
                resourceId = R.raw.song1,
                title = "Chanson 1",
                artist = "Artiste 1",
                duration = "3:45"
            ),
            Song(
                id = 2,
                resourceId = R.raw.song2,
                title = "Chanson 2",
                artist = "Artiste 2",
                duration = "4:12"
            ),
            Song(
                id = 3,
                resourceId = R.raw.song3,
                title = "Chanson 3",
                artist = "Artiste 3",
                duration = "3:28"
            )
        )

        setContent {
            AudioTheme {
                AudioPlayerScreen(
                    songs = songs,
                    currentPlayingSongId = currentPlayingSongId,
                    onPlayClick = { song ->
                        playSong(song)
                    },
                    onPauseClick = {
                        pauseSong()
                    }
                )
            }
        }
    }

    /**
     * Démarre la lecture d'une chanson via le service
     */
    private fun playSong(song: Song) {
        val intent = Intent(this, AudioPlayerService::class.java).apply {
            action = AudioPlayerService.ACTION_PLAY
            putExtra(AudioPlayerService.EXTRA_SONG_RESOURCE_ID, song.resourceId)
            putExtra(AudioPlayerService.EXTRA_SONG_TITLE, song.title)
            putExtra(AudioPlayerService.EXTRA_SONG_ARTIST, song.artist)
        }

        // Démarrer le service en mode foreground
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }

        // Mettre à jour l'état de l'interface
        currentPlayingSongId = song.id
    }

    /**
     * Met en pause la chanson en cours
     */
    private fun pauseSong() {
        val intent = Intent(this, AudioPlayerService::class.java).apply {
            action = AudioPlayerService.ACTION_PAUSE
        }
        startService(intent)

        // Mettre à jour l'état de l'interface
        currentPlayingSongId = null
    }

    override fun onDestroy() {
        super.onDestroy()
        // Note: Le service continue de fonctionner même après la fermeture de l'activité
        // C'est le comportement souhaité pour la lecture en arrière-plan
    }
}