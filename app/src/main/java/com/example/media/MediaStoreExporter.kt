package com.example.media

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import com.example.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object MediaStoreExporter {

    private const val CHANNEL_ID = "clipforge_exports"
    private const val NOTIFICATION_ID_DONE = 4102

    data class SavedOutput(
        val contentUri: Uri,
        val displayPath: String,
        val fileSizeBytes: Long
    )

    suspend fun saveToMoviesClipForge(
        context: Context,
        tempExportFile: File,
        displayTitle: String
    ): Result<SavedOutput> = withContext(Dispatchers.IO) {
        runCatching {
            if (!tempExportFile.exists() || tempExportFile.length() == 0L) {
                throw IllegalStateException("Export failed: output MP4 file was not created.")
            }

            val safeName = displayTitle
                .replace(Regex("[^a-zA-Z0-9_\\- ]"), "_")
                .trim()
                .ifBlank { "ClipForge_Short" }
                .take(40) + "_${System.currentTimeMillis()}.mp4"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Video.Media.DISPLAY_NAME, safeName)
                    put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                    put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/ClipForge")
                    put(MediaStore.Video.Media.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                val itemUri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
                    ?: throw IllegalStateException("Unable to create MediaStore entry in Movies/ClipForge.")

                try {
                    resolver.openOutputStream(itemUri)?.use { out ->
                        FileInputStream(tempExportFile).use { input ->
                            input.copyTo(out)
                        }
                    } ?: throw IllegalStateException("Failed to open MediaStore output stream.")

                    val completeValues = ContentValues().apply {
                        put(MediaStore.Video.Media.IS_PENDING, 0)
                    }
                    resolver.update(itemUri, completeValues, null, null)

                    SavedOutput(
                        contentUri = itemUri,
                        displayPath = "Movies/ClipForge/$safeName",
                        fileSizeBytes = tempExportFile.length()
                    )
                } catch (e: Exception) {
                    runCatching { resolver.delete(itemUri, null, null) }
                    throw e
                }
            } else {
                val moviesDir = File(
                    context.getExternalFilesDir(Environment.DIRECTORY_MOVIES),
                    "ClipForge"
                ).apply { mkdirs() }
                val destFile = File(moviesDir, safeName)
                FileInputStream(tempExportFile).use { input ->
                    FileOutputStream(destFile).use { out ->
                        input.copyTo(out)
                    }
                }
                val contentUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    destFile
                )
                SavedOutput(
                    contentUri = contentUri,
                    displayPath = destFile.absolutePath,
                    fileSizeBytes = destFile.length()
                )
            }
        }
    }

    fun buildShareIntent(context: Context, uriString: String, title: String): Intent {
        val uri = Uri.parse(uriString)
        val shareUri = if (uri.scheme == "file" && !uri.path.isNullOrBlank()) {
            runCatching {
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    File(uri.path!!)
                )
            }.getOrDefault(uri)
        } else {
            uri
        }

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "video/mp4"
            putExtra(Intent.EXTRA_STREAM, shareUri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(sendIntent, "Share $title")
    }

    fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.export_channel_name),
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = context.getString(R.string.export_channel_desc)
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    fun postCompletionNotification(context: Context, title: String, savedPath: String) {
        try {
            ensureNotificationChannel(context)
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentTitle("Short Ready: $title")
                .setContentText("Saved to $savedPath")
                .setAutoCancel(true)
                .build()
            manager.notify(NOTIFICATION_ID_DONE, notification)
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS denied by user; export itself remains successful
        } catch (_: Exception) {
        }
    }

    fun channelId(): String = CHANNEL_ID
}
