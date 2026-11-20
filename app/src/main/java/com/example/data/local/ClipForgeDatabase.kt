package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ProjectEntity::class, ExportRecordEntity::class],
    version = 1,
    exportSchema = false
)
abstract class ClipForgeDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun exportRecordDao(): ExportRecordDao

    companion object {
        @Volatile
        private var INSTANCE: ClipForgeDatabase? = null

        fun getInstance(context: Context): ClipForgeDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    ClipForgeDatabase::class.java,
                    "clipforge_studio.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
