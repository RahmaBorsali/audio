package com.example.audio.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.data.Song
import com.example.audio.ui.theme.MusicBackground
import com.example.audio.ui.theme.MusicPrimary
import com.example.audio.ui.theme.MusicSecondary
import kotlin.math.roundToInt

import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongDetailsScreen(
    song: Song,
    isPlaying: Boolean,
    positionMs: Int,
    durationMs: Int,
    onSeekTo: (Int) -> Unit,
    onBack: () -> Unit,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
) {
    val sliderEnabled = durationMs > 0
    val dur = if (durationMs > 0) durationMs else 1
    val pos = if (durationMs > 0) positionMs.coerceIn(0, dur) else 0

    val sliderValueFromPlayer = (pos.toFloat() / dur).coerceIn(0f, 1f)

    var userDragging by remember { mutableStateOf(false) }
    var sliderUiValue by remember { mutableStateOf(sliderValueFromPlayer) }

    LaunchedEffect(sliderValueFromPlayer) {
        if (!userDragging) sliderUiValue = sliderValueFromPlayer
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text("PLAYING FROM PLAYLIST", fontSize = 10.sp, letterSpacing = 1.sp, color = Color.White.copy(alpha = 0.7f))
                        Text("Ma Musique", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ExpandMore, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                },
                actions = {
                    IconButton(onClick = { /* Menu */ }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Black // Fond noir pur style Spotify
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(30.dp))

            // Cover Art (Gros carré)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(8.dp))
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
                    Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color.White.copy(alpha = 0.3f), modifier = Modifier.size(100.dp))
                }
            }

            Spacer(Modifier.height(40.dp))

            // Title and Artist
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1
                    )
                    Text(
                        text = song.artist,
                        fontSize = 16.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        maxLines = 1
                    )
                }
                IconButton(onClick = { /* Favorite */ }) {
                    Icon(Icons.Default.FavoriteBorder, contentDescription = "Like", tint = MusicPrimary, modifier = Modifier.size(28.dp))
                }
            }

            Spacer(Modifier.height(20.dp))

            // Seekbar
            Column {
                Slider(
                    value = sliderUiValue,
                    enabled = sliderEnabled,
                    onValueChange = { v ->
                        userDragging = true
                        sliderUiValue = v
                    },
                    onValueChangeFinished = {
                        if (!sliderEnabled) return@Slider
                        userDragging = false
                        onSeekTo((sliderUiValue * dur).roundToInt())
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.White,
                        inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val shownPos = if (sliderEnabled) {
                        if (userDragging) (sliderUiValue * dur).roundToInt() else pos
                    } else 0

                    Text(formatTime(shownPos), color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)

                    Text(
                        if (sliderEnabled) formatTime(dur) else song.duration,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { /* Shuffle */ }) {
                    Icon(Icons.Default.MusicNote, contentDescription = "Shuffle", tint = MusicPrimary, modifier = Modifier.size(24.dp))
                }

                IconButton(onClick = onPrevious) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", tint = Color.White, modifier = Modifier.size(44.dp))
                }

                // Play/Pause Button (Gros bouton blanc)
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = { if (isPlaying) onPause() else onPlay() },
                        modifier = Modifier.size(64.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "PlayPause",
                            tint = Color.Black,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                IconButton(onClick = onNext) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = Color.White, modifier = Modifier.size(44.dp))
                }

                IconButton(onClick = { /* Repeat */ }) {
                    Icon(Icons.Default.MusicNote, contentDescription = "Repeat", tint = MusicPrimary, modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color.White.copy(alpha = 0.65f))
        Text(value, color = Color.White, fontWeight = FontWeight.SemiBold)
    }
    Spacer(Modifier.height(8.dp))
}

private fun formatTime(ms: Int): String {
    val totalSec = ms / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return "%d:%02d".format(min, sec)
}
