package com.example.audio.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.audio.data.Song
import com.example.audio.data.remote.DeezerApiService
import com.example.audio.ui.components.SongItem
import com.example.audio.ui.theme.MusicBackground
import com.example.audio.ui.theme.MusicPrimary
import kotlinx.coroutines.launch

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.audio.ui.theme.MusicBackground
import com.example.audio.ui.theme.MusicCard
import com.example.audio.ui.theme.MusicPrimary
import com.example.audio.ui.theme.MusicTextPrimary
import com.example.audio.ui.theme.MusicTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    apiService: DeezerApiService,
    activeSongId: Long?,
    isServicePlaying: Boolean,
    onPlayClick: (Song, List<Song>) -> Unit,
    onPauseClick: () -> Unit,
    onSongClick: (Song) -> Unit,
    onAddToLibrary: (Song) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<Song>>(emptyList()) }
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MusicBackground)
            .padding(bottom = 80.dp)
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = "Rechercher",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Spotify Style Search Bar
        TextField(
            value = query,
            onValueChange = { 
                query = it
                if (it.length > 2) {
                    scope.launch {
                        isLoading = true
                        try {
                            val response = apiService.searchSongs(it)
                            searchResults = response.data.map { track ->
                                Song(
                                    id = track.id,
                                    uri = track.preview,
                                    title = track.title,
                                    artist = track.artist.name,
                                    duration = "${track.duration / 60}:${String.format("%02d", track.duration % 60)}",
                                    albumArt = track.album.cover,
                                    isLocal = false,
                                    albumName = track.album.title
                                )
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        } finally {
                            isLoading = false
                        }
                    }
                } else if (it.isEmpty()) {
                    searchResults = emptyList()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(4.dp)),
            placeholder = { Text("Que souhaitez-vous écouter ?", color = Color.DarkGray) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Black) },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black,
                cursorColor = Color.Black,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MusicPrimary)
            }
        } else if (searchResults.isEmpty() && query.isEmpty()) {
            // Afficher des catégories ou rien
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Explorez des millions de titres", color = MusicTextSecondary)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(searchResults) { song ->
                    SongItem(
                        song = song,
                        isPlaying = song.id == activeSongId && isServicePlaying,
                        onPlayClick = { onPlayClick(song, searchResults) },
                        onPauseClick = onPauseClick,
                        onSongClick = { onSongClick(song) },
                        onAddClick = { onAddToLibrary(song) }
                    )
                }
            }
        }
    }
}
