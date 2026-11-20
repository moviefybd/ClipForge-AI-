package com.example.model

import kotlinx.serialization.Serializable

@Serializable
data class VideoMetadata(
    val uriString: String,
    val title: String,
    val durationMs: Long,
    val width: Int,
    val height: Int,
    val rotationDegrees: Int = 0,
    val fps: Float = 30f,
    val fileSizeBytes: Long = 0L,
    val mimeType: String = "video/mp4",
    val hasAudio: Boolean = true,
    val thumbnailPath: String? = null
) {
    val effectiveWidth: Int
        get() = if (rotationDegrees == 90 || rotationDegrees == 270) height else width

    val effectiveHeight: Int
        get() = if (rotationDegrees == 90 || rotationDegrees == 270) width else height

    val isVertical: Boolean
        get() = effectiveHeight > effectiveWidth

    val aspectRatioLabel: String
        get() = when {
            effectiveWidth <= 0 || effectiveHeight <= 0 -> "Unknown"
            kotlin.math.abs((effectiveWidth.toFloat() / effectiveHeight) - (9f / 16f)) < 0.08f -> "9:16"
            kotlin.math.abs((effectiveWidth.toFloat() / effectiveHeight) - (16f / 9f)) < 0.08f -> "16:9"
            kotlin.math.abs((effectiveWidth.toFloat() / effectiveHeight) - 1f) < 0.08f -> "1:1"
            else -> "${effectiveWidth}x${effectiveHeight}"
        }
}

@Serializable
data class SuggestedClip(
    val id: String,
    val startTimeSec: Double,
    val endTimeSec: Double,
    val durationSec: Double,
    val title: String,
    val description: String,
    val reason: String,
    val isSelected: Boolean = true,
    val thumbnailPath: String? = null
) {
    val startMs: Long get() = (startTimeSec * 1000.0).toLong().coerceAtLeast(0L)
    val endMs: Long get() = (endTimeSec * 1000.0).toLong().coerceAtLeast(startMs + 1000L)
}

@Serializable
enum class CropMode(val displayName: String, val subtitle: String) {
    CENTER_CROP("Center Crop", "Fill the complete 9:16 vertical frame"),
    BLUR_BACKGROUND("Blur Background", "Full-width video over blurred backdrop"),
    FIT_ORIGINAL("Fit Original", "Preserve entire frame with dark matte"),
    SMART_CROP("Smart Crop", "Auto-center detected speaker/subject")
}

@Serializable
data class CropSettings(
    val mode: CropMode = CropMode.CENTER_CROP,
    val manualPanX: Float = 0f, // -1f (left) to +1f (right)
    val manualPanY: Float = 0f, // -1f (top) to +1f (bottom)
    val zoom: Float = 1.0f,     // 1.0f to 2.5f
    val detectedSubjectCenterX: Float? = null // 0f..1f from FaceDetector
)

@Serializable
enum class SubtitleLanguage(val displayName: String, val promptLabel: String) {
    ENGLISH("English", "English"),
    BENGALI("Bengali (বাংলা)", "Bengali (বাংলা script)"),
    AUTO("Auto Detect", "the spoken language in the audio (preserve Bengali or English script)")
}

@Serializable
enum class SubtitlePosition(val displayName: String) {
    TOP("Top"),
    CENTER("Center"),
    BOTTOM("Bottom")
}

@Serializable
enum class SubtitleAlign(val displayName: String) {
    LEFT("Left"),
    CENTER("Center"),
    RIGHT("Right")
}

@Serializable
enum class SubtitleFontFamily(val displayName: String) {
    SANS("Sans Clean"),
    DISPLAY("Display Bold"),
    MONO("Studio Mono"),
    SERIF("Editorial Serif")
}

@Serializable
data class SubtitleStyle(
    val fontSizeSp: Float = 22f,
    val fontFamily: SubtitleFontFamily = SubtitleFontFamily.SANS,
    val textColorHex: String = "#FFFFFF",
    val backgroundColorHex: String = "#10121A",
    val backgroundAlpha: Float = 0.72f,
    val outlineEnabled: Boolean = true,
    val outlineColorHex: String = "#000000",
    val shadowEnabled: Boolean = true,
    val position: SubtitlePosition = SubtitlePosition.BOTTOM,
    val alignment: SubtitleAlign = SubtitleAlign.CENTER,
    val timingOffsetMs: Long = 0L
)

@Serializable
data class SubtitleCue(
    val id: String,
    val startMs: Long,
    val endMs: Long,
    val text: String
)

@Serializable
enum class WatermarkType(val displayName: String) {
    TEXT("Text"),
    IMAGE("PNG Image")
}

@Serializable
enum class WatermarkPosition(val displayName: String) {
    TOP_LEFT("Top Left"),
    TOP_RIGHT("Top Right"),
    BOTTOM_LEFT("Bottom Left"),
    BOTTOM_RIGHT("Bottom Right"),
    CENTER("Center")
}

@Serializable
enum class WatermarkWeight(val displayName: String) {
    NORMAL("Regular"),
    MEDIUM("Medium"),
    BOLD("Bold")
}

@Serializable
data class WatermarkConfig(
    val enabled: Boolean = true,
    val type: WatermarkType = WatermarkType.TEXT,
    val text: String = "ClipForge AI",
    val imageUriString: String? = null,
    val position: WatermarkPosition = WatermarkPosition.TOP_RIGHT,
    val fontSizeSp: Float = 18f,
    val fontWeight: WatermarkWeight = WatermarkWeight.BOLD,
    val textColorHex: String = "#FFFFFF",
    val opacity: Float = 0.85f,
    val imageSizeDp: Float = 72f,
    val paddingDp: Float = 20f
)

@Serializable
data class ExportJobRequest(
    val projectId: Long? = null,
    val title: String,
    val sourceUriString: String,
    val sourceWidth: Int,
    val sourceHeight: Int,
    val sourceRotationDegrees: Int,
    val sourceDurationMs: Long,
    val startMs: Long = 0L,
    val endMs: Long = sourceDurationMs,
    val cropSettings: CropSettings = CropSettings(),
    val burnSubtitles: Boolean = false,
    val subtitleCues: List<SubtitleCue> = emptyList(),
    val subtitleStyle: SubtitleStyle = SubtitleStyle(),
    val watermarkConfig: WatermarkConfig = WatermarkConfig(),
    val muteAudio: Boolean = false,
    val targetWidth: Int = 1080,
    val targetHeight: Int = 1920
)

sealed interface AiAnalysisState {
    data object Idle : AiAnalysisState
    data class Analyzing(val stage: String, val progressFraction: Float = 0f) : AiAnalysisState
    data class Success(val clips: List<SuggestedClip>) : AiAnalysisState
    data class Error(val message: String) : AiAnalysisState
}

sealed interface SubtitleGenState {
    data object Idle : SubtitleGenState
    data class Generating(val stage: String) : SubtitleGenState
    data class Success(val cues: List<SubtitleCue>) : SubtitleGenState
    data class Error(val message: String) : SubtitleGenState
}

sealed interface UrlDownloadState {
    data object Idle : UrlDownloadState
    data class Validating(val url: String) : UrlDownloadState
    data class Downloading(val progressPercent: Int, val downloadedMb: Float, val totalMb: Float?) : UrlDownloadState
    data class Completed(val localFilePath: String, val metadata: VideoMetadata) : UrlDownloadState
    data class Error(val message: String) : UrlDownloadState
}

sealed interface ExportUiState {
    data object Idle : ExportUiState
    data class Exporting(
        val currentIndex: Int,
        val totalCount: Int,
        val currentTitle: String,
        val progressPercent: Int,
        val stageLabel: String
    ) : ExportUiState
    data class Completed(
        val exportedItems: List<ExportedVideoSummary>
    ) : ExportUiState
    data object Cancelled : ExportUiState
    data class Error(val message: String) : ExportUiState
}

@Serializable
data class ExportedVideoSummary(
    val title: String,
    val outputUriString: String,
    val filePath: String,
    val durationMs: Long,
    val fileSizeBytes: Long,
    val width: Int,
    val height: Int
)

@Serializable
data class AppSettings(
    val defaultClipDurationSec: Int = 30,
    val defaultCropMode: CropMode = CropMode.CENTER_CROP,
    val defaultSubtitleLanguage: SubtitleLanguage = SubtitleLanguage.ENGLISH,
    val defaultWatermarkText: String = "ClipForge AI",
    val notifyOnExportComplete: Boolean = true,
    val autoCleanupTempFiles: Boolean = true
)

fun formatTimestampMs(ms: Long): String {
    val safe = ms.coerceAtLeast(0L)
    val totalSec = safe / 1000L
    val hours = totalSec / 3600L
    val minutes = (totalSec % 3600L) / 60L
    val seconds = totalSec % 60L
    return if (hours > 0L) {
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}

fun formatTimestampWithMillis(ms: Long): String {
    val safe = ms.coerceAtLeast(0L)
    val totalSec = safe / 1000L
    val minutes = totalSec / 60L
    val seconds = totalSec % 60L
    val millis = (safe % 1000L) / 10L
    return String.format("%02d:%02d.%02d", minutes, seconds, millis)
}

fun formatFileSize(bytes: Long): String {
    if (bytes <= 0L) return "0 KB"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> String.format("%.2f GB", gb)
        mb >= 1.0 -> String.format("%.1f MB", mb)
        else -> String.format("%.0f KB", kb)
    }
}
