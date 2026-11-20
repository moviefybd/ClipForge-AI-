package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CropPortrait
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.media.MediaStoreExporter
import com.example.model.CropMode
import com.example.model.ExportUiState
import com.example.model.ExportedVideoSummary
import com.example.model.formatFileSize
import com.example.model.formatTimestampMs
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ErrorBanner
import com.example.ui.components.StatusPill
import com.example.ui.components.StudioCard
import com.example.ui.components.StudioTopBar
import com.example.ui.components.VideoPlayerStage
import com.example.ui.theme.AccentLavender
import com.example.ui.theme.PrimaryPurple
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StudioBackground
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCardElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.ActiveEditorState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OneClickConvertScreen(
    editorState: ActiveEditorState,
    onPickVideo: () -> Unit,
    onSelectCropMode: (CropMode) -> Unit,
    onUpdateManualCrop: (Float, Float, Float) -> Unit,
    onConvertClick: () -> Unit,
    onOpenFullEditor: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        onConvertClick()
    }

    fun startConvertWithNotificationCheck() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            onConvertClick()
        }
    }

    Scaffold(
        containerColor = StudioBackground,
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            StudioTopBar(
                title = "One Click 9:16",
                subtitle = "Convert any video into 1080×1920 vertical Shorts",
                onBack = onBack
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .navigationBarsPadding(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (editorState.metadata == null) {
                item {
                    EmptyStateCard(
                        title = "Select Video to Convert",
                        description = "Choose any landscape, square, or vertical video from your device and convert it to 1080×1920 9:16 MP4 in one tap.",
                        icon = Icons.Filled.CropPortrait,
                        actionLabel = "Select Video",
                        onAction = onPickVideo
                    )
                }
            } else {
                val meta = editorState.metadata
                item {
                    VideoPlayerStage(
                        uriString = meta.uriString,
                        startMs = editorState.clipStartMs,
                        endMs = editorState.clipEndMs,
                        cropSettings = editorState.cropSettings,
                        showVerticalStage = true,
                        subtitlesEnabled = editorState.subtitlesEnabled,
                        subtitleCues = editorState.subtitleCues,
                        subtitleStyle = editorState.subtitleStyle,
                        watermarkConfig = editorState.watermarkConfig
                    )
                }

                item {
                    StudioCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Crop Mode", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            OutlinedButton(onClick = onPickVideo) {
                                Text("Change Video")
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CropMode.entries.forEach { mode ->
                                FilterChip(
                                    selected = editorState.cropSettings.mode == mode,
                                    onClick = { onSelectCropMode(mode) },
                                    label = { Text(mode.displayName) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = PrimaryPurple,
                                        selectedLabelColor = TextPrimary
                                    ),
                                    modifier = Modifier.testTag("one_click_crop_${mode.name}")
                                )
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = editorState.cropSettings.mode.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )

                        if (editorState.cropSettings.mode == CropMode.CENTER_CROP ||
                            editorState.cropSettings.mode == CropMode.SMART_CROP
                        ) {
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = "Manual Pan X (${String.format("%.2f", editorState.cropSettings.manualPanX)}) • Zoom (${String.format("%.1fx", editorState.cropSettings.zoom)})",
                                style = MaterialTheme.typography.labelSmall,
                                color = AccentLavender
                            )
                            Slider(
                                value = editorState.cropSettings.manualPanX,
                                onValueChange = {
                                    onUpdateManualCrop(it, editorState.cropSettings.manualPanY, editorState.cropSettings.zoom)
                                },
                                valueRange = -0.8f..0.8f
                            )
                            Slider(
                                value = editorState.cropSettings.zoom,
                                onValueChange = {
                                    onUpdateManualCrop(editorState.cropSettings.manualPanX, editorState.cropSettings.manualPanY, it)
                                },
                                valueRange = 1.0f..2.2f
                            )
                        }
                    }
                }

                item {
                    StudioCard {
                        Text("Output Specification", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "9:16 Vertical • 1080×1920 • MP4 (H.264 + AAC) • Saved to Movies/ClipForge",
                            style = MaterialTheme.typography.bodySmall,
                            color = AccentLavender
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = onOpenFullEditor,
                                border = BorderStroke(1.dp, StudioBorder),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Advanced")
                            }
                            Button(
                                onClick = { startConvertWithNotificationCheck() },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1.6f)
                                    .height(50.dp)
                                    .testTag("one_click_convert_button")
                            ) {
                                Text("Convert to 9:16")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VideoPreviewScreen(
    uriString: String,
    title: String,
    startMs: Long,
    endMs: Long,
    showOverlays: Boolean,
    editorState: ActiveEditorState,
    onEditInStudio: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    Scaffold(
        containerColor = StudioBackground,
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            StudioTopBar(
                title = "Video Preview",
                subtitle = title,
                onBack = onBack
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .navigationBarsPadding(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                VideoPlayerStage(
                    uriString = uriString,
                    startMs = startMs,
                    endMs = endMs,
                    cropSettings = if (showOverlays) editorState.cropSettings else com.example.model.CropSettings(),
                    showVerticalStage = true,
                    subtitlesEnabled = showOverlays && editorState.subtitlesEnabled,
                    subtitleCues = if (showOverlays) editorState.subtitleCues else emptyList(),
                    subtitleStyle = editorState.subtitleStyle,
                    watermarkConfig = if (showOverlays) editorState.watermarkConfig else com.example.model.WatermarkConfig(enabled = false)
                )
            }

            item {
                StudioCard {
                    Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    if (endMs > startMs) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Clip Segment: ${formatTimestampMs(startMs)} - ${formatTimestampMs(endMs)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = AccentLavender
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val intent = MediaStoreExporter.buildShareIntent(context, uriString, title)
                                context.startActivity(intent)
                            },
                            modifier = Modifier.weight(1f).testTag("preview_share_btn")
                        ) {
                            Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Share")
                        }
                        Button(
                            onClick = onEditInStudio,
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                            modifier = Modifier.weight(1f).testTag("preview_edit_btn")
                        ) {
                            Icon(Icons.Filled.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Edit in Studio")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExportProgressScreen(
    exportState: ExportUiState,
    onCancelExport: () -> Unit,
    onRetryExport: () -> Unit,
    onPreviewExported: (ExportedVideoSummary) -> Unit,
    onViewHistory: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    Scaffold(
        containerColor = StudioBackground,
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            StudioTopBar(
                title = "Export Engine",
                subtitle = "Media3 Hardware H.264 / AAC Pipeline",
                onBack = onBack
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .navigationBarsPadding(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (exportState) {
                is ExportUiState.Idle -> {
                    item {
                        EmptyStateCard(
                            title = "No Active Export",
                            description = "Start a conversion from One Click 9:16, AI Clips, or the Video Editor.",
                            actionLabel = "Back",
                            onAction = onBack
                        )
                    }
                }

                is ExportUiState.Exporting -> {
                    item {
                        StudioCard(highlighted = true) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(
                                        progress = { (exportState.progressPercent / 100f).coerceIn(0.02f, 1f) },
                                        color = PrimaryPurple,
                                        trackColor = StudioCardElevated,
                                        strokeWidth = 6.dp,
                                        modifier = Modifier.size(96.dp)
                                    )
                                    Text(
                                        text = "${exportState.progressPercent}%",
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = TextPrimary
                                    )
                                }
                                Text(
                                    text = "Clip ${exportState.currentIndex} of ${exportState.totalCount}: ${exportState.currentTitle}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary
                                )
                                Text(
                                    text = exportState.stageLabel,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = AccentLavender
                                )
                                LinearProgressIndicator(
                                    progress = { (exportState.progressPercent / 100f).coerceIn(0.02f, 1f) },
                                    color = PrimaryPurple,
                                    trackColor = StudioCardElevated,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(999.dp))
                                )
                                OutlinedButton(
                                    onClick = onCancelExport,
                                    border = BorderStroke(1.dp, StudioBorder),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("cancel_export_button")
                                ) {
                                    Icon(Icons.Filled.Cancel, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Cancel Export")
                                }
                            }
                        }
                    }
                }

                is ExportUiState.Completed -> {
                    item {
                        StudioCard(highlighted = true) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = "Export complete",
                                    tint = StatusSuccess,
                                    modifier = Modifier.size(28.dp)
                                )
                                Column {
                                    Text("Export Complete", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                                    Text(
                                        text = "Saved ${exportState.exportedItems.size} MP4 video(s) to Movies/ClipForge",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = StatusSuccess
                                    )
                                }
                            }
                        }
                    }

                    items(exportState.exportedItems) { item ->
                        StudioCard {
                            Text(item.title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "${item.width}×${item.height} • ${formatTimestampMs(item.durationMs)} • ${formatFileSize(item.fileSizeBytes)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = AccentLavender
                            )
                            Text(
                                text = "Location: ${item.filePath}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            Spacer(Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                Button(
                                    onClick = { onPreviewExported(item) },
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                                    modifier = Modifier.weight(1f).testTag("export_done_preview_btn")
                                ) {
                                    Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Preview")
                                }
                                OutlinedButton(
                                    onClick = {
                                        val share = MediaStoreExporter.buildShareIntent(
                                            context,
                                            item.outputUriString,
                                            item.title
                                        )
                                        context.startActivity(share)
                                    },
                                    modifier = Modifier.weight(1f).testTag("export_done_share_btn")
                                ) {
                                    Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Share")
                                }
                            }
                        }
                    }

                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = onRetryExport,
                                modifier = Modifier.weight(1f).testTag("export_again_btn")
                            ) {
                                Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Export Again")
                            }
                            Button(
                                onClick = onViewHistory,
                                colors = ButtonDefaults.buttonColors(containerColor = StudioCardElevated),
                                modifier = Modifier.weight(1f).testTag("export_view_history_btn")
                            ) {
                                Text("Export History")
                            }
                        }
                    }
                }

                is ExportUiState.Cancelled -> {
                    item {
                        StudioCard {
                            Text("Export Cancelled", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "The video export was cancelled cleanly and temporary files were removed.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                            Spacer(Modifier.height(12.dp))
                            Button(
                                onClick = onRetryExport,
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Retry Export")
                            }
                        }
                    }
                }

                is ExportUiState.Error -> {
                    item {
                        ErrorBanner(message = exportState.message)
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = onRetryExport,
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Retry Export")
                        }
                    }
                }
            }
        }
    }
}
