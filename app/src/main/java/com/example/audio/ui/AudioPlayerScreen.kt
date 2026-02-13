package com.example.audio.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.data.Song
import com.example.audio.ui.components.SongItem
import com.example.audio.ui.theme.MusicBackground
import com.example.audio.ui.theme.MusicPrimary
import com.example.audio.ui.theme.MusicSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioPlayerScreen(
    songs: List<Song>,
    activeSongId: Int?,          // ✅ chanson chargée dans le service (même en pause)
    isServicePlaying: Boolean,   // ✅ play/pause réel
    onPlayClick: (Song) -> Unit,
    onPauseClick: () -> Unit,
    onSongClick: (Song) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Ma Musique",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                ),
                modifier = Modifier.background(
                    brush = Brush.verticalGradient(
                        colors = listOf(MusicPrimary, MusicSecondary)
                    )
                )
            )
        },
        containerColor = MusicBackground
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (songs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Aucune chanson disponible\nAjoutez des fichiers MP3 dans res/raw/",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(songs) { song ->
                        val isThisRowPlaying = (song.id == activeSongId) && isServicePlaying

                        SongItem(
                            song = song,
                            isPlaying = isThisRowPlaying, // ✅ uniquement si le service joue ET c'est la chanson active
                            onPlayClick = { onPlayClick(song) },
                            onPauseClick = onPauseClick,
                            onSongClick = { onSongClick(song) }
                        )
                    }
                }
            }
        }
    }
}
