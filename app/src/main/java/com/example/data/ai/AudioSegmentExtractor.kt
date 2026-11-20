package com.example.data.ai

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer

private const val TAG = "AudioSegmentExtractor"

data class VideoAnalysisSegment(
    val index: Int,
    val startMs: Long,
    val endMs: Long,
    val audioBytes: ByteArray?,
    val audioMimeType: String,
    val sampledFrames: List<Pair<Long, Bitmap>>
) {
    fun recycleFrames() {
        sampledFrames.forEach { (_, bmp) ->
            if (!bmp.isRecycled) bmp.recycle()
        }
    }
}

object AudioSegmentExtractor {

    private const val MAX_INLINE_VIDEO_BYTES = 14L * 1024L * 1024L
    private const val MAX_SEGMENT_DURATION_MS = 180_000L // 3 minutes per segment
    private const val MAX_AUDIO_CHUNK_BYTES = 9L * 1024L * 1024L // safely < 20 MB payload limit

    suspend fun canSendWholeVideoInline(context: Context, uriString: String): ByteArray? =
        withContext(Dispatchers.IO) {
            try {
                val uri = Uri.parse(uriString)
                if (uri.scheme == "file") {
                    val file = File(uri.path ?: return@withContext null)
                    if (file.exists() && file.length() in 1L..MAX_INLINE_VIDEO_BYTES) {
                        return@withContext file.readBytes()
                    }
                    null
                } else {
                    val size = context.contentResolver.openAssetFileDescriptor(uri, "r")?.use {
                        it.length
                    } ?: -1L
                    if (size in 1L..MAX_INLINE_VIDEO_BYTES) {
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            val bytes = input.readBytes()
                            if (bytes.size <= MAX_INLINE_VIDEO_BYTES) bytes else null
                        }
                    } else null
                }
            } catch (e: Exception) {
                Log.w(TAG, "Unable to read inline video bytes", e)
                null
            }
        }

    suspend fun extractSegments(
        context: Context,
        uriString: String,
        durationMs: Long,
        includeFrames: Boolean
    ): List<VideoAnalysisSegment> = withContext(Dispatchers.IO) {
        val safeDuration = durationMs.coerceAtLeast(1000L)
        val segmentCount = ((safeDuration + MAX_SEGMENT_DURATION_MS - 1L) / MAX_SEGMENT_DURATION_MS)
            .toInt()
            .coerceIn(1, 10)
        val segLengthMs = (safeDuration + segmentCount - 1L) / segmentCount

        val retriever = MediaMetadataRetriever()
        val uri = Uri.parse(uriString)
        val segments = mutableListOf<VideoAnalysisSegment>()

        try {
            if (includeFrames) {
                runCatching {
                    if (uri.scheme == "file") {
                        retriever.setDataSource(uri.path)
                    } else {
                        retriever.setDataSource(context, uri)
                    }
                }
            }

            for (i in 0 until segmentCount) {
                val startMs = i * segLengthMs
                val endMs = ((i + 1) * segLengthMs).coerceAtMost(safeDuration)
                if (endMs <= startMs) continue

                val audioBytes = extractAudioChunkBytes(context, uri, startMs, endMs)
                val frames = if (includeFrames) {
                    sampleKeyframes(retriever, startMs, endMs, count = 4)
                } else {
                    emptyList()
                }

                if (audioBytes != null || frames.isNotEmpty()) {
                    segments.add(
                        VideoAnalysisSegment(
                            index = i,
                            startMs = startMs,
                            endMs = endMs,
                            audioBytes = audioBytes,
                            audioMimeType = "audio/mp4",
                            sampledFrames = frames
                        )
                    )
                }
            }
        } finally {
            runCatching { retriever.release() }
        }
        segments
    }

    private fun extractAudioChunkBytes(
        context: Context,
        uri: Uri,
        startMs: Long,
        endMs: Long
    ): ByteArray? {
        val extractor = MediaExtractor()
        val tempFile = File(context.cacheDir, "ai_audio_seg_${System.currentTimeMillis()}.m4a")
        var muxer: MediaMuxer? = null

        return try {
            if (uri.scheme == "file") {
                extractor.setDataSource(uri.path!!)
            } else {
                extractor.setDataSource(context, uri, null)
            }

            var audioTrackIndex = -1
            var audioFormat: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val fmt = extractor.getTrackFormat(i)
                val mime = fmt.getString(MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    audioFormat = fmt
                    break
                }
            }

            if (audioTrackIndex == -1 || audioFormat == null) return null

            extractor.selectTrack(audioTrackIndex)
            extractor.seekTo(startMs * 1000L, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

            muxer = MediaMuxer(tempFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val muxTrackIndex = muxer.addTrack(audioFormat)
            muxer.start()

            val bufferSize = if (audioFormat.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                audioFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE).coerceIn(64 * 1024, 512 * 1024)
            } else {
                256 * 1024
            }
            val buffer = ByteBuffer.allocate(bufferSize)
            val bufferInfo = MediaCodec.BufferInfo()
            val endUs = endMs * 1000L
            var totalBytes = 0L

            while (true) {
                val sampleSize = extractor.readSampleData(buffer, 0)
                if (sampleSize < 0) break
                val sampleTimeUs = extractor.sampleTime
                if (sampleTimeUs > endUs) break

                totalBytes += sampleSize
                if (totalBytes > MAX_AUDIO_CHUNK_BYTES) break

                bufferInfo.offset = 0
                bufferInfo.size = sampleSize
                bufferInfo.presentationTimeUs = (sampleTimeUs - startMs * 1000L).coerceAtLeast(0L)
                bufferInfo.flags = extractor.sampleFlags
                muxer.writeSampleData(muxTrackIndex, buffer, bufferInfo)
                extractor.advance()
            }

            muxer.stop()
            muxer.release()
            muxer = null

            if (tempFile.exists() && tempFile.length() > 128L) tempFile.readBytes() else null
        } catch (e: Exception) {
            Log.w(TAG, "Audio segment extraction fallback", e)
            null
        } finally {
            runCatching { muxer?.release() }
            runCatching { extractor.release() }
            runCatching { tempFile.delete() }
        }
    }

    private fun sampleKeyframes(
        retriever: MediaMetadataRetriever,
        startMs: Long,
        endMs: Long,
        count: Int
    ): List<Pair<Long, Bitmap>> {
        val frames = mutableListOf<Pair<Long, Bitmap>>()
        val span = (endMs - startMs).coerceAtLeast(1000L)
        for (i in 0 until count) {
            val tMs = startMs + ((span * (i + 0.5)) / count).toLong()
            val raw = runCatching {
                retriever.getFrameAtTime(tMs * 1000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            }.getOrNull() ?: continue
            val targetW = 480
            val targetH = ((raw.height.toFloat() / raw.width.coerceAtLeast(1)) * targetW)
                .toInt()
                .coerceIn(180, 640)
            val scaled = Bitmap.createScaledBitmap(raw, targetW, targetH, true)
            if (scaled !== raw) raw.recycle()
            frames.add(tMs to scaled)
        }
        return frames
    }
}
