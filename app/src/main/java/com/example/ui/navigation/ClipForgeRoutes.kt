package com.example.ui.navigation

import kotlinx.serialization.Serializable

sealed interface ClipForgeRoute {
    @Serializable data object Home : ClipForgeRoute
    @Serializable data object AiShortsGenerator : ClipForgeRoute
    @Serializable data class VideoUrlInput(val nextTarget: String = "analysis") : ClipForgeRoute
    @Serializable data class LocalVideoPicker(val nextTarget: String = "analysis") : ClipForgeRoute
    @Serializable data object VideoAnalysis : ClipForgeRoute
    @Serializable data object ClipSelection : ClipForgeRoute
    @Serializable data object VideoEditor : ClipForgeRoute
    @Serializable data object SubtitleEditor : ClipForgeRoute
    @Serializable data object WatermarkEditor : ClipForgeRoute
    @Serializable data object OneClickConvert : ClipForgeRoute
    @Serializable data class VideoPreview(
        val uriString: String,
        val title: String,
        val startMs: Long = 0L,
        val endMs: Long = -1L,
        val showOverlays: Boolean = false
    ) : ClipForgeRoute
    @Serializable data object ExportProgress : ClipForgeRoute
    @Serializable data object MyProjects : ClipForgeRoute
    @Serializable data object ExportHistory : ClipForgeRoute
    @Serializable data object Settings : ClipForgeRoute
    @Serializable data object AboutPrivacy : ClipForgeRoute
}
