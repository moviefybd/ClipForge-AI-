package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ExportRecordDao {
    @Query("SELECT * FROM export_records ORDER BY createdAt DESC")
    fun getAllExports(): Flow<List<ExportRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExport(record: ExportRecordEntity): Long

    @Query("DELETE FROM export_records WHERE id = :id")
    suspend fun deleteExportById(id: Long)

    @Query("DELETE FROM export_records")
    suspend fun deleteAllExports()
}
