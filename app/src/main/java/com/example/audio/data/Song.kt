package com.example.audio.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "songs")
data class Song(
    @PrimaryKey val id: Long,
    val uri: String,
    val title: String,
    val artist: String,
    val duration: String,
    val albumArt: String? = null,
    val isLocal: Boolean = true,
    val isDownloaded: Boolean = false,
    val albumName: String? = "Album Inconnu"
)
