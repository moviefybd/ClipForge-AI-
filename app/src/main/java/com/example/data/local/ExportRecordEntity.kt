package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "export_records")
data class ExportRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val projectId: Long?,
    val title: String,
    val outputUri: String,
    val filePath: String,
    val sourceUri: String,
    val durationMs: Long,
    val width: Int,
    val height: Int,
    val fileSizeBytes: Long,
    val cropMode: String,
    val burnedSubtitles: Boolean,
    val burnedWatermark: Boolean,
    val createdAt: Long = System.currentTimeMillis()
)
