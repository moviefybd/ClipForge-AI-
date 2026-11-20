package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val sourceUri: String,
    val sourceTitle: String,
    val sourceDurationMs: Long,
    val sourceWidth: Int,
    val sourceHeight: Int,
    val sourceRotationDegrees: Int,
    val sourceFps: Float,
    val sourceFileSizeBytes: Long,
    val thumbnailPath: String?,
    val clipStartMs: Long,
    val clipEndMs: Long,
    val cropSettingsJson: String,
    val subtitlesEnabled: Boolean,
    val subtitleCuesJson: String,
    val subtitleStyleJson: String,
    val watermarkConfigJson: String,
    val suggestedClipsJson: String,
    val latestExportUri: String?,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
