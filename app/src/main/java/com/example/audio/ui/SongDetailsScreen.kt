package com.example.audio.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
            CenterAlignedTopAppBar(
                title = { Text("Now Playing", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent),
                modifier = Modifier.background(Brush.verticalGradient(listOf(MusicPrimary, MusicSecondary)))
            )
        },
        containerColor = MusicBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(18.dp))

            Box(
                modifier = Modifier
                    .size(190.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(MusicSecondary.copy(alpha = 0.55f), MusicPrimary.copy(alpha = 0.35f))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color.White, modifier = Modifier.size(74.dp))
            }

            Spacer(Modifier.height(20.dp))

            Text(song.title, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.height(6.dp))
            Text(song.artist, fontSize = 16.sp, color = Color.White.copy(alpha = 0.7f))

            Spacer(Modifier.height(22.dp))

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
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val shownPos = if (sliderEnabled) {
                    if (userDragging) (sliderUiValue * dur).roundToInt() else pos
                } else 0

                Text(formatTime(shownPos), color = Color.White.copy(alpha = 0.7f))

                // ✅ si pas actif, afficher la durée du modèle
                Text(
                    if (sliderEnabled) formatTime(dur) else song.duration,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }

            Spacer(Modifier.height(28.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPrevious) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", tint = Color.White, modifier = Modifier.size(34.dp))
                }

                Spacer(Modifier.width(18.dp))

                Box(
                    modifier = Modifier
                        .size(74.dp)
                        .clip(CircleShape)
                        .background(Brush.verticalGradient(listOf(MusicPrimary, MusicSecondary))),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = { if (isPlaying) onPause() else onPlay() },
                        modifier = Modifier.size(74.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "PlayPause",
                            tint = Color.White,
                            modifier = Modifier.size(38.dp)
                        )
                    }
                }

                Spacer(Modifier.width(18.dp))

                IconButton(onClick = onNext) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = Color.White, modifier = Modifier.size(34.dp))
                }
            }

            Spacer(Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.06f)),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Détails", color = Color.White, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(10.dp))
                    DetailRow("Titre", song.title)
                    DetailRow("Artiste", song.artist)
                    DetailRow("Durée", if (durationMs > 0) formatTime(durationMs) else song.duration)
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
