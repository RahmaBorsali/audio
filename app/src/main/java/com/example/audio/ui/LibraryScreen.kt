package com.example.audio.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.data.Song
import com.example.audio.ui.components.SongItem
import com.example.audio.ui.theme.MusicBackground

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.audio.ui.theme.MusicBackground
import com.example.audio.ui.theme.MusicCard
import com.example.audio.ui.theme.MusicPrimary
import com.example.audio.ui.theme.MusicTextPrimary
import com.example.audio.ui.theme.MusicTextSecondary

@Composable
fun LibraryScreen(
    librarySongs: List<Song>,
    activeSongId: Long?,
    isServicePlaying: Boolean,
    onPlayClick: (Song) -> Unit,
    onPauseClick: () -> Unit,
    onSongClick: (Song) -> Unit,
    onDownloadClick: (Song) -> Unit,
    onDeleteClick: (Song) -> Unit,
    onAddClick: () -> Unit
) {
    var selectedFilter by remember { mutableStateOf("Playlists") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MusicBackground)
            .padding(bottom = 80.dp)
    ) {
        // Header Library
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .padding(top = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(35.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF0AD4E)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, contentDescription = null, tint = Color.Black, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Bibliothèque",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onAddClick) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
            }
        }

        // Chips Filtres
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LibraryFilterChip("Playlists", selectedFilter == "Playlists") { selectedFilter = "Playlists" }
            LibraryFilterChip("Artistes", selectedFilter == "Artistes") { selectedFilter = "Artistes" }
            LibraryFilterChip("Albums", selectedFilter == "Albums") { selectedFilter = "Albums" }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (librarySongs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Votre bibliothèque est vide.", color = MusicTextSecondary)
            }
        } else {
            when (selectedFilter) {
                "Playlists" -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(librarySongs) { song ->
                            SongItem(
                                song = song,
                                isPlaying = song.id == activeSongId && isServicePlaying,
                                onPlayClick = { onPlayClick(song) },
                                onPauseClick = onPauseClick,
                                onSongClick = { onSongClick(song) },
                                onDownloadClick = { onDownloadClick(song) },
                                onDeleteClick = { onDeleteClick(song) }
                            )
                        }
                    }
                }
                "Artistes" -> {
                    val artists = librarySongs.groupBy { it.artist }
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(artists.keys.toList()) { artist ->
                            ArtistItem(artist, artists[artist]?.size ?: 0)
                        }
                    }
                }
                "Albums" -> {
                    val albums = librarySongs.groupBy { it.albumName ?: "Album Inconnu" }
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(albums.keys.toList()) { albumName ->
                            val albumSongs = albums[albumName] ?: emptyList()
                            AlbumItem(albumName, albumSongs.firstOrNull()?.artist ?: "Artiste Inconnu", albumSongs.firstOrNull()?.albumArt)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ArtistItem(name: String, songCount: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(MusicCard),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Person, contentDescription = null, tint = MusicTextSecondary, modifier = Modifier.size(30.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(name, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text("$songCount titres", color = MusicTextSecondary, fontSize = 14.sp)
        }
    }
}

@Composable
fun AlbumItem(name: String, artist: String, art: String?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MusicCard),
            contentAlignment = Alignment.Center
        ) {
            if (art != null) {
                AsyncImage(model = art, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Icon(Icons.Default.MusicNote, contentDescription = null, tint = MusicTextSecondary, modifier = Modifier.size(30.dp))
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(name, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(artist, color = MusicTextSecondary, fontSize = 14.sp)
        }
    }
}

@Composable
fun LibraryFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        color = if (selected) MusicPrimary else MusicCard,
        shape = CircleShape,
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            color = if (selected) Color.Black else Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}
