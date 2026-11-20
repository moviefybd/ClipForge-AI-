package com.example.data.local

import com.example.model.CropSettings
import com.example.model.SubtitleCue
import com.example.model.SubtitleStyle
import com.example.model.SuggestedClip
import com.example.model.VideoMetadata
import com.example.model.WatermarkConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

data class ProjectWorkspaceData(
    val id: Long = 0L,
    val name: String,
    val metadata: VideoMetadata,
    val clipStartMs: Long = 0L,
    val clipEndMs: Long = metadata.durationMs,
    val cropSettings: CropSettings = CropSettings(),
    val subtitlesEnabled: Boolean = false,
    val subtitleCues: List<SubtitleCue> = emptyList(),
    val subtitleStyle: SubtitleStyle = SubtitleStyle(),
    val watermarkConfig: WatermarkConfig = WatermarkConfig(),
    val suggestedClips: List<SuggestedClip> = emptyList(),
    val latestExportUri: String? = null
)

class ProjectRepository(
    private val projectDao: ProjectDao,
    private val exportRecordDao: ExportRecordDao
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    val allProjects: Flow<List<ProjectEntity>> = projectDao.getAllProjects()
    val allExports: Flow<List<ExportRecordEntity>> = exportRecordDao.getAllExports()

    suspend fun saveWorkspace(data: ProjectWorkspaceData): Long {
        val now = System.currentTimeMillis()
        val entity = ProjectEntity(
            id = data.id,
            name = data.name,
            sourceUri = data.metadata.uriString,
            sourceTitle = data.metadata.title,
            sourceDurationMs = data.metadata.durationMs,
            sourceWidth = data.metadata.width,
            sourceHeight = data.metadata.height,
            sourceRotationDegrees = data.metadata.rotationDegrees,
            sourceFps = data.metadata.fps,
            sourceFileSizeBytes = data.metadata.fileSizeBytes,
            thumbnailPath = data.metadata.thumbnailPath,
            clipStartMs = data.clipStartMs,
            clipEndMs = data.clipEndMs,
            cropSettingsJson = json.encodeToString(data.cropSettings),
            subtitlesEnabled = data.subtitlesEnabled,
            subtitleCuesJson = json.encodeToString(data.subtitleCues),
            subtitleStyleJson = json.encodeToString(data.subtitleStyle),
            watermarkConfigJson = json.encodeToString(data.watermarkConfig),
            suggestedClipsJson = json.encodeToString(data.suggestedClips),
            latestExportUri = data.latestExportUri,
            createdAt = if (data.id == 0L) now else (projectDao.getProjectById(data.id)?.createdAt ?: now),
            updatedAt = now
        )
        return projectDao.insertProject(entity)
    }

    suspend fun loadWorkspace(projectId: Long): ProjectWorkspaceData? {
        val entity = projectDao.getProjectById(projectId) ?: return null
        return entityToWorkspace(entity)
    }

    fun entityToWorkspace(entity: ProjectEntity): ProjectWorkspaceData {
        val crop = runCatching { json.decodeFromString<CropSettings>(entity.cropSettingsJson) }
            .getOrDefault(CropSettings())
        val cues = runCatching { json.decodeFromString<List<SubtitleCue>>(entity.subtitleCuesJson) }
            .getOrDefault(emptyList())
        val style = runCatching { json.decodeFromString<SubtitleStyle>(entity.subtitleStyleJson) }
            .getOrDefault(SubtitleStyle())
        val watermark = runCatching { json.decodeFromString<WatermarkConfig>(entity.watermarkConfigJson) }
            .getOrDefault(WatermarkConfig())
        val clips = runCatching { json.decodeFromString<List<SuggestedClip>>(entity.suggestedClipsJson) }
            .getOrDefault(emptyList())

        return ProjectWorkspaceData(
            id = entity.id,
            name = entity.name,
            metadata = VideoMetadata(
                uriString = entity.sourceUri,
                title = entity.sourceTitle,
                durationMs = entity.sourceDurationMs,
                width = entity.sourceWidth,
                height = entity.sourceHeight,
                rotationDegrees = entity.sourceRotationDegrees,
                fps = entity.sourceFps,
                fileSizeBytes = entity.sourceFileSizeBytes,
                thumbnailPath = entity.thumbnailPath
            ),
            clipStartMs = entity.clipStartMs,
            clipEndMs = entity.clipEndMs,
            cropSettings = crop,
            subtitlesEnabled = entity.subtitlesEnabled,
            subtitleCues = cues,
            subtitleStyle = style,
            watermarkConfig = watermark,
            suggestedClips = clips,
            latestExportUri = entity.latestExportUri
        )
    }

    suspend fun renameProject(id: Long, newName: String) {
        projectDao.renameProject(id, newName.trim())
    }

    suspend fun deleteProject(id: Long) {
        projectDao.deleteProjectById(id)
    }

    suspend fun recordExport(record: ExportRecordEntity): Long {
        return exportRecordDao.insertExport(record)
    }

    suspend fun deleteExportRecord(id: Long) {
        exportRecordDao.deleteExportById(id)
    }

    suspend fun deleteAllData() {
        projectDao.deleteAllProjects()
        exportRecordDao.deleteAllExports()
    }
}
