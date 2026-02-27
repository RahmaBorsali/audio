package com.example.audio.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.audio.data.Song
import com.example.audio.ui.theme.MusicBackground
import com.example.audio.ui.theme.MusicCard
import com.example.audio.ui.theme.MusicPrimary
import com.example.audio.ui.theme.MusicTextPrimary
import com.example.audio.ui.theme.MusicTextSecondary
import com.example.audio.ui.components.SongItem

@Composable
fun HomeScreen(
    recentSongs: List<Song>,
    activeSongId: Long?,
    isServicePlaying: Boolean,
    onPlayClick: (Song) -> Unit,
    onPauseClick: () -> Unit,
    onSongClick: (Song) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MusicBackground)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 100.dp) // Pour ne pas être caché par le miniplayer
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = "Bonjour",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (recentSongs.isNotEmpty()) {
            SectionTitle("Écoutés récemment")
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                items(recentSongs) { song ->
                    val isPlaying = (song.id == activeSongId) && isServicePlaying
                    HomeSongCard(song, isPlaying, onSongClick)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        SectionTitle("Découvrir")
        // Liste verticale pour la découverte
        recentSongs.take(10).forEach { song ->
            val isThisRowPlaying = (song.id == activeSongId) && isServicePlaying
            SongItem(
                song = song,
                isPlaying = isThisRowPlaying,
                onPlayClick = { onPlayClick(song) },
                onPauseClick = onPauseClick,
                onSongClick = { onSongClick(song) }
            )
        }
        
        if (recentSongs.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Text("Recherchez des titres pour commencer !", color = MusicTextSecondary, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
fun HomeSongCard(song: Song, isPlaying: Boolean, onClick: (Song) -> Unit) {
    Column(
        modifier = Modifier
            .width(150.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isPlaying) Color.White.copy(alpha = 0.1f) else MusicCard)
            .clickable { onClick(song) }
            .padding(12.dp)
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            AsyncImage(
                model = song.albumArt,
                contentDescription = null,
                modifier = Modifier
                    .size(126.dp)
                    .clip(RoundedCornerShape(4.dp)),
                contentScale = ContentScale.Crop
            )
            if (isPlaying) {
                Icon(
                    imageVector = Icons.Default.Pause,
                    contentDescription = null,
                    tint = MusicPrimary,
                    modifier = Modifier
                        .padding(8.dp)
                        .size(24.dp)
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                        .padding(4.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = song.title,
            color = if (isPlaying) MusicPrimary else MusicTextPrimary,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            fontSize = 14.sp
        )
        Text(
            text = song.artist,
            color = MusicTextSecondary,
            fontSize = 12.sp,
            maxLines = 1
        )
    }
}
