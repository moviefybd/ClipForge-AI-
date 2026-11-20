package com.example.media

import android.content.Context
import android.net.Uri
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

enum class YouTubeLicenseStatus {
    VERIFIED_CREATIVE_COMMONS,
    UNVERIFIED_OR_STANDARD
}

data class YouTubeMetadataResult(
    val videoId: String,
    val watchUrl: String,
    val title: String?,
    val channelTitle: String?,
    val thumbnailUrl: String?,
    val rawLicense: String?,
    val licenseStatus: YouTubeLicenseStatus,
    val message: String,
    val detailNote: String,
    val sourceLabel: String
)

object YouTubeLicenseChecker {

    const val VERIFIED_MESSAGE = "License verified — eligible for permitted use."
    const val WARNING_MESSAGE =
        "Copyright permission not verified. Please use content you own or have permission to edit."

    private const val PLACEHOLDER_KEY = "YOUR_YOUTUBE_DATA_API_KEY"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val videoIdRegex = Regex("^[A-Za-z0-9_-]{11}$")

    fun extractYouTubeVideoId(rawUrl: String): String? {
        val uri = runCatching { Uri.parse(rawUrl.trim()) }.getOrNull() ?: return null
        val host = uri.host?.lowercase()?.removePrefix("www.") ?: return null
        val segments = uri.pathSegments.orEmpty()

        val candidate = when {
            host == "youtu.be" -> segments.firstOrNull()
            host == "youtube.com" || host == "m.youtube.com" || host == "music.youtube.com" || host == "youtube-nocookie.com" -> {
                val queryV = uri.getQueryParameter("v")
                when {
                    !queryV.isNullOrBlank() -> queryV
                    segments.size >= 2 && segments[0] in setOf("shorts", "embed", "live", "v") -> segments[1]
                    else -> null
                }
            }
            else -> null
        }?.trim()

        return candidate?.takeIf { videoIdRegex.matches(it) }
    }

    fun canonicalWatchUrl(videoId: String): String =
        "https://www.youtube.com/watch?v=$videoId"

    suspend fun checkVideoLicense(
        context: Context,
        rawUrl: String
    ): Result<YouTubeMetadataResult> = withContext(Dispatchers.IO) {
        runCatching {
            val videoId = extractYouTubeVideoId(rawUrl)
                ?: throw IllegalArgumentException("Could not identify a valid YouTube video ID from this URL.")

            if (!VideoUrlDownloader.isNetworkAvailable(context)) {
                throw IllegalStateException("No internet connection detected. Connect to verify YouTube license metadata.")
            }

            val watchUrl = canonicalWatchUrl(videoId)
            val apiKey = optionalYouTubeApiKey()
            val hasApiKey = apiKey.isNotEmpty() && apiKey != PLACEHOLDER_KEY

            if (hasApiKey) {
                val apiResult = fetchFromYouTubeDataApi(videoId, watchUrl, apiKey)
                if (apiResult != null) {
                    return@runCatching apiResult
                }
            }

            // Fallback to official public YouTube oEmbed endpoint for title/author identification.
            // oEmbed does not report license terms, so copyright permission remains strictly unverified.
            fetchFromOEmbed(videoId, watchUrl, apiKeyConfigured = hasApiKey)
        }
    }

    private fun optionalYouTubeApiKey(): String = runCatching {
        BuildConfig::class.java.getField("YOUTUBE_API_KEY").get(null) as? String
    }.getOrNull()?.trim().orEmpty()

    private fun fetchFromYouTubeDataApi(
        videoId: String,
        watchUrl: String,
        apiKey: String
    ): YouTubeMetadataResult? {
        val url = "https://www.googleapis.com/youtube/v3/videos" +
            "?part=status,snippet&id=${URLEncoder.encode(videoId, "UTF-8")}" +
            "&key=${URLEncoder.encode(apiKey, "UTF-8")}"
        val request = Request.Builder().url(url).get().build()

        return runCatching {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                val body = response.body?.string().orEmpty()
                if (body.isBlank()) return@use null
                parseYouTubeApiResponse(videoId, watchUrl, body)
            }
        }.getOrNull()
    }

    fun parseYouTubeApiResponse(
        videoId: String,
        watchUrl: String,
        jsonBody: String
    ): YouTubeMetadataResult {
        val root = JSONObject(jsonBody)
        val items = root.optJSONArray("items")
        if (items == null || items.length() == 0) {
            return YouTubeMetadataResult(
                videoId = videoId,
                watchUrl = watchUrl,
                title = null,
                channelTitle = null,
                thumbnailUrl = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg",
                rawLicense = null,
                licenseStatus = YouTubeLicenseStatus.UNVERIFIED_OR_STANDARD,
                message = WARNING_MESSAGE,
                detailNote = "Video metadata was unavailable or private via YouTube Data API v3.",
                sourceLabel = "YouTube Data API v3"
            )
        }

        val item = items.getJSONObject(0)
        val snippet = item.optJSONObject("snippet")
        val status = item.optJSONObject("status")

        val title = snippet?.optString("title")?.takeIf { it.isNotBlank() }
        val channel = snippet?.optString("channelTitle")?.takeIf { it.isNotBlank() }
        val thumbs = snippet?.optJSONObject("thumbnails")
        val thumbUrl = thumbs?.optJSONObject("high")?.optString("url")
            ?.takeIf { it.isNotBlank() }
            ?: thumbs?.optJSONObject("medium")?.optString("url")?.takeIf { it.isNotBlank() }
            ?: "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"

        val license = status?.optString("license")?.trim()?.takeIf { it.isNotBlank() }
        val privacy = status?.optString("privacyStatus")?.trim().orEmpty()

        val isCreativeCommons = license.equals("creativeCommon", ignoreCase = true) &&
            privacy != "private"

        return if (isCreativeCommons) {
            YouTubeMetadataResult(
                videoId = videoId,
                watchUrl = watchUrl,
                title = title,
                channelTitle = channel,
                thumbnailUrl = thumbUrl,
                rawLicense = license,
                licenseStatus = YouTubeLicenseStatus.VERIFIED_CREATIVE_COMMONS,
                message = VERIFIED_MESSAGE,
                detailNote = "Verified Creative Commons Attribution (CC BY) license in YouTube Data API v3. Remember to credit the creator when publishing.",
                sourceLabel = "YouTube Data API v3"
            )
        } else {
            val licenseDesc = when {
                license.equals("youtube", ignoreCase = true) ->
                    "Standard YouTube License detected. Direct downloading or third-party re-editing requires creator authorization."
                else ->
                    "License metadata is unclear or does not indicate Creative Commons reuse permission."
            }
            YouTubeMetadataResult(
                videoId = videoId,
                watchUrl = watchUrl,
                title = title,
                channelTitle = channel,
                thumbnailUrl = thumbUrl,
                rawLicense = license,
                licenseStatus = YouTubeLicenseStatus.UNVERIFIED_OR_STANDARD,
                message = WARNING_MESSAGE,
                detailNote = licenseDesc,
                sourceLabel = "YouTube Data API v3"
            )
        }
    }

    private fun fetchFromOEmbed(
        videoId: String,
        watchUrl: String,
        apiKeyConfigured: Boolean
    ): YouTubeMetadataResult {
        val oembedUrl = "https://www.youtube.com/oembed?url=" +
            URLEncoder.encode(watchUrl, "UTF-8") + "&format=json"
        val request = Request.Builder().url(oembedUrl).get().build()

        var title: String? = null
        var author: String? = null
        var thumb: String? = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"

        runCatching {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string().orEmpty()
                    if (body.isNotBlank()) {
                        val obj = JSONObject(body)
                        title = obj.optString("title").takeIf { it.isNotBlank() }
                        author = obj.optString("author_name").takeIf { it.isNotBlank() }
                        thumb = obj.optString("thumbnail_url").takeIf { it.isNotBlank() } ?: thumb
                    }
                }
            }
        }

        val note = if (apiKeyConfigured) {
            "YouTube Data API v3 did not return verified Creative Commons license metadata for this video."
        } else {
            "Public YouTube metadata identified this video, but license reuse permission could not be verified."
        }

        return YouTubeMetadataResult(
            videoId = videoId,
            watchUrl = watchUrl,
            title = title,
            channelTitle = author,
            thumbnailUrl = thumb,
            rawLicense = null,
            licenseStatus = YouTubeLicenseStatus.UNVERIFIED_OR_STANDARD,
            message = WARNING_MESSAGE,
            detailNote = note,
            sourceLabel = "YouTube oEmbed Metadata"
        )
    }
}
