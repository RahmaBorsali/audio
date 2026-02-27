package com.example.audio.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

interface DeezerApiService {
    @GET("search")
    suspend fun searchSongs(@Query("q") query: String): DeezerSearchResponse
}
