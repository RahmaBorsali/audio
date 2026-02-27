package com.example.audio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.audio.data.Song
import com.example.audio.ui.theme.MusicPrimary
import com.example.audio.ui.theme.MusicTextPrimary
import com.example.audio.ui.theme.MusicTextSecondary

import androidx.compose.material.icons.filled.Delete

@Composable
fun SongItem(
    song: Song,
    isPlaying: Boolean,
    onPlayClick: () -> Unit,
    onPauseClick: () -> Unit,
    onSongClick: () -> Unit,
    onAddClick: (() -> Unit)? = null,
    onDownloadClick: (() -> Unit)? = null,
    onDeleteClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onSongClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Image de l'album ou icône
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF282828)),
            contentAlignment = Alignment.Center
        ) {
            if (song.albumArt != null) {
                AsyncImage(
                    model = song.albumArt,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = MusicTextSecondary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Infos morceau
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = if (isPlaying) MusicPrimary else MusicTextPrimary,
                maxLines = 1
            )
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = MusicTextSecondary,
                maxLines = 1
            )
        }

        // Actions (Cœur, Download, etc.)
        if (onAddClick != null) {
            IconButton(onClick = onAddClick) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add",
                    tint = MusicTextSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        if (onDownloadClick != null) {
            IconButton(onClick = onDownloadClick) {
                Icon(
                    Icons.Default.Download,
                    contentDescription = "Download",
                    tint = if (song.isDownloaded) MusicPrimary else MusicTextSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        if (onDeleteClick != null) {
            IconButton(onClick = onDeleteClick) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MusicTextSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // Bouton Play/Pause discret à droite
        IconButton(onClick = if (isPlaying) onPauseClick else onPlayClick) {
            Icon(
                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = null,
                tint = if (isPlaying) MusicPrimary else Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}
