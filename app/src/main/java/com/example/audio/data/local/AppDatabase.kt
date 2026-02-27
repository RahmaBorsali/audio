package com.example.audio.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.audio.data.Song

@Database(entities = [Song::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
}
