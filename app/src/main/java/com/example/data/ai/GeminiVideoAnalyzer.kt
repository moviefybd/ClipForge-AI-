package com.example.data.ai

import android.content.Context
import android.util.Log
import com.example.media.VideoMetadataHelper
import com.example.media.VideoUrlDownloader
import com.example.model.SubtitleCue
import com.example.model.SubtitleLanguage
import com.example.model.SuggestedClip
import com.example.model.VideoMetadata
import com.example.model.formatTimestampMs
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.util.UUID

private const val TAG = "GeminiVideoAnalyzer"

class GeminiVideoAnalyzer(private val context: Context) {

    private val clipsSchema = Schema.obj(
        properties = mapOf(
            "clips" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "startTime" to Schema.double(description = "Clip start timestamp in seconds within video bounds"),
                        "endTime" to Schema.double(description = "Clip end timestamp in seconds within video bounds"),
                        "duration" to Schema.integer(description = "Clip duration in seconds"),
                        "title" to Schema.string(description = "Short punchy title for this engaging moment"),
                        "description" to Schema.string(description = "Concise summary of what happens in this segment"),
                        "reason" to Schema.string(description = "Why this moment works well as a short vertical hook or clip")
                    )
                ),
                description = "List of engaging short-form video moments found in the media"
            )
        )
    )

    private val subtitlesSchema = Schema.obj(
        properties = mapOf(
            "cues" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "startMs" to Schema.integer(description = "Subtitle cue start time in milliseconds relative to segment start"),
                        "endMs" to Schema.integer(description = "Subtitle cue end time in milliseconds relative to segment start"),
                        "text" to Schema.string(description = "Verbatim transcribed spoken line")
                    )
                ),
                description = "Time-aligned spoken subtitle cues"
            )
        )
    )

    private val clipsModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = clipsSchema
            }
        )
    }

    private val subtitleModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = subtitlesSchema
            }
        )
    }

    suspend fun analyzeVideoForShorts(
        metadata: VideoMetadata,
        targetClipDurationSec: Int,
        onStageProgress: (stage: String, fraction: Float) -> Unit
    ): Result<List<SuggestedClip>> {
        if (!VideoUrlDownloader.isNetworkAvailable(context)) {
            return Result.failure(
                IllegalStateException("No internet connection. Connect to Wi-Fi or mobile data to run AI analysis.")
            )
        }

        val totalSec = (metadata.durationMs / 1000.0).coerceAtLeast(1.0)
        val clampedDuration = targetClipDurationSec.coerceIn(5, totalSec.toInt().coerceAtLeast(5))

        return try {
            onStageProgress("Checking media payload size...", 0.12f)
            val inlineVideo = AudioSegmentExtractor.canSendWholeVideoInline(context, metadata.uriString)
            val allClips = mutableListOf<SuggestedClip>()

            if (inlineVideo != null) {
                onStageProgress("Analyzing video & audio with Gemini AI...", 0.45f)
                val promptText = buildClipPrompt(
                    totalDurationSec = totalSec,
                    segmentStartSec = 0.0,
                    segmentEndSec = totalSec,
                    targetDurationSec = clampedDuration
                )
                val response = withTimeout(60_000L) {
                    clipsModel.generateContent(
                        content {
                            inlineData(inlineVideo, metadata.mimeType.ifBlank { "video/mp4" })
                            text(promptText)
                        }
                    )
                }
                val rawJson = response.text ?: throw IllegalStateException("AI returned an empty response.")
                allClips.addAll(parseAndValidateClips(rawJson, 0.0, totalSec, clampedDuration))
            } else {
                onStageProgress("Extracting audio & keyframes for segmented analysis...", 0.25f)
                val segments = AudioSegmentExtractor.extractSegments(
                    context = context,
                    uriString = metadata.uriString,
                    durationMs = metadata.durationMs,
                    includeFrames = true
                )
                if (segments.isEmpty()) {
                    return Result.failure(
                        IllegalStateException("Could not extract audio or video frames from this file for AI analysis.")
                    )
                }

                try {
                    segments.forEachIndexed { idx, seg ->
                        val frac = 0.3f + (0.55f * (idx.toFloat() / segments.size.coerceAtLeast(1)))
                        val segStartSec = seg.startMs / 1000.0
                        val segEndSec = seg.endMs / 1000.0
                        onStageProgress(
                            "Analyzing segment ${idx + 1}/${segments.size} (${formatTimestampMs(seg.startMs)} - ${formatTimestampMs(seg.endMs)})...",
                            frac
                        )
                        val promptText = buildClipPrompt(
                            totalDurationSec = totalSec,
                            segmentStartSec = segStartSec,
                            segmentEndSec = segEndSec,
                            targetDurationSec = clampedDuration
                        )
                        val response = withTimeout(60_000L) {
                            clipsModel.generateContent(
                                content {
                                    seg.audioBytes?.let { inlineData(it, seg.audioMimeType) }
                                    seg.sampledFrames.forEach { (frameMs, bmp) ->
                                        text("Video frame at ${String.format("%.1f", frameMs / 1000.0)}s:")
                                        image(bmp)
                                    }
                                    text(promptText)
                                }
                            )
                        }
                        val rawJson = response.text ?: throw IllegalStateException("AI returned an empty response.")
                        allClips.addAll(
                            parseAndValidateClips(
                                jsonString = rawJson,
                                minBoundSec = segStartSec,
                                maxBoundSec = segEndSec,
                                targetDurationSec = clampedDuration,
                                offsetSecIfRelative = segStartSec
                            )
                        )
                    }
                } finally {
                    segments.forEach { it.recycleFrames() }
                }
            }

            if (allClips.isEmpty()) {
                return Result.failure(
                    IllegalStateException("Gemini AI could not identify valid timestamped highlights within this video's duration.")
                )
            }

            onStageProgress("Generating clip thumbnails...", 0.9f)
            val deduplicated = allClips
                .distinctBy { "${(it.startTimeSec / 5.0).toInt()}" }
                .sortedBy { it.startTimeSec }
                .mapIndexed { idx, clip ->
                    val thumb = VideoMetadataHelper.extractFrameAt(
                        context = context,
                        uriString = metadata.uriString,
                        timeMs = clip.startMs + 500L,
                        targetTag = "ai_clip_$idx"
                    )
                    clip.copy(thumbnailPath = thumb)
                }

            Result.success(deduplicated)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI video analysis timed out", e)
            Result.failure(IllegalStateException("AI analysis timed out after 60 seconds. Please try again."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI video analysis failed", e)
            Result.failure(
                IllegalStateException(e.localizedMessage ?: "AI video analysis failed.")
            )
        }
    }

    suspend fun generateSubtitles(
        metadata: VideoMetadata,
        language: SubtitleLanguage,
        startMs: Long = 0L,
        endMs: Long = metadata.durationMs,
        onStage: (String) -> Unit
    ): Result<List<SubtitleCue>> {
        if (!metadata.hasAudio) {
            return Result.failure(
                IllegalStateException("This video has no audio track to transcribe.")
            )
        }
        if (!VideoUrlDownloader.isNetworkAvailable(context)) {
            return Result.failure(
                IllegalStateException("No internet connection. Connect to internet to generate AI subtitles.")
            )
        }

        return try {
            onStage("Extracting speech audio track...")
            val segments = AudioSegmentExtractor.extractSegments(
                context = context,
                uriString = metadata.uriString,
                durationMs = metadata.durationMs,
                includeFrames = false
            )

            val relevantSegments = segments.filter { seg ->
                seg.endMs >= startMs && seg.startMs <= endMs && seg.audioBytes != null
            }

            if (relevantSegments.isEmpty()) {
                return Result.failure(
                    IllegalStateException("Unable to extract readable audio samples for transcription.")
                )
            }

            val allCues = mutableListOf<SubtitleCue>()
            relevantSegments.forEachIndexed { index, seg ->
                onStage("Transcribing ${language.displayName} audio (${index + 1}/${relevantSegments.size})...")
                val segDurationMs = (seg.endMs - seg.startMs).coerceAtLeast(1000L)
                val prompt = """
                    Transcribe the spoken speech in this audio accurately in ${language.promptLabel}.
                    Return short, natural subtitle lines suitable for vertical 9:16 Shorts (1 to 8 words per cue).
                    Timestamps startMs and endMs MUST be in milliseconds relative to the start of THIS audio clip (between 0 and $segDurationMs).
                    Do NOT invent speech if there is silence or only music; return an empty cues array if no speech is present.
                """.trimIndent()

                val response = withTimeout(60_000L) {
                    subtitleModel.generateContent(
                        content {
                            inlineData(seg.audioBytes!!, seg.audioMimeType)
                            text(prompt)
                        }
                    )
                }

                val jsonStr = response.text ?: throw IllegalStateException("Transcription returned empty text.")
                val root = JSONObject(jsonStr)
                val arr = root.optJSONArray("cues") ?: org.json.JSONArray()

                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val relStart = obj.optLong("startMs", -1L)
                    val relEnd = obj.optLong("endMs", -1L)
                    val text = obj.optString("text", "").trim()
                    if (text.isBlank() || relStart < 0L || relEnd <= relStart) continue

                    val absStart = (seg.startMs + relStart).coerceIn(0L, metadata.durationMs)
                    val absEnd = (seg.startMs + relEnd).coerceIn(absStart + 200L, metadata.durationMs)
                    if (absEnd > startMs && absStart < endMs) {
                        allCues.add(
                            SubtitleCue(
                                id = UUID.randomUUID().toString(),
                                startMs = absStart,
                                endMs = absEnd,
                                text = text
                            )
                        )
                    }
                }
            }

            Result.success(allCues.sortedBy { it.startMs })
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Subtitle generation timed out", e)
            Result.failure(IllegalStateException("Subtitle transcription timed out after 60 seconds."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Subtitle generation failed", e)
            Result.failure(
                IllegalStateException(e.localizedMessage ?: "Failed to generate subtitles.")
            )
        }
    }

    private fun buildClipPrompt(
        totalDurationSec: Double,
        segmentStartSec: Double,
        segmentEndSec: Double,
        targetDurationSec: Int
    ): String {
        val segSpan = (segmentEndSec - segmentStartSec).coerceAtLeast(1.0)
        return """
            Analyze this video/audio segment spanning from ${String.format("%.1f", segmentStartSec)}s to ${String.format("%.1f", segmentEndSec)}s (total video duration: ${String.format("%.1f", totalDurationSec)}s).
            Identify the most engaging moments for vertical 9:16 Shorts:
            - Strong opening hooks
            - Important or insightful conversations
            - Educational tips or useful explanations
            - Funny, emotional, or exciting scenes
            
            Rules:
            - Target clip duration around $targetDurationSec seconds (if the segment is shorter than $targetDurationSec seconds, fit within the segment).
            - Use ABSOLUTE video timestamps in seconds between ${String.format("%.1f", segmentStartSec)} and ${String.format("%.1f", segmentEndSec)} (within segment span ${String.format("%.1f", segSpan)}s).
            - Never invent timestamps outside the actual video duration.
            - Return 1 to 5 high-value clips depending on content density.
        """.trimIndent()
    }

    private fun parseAndValidateClips(
        jsonString: String,
        minBoundSec: Double,
        maxBoundSec: Double,
        targetDurationSec: Int,
        offsetSecIfRelative: Double = 0.0
    ): List<SuggestedClip> {
        val root = JSONObject(jsonString)
        val array = root.optJSONArray("clips") ?: return emptyList()
        val parsed = mutableListOf<SuggestedClip>()

        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            var start = obj.optDouble("startTime", -1.0)
            var end = obj.optDouble("endTime", -1.0)
            if (start < 0.0 || end <= start) continue

            // If segment model returned timestamps relative to segment 0..span, shift to absolute segment window
            if (offsetSecIfRelative > 0.0 && end <= (maxBoundSec - minBoundSec) + 2.0 && start < minBoundSec) {
                start += offsetSecIfRelative
                end += offsetSecIfRelative
            }

            val clampedStart = start.coerceIn(minBoundSec, (maxBoundSec - 1.0).coerceAtLeast(minBoundSec))
            var clampedEnd = end.coerceIn(clampedStart + 1.0, maxBoundSec)
            if (clampedEnd - clampedStart > targetDurationSec + 15.0) {
                clampedEnd = (clampedStart + targetDurationSec).coerceAtMost(maxBoundSec)
            }
            val duration = (clampedEnd - clampedStart).coerceAtLeast(1.0)

            val title = obj.optString("title", "Highlight ${i + 1}").trim().ifBlank { "Highlight ${i + 1}" }
            val desc = obj.optString("description", "").trim()
            val reason = obj.optString("reason", "").trim()

            parsed.add(
                SuggestedClip(
                    id = UUID.randomUUID().toString(),
                    startTimeSec = clampedStart,
                    endTimeSec = clampedEnd,
                    durationSec = duration,
                    title = title,
                    description = desc.ifBlank { "Engaging segment identified by Gemini AI." },
                    reason = reason.ifBlank { "High engagement hook or key information." },
                    isSelected = true
                )
            )
        }
        return parsed
    }
}
