package com.example.audio.data.remote

import com.google.gson.annotations.SerializedName

data class DeezerSearchResponse(
    val data: List<DeezerTrack>
)

data class DeezerTrack(
    val id: Long,
    val title: String,
    val duration: Int,
    val preview: String,
    val artist: DeezerArtist,
    val album: DeezerAlbum
)

data class DeezerArtist(
    val name: String
)

data class DeezerAlbum(
    val title: String,
    @SerializedName("cover_medium") val cover: String
)
