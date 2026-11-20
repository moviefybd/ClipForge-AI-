package com.example.media

import android.content.Context
import android.graphics.Bitmap
import android.media.FaceDetector
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Environment
import android.os.StatFs
import android.provider.OpenableColumns
import android.util.Log
import com.example.model.VideoMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

private const val TAG = "VideoMetadataHelper"

object VideoMetadataHelper {

    suspend fun extractMetadata(context: Context, uri: Uri): Result<VideoMetadata> =
        withContext(Dispatchers.IO) {
            runCatching {
                val retriever = MediaMetadataRetriever()
                try {
                    if (uri.scheme == "file") {
                        retriever.setDataSource(uri.path)
                    } else {
                        retriever.setDataSource(context, uri)
                    }

                    val durationMs = retriever.extractMetadata(
                        MediaMetadataRetriever.METADATA_KEY_DURATION
                    )?.toLongOrNull() ?: 0L

                    if (durationMs <= 0L) {
                        throw IllegalArgumentException(
                            "Invalid or corrupted video file: unable to read video duration."
                        )
                    }

                    val width = retriever.extractMetadata(
                        MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH
                    )?.toIntOrNull() ?: 0

                    val height = retriever.extractMetadata(
                        MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT
                    )?.toIntOrNull() ?: 0

                    if (width <= 0 || height <= 0) {
                        throw IllegalArgumentException(
                            "No valid video track found in the selected file."
                        )
                    }

                    val rotation = retriever.extractMetadata(
                        MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION
                    )?.toIntOrNull() ?: 0

                    val hasAudio = retriever.extractMetadata(
                        MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO
                    )?.equals("yes", ignoreCase = true) ?: false

                    val mimeType = retriever.extractMetadata(
                        MediaMetadataRetriever.METADATA_KEY_MIMETYPE
                    ) ?: "video/mp4"

                    val fps = retriever.extractMetadata(
                        MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE
                    )?.toFloatOrNull() ?: readFpsFromExtractor(context, uri)

                    val (displayName, fileSize) = queryDisplayNameAndSize(context, uri)

                    val frameBitmap = retriever.getFrameAtTime(
                        (durationMs * 1000L / 4L).coerceAtMost(2_000_000L),
                        MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                    )
                    val thumbPath = frameBitmap?.let { saveThumbnail(context, it, "thumb_${System.currentTimeMillis()}") }

                    VideoMetadata(
                        uriString = uri.toString(),
                        title = displayName,
                        durationMs = durationMs,
                        width = width,
                        height = height,
                        rotationDegrees = rotation,
                        fps = fps,
                        fileSizeBytes = fileSize,
                        mimeType = mimeType,
                        hasAudio = hasAudio,
                        thumbnailPath = thumbPath
                    )
                } finally {
                    runCatching { retriever.release() }
                }
            }
        }

    suspend fun extractFrameAt(
        context: Context,
        uriString: String,
        timeMs: Long,
        targetTag: String = "clip"
    ): String? = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            val uri = Uri.parse(uriString)
            if (uri.scheme == "file") {
                retriever.setDataSource(uri.path)
            } else {
                retriever.setDataSource(context, uri)
            }
            val bitmap = retriever.getFrameAtTime(
                timeMs.coerceAtLeast(0L) * 1000L,
                MediaMetadataRetriever.OPTION_CLOSEST_SYNC
            ) ?: return@withContext null
            saveThumbnail(context, bitmap, "${targetTag}_${timeMs}")
        } catch (e: Exception) {
            Log.w(TAG, "Failed extracting frame at ${timeMs}ms", e)
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    suspend fun extractSampleFrameBitmap(
        context: Context,
        uriString: String,
        timeMs: Long
    ): Bitmap? = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            val uri = Uri.parse(uriString)
            if (uri.scheme == "file") {
                retriever.setDataSource(uri.path)
            } else {
                retriever.setDataSource(context, uri)
            }
            retriever.getFrameAtTime(
                timeMs.coerceAtLeast(0L) * 1000L,
                MediaMetadataRetriever.OPTION_CLOSEST_SYNC
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed reading frame bitmap", e)
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    /**
     * Uses Android's on-device FaceDetector across sampled frames to estimate the primary
     * subject's normalized horizontal center (0.0 = left, 0.5 = center, 1.0 = right) for Smart Crop.
     */
    suspend fun detectSubjectCenterX(
        context: Context,
        uriString: String,
        durationMs: Long
    ): Float? = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            val uri = Uri.parse(uriString)
            if (uri.scheme == "file") {
                retriever.setDataSource(uri.path)
            } else {
                retriever.setDataSource(context, uri)
            }
            val sampleTimes = listOf(0.15, 0.4, 0.7).map { (durationMs * it).toLong() }
            val detectedXs = mutableListOf<Float>()
            val midpoint = android.graphics.PointF()

            for (tMs in sampleTimes) {
                val raw = retriever.getFrameAtTime(
                    tMs * 1000L,
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                ) ?: continue
                // FaceDetector requires RGB_565 and even width
                val evenW = (raw.width / 2) * 2
                val evenH = (raw.height / 2) * 2
                if (evenW <= 0 || evenH <= 0) continue
                val scaled = Bitmap.createScaledBitmap(raw, evenW.coerceAtMost(640), evenH.coerceAtMost(640), true)
                val w = (scaled.width / 2) * 2
                val h = (scaled.height / 2) * 2
                if (w <= 0 || h <= 0) {
                    if (raw !== scaled) raw.recycle()
                    scaled.recycle()
                    continue
                }
                val rgb565 = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565)
                val canvas = android.graphics.Canvas(rgb565)
                canvas.drawBitmap(scaled, 0f, 0f, null)

                val faces = arrayOfNulls<FaceDetector.Face>(3)
                val detector = FaceDetector(w, h, 3)
                val count = detector.findFaces(rgb565, faces)
                for (i in 0 until count) {
                    val face = faces[i] ?: continue
                    face.getMidPoint(midpoint)
                    detectedXs.add((midpoint.x / w.toFloat()).coerceIn(0f, 1f))
                }
                if (raw !== scaled) raw.recycle()
                scaled.recycle()
                rgb565.recycle()
            }

            if (detectedXs.isNotEmpty()) detectedXs.average().toFloat() else null
        } catch (e: Exception) {
            Log.w(TAG, "Smart crop detection failed, falling back to center", e)
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    fun hasEnoughStorage(context: Context, requiredBytes: Long = 100L * 1024L * 1024L): Boolean {
        return try {
            val path = context.cacheDir ?: Environment.getDataDirectory()
            val stat = StatFs(path.absolutePath)
            stat.availableBytes >= requiredBytes
        } catch (_: Exception) {
            true
        }
    }

    fun isUriAccessible(context: Context, uriString: String?): Boolean {
        if (uriString.isNullOrBlank()) return false
        return try {
            val uri = Uri.parse(uriString)
            if (uri.scheme == "file") {
                val file = uri.path?.let { File(it) }
                file != null && file.exists() && file.length() > 0L
            } else {
                context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { true } ?: false
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun readFpsFromExtractor(context: Context, uri: Uri): Float {
        val extractor = MediaExtractor()
        return try {
            if (uri.scheme == "file") {
                val path = uri.path ?: return 30f
                extractor.setDataSource(path)
            } else {
                extractor.setDataSource(context, uri, null)
            }
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("video/") && format.containsKey(MediaFormat.KEY_FRAME_RATE)) {
                    return format.getInteger(MediaFormat.KEY_FRAME_RATE).toFloat()
                }
            }
            30f
        } catch (_: Exception) {
            30f
        } finally {
            runCatching { extractor.release() }
        }
    }

    private fun queryDisplayNameAndSize(context: Context, uri: Uri): Pair<String, Long> {
        if (uri.scheme == "file") {
            val f = File(uri.path ?: "")
            return (f.name.ifBlank { "video.mp4" }) to f.length()
        }
        var name = "Imported Video"
        var size = 0L
        runCatching {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIdx >= 0) name = cursor.getString(nameIdx) ?: name
                    if (sizeIdx >= 0) size = cursor.getLong(sizeIdx)
                }
            }
        }
        return name to size
    }

    private fun saveThumbnail(context: Context, bitmap: Bitmap, namePrefix: String): String {
        val dir = File(context.filesDir, "thumbnails").apply { mkdirs() }
        val file = File(dir, "$namePrefix.jpg")
        val scaled = if (bitmap.width > 540) {
            val ratio = 540f / bitmap.width
            Bitmap.createScaledBitmap(bitmap, 540, (bitmap.height * ratio).toInt().coerceAtLeast(1), true)
        } else bitmap

        FileOutputStream(file).use { out ->
            scaled.compress(Bitmap.CompressFormat.JPEG, 85, out)
        }
        if (scaled !== bitmap) scaled.recycle()
        return file.absolutePath
    }
}
