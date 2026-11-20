package com.example.ui.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.example.data.ai.GeminiVideoAnalyzer
import com.example.data.local.ClipForgeDatabase
import com.example.data.local.ExportRecordEntity
import com.example.data.local.ProjectEntity
import com.example.data.local.ProjectRepository
import com.example.data.local.ProjectWorkspaceData
import com.example.data.local.SettingsRepository
import com.example.media.MediaStoreExporter
import com.example.media.SrtParser
import com.example.media.VideoMetadataHelper
import com.example.media.VideoProcessingEngine
import com.example.media.VideoUrlDownloader
import com.example.media.YouTubeLicenseChecker
import com.example.media.YouTubeLicenseStatus
import com.example.model.AiAnalysisState
import com.example.model.AppSettings
import com.example.model.CropMode
import com.example.model.CropSettings
import com.example.model.ExportJobRequest
import com.example.model.ExportUiState
import com.example.model.ExportedVideoSummary
import com.example.model.SubtitleCue
import com.example.model.SubtitleGenState
import com.example.model.SubtitleLanguage
import com.example.model.SubtitleStyle
import com.example.model.SuggestedClip
import com.example.model.UrlDownloadState
import com.example.model.VideoMetadata
import com.example.model.WatermarkConfig
import com.example.service.VideoExportService
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

private const val TAG = "ClipForgeViewModel"

data class ActiveEditorState(
    val projectId: Long = 0L,
    val projectName: String = "Untitled Short",
    val metadata: VideoMetadata? = null,
    val clipStartMs: Long = 0L,
    val clipEndMs: Long = 0L,
    val targetClipDurationSec: Int = 30,
    val cropSettings: CropSettings = CropSettings(),
    val subtitlesEnabled: Boolean = false,
    val subtitleCues: List<SubtitleCue> = emptyList(),
    val subtitleStyle: SubtitleStyle = SubtitleStyle(),
    val watermarkConfig: WatermarkConfig = WatermarkConfig(),
    val muteAudio: Boolean = false,
    val suggestedClips: List<SuggestedClip> = emptyList(),
    val latestExportUri: String? = null,
    val isLoadingVideo: Boolean = false,
    val statusMessage: String? = null,
    val errorMessage: String? = null
)

@OptIn(UnstableApi::class)
class ClipForgeViewModel(application: Application) : AndroidViewModel(application) {

    private val db = ClipForgeDatabase.getInstance(application)
    private val projectRepository = ProjectRepository(db.projectDao(), db.exportRecordDao())
    private val settingsRepository = SettingsRepository(application)
    private val geminiAnalyzer = GeminiVideoAnalyzer(application)
    private val processingEngine = VideoProcessingEngine(application)

    val settings: StateFlow<AppSettings> = settingsRepository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    val projects: StateFlow<List<ProjectEntity>> = projectRepository.allProjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val exportHistory: StateFlow<List<ExportRecordEntity>> = projectRepository.allExports
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _editorState = MutableStateFlow(ActiveEditorState())
    val editorState: StateFlow<ActiveEditorState> = _editorState.asStateFlow()

    private val _aiState = MutableStateFlow<AiAnalysisState>(AiAnalysisState.Idle)
    val aiState: StateFlow<AiAnalysisState> = _aiState.asStateFlow()

    private val _subtitleGenState = MutableStateFlow<SubtitleGenState>(SubtitleGenState.Idle)
    val subtitleGenState: StateFlow<SubtitleGenState> = _subtitleGenState.asStateFlow()

    private val _urlDownloadState = MutableStateFlow<UrlDownloadState>(UrlDownloadState.Idle)
    val urlDownloadState: StateFlow<UrlDownloadState> = _urlDownloadState.asStateFlow()

    private val _exportState = MutableStateFlow<ExportUiState>(ExportUiState.Idle)
    val exportState: StateFlow<ExportUiState> = _exportState.asStateFlow()

    private val _cacheSizeBytes = MutableStateFlow(calculateCacheBytes())
    val cacheSizeBytes: StateFlow<Long> = _cacheSizeBytes.asStateFlow()

    private var urlJob: Job? = null
    private var exportJob: Job? = null
    private var lastExportRequests: List<ExportJobRequest> = emptyList()

    init {
        viewModelScope.launch {
            val s = settingsRepository.settingsFlow.first()
            _editorState.update {
                it.copy(
                    targetClipDurationSec = s.defaultClipDurationSec,
                    cropSettings = it.cropSettings.copy(mode = s.defaultCropMode),
                    watermarkConfig = it.watermarkConfig.copy(text = s.defaultWatermarkText)
                )
            }
        }
    }

    fun clearEditorMessages() {
        _editorState.update { it.copy(errorMessage = null, statusMessage = null) }
    }

    fun selectLocalVideo(uri: Uri, takePersistable: Boolean = false) {
        val ctx = getApplication<Application>()
        if (takePersistable) {
            runCatching {
                ctx.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
        }
        viewModelScope.launch {
            _editorState.update { it.copy(isLoadingVideo = true, errorMessage = null, statusMessage = null) }
            val result = VideoMetadataHelper.extractMetadata(ctx, uri)
            result.onSuccess { meta ->
                val currentSettings = settings.value
                val baseName = meta.title.substringBeforeLast('.').ifBlank { "New Short" }
                _editorState.value = ActiveEditorState(
                    projectId = 0L,
                    projectName = baseName,
                    metadata = meta,
                    clipStartMs = 0L,
                    clipEndMs = meta.durationMs,
                    targetClipDurationSec = currentSettings.defaultClipDurationSec,
                    cropSettings = CropSettings(mode = currentSettings.defaultCropMode),
                    subtitlesEnabled = false,
                    subtitleCues = emptyList(),
                    subtitleStyle = SubtitleStyle(),
                    watermarkConfig = WatermarkConfig(text = currentSettings.defaultWatermarkText),
                    isLoadingVideo = false,
                    statusMessage = "Loaded ${meta.title}"
                )
                _aiState.value = AiAnalysisState.Idle
                _subtitleGenState.value = SubtitleGenState.Idle
                saveCurrentProjectSilently()
            }.onFailure { err ->
                _editorState.update {
                    it.copy(
                        isLoadingVideo = false,
                        errorMessage = err.localizedMessage ?: "Failed to read video metadata."
                    )
                }
            }
        }
    }

    fun downloadVideoFromUrl(rawUrl: String) {
        when (val validation = VideoUrlDownloader.validateUrl(rawUrl)) {
            is VideoUrlDownloader.ValidationResult.Invalid -> {
                _urlDownloadState.value = UrlDownloadState.Error(validation.reason)
                return
            }

            is VideoUrlDownloader.ValidationResult.YouTube -> {
                urlJob?.cancel()
                urlJob = viewModelScope.launch {
                    val ctx = getApplication<Application>()
                    val canonicalUrl = YouTubeLicenseChecker.canonicalWatchUrl(validation.videoId)
                    _urlDownloadState.value = UrlDownloadState.CheckingYouTubeLicense(canonicalUrl)
                    val licenseResult = YouTubeLicenseChecker.checkVideoLicense(ctx, validation.normalizedUrl)
                    licenseResult.onSuccess { info ->
                        _urlDownloadState.value =
                            if (info.licenseStatus == YouTubeLicenseStatus.VERIFIED_CREATIVE_COMMONS) {
                                UrlDownloadState.YouTubeLicenseVerified(info)
                            } else {
                                UrlDownloadState.YouTubePermissionWarning(info)
                            }
                    }.onFailure { err ->
                        _urlDownloadState.value = UrlDownloadState.Error(
                            err.localizedMessage ?: "Unable to verify YouTube video license metadata."
                        )
                    }
                }
                return
            }

            is VideoUrlDownloader.ValidationResult.Valid -> {
                val validUrl = validation.normalizedUrl
                urlJob?.cancel()
                urlJob = viewModelScope.launch {
                    val ctx = getApplication<Application>()
                    _urlDownloadState.value = UrlDownloadState.Validating(validUrl)
                    val downloadResult = VideoUrlDownloader.downloadDirectVideo(
                        context = ctx,
                        url = validUrl,
                        onProgress = { pct, downloadedMb, totalMb ->
                            _urlDownloadState.value = UrlDownloadState.Downloading(pct, downloadedMb, totalMb)
                        }
                    )
                    downloadResult.onSuccess { file ->
                        val uri = Uri.fromFile(file)
                        val metaResult = VideoMetadataHelper.extractMetadata(ctx, uri)
                        metaResult.onSuccess { meta ->
                            val currentSettings = settings.value
                            _editorState.value = ActiveEditorState(
                                projectId = 0L,
                                projectName = meta.title.substringBeforeLast('.').ifBlank { "URL Short" },
                                metadata = meta,
                                clipStartMs = 0L,
                                clipEndMs = meta.durationMs,
                                targetClipDurationSec = currentSettings.defaultClipDurationSec,
                                cropSettings = CropSettings(mode = currentSettings.defaultCropMode),
                                watermarkConfig = WatermarkConfig(text = currentSettings.defaultWatermarkText)
                            )
                            _aiState.value = AiAnalysisState.Idle
                            _subtitleGenState.value = SubtitleGenState.Idle
                            _urlDownloadState.value = UrlDownloadState.Completed(file.absolutePath, meta)
                            _cacheSizeBytes.value = calculateCacheBytes()
                            saveCurrentProjectSilently()
                        }.onFailure { err ->
                            file.delete()
                            _urlDownloadState.value = UrlDownloadState.Error(
                                err.localizedMessage ?: "Downloaded file is not a valid video."
                            )
                        }
                    }.onFailure { err ->
                        _urlDownloadState.value = UrlDownloadState.Error(
                            err.localizedMessage ?: "Failed to download video from URL."
                        )
                    }
                }
            }
        }
    }

    fun cancelUrlDownload() {
        urlJob?.cancel()
        _urlDownloadState.value = UrlDownloadState.Idle
    }

    fun resetUrlDownloadState() {
        _urlDownloadState.value = UrlDownloadState.Idle
    }

    fun setTargetClipDuration(seconds: Int) {
        _editorState.update { it.copy(targetClipDurationSec = seconds.coerceIn(5, 180)) }
    }

    fun analyzeActiveVideo() {
        val meta = _editorState.value.metadata
        if (meta == null) {
            _aiState.value = AiAnalysisState.Error("Please select or import a video first.")
            return
        }
        viewModelScope.launch {
            _aiState.value = AiAnalysisState.Analyzing("Preparing media for Gemini AI...", 0.05f)
            val result = geminiAnalyzer.analyzeVideoForShorts(
                metadata = meta,
                targetClipDurationSec = _editorState.value.targetClipDurationSec,
                onStageProgress = { stage, frac ->
                    _aiState.value = AiAnalysisState.Analyzing(stage, frac)
                }
            )
            result.onSuccess { clips ->
                _editorState.update { it.copy(suggestedClips = clips) }
                _aiState.value = AiAnalysisState.Success(clips)
                saveCurrentProjectSilently()
            }.onFailure { err ->
                _aiState.value = AiAnalysisState.Error(
                    err.localizedMessage ?: "AI video analysis failed."
                )
            }
        }
    }

    fun toggleClipSelection(clipId: String) {
        val updated = _editorState.value.suggestedClips.map {
            if (it.id == clipId) it.copy(isSelected = !it.isSelected) else it
        }
        _editorState.update { it.copy(suggestedClips = updated) }
        if (_aiState.value is AiAnalysisState.Success) {
            _aiState.value = AiAnalysisState.Success(updated)
        }
    }

    fun selectAllClips(selected: Boolean) {
        val updated = _editorState.value.suggestedClips.map { it.copy(isSelected = selected) }
        _editorState.update { it.copy(suggestedClips = updated) }
        if (_aiState.value is AiAnalysisState.Success) {
            _aiState.value = AiAnalysisState.Success(updated)
        }
    }

    fun applyClipRangeToEditor(clip: SuggestedClip) {
        _editorState.update {
            it.copy(
                projectName = clip.title,
                clipStartMs = clip.startMs,
                clipEndMs = clip.endMs
            )
        }
    }

    fun updateTrimRange(startMs: Long, endMs: Long) {
        val maxDur = _editorState.value.metadata?.durationMs ?: Long.MAX_VALUE
        val safeStart = startMs.coerceIn(0L, (maxDur - 1000L).coerceAtLeast(0L))
        val safeEnd = endMs.coerceIn(safeStart + 1000L, maxDur)
        _editorState.update { it.copy(clipStartMs = safeStart, clipEndMs = safeEnd) }
    }

    fun updateCropMode(mode: CropMode) {
        _editorState.update {
            it.copy(cropSettings = it.cropSettings.copy(mode = mode))
        }
        if (mode == CropMode.SMART_CROP) {
            val meta = _editorState.value.metadata ?: return
            viewModelScope.launch {
                val detectedX = VideoMetadataHelper.detectSubjectCenterX(
                    getApplication(),
                    meta.uriString,
                    meta.durationMs
                )
                _editorState.update {
                    it.copy(
                        cropSettings = it.cropSettings.copy(detectedSubjectCenterX = detectedX),
                        statusMessage = if (detectedX != null) {
                            "Smart Crop locked onto primary subject."
                        } else {
                            "No face detected; centered crop frame."
                        }
                    )
                }
            }
        }
    }

    fun updateManualCrop(panX: Float, panY: Float, zoom: Float) {
        _editorState.update {
            it.copy(
                cropSettings = it.cropSettings.copy(
                    manualPanX = panX.coerceIn(-1f, 1f),
                    manualPanY = panY.coerceIn(-1f, 1f),
                    zoom = zoom.coerceIn(1f, 2.5f)
                )
            )
        }
    }

    fun updateMuteAudio(mute: Boolean) {
        _editorState.update { it.copy(muteAudio = mute) }
    }

    // --- Subtitles ---
    fun setSubtitlesEnabled(enabled: Boolean) {
        _editorState.update { it.copy(subtitlesEnabled = enabled) }
    }

    fun generateAutomaticSubtitles(language: SubtitleLanguage) {
        val state = _editorState.value
        val meta = state.metadata
        if (meta == null) {
            _subtitleGenState.value = SubtitleGenState.Error("Select a video before generating subtitles.")
            return
        }
        viewModelScope.launch {
            _subtitleGenState.value = SubtitleGenState.Generating("Preparing audio track...")
            val result = geminiAnalyzer.generateSubtitles(
                metadata = meta,
                language = language,
                startMs = 0L,
                endMs = meta.durationMs,
                onStage = { stage ->
                    _subtitleGenState.value = SubtitleGenState.Generating(stage)
                }
            )
            result.onSuccess { cues ->
                _editorState.update {
                    it.copy(
                        subtitlesEnabled = true,
                        subtitleCues = cues
                    )
                }
                _subtitleGenState.value = SubtitleGenState.Success(cues)
                saveCurrentProjectSilently()
            }.onFailure { err ->
                _subtitleGenState.value = SubtitleGenState.Error(
                    err.localizedMessage ?: "Failed to generate subtitles."
                )
            }
        }
    }

    fun addManualSubtitleCue(startMs: Long, endMs: Long, text: String) {
        val cleanText = text.trim()
        if (cleanText.isEmpty()) return
        val safeEnd = endMs.coerceAtLeast(startMs + 500L)
        val cue = SubtitleCue(
            id = UUID.randomUUID().toString(),
            startMs = startMs.coerceAtLeast(0L),
            endMs = safeEnd,
            text = cleanText
        )
        _editorState.update {
            it.copy(
                subtitlesEnabled = true,
                subtitleCues = (it.subtitleCues + cue).sortedBy { c -> c.startMs }
            )
        }
    }

    fun updateSubtitleCue(cueId: String, startMs: Long, endMs: Long, text: String) {
        val updated = _editorState.value.subtitleCues.map { cue ->
            if (cue.id == cueId) {
                cue.copy(
                    startMs = startMs.coerceAtLeast(0L),
                    endMs = endMs.coerceAtLeast(startMs + 250L),
                    text = text
                )
            } else cue
        }.sortedBy { it.startMs }
        _editorState.update { it.copy(subtitleCues = updated) }
    }

    fun deleteSubtitleCue(cueId: String) {
        _editorState.update {
            it.copy(subtitleCues = it.subtitleCues.filterNot { c -> c.id == cueId })
        }
    }

    fun updateSubtitleStyle(transform: (SubtitleStyle) -> SubtitleStyle) {
        _editorState.update { it.copy(subtitleStyle = transform(it.subtitleStyle)) }
    }

    fun importSrtFromUri(uri: Uri) {
        val ctx = getApplication<Application>()
        viewModelScope.launch {
            runCatching {
                ctx.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use {
                    it.readText()
                } ?: throw IllegalStateException("Unable to read selected SRT file.")
            }.onSuccess { raw ->
                val parsed = SrtParser.parse(raw)
                if (parsed.isEmpty()) {
                    _editorState.update { it.copy(errorMessage = "No valid SRT subtitle cues found in file.") }
                } else {
                    _editorState.update {
                        it.copy(
                            subtitlesEnabled = true,
                            subtitleCues = parsed,
                            statusMessage = "Imported ${parsed.size} subtitle cues."
                        )
                    }
                    saveCurrentProjectSilently()
                }
            }.onFailure { err ->
                _editorState.update {
                    it.copy(errorMessage = err.localizedMessage ?: "Failed to import SRT file.")
                }
            }
        }
    }

    fun exportSrtToUri(uri: Uri) {
        val ctx = getApplication<Application>()
        val cues = _editorState.value.subtitleCues
        if (cues.isEmpty()) {
            _editorState.update { it.copy(errorMessage = "No subtitles to export.") }
            return
        }
        viewModelScope.launch {
            runCatching {
                val srtText = SrtParser.format(cues)
                ctx.contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use {
                    it.write(srtText)
                } ?: throw IllegalStateException("Unable to open output stream for SRT.")
            }.onSuccess {
                _editorState.update { it.copy(statusMessage = "Exported ${cues.size} cues as .srt") }
            }.onFailure { err ->
                _editorState.update {
                    it.copy(errorMessage = err.localizedMessage ?: "Failed to save SRT file.")
                }
            }
        }
    }

    // --- Watermark ---
    fun updateWatermarkConfig(transform: (WatermarkConfig) -> WatermarkConfig) {
        _editorState.update { it.copy(watermarkConfig = transform(it.watermarkConfig)) }
    }

    // --- Export Execution ---
    fun exportActiveVideo() {
        val state = _editorState.value
        val meta = state.metadata ?: return
        val req = ExportJobRequest(
            projectId = state.projectId.takeIf { it > 0L },
            title = state.projectName.ifBlank { "ClipForge_Short" },
            sourceUriString = meta.uriString,
            sourceWidth = meta.width,
            sourceHeight = meta.height,
            sourceRotationDegrees = meta.rotationDegrees,
            sourceDurationMs = meta.durationMs,
            startMs = state.clipStartMs,
            endMs = state.clipEndMs.takeIf { it > state.clipStartMs } ?: meta.durationMs,
            cropSettings = state.cropSettings,
            burnSubtitles = state.subtitlesEnabled && state.subtitleCues.isNotEmpty(),
            subtitleCues = state.subtitleCues,
            subtitleStyle = state.subtitleStyle,
            watermarkConfig = state.watermarkConfig,
            muteAudio = state.muteAudio
        )
        runExportBatch(listOf(req))
    }

    fun exportClips(clips: List<SuggestedClip>) {
        val state = _editorState.value
        val meta = state.metadata ?: return
        if (clips.isEmpty()) return
        val requests = clips.map { clip ->
            ExportJobRequest(
                projectId = state.projectId.takeIf { it > 0L },
                title = clip.title,
                sourceUriString = meta.uriString,
                sourceWidth = meta.width,
                sourceHeight = meta.height,
                sourceRotationDegrees = meta.rotationDegrees,
                sourceDurationMs = meta.durationMs,
                startMs = clip.startMs,
                endMs = clip.endMs,
                cropSettings = state.cropSettings,
                burnSubtitles = state.subtitlesEnabled && state.subtitleCues.isNotEmpty(),
                subtitleCues = state.subtitleCues,
                subtitleStyle = state.subtitleStyle,
                watermarkConfig = state.watermarkConfig,
                muteAudio = state.muteAudio
            )
        }
        runExportBatch(requests)
    }

    fun retryLastExport() {
        if (lastExportRequests.isNotEmpty()) {
            runExportBatch(lastExportRequests)
        }
    }

    private fun runExportBatch(requests: List<ExportJobRequest>) {
        lastExportRequests = requests
        exportJob?.cancel()
        exportJob = viewModelScope.launch {
            val ctx = getApplication<Application>()
            val completed = mutableListOf<ExportedVideoSummary>()
            val notifyEnabled = settings.value.notifyOnExportComplete

            try {
                VideoExportService.start(ctx, requests.first().title)

                for ((index, req) in requests.withIndex()) {
                    _exportState.value = ExportUiState.Exporting(
                        currentIndex = index + 1,
                        totalCount = requests.size,
                        currentTitle = req.title,
                        progressPercent = 0,
                        stageLabel = "Starting render..."
                    )

                    val exportResult = processingEngine.exportVideo(req) { pct, stage ->
                        _exportState.value = ExportUiState.Exporting(
                            currentIndex = index + 1,
                            totalCount = requests.size,
                            currentTitle = req.title,
                            progressPercent = pct,
                            stageLabel = stage
                        )
                        VideoExportService.updateProgress(ctx, req.title, pct)
                    }

                    val tempFile = exportResult.getOrElse { throw it }

                    _exportState.value = ExportUiState.Exporting(
                        currentIndex = index + 1,
                        totalCount = requests.size,
                        currentTitle = req.title,
                        progressPercent = 99,
                        stageLabel = "Saving MP4 to Movies/ClipForge..."
                    )

                    val saved = MediaStoreExporter.saveToMoviesClipForge(
                        context = ctx,
                        tempExportFile = tempFile,
                        displayTitle = req.title
                    ).getOrElse { throw it }

                    if (settings.value.autoCleanupTempFiles) {
                        tempFile.delete()
                    }

                    val clipDur = (req.endMs - req.startMs).coerceAtLeast(1000L)
                    projectRepository.recordExport(
                        ExportRecordEntity(
                            projectId = req.projectId,
                            title = req.title,
                            outputUri = saved.contentUri.toString(),
                            filePath = saved.displayPath,
                            sourceUri = req.sourceUriString,
                            durationMs = clipDur,
                            width = req.targetWidth,
                            height = req.targetHeight,
                            fileSizeBytes = saved.fileSizeBytes,
                            cropMode = req.cropSettings.mode.displayName,
                            burnedSubtitles = req.burnSubtitles,
                            burnedWatermark = req.watermarkConfig.enabled
                        )
                    )

                    completed.add(
                        ExportedVideoSummary(
                            title = req.title,
                            outputUriString = saved.contentUri.toString(),
                            filePath = saved.displayPath,
                            durationMs = clipDur,
                            fileSizeBytes = saved.fileSizeBytes,
                            width = req.targetWidth,
                            height = req.targetHeight
                        )
                    )
                }

                val latestUri = completed.lastOrNull()?.outputUriString
                _editorState.update { it.copy(latestExportUri = latestUri) }
                saveCurrentProjectSilently()
                _cacheSizeBytes.value = calculateCacheBytes()

                if (notifyEnabled && completed.isNotEmpty()) {
                    val last = completed.last()
                    MediaStoreExporter.postCompletionNotification(ctx, last.title, last.filePath)
                }
                _exportState.value = ExportUiState.Completed(completed)
            } catch (e: kotlinx.coroutines.CancellationException) {
                processingEngine.cancelActiveExport()
                _exportState.value = ExportUiState.Cancelled
            } catch (e: Exception) {
                Log.e(TAG, "Batch export failed", e)
                _exportState.value = ExportUiState.Error(
                    e.localizedMessage ?: "Export failed unexpectedly."
                )
            } finally {
                VideoExportService.stop(ctx)
            }
        }
    }

    fun cancelExport() {
        processingEngine.cancelActiveExport()
        exportJob?.cancel()
        VideoExportService.stop(getApplication())
        _exportState.value = ExportUiState.Cancelled
    }

    fun resetExportState() {
        _exportState.value = ExportUiState.Idle
    }

    // --- Projects & History ---
    fun saveCurrentProject() {
        viewModelScope.launch {
            val savedId = saveCurrentProjectInternal()
            if (savedId != null) {
                _editorState.update { it.copy(projectId = savedId, statusMessage = "Project saved.") }
            }
        }
    }

    private fun saveCurrentProjectSilently() {
        viewModelScope.launch {
            val savedId = saveCurrentProjectInternal()
            if (savedId != null && _editorState.value.projectId == 0L) {
                _editorState.update { it.copy(projectId = savedId) }
            }
        }
    }

    private suspend fun saveCurrentProjectInternal(): Long? {
        val state = _editorState.value
        val meta = state.metadata ?: return null
        return projectRepository.saveWorkspace(
            ProjectWorkspaceData(
                id = state.projectId,
                name = state.projectName.ifBlank { meta.title },
                metadata = meta,
                clipStartMs = state.clipStartMs,
                clipEndMs = state.clipEndMs,
                cropSettings = state.cropSettings,
                subtitlesEnabled = state.subtitlesEnabled,
                subtitleCues = state.subtitleCues,
                subtitleStyle = state.subtitleStyle,
                watermarkConfig = state.watermarkConfig,
                suggestedClips = state.suggestedClips,
                latestExportUri = state.latestExportUri
            )
        )
    }

    fun openProject(entity: ProjectEntity, onLoaded: () -> Unit) {
        viewModelScope.launch {
            val workspace = projectRepository.entityToWorkspace(entity)
            _editorState.value = ActiveEditorState(
                projectId = workspace.id,
                projectName = workspace.name,
                metadata = workspace.metadata,
                clipStartMs = workspace.clipStartMs,
                clipEndMs = workspace.clipEndMs,
                cropSettings = workspace.cropSettings,
                subtitlesEnabled = workspace.subtitlesEnabled,
                subtitleCues = workspace.subtitleCues,
                subtitleStyle = workspace.subtitleStyle,
                watermarkConfig = workspace.watermarkConfig,
                suggestedClips = workspace.suggestedClips,
                latestExportUri = workspace.latestExportUri
            )
            _aiState.value = if (workspace.suggestedClips.isNotEmpty()) {
                AiAnalysisState.Success(workspace.suggestedClips)
            } else {
                AiAnalysisState.Idle
            }
            onLoaded()
        }
    }

    fun renameProject(id: Long, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            projectRepository.renameProject(id, newName)
            if (_editorState.value.projectId == id) {
                _editorState.update { it.copy(projectName = newName.trim()) }
            }
        }
    }

    fun deleteProject(id: Long) {
        viewModelScope.launch {
            projectRepository.deleteProject(id)
            if (_editorState.value.projectId == id) {
                _editorState.update { it.copy(projectId = 0L) }
            }
        }
    }

    fun deleteExportRecord(id: Long) {
        viewModelScope.launch {
            projectRepository.deleteExportRecord(id)
        }
    }

    fun isMediaAccessible(uriString: String?): Boolean {
        return VideoMetadataHelper.isUriAccessible(getApplication(), uriString)
    }

    // --- Settings & Privacy ---
    fun updateDefaultClipDuration(sec: Int) {
        viewModelScope.launch { settingsRepository.updateDefaultClipDuration(sec) }
    }

    fun updateDefaultCropMode(mode: CropMode) {
        viewModelScope.launch { settingsRepository.updateDefaultCropMode(mode) }
    }

    fun updateDefaultSubtitleLanguage(lang: SubtitleLanguage) {
        viewModelScope.launch { settingsRepository.updateDefaultSubtitleLanguage(lang) }
    }

    fun updateDefaultWatermarkText(text: String) {
        viewModelScope.launch { settingsRepository.updateDefaultWatermarkText(text) }
    }

    fun updateNotifyOnExport(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateNotifyOnExport(enabled) }
    }

    fun updateAutoCleanupTemp(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.updateAutoCleanupTemp(enabled) }
    }

    fun clearTemporaryCache() {
        viewModelScope.launch {
            val ctx = getApplication<Application>()
            listOf("exports_tmp", "url_videos").forEach { dirName ->
                File(ctx.cacheDir, dirName).deleteRecursively()
            }
            ctx.cacheDir.listFiles()?.forEach { f ->
                if (f.name.startsWith("ai_audio_seg_")) f.delete()
            }
            _cacheSizeBytes.value = calculateCacheBytes()
            _editorState.update { it.copy(statusMessage = "Temporary media cache cleared.") }
        }
    }

    fun deleteAllProjectsAndData() {
        viewModelScope.launch {
            projectRepository.deleteAllData()
            settingsRepository.resetDefaults()
            clearTemporaryCache()
            File(getApplication<Application>().filesDir, "thumbnails").deleteRecursively()
            _editorState.value = ActiveEditorState(statusMessage = "All local projects, exports, and thumbnails deleted.")
            _aiState.value = AiAnalysisState.Idle
            _subtitleGenState.value = SubtitleGenState.Idle
        }
    }

    private fun calculateCacheBytes(): Long {
        val cache = getApplication<Application>().cacheDir
        return cache.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }
}
