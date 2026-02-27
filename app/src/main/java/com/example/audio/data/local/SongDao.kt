package com.example.audio.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.audio.data.Song
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {
    @Query("SELECT * FROM songs WHERE isLocal = 0")
    fun getLibrarySongs(): Flow<List<Song>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSong(song: Song)

    @Delete
    suspend fun deleteSong(song: Song)

    @Query("SELECT EXISTS(SELECT * FROM songs WHERE id = :id)")
    suspend fun isSongInLibrary(id: Long): Boolean

    @Query("UPDATE songs SET isDownloaded = :downloaded WHERE id = :id")
    suspend fun updateDownloadStatus(id: Long, downloaded: Boolean)
}
