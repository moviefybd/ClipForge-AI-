package com.example.media

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.util.Patterns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

object VideoUrlDownloader {

    private val restrictedHosts = listOf(
        "youtube.com",
        "youtu.be",
        "tiktok.com",
        "instagram.com",
        "facebook.com",
        "fb.watch",
        "twitter.com",
        "x.com",
        "vimeo.com",
        "dailymotion.com",
        "netflix.com"
    )

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    sealed interface ValidationResult {
        data class Valid(val normalizedUrl: String) : ValidationResult
        data class Invalid(val reason: String) : ValidationResult
    }

    fun validateUrl(rawUrl: String): ValidationResult {
        val trimmed = rawUrl.trim()
        if (trimmed.isEmpty()) {
            return ValidationResult.Invalid("Please enter a video URL.")
        }
        if (!trimmed.startsWith("https://", ignoreCase = true) &&
            !trimmed.startsWith("http://", ignoreCase = true)
        ) {
            return ValidationResult.Invalid("URL must start with https:// or http://.")
        }
        if (!Patterns.WEB_URL.matcher(trimmed).matches()) {
            return ValidationResult.Invalid("The URL format appears invalid. Check for typos.")
        }
        val uri = Uri.parse(trimmed)
        val host = uri.host?.lowercase().orEmpty()
        if (host.isEmpty()) {
            return ValidationResult.Invalid("Unable to determine the server host from this URL.")
        }
        if (restrictedHosts.any { host == it || host.endsWith(".$it") }) {
            return ValidationResult.Invalid(
                "Streaming platform pages ($host) cannot be downloaded due to platform terms and copyright. Please paste a direct, authorized video file URL (such as .mp4, .mov, or .webm) or import a local video from your device."
            )
        }
        return ValidationResult.Valid(trimmed)
    }

    fun isNetworkAvailable(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return true
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    suspend fun downloadDirectVideo(
        context: Context,
        url: String,
        maxBytes: Long = 350L * 1024L * 1024L,
        onProgress: (percent: Int, downloadedMb: Float, totalMb: Float?) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            if (!isNetworkAvailable(context)) {
                throw IllegalStateException("No internet connection detected. Check Wi-Fi or mobile data.")
            }

            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IllegalStateException(
                        "Server returned HTTP ${response.code}. Ensure the URL is publicly accessible or authorized."
                    )
                }

                val body = response.body ?: throw IllegalStateException("Empty response body from server.")
                val contentType = response.header("Content-Type")?.lowercase().orEmpty()
                val looksLikeVideoHeader = contentType.startsWith("video/") ||
                    contentType.contains("octet-stream") ||
                    url.substringBefore('?').lowercase().let {
                        it.endsWith(".mp4") || it.endsWith(".mov") || it.endsWith(".webm") || it.endsWith(".m4v")
                    }

                if (!looksLikeVideoHeader) {
                    throw IllegalArgumentException(
                        "URL returned '$contentType' instead of a direct video file. Paste a direct MP4/MOV/WebM file link."
                    )
                }

                val contentLength = body.contentLength()
                if (contentLength > maxBytes) {
                    throw IllegalArgumentException(
                        "Remote video exceeds the 350 MB direct download limit. Import it locally instead."
                    )
                }

                val requiredBytes = if (contentLength > 0) contentLength + 32L * 1024L * 1024L else 100L * 1024L * 1024L
                if (!VideoMetadataHelper.hasEnoughStorage(context, requiredBytes)) {
                    throw IllegalStateException("Insufficient device storage to download this video.")
                }

                val downloadDir = File(context.cacheDir, "url_videos").apply { mkdirs() }
                val fileName = "url_import_${System.currentTimeMillis()}.mp4"
                val outFile = File(downloadDir, fileName)

                body.byteStream().use { input ->
                    FileOutputStream(outFile).use { output ->
                        val buffer = ByteArray(16 * 1024)
                        var bytesCopied = 0L
                        var read: Int
                        while (input.read(buffer).also { read = it } != -1) {
                            if (!coroutineContext.isActive) {
                                outFile.delete()
                                throw kotlinx.coroutines.CancellationException("Download cancelled")
                            }
                            output.write(buffer, 0, read)
                            bytesCopied += read
                            if (bytesCopied > maxBytes) {
                                outFile.delete()
                                throw IllegalArgumentException("Video exceeded 350 MB maximum download size.")
                            }
                            val pct = if (contentLength > 0) {
                                ((bytesCopied * 100L) / contentLength).toInt().coerceIn(0, 100)
                            } else 0
                            val downloadedMb = bytesCopied / (1024f * 1024f)
                            val totalMb = if (contentLength > 0) contentLength / (1024f * 1024f) else null
                            onProgress(pct, downloadedMb, totalMb)
                        }
                    }
                }

                if (!outFile.exists() || outFile.length() == 0L) {
                    throw IllegalStateException("Downloaded file was empty.")
                }
                outFile
            }
        }
    }
}
