package com.example.service

import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.media.MediaStoreExporter

class VideoExportService : Service() {

    private var isForegroundStarted = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val title = intent.getStringExtra(EXTRA_TITLE) ?: "Exporting Short"
                val progress = intent.getIntExtra(EXTRA_PROGRESS, 0)
                val notification = buildNotification(title, progress)
                runCatching {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        startForeground(
                            NOTIFICATION_ID,
                            notification,
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                        )
                    } else {
                        startForeground(NOTIFICATION_ID, notification)
                    }
                    isForegroundStarted = true
                }
                if (stopRequested.getAndSet(false)) {
                    runCatching {
                        if (isForegroundStarted) {
                            stopForeground(STOP_FOREGROUND_REMOVE)
                            isForegroundStarted = false
                        }
                    }
                    stopSelf()
                }
            }
            ACTION_UPDATE -> {
                val title = intent.getStringExtra(EXTRA_TITLE) ?: "Exporting Short"
                val progress = intent.getIntExtra(EXTRA_PROGRESS, 0)
                runCatching {
                    val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                    manager?.notify(NOTIFICATION_ID, buildNotification(title, progress))
                }
            }
            ACTION_STOP, null -> {
                stopRequested.set(false)
                runCatching {
                    if (isForegroundStarted) {
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        isForegroundStarted = false
                    }
                }
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        stopRequested.set(false)
        runCatching {
            if (isForegroundStarted) {
                stopForeground(STOP_FOREGROUND_REMOVE)
                isForegroundStarted = false
            }
        }
        super.onDestroy()
    }

    private fun buildNotification(title: String, progress: Int): Notification {
        MediaStoreExporter.ensureNotificationChannel(this)
        return NotificationCompat.Builder(this, MediaStoreExporter.channelId())
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setContentTitle("ClipForge AI • $title")
            .setContentText("Rendering 9:16 video ($progress%)")
            .setProgress(100, progress.coerceIn(0, 100), progress <= 0)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    companion object {
        private const val NOTIFICATION_ID = 4101
        private val stopRequested = java.util.concurrent.atomic.AtomicBoolean(false)
        const val ACTION_START = "com.example.action.EXPORT_START"
        const val ACTION_UPDATE = "com.example.action.EXPORT_UPDATE"
        const val ACTION_STOP = "com.example.action.EXPORT_STOP"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_PROGRESS = "extra_progress"

        fun start(context: Context, title: String) {
            stopRequested.set(false)
            val intent = Intent(context, VideoExportService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_PROGRESS, 0)
            }
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            }
        }

        fun updateProgress(context: Context, title: String, progress: Int) {
            if (stopRequested.get()) return
            val intent = Intent(context, VideoExportService::class.java).apply {
                action = ACTION_UPDATE
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_PROGRESS, progress)
            }
            runCatching { context.startService(intent) }
        }

        fun stop(context: Context) {
            stopRequested.set(true)
            val stopIntent = Intent(context, VideoExportService::class.java).apply {
                action = ACTION_STOP
            }
            runCatching { context.startService(stopIntent) }
                .onFailure {
                    runCatching { context.stopService(Intent(context, VideoExportService::class.java)) }
                }
        }
    }
}
