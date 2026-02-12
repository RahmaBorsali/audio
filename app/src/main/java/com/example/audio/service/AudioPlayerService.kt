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
import com.example.audio.R

/**
 * Service qui gère la lecture audio en arrière-plan
 * Ce service continue de fonctionner même quand l'application est fermée
 */
class AudioPlayerService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var currentSongTitle: String = ""
    private var currentSongArtist: String = ""
    private var currentResourceId: Int = -1 // Sauvegarder l'ID de la chanson actuelle
    
    companion object {
        const val ACTION_PLAY = "ACTION_PLAY"
        const val ACTION_PAUSE = "ACTION_PAUSE"
        const val ACTION_STOP = "ACTION_STOP"
        const val EXTRA_SONG_RESOURCE_ID = "EXTRA_SONG_RESOURCE_ID"
        const val EXTRA_SONG_TITLE = "EXTRA_SONG_TITLE"
        const val EXTRA_SONG_ARTIST = "EXTRA_SONG_ARTIST"
        
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "audio_player_channel"
    }

    override fun onCreate() {
        super.onCreate()
        // Créer le canal de notification (nécessaire pour Android 8.0+)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> {
                val resourceId = intent.getIntExtra(EXTRA_SONG_RESOURCE_ID, -1)
                
                // Si on a un nouvel ID de ressource, c'est une nouvelle chanson
                if (resourceId != -1 && resourceId != 0) {
                    currentSongTitle = intent.getStringExtra(EXTRA_SONG_TITLE) ?: "Chanson"
                    currentSongArtist = intent.getStringExtra(EXTRA_SONG_ARTIST) ?: "Artiste"
                    currentResourceId = resourceId
                    playSong(resourceId)
                } else {
                    // Sinon, reprendre la chanson en pause
                    resumeSong()
                }
            }
            ACTION_PAUSE -> {
                pauseSong()
            }
            ACTION_STOP -> {
                stopSong()
                stopSelf() // Arrêter le service
            }
        }
        
        return START_STICKY // Le service redémarre automatiquement s'il est tué
    }

    /**
     * Démarre la lecture d'une chanson
     */
    private fun playSong(resourceId: Int) {
        try {
            // Si une chanson est déjà en cours, l'arrêter
            mediaPlayer?.release()
            
            // Créer un nouveau MediaPlayer avec la ressource audio
            mediaPlayer = MediaPlayer.create(this, resourceId)
            
            // Configurer le listener pour savoir quand la chanson se termine
            mediaPlayer?.setOnCompletionListener {
                // Quand la chanson se termine, on arrête le service
                stopSong()
            }
            
            // Démarrer la lecture
            mediaPlayer?.start()
            
            // Afficher la notification en mode foreground
            startForeground(NOTIFICATION_ID, createNotification(true))
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Met en pause la chanson en cours
     */
    private fun pauseSong() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                // Mettre à jour la notification pour afficher le bouton Play
                startForeground(NOTIFICATION_ID, createNotification(false))
            }
        }
    }

    /**
     * Reprend la lecture de la chanson en pause
     */
    private fun resumeSong() {
        mediaPlayer?.let {
            if (!it.isPlaying) {
                it.start()
                // Mettre à jour la notification pour afficher le bouton Pause
                startForeground(NOTIFICATION_ID, createNotification(true))
            }
        }
    }

    /**
     * Arrête complètement la lecture
     */
    private fun stopSong() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
            }
            it.release()
        }
        mediaPlayer = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    /**
     * Crée le canal de notification (obligatoire pour Android 8.0+)
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Lecteur Audio",
                NotificationManager.IMPORTANCE_LOW // Importance basse pour ne pas déranger
            ).apply {
                description = "Contrôles de lecture audio"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Crée la notification affichée pendant la lecture
     */
    private fun createNotification(isPlaying: Boolean): Notification {
        // Intent pour ouvrir l'application quand on clique sur la notification
        val openAppIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Intent pour le bouton Play/Pause
        val playPauseAction = if (isPlaying) {
            val pauseIntent = Intent(this, AudioPlayerService::class.java).apply {
                action = ACTION_PAUSE
            }
            val pausePendingIntent = PendingIntent.getService(
                this,
                0,
                pauseIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            NotificationCompat.Action(
                android.R.drawable.ic_media_pause,
                "Pause",
                pausePendingIntent
            )
        } else {
            val playIntent = Intent(this, AudioPlayerService::class.java).apply {
                action = ACTION_PLAY
                putExtra(EXTRA_SONG_RESOURCE_ID, 0) // Reprendre la chanson actuelle
            }
            val playPendingIntent = PendingIntent.getService(
                this,
                0,
                playIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            NotificationCompat.Action(
                android.R.drawable.ic_media_play,
                "Play",
                playPendingIntent
            )
        }

        // Intent pour le bouton Stop
        val stopIntent = Intent(this, AudioPlayerService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1, // ID différent pour éviter les conflits
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val stopAction = NotificationCompat.Action(
            android.R.drawable.ic_delete,
            "Stop",
            stopPendingIntent
        )

        // Construire la notification avec les deux boutons
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(currentSongTitle)
            .setContentText(currentSongArtist)
            .setSmallIcon(android.R.drawable.ic_media_play) // Icône par défaut
            .setContentIntent(pendingIntent)
            .addAction(playPauseAction) // Bouton Play ou Pause
            .addAction(stopAction)       // Bouton Stop
            .setOngoing(isPlaying) // La notification ne peut pas être balayée si en lecture
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? {
        // Ce service n'est pas bindable
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        // Libérer les ressources quand le service est détruit
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
