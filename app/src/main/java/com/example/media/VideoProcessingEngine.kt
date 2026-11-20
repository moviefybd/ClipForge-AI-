package com.example.media

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.OverlaySettings
import androidx.media3.common.VideoCompositorSettings
import androidx.media3.common.util.Size
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.BitmapOverlay
import androidx.media3.effect.Crop
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.Presentation
import androidx.media3.effect.StaticOverlaySettings
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import com.example.model.CropMode
import com.example.model.ExportJobRequest
import com.example.model.SubtitleCue
import com.example.model.SubtitleStyle
import com.example.model.WatermarkPosition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume

private const val TAG = "VideoProcessingEngine"

@UnstableApi
class VideoProcessingEngine(private val context: Context) {

    @Volatile
    private var activeTransformer: Transformer? = null

    fun cancelActiveExport() {
        Handler(Looper.getMainLooper()).post {
            runCatching { activeTransformer?.cancel() }
            activeTransformer = null
        }
    }

    suspend fun exportVideo(
        request: ExportJobRequest,
        onProgress: (percent: Int, stage: String) -> Unit
    ): Result<File> {
        if (!VideoMetadataHelper.hasEnoughStorage(context, 64L * 1024L * 1024L)) {
            return Result.failure(IllegalStateException("Insufficient storage space to export video."))
        }

        val tempDir = File(context.cacheDir, "exports_tmp").apply { mkdirs() }
        val outputFile = File(tempDir, "export_${System.currentTimeMillis()}.mp4")
        if (outputFile.exists()) outputFile.delete()

        onProgress(2, "Preparing video pipeline...")

        // Precompute optional smart crop center, blurred backdrop image, and watermark/subtitle bitmaps on IO
        val smartSubjectX = if (request.cropSettings.mode == CropMode.SMART_CROP &&
            request.cropSettings.detectedSubjectCenterX == null
        ) {
            VideoMetadataHelper.detectSubjectCenterX(
                context,
                request.sourceUriString,
                request.sourceDurationMs
            )
        } else {
            request.cropSettings.detectedSubjectCenterX
        }

        val backdropFile: File? = if (request.cropSettings.mode == CropMode.BLUR_BACKGROUND) {
            withContext(Dispatchers.IO) {
                val sampleBitmap = VideoMetadataHelper.extractSampleFrameBitmap(
                    context,
                    request.sourceUriString,
                    (request.startMs + (request.endMs - request.startMs) / 3L)
                )
                val backdrop = OverlayRenderer.createBlurredBackdropBitmap(
                    sampleBitmap,
                    request.targetWidth,
                    request.targetHeight
                )
                sampleBitmap?.recycle()
                val f = File(tempDir, "blur_bg_${System.currentTimeMillis()}.png")
                FileOutputStream(f).use { backdrop.compress(Bitmap.CompressFormat.PNG, 92, it) }
                backdrop.recycle()
                f
            }
        } else null

        val watermarkBitmap = withContext(Dispatchers.IO) {
            OverlayRenderer.renderWatermarkBitmap(context, request.watermarkConfig, request.targetWidth)
        }

        val overlayEffects = buildList<Effect> {
            val overlays = mutableListOf<androidx.media3.effect.TextureOverlay>()

            if (request.burnSubtitles && request.subtitleCues.isNotEmpty()) {
                val shiftedCues = request.subtitleCues.map { cue ->
                    val start = (cue.startMs + request.subtitleStyle.timingOffsetMs - request.startMs).coerceAtLeast(0L)
                    val end = (cue.endMs + request.subtitleStyle.timingOffsetMs - request.startMs).coerceAtLeast(start + 100L)
                    cue.copy(startMs = start, endMs = end)
                }
                overlays.add(
                    DynamicSubtitleBitmapOverlay(
                        context = context,
                        cues = shiftedCues,
                        style = request.subtitleStyle,
                        frameWidth = request.targetWidth,
                        frameHeight = request.targetHeight
                    )
                )
            }

            if (watermarkBitmap != null && request.watermarkConfig.enabled) {
                val (anchorX, anchorY) = when (request.watermarkConfig.position) {
                    WatermarkPosition.TOP_LEFT -> -0.84f to 0.88f
                    WatermarkPosition.TOP_RIGHT -> 0.84f to 0.88f
                    WatermarkPosition.BOTTOM_LEFT -> -0.84f to -0.84f
                    WatermarkPosition.BOTTOM_RIGHT -> 0.84f to -0.84f
                    WatermarkPosition.CENTER -> 0f to 0f
                }
                val overlayAnchorX = when (request.watermarkConfig.position) {
                    WatermarkPosition.TOP_LEFT, WatermarkPosition.BOTTOM_LEFT -> -1f
                    WatermarkPosition.TOP_RIGHT, WatermarkPosition.BOTTOM_RIGHT -> 1f
                    WatermarkPosition.CENTER -> 0f
                }
                val overlayAnchorY = when (request.watermarkConfig.position) {
                    WatermarkPosition.TOP_LEFT, WatermarkPosition.TOP_RIGHT -> 1f
                    WatermarkPosition.BOTTOM_LEFT, WatermarkPosition.BOTTOM_RIGHT -> -1f
                    WatermarkPosition.CENTER -> 0f
                }
                val wmSettings = StaticOverlaySettings.Builder()
                    .setOverlayFrameAnchor(overlayAnchorX, overlayAnchorY)
                    .setBackgroundFrameAnchor(anchorX, anchorY)
                    .build()
                overlays.add(BitmapOverlay.createStaticBitmapOverlay(watermarkBitmap, wmSettings))
            }

            if (overlays.isNotEmpty()) {
                add(OverlayEffect(overlays))
            }
        }

        return suspendCancellableCoroutine { continuation ->
            val mainHandler = Handler(Looper.getMainLooper())
            mainHandler.post {
                try {
                    val clipDurationMs = (request.endMs - request.startMs).coerceAtLeast(1000L)
                    val inputMediaItem = MediaItem.Builder()
                        .setUri(Uri.parse(request.sourceUriString))
                        .apply {
                            if (request.startMs > 0L || request.endMs < request.sourceDurationMs) {
                                setClippingConfiguration(
                                    MediaItem.ClippingConfiguration.Builder()
                                        .setStartPositionMs(request.startMs.coerceAtLeast(0L))
                                        .setEndPositionMs(request.endMs.coerceAtLeast(request.startMs + 500L))
                                        .build()
                                )
                            }
                        }
                        .build()

                    val cropVideoEffects = buildVideoEffectsForCrop(
                        request = request,
                        smartSubjectX = smartSubjectX
                    )

                    val transformerListener = object : Transformer.Listener {
                        override fun onCompleted(composition: Composition, result: ExportResult) {
                            activeTransformer = null
                            backdropFile?.delete()
                            watermarkBitmap?.recycle()
                            if (outputFile.exists() && outputFile.length() > 0L) {
                                onProgress(100, "Finalizing MP4...")
                                if (continuation.isActive) continuation.resume(Result.success(outputFile))
                            } else {
                                if (continuation.isActive) {
                                    continuation.resume(
                                        Result.failure(IllegalStateException("Export finished but output MP4 is empty."))
                                    )
                                }
                            }
                        }

                        override fun onError(
                            composition: Composition,
                            result: ExportResult,
                            exception: ExportException
                        ) {
                            activeTransformer = null
                            backdropFile?.delete()
                            watermarkBitmap?.recycle()
                            outputFile.delete()
                            Log.e(TAG, "Transformer export error", exception)
                            val readable = when (exception.errorCode) {
                                ExportException.ERROR_CODE_ENCODER_INIT_FAILED ->
                                    "Device video encoder failed to initialize for 1080x1920 H.264."
                                ExportException.ERROR_CODE_DECODER_INIT_FAILED ->
                                    "Unsupported input video codec on this device."
                                ExportException.ERROR_CODE_IO_UNSPECIFIED,
                                ExportException.ERROR_CODE_IO_FILE_NOT_FOUND ->
                                    "Could not read source video file."
                                else -> exception.localizedMessage ?: "Video export failed (${exception.errorCodeName})."
                            }
                            if (continuation.isActive) {
                                continuation.resume(Result.failure(IllegalStateException(readable, exception)))
                            }
                        }
                    }

                    val transformer = Transformer.Builder(context)
                        .setVideoMimeType(MimeTypes.VIDEO_H264)
                        .setAudioMimeType(MimeTypes.AUDIO_AAC)
                        .addListener(transformerListener)
                        .build()
                    activeTransformer = transformer

                    continuation.invokeOnCancellation {
                        mainHandler.post {
                            runCatching { transformer.cancel() }
                            activeTransformer = null
                            backdropFile?.delete()
                            outputFile.delete()
                        }
                    }

                    if (request.cropSettings.mode == CropMode.BLUR_BACKGROUND && backdropFile != null) {
                        val bgMediaItem = MediaItem.Builder()
                            .setUri(Uri.fromFile(backdropFile))
                            .setImageDurationMs(clipDurationMs)
                            .build()
                        val bgEdited = EditedMediaItem.Builder(bgMediaItem)
                            .setFrameRate(30)
                            .setRemoveAudio(true)
                            .build()

                        val srcW = request.sourceWidth.coerceAtLeast(1)
                        val srcH = request.sourceHeight.coerceAtLeast(1)
                        val rot = request.sourceRotationDegrees
                        val effW = if (rot == 90 || rot == 270) srcH else srcW
                        val effH = if (rot == 90 || rot == 270) srcW else srcH
                        val fitHeight = ((effH.toFloat() / effW.toFloat()) * request.targetWidth)
                            .toInt()
                            .coerceIn(240, request.targetHeight)
                            .let { it - (it % 2) } // ensure even height for H.264

                        val fgEffects = Effects(
                            emptyList(),
                            cropVideoEffects + listOf(
                                Presentation.createForWidthAndHeight(
                                    request.targetWidth,
                                    fitHeight,
                                    Presentation.LAYOUT_SCALE_TO_FIT
                                )
                            )
                        )
                        val fgEdited = EditedMediaItem.Builder(inputMediaItem)
                            .setRemoveAudio(request.muteAudio)
                            .setEffects(fgEffects)
                            .build()

                        val bgSeq = EditedMediaItemSequence.Builder(listOf(bgEdited)).build()
                        val fgSeq = EditedMediaItemSequence.Builder(listOf(fgEdited)).build()

                        val compositorSettings = object : VideoCompositorSettings {
                            override fun getOutputSize(inputSizes: List<Size>): Size {
                                return Size(request.targetWidth, request.targetHeight)
                            }

                            override fun getOverlaySettings(
                                inputId: Int,
                                presentationTimeUs: Long
                            ): OverlaySettings {
                                return StaticOverlaySettings.Builder().build()
                            }
                        }

                        val composition = Composition.Builder(listOf(bgSeq, fgSeq))
                            .setVideoCompositorSettings(compositorSettings)
                            .setEffects(Effects(emptyList(), overlayEffects))
                            .build()

                        transformer.start(composition, outputFile.absolutePath)
                    } else {
                        val presentationEffect = when (request.cropSettings.mode) {
                            CropMode.FIT_ORIGINAL -> Presentation.createForWidthAndHeight(
                                request.targetWidth,
                                request.targetHeight,
                                Presentation.LAYOUT_SCALE_TO_FIT
                            )
                            else -> Presentation.createForWidthAndHeight(
                                request.targetWidth,
                                request.targetHeight,
                                Presentation.LAYOUT_SCALE_TO_FIT_WITH_CROP
                            )
                        }

                        val allVideoEffects = cropVideoEffects + presentationEffect + overlayEffects
                        val editedItem = EditedMediaItem.Builder(inputMediaItem)
                            .setRemoveAudio(request.muteAudio)
                            .setEffects(Effects(emptyList(), allVideoEffects))
                            .build()

                        transformer.start(editedItem, outputFile.absolutePath)
                    }

                    // Real progress polling via Transformer.getProgress
                    val progressHolder = ProgressHolder()
                    val poller = object : Runnable {
                        override fun run() {
                            val current = activeTransformer ?: return
                            val state = current.getProgress(progressHolder)
                            when (state) {
                                Transformer.PROGRESS_STATE_AVAILABLE -> {
                                    val pct = progressHolder.progress.coerceIn(1, 99)
                                    val stage = when {
                                        pct < 30 -> "Decoding & cropping to 9:16..."
                                        pct < 75 -> "Rendering frames & overlays..."
                                        else -> "Encoding H.264 / AAC MP4..."
                                    }
                                    onProgress(pct, stage)
                                    mainHandler.postDelayed(this, 300L)
                                }
                                Transformer.PROGRESS_STATE_WAITING_FOR_AVAILABILITY -> {
                                    onProgress(5, "Initializing hardware codecs...")
                                    mainHandler.postDelayed(this, 300L)
                                }
                                else -> {
                                    // Stopped or completed
                                }
                            }
                        }
                    }
                    mainHandler.postDelayed(poller, 250L)
                } catch (e: Exception) {
                    activeTransformer = null
                    backdropFile?.delete()
                    watermarkBitmap?.recycle()
                    outputFile.delete()
                    Log.e(TAG, "Failed starting Transformer", e)
                    if (continuation.isActive) {
                        continuation.resume(
                            Result.failure(IllegalStateException(e.localizedMessage ?: "Failed to start video export.", e))
                        )
                    }
                }
            }
        }
    }

    private fun buildVideoEffectsForCrop(
        request: ExportJobRequest,
        smartSubjectX: Float?
    ): List<Effect> {
        val crop = request.cropSettings
        val effects = mutableListOf<Effect>()

        val hasPanOrZoom = crop.zoom > 1.02f ||
            kotlin.math.abs(crop.manualPanX) > 0.02f ||
            kotlin.math.abs(crop.manualPanY) > 0.02f ||
            (crop.mode == CropMode.SMART_CROP && smartSubjectX != null)

        if (hasPanOrZoom && crop.mode != CropMode.FIT_ORIGINAL && crop.mode != CropMode.BLUR_BACKGROUND) {
            val zoom = crop.zoom.coerceIn(1f, 2.5f)
            val halfSpan = 1f / zoom
            val centerX = when {
                crop.mode == CropMode.SMART_CROP && smartSubjectX != null ->
                    ((smartSubjectX * 2f) - 1f + crop.manualPanX).coerceIn(-1f + halfSpan, 1f - halfSpan)
                else -> crop.manualPanX.coerceIn(-1f + halfSpan, 1f - halfSpan)
            }
            val centerY = crop.manualPanY.coerceIn(-1f + halfSpan, 1f - halfSpan)

            val left = (centerX - halfSpan).coerceAtLeast(-1f)
            val right = (centerX + halfSpan).coerceAtMost(1f)
            val bottom = (centerY - halfSpan).coerceAtLeast(-1f)
            val top = (centerY + halfSpan).coerceAtMost(1f)
            if (right > left + 0.1f && top > bottom + 0.1f) {
                effects.add(Crop(left, right, bottom, top))
            }
        }
        return effects
    }

    /**
     * Time-varying BitmapOverlay that switches cached cue bitmaps based on presentationTimeUs
     * so burned subtitles stay synchronized without re-uploading textures every frame.
     */
    private class DynamicSubtitleBitmapOverlay(
        private val context: Context,
        private val cues: List<SubtitleCue>,
        private val style: SubtitleStyle,
        private val frameWidth: Int,
        private val frameHeight: Int
    ) : BitmapOverlay() {

        private val emptyBitmap: Bitmap by lazy {
            Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)
        }
        private var cachedCueIndex: Int = -2
        private var cachedBitmap: Bitmap? = null

        override fun getBitmap(presentationTimeUs: Long): Bitmap {
            val timeMs = presentationTimeUs / 1000L
            val activeIdx = cues.indexOfFirst { timeMs in it.startMs..it.endMs }
            if (activeIdx == cachedCueIndex && cachedBitmap != null) {
                return cachedBitmap!!
            }
            cachedCueIndex = activeIdx
            val nextBitmap = if (activeIdx >= 0) {
                OverlayRenderer.renderSubtitleOverlayBitmap(
                    context = context,
                    text = cues[activeIdx].text,
                    style = style,
                    frameWidth = frameWidth,
                    frameHeight = frameHeight
                )
            } else {
                emptyBitmap
            }
            val old = cachedBitmap
            cachedBitmap = nextBitmap
            if (old != null && old !== emptyBitmap && old !== nextBitmap) {
                old.recycle()
            }
            return nextBitmap
        }
    }
}
