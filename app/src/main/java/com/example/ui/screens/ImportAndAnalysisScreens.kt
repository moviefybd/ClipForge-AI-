package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.media.YouTubeMetadataResult
import com.example.model.AiAnalysisState
import com.example.model.SuggestedClip
import com.example.model.UrlDownloadState
import com.example.model.VideoMetadata
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
import com.example.ui.theme.PrimaryPurpleContainer
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusErrorContainer
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.StudioBackground
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCardElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.ActiveEditorState
import java.io.File

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiShortsGeneratorScreen(
    editorState: ActiveEditorState,
    onSelectDuration: (Int) -> Unit,
    onOpenUrlInput: () -> Unit,
    onOpenLocalPicker: () -> Unit,
    onGoToAnalysis: () -> Unit,
    onBack: () -> Unit
) {
    val presetDurations = listOf(15, 30, 45)
    var showCustomSlider by remember {
        mutableStateOf(editorState.targetClipDurationSec !in presetDurations)
    }

    Scaffold(
        containerColor = StudioBackground,
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            StudioTopBar(
                title = "AI Shorts Generator",
                subtitle = "Find engaging hooks and moments automatically",
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
                StudioCard {
                    Text(
                        text = "Choose Video Source",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Import a video from your device, paste a direct authorized MP4/MOV/WebM URL, or check a YouTube link's reuse license.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(Modifier.height(14.dp))

                    // Option B: Local Video (Primary)
                    Button(
                        onClick = onOpenLocalPicker,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("ai_shorts_local_button")
                    ) {
                        Icon(Icons.Filled.PhotoLibrary, contentDescription = null)
                        Spacer(Modifier.width(10.dp))
                        Text("Import Local Video (SAF / Picker)")
                    }

                    Spacer(Modifier.height(10.dp))

                    // Option A: Video URL
                    OutlinedButton(
                        onClick = onOpenUrlInput,
                        border = BorderStroke(1.dp, StudioBorder),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("ai_shorts_url_button")
                    ) {
                        Icon(Icons.Filled.Link, contentDescription = null, tint = AccentLavender)
                        Spacer(Modifier.width(10.dp))
                        Text("Paste Video or YouTube URL", color = TextPrimary)
                    }
                }
            }

            item {
                StudioCard {
                    Text(
                        text = "Target Clip Duration",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Default is 30 seconds. Maximum default suggestion window is 45 seconds unless custom.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Spacer(Modifier.height(12.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        presetDurations.forEach { sec ->
                            val selected = !showCustomSlider && editorState.targetClipDurationSec == sec
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    showCustomSlider = false
                                    onSelectDuration(sec)
                                },
                                label = { Text("${sec}s") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryPurple,
                                    selectedLabelColor = TextPrimary
                                ),
                                modifier = Modifier.testTag("duration_chip_$sec")
                            )
                        }
                        FilterChip(
                            selected = showCustomSlider,
                            onClick = { showCustomSlider = true },
                            label = { Text("Custom (${editorState.targetClipDurationSec}s)") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryPurple,
                                selectedLabelColor = TextPrimary
                            ),
                            modifier = Modifier.testTag("duration_chip_custom")
                        )
                    }

                    if (showCustomSlider) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "Custom length: ${editorState.targetClipDurationSec} seconds",
                            style = MaterialTheme.typography.labelMedium,
                            color = AccentLavender
                        )
                        Slider(
                            value = editorState.targetClipDurationSec.toFloat(),
                            onValueChange = { onSelectDuration(it.toInt()) },
                            valueRange = 10f..90f,
                            colors = SliderDefaults.colors(
                                thumbColor = PrimaryPurple,
                                activeTrackColor = PrimaryPurple
                            )
                        )
                    }
                }
            }

            if (editorState.metadata != null) {
                item {
                    VideoMetadataSummaryCard(
                        metadata = editorState.metadata,
                        primaryActionLabel = "Continue to AI Video Analysis",
                        onPrimaryAction = onGoToAnalysis
                    )
                }
            }
        }
    }
}

@Composable
fun VideoUrlInputScreen(
    urlState: UrlDownloadState,
    onStartDownload: (String) -> Unit,
    onCancelDownload: () -> Unit,
    onResetState: () -> Unit,
    onOpenLocalPicker: () -> Unit,
    onContinueToTarget: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var urlText by remember { mutableStateOf("") }

    Scaffold(
        containerColor = StudioBackground,
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            StudioTopBar(
                title = "Video URL Input",
                subtitle = "Authorized direct video URLs & YouTube license check",
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
                StudioCard {
                    Text(
                        text = "Paste Video URL",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Paste a direct authorized video file URL (.mp4, .mov, .webm) to import immediately, or paste a YouTube URL to identify the video and check its reuse license via authorized metadata.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Spacer(Modifier.height(14.dp))
                    OutlinedTextField(
                        value = urlText,
                        onValueChange = {
                            urlText = it
                            if (urlState is UrlDownloadState.Error ||
                                urlState is UrlDownloadState.YouTubeLicenseVerified ||
                                urlState is UrlDownloadState.YouTubePermissionWarning
                            ) {
                                onResetState()
                            }
                        },
                        label = { Text("Video or YouTube URL (https://...)") },
                        placeholder = { Text("https://example.com/sample_talk.mp4") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryPurple,
                            unfocusedBorderColor = StudioBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("video_url_textfield")
                    )
                    Spacer(Modifier.height(14.dp))

                    when (urlState) {
                        is UrlDownloadState.Idle -> {
                            Button(
                                onClick = { onStartDownload(urlText) },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("fetch_url_button")
                            ) {
                                Text("Verify & Load Video")
                            }
                        }

                        is UrlDownloadState.Validating -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = PrimaryPurple,
                                    strokeWidth = 2.5.dp
                                )
                                Text(
                                    text = "Validating URL & content headers...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                        }

                        is UrlDownloadState.CheckingYouTubeLicense -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.testTag("youtube_license_checking")
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = PrimaryPurple,
                                    strokeWidth = 2.5.dp
                                )
                                Text(
                                    text = "Identifying YouTube video and verifying license metadata...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                        }

                        is UrlDownloadState.YouTubeLicenseVerified -> {
                            YouTubeMetadataCard(
                                info = urlState.info,
                                isVerified = true,
                                modifier = Modifier.testTag("youtube_license_verified_card")
                            )
                            Spacer(Modifier.height(12.dp))
                            Button(
                                onClick = onOpenLocalPicker,
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("youtube_authorized_import_local_button")
                            ) {
                                Icon(Icons.Filled.PhotoLibrary, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Continue: Import Authorized Video File")
                            }
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = {
                                    runCatching {
                                        context.startActivity(
                                            Intent(Intent.ACTION_VIEW, Uri.parse(urlState.info.watchUrl))
                                        )
                                    }
                                },
                                border = BorderStroke(1.dp, StudioBorder),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("youtube_open_watch_button")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = AccentLavender)
                                Spacer(Modifier.width(8.dp))
                                Text("Open Video & Attribution on YouTube", color = TextPrimary)
                            }
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = {
                                    urlText = ""
                                    onResetState()
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Paste Direct Authorized MP4 Link Instead", color = AccentLavender)
                            }
                        }

                        is UrlDownloadState.YouTubePermissionWarning -> {
                            YouTubeMetadataCard(
                                info = urlState.info,
                                isVerified = false,
                                modifier = Modifier.testTag("youtube_permission_warning_card")
                            )
                            Spacer(Modifier.height(12.dp))
                            Button(
                                onClick = onOpenLocalPicker,
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("youtube_warning_import_local_button")
                            ) {
                                Icon(Icons.Filled.PhotoLibrary, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Import Local Video You Own")
                            }
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = {
                                    urlText = ""
                                    onResetState()
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Try Another URL", color = TextPrimary)
                            }
                        }

                        is UrlDownloadState.Downloading -> {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                val totalLabel = urlState.totalMb?.let { String.format("/ %.1f MB", it) }.orEmpty()
                                Text(
                                    text = "Downloading ${String.format("%.1f MB", urlState.downloadedMb)} $totalLabel (${urlState.progressPercent}%)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = AccentLavender
                                )
                                LinearProgressIndicator(
                                    progress = { (urlState.progressPercent / 100f).coerceIn(0f, 1f) },
                                    color = PrimaryPurple,
                                    trackColor = StudioCardElevated,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(999.dp))
                                )
                                OutlinedButton(
                                    onClick = onCancelDownload,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Cancel Download")
                                }
                            }
                        }

                        is UrlDownloadState.Completed -> {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                StatusPill(label = "Video Ready (${urlState.metadata.aspectRatioLabel})", color = StatusSuccess)
                                Button(
                                    onClick = onContinueToTarget,
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .testTag("url_continue_button")
                                ) {
                                    Text("Proceed with Video")
                                }
                            }
                        }

                        is UrlDownloadState.Error -> {
                            ErrorBanner(message = urlState.message, onDismiss = onResetState)
                            Spacer(Modifier.height(10.dp))
                            Button(
                                onClick = { onStartDownload(urlText) },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Retry URL")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun YouTubeMetadataCard(
    info: YouTubeMetadataResult,
    isVerified: Boolean,
    modifier: Modifier = Modifier
) {
    val accentColor = if (isVerified) StatusSuccess else StatusWarning
    val containerColor = if (isVerified) {
        StatusSuccess.copy(alpha = 0.12f)
    } else {
        StatusErrorContainer
    }
    val borderColor = if (isVerified) {
        StatusSuccess.copy(alpha = 0.5f)
    } else {
        StatusError.copy(alpha = 0.6f)
    }

    Surface(
        color = containerColor,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (isVerified) Icons.Filled.Verified else Icons.Filled.WarningAmber,
                    contentDescription = if (isVerified) "License verified" else "Copyright warning",
                    tint = if (isVerified) StatusSuccess else StatusError
                )
                Text(
                    text = info.message,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!info.thumbnailUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = info.thumbnailUrl,
                        contentDescription = info.title ?: "YouTube video thumbnail",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .width(96.dp)
                            .height(54.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(StudioCardElevated)
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = info.title ?: "YouTube Video (${info.videoId})",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!info.channelTitle.isNullOrBlank()) {
                        Text(
                            text = "Channel: ${info.channelTitle}",
                            style = MaterialTheme.typography.bodySmall,
                            color = AccentLavender,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    StatusPill(
                        label = info.sourceLabel,
                        color = accentColor
                    )
                }
            }

            Text(
                text = info.detailNote,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}

@Composable
fun LocalVideoPickerScreen(
    editorState: ActiveEditorState,
    onVideoPicked: (Uri, Boolean) -> Unit,
    onDismissError: () -> Unit,
    onOpenAnalysis: () -> Unit,
    onOpenOneClick: () -> Unit,
    onOpenEditor: () -> Unit,
    onOpenSubtitles: () -> Unit,
    onOpenWatermark: () -> Unit,
    onBack: () -> Unit
) {
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) onVideoPicked(uri, false)
    }

    val safDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) onVideoPicked(uri, true)
    }

    Scaffold(
        containerColor = StudioBackground,
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            StudioTopBar(
                title = "Import Local Video",
                subtitle = "Zero-permission Photo Picker & Storage Access Framework",
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
                ErrorBanner(message = editorState.errorMessage, onDismiss = onDismissError)
            }

            item {
                StudioCard {
                    Text(
                        text = "Select Video From Device",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "ClipForge AI uses Android's privacy-safe Photo Picker and Storage Access Framework without requesting broad access to all files.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Spacer(Modifier.height(14.dp))

                    Button(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("launch_photo_picker_video")
                    ) {
                        Icon(Icons.Filled.PhotoLibrary, contentDescription = null)
                        Spacer(Modifier.width(10.dp))
                        Text("Pick from Gallery (Photo Picker)")
                    }

                    Spacer(Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = { safDocumentLauncher.launch(arrayOf("video/*")) },
                        border = BorderStroke(1.dp, StudioBorder),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("launch_saf_video_picker")
                    ) {
                        Icon(Icons.Filled.FolderOpen, contentDescription = null, tint = AccentLavender)
                        Spacer(Modifier.width(10.dp))
                        Text("Browse Files (Storage Access Framework)", color = TextPrimary)
                    }
                }
            }

            if (editorState.isLoadingVideo) {
                item {
                    StudioCard {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(color = PrimaryPurple, modifier = Modifier.size(24.dp))
                            Text("Inspecting video codec, resolution & duration...", color = TextSecondary)
                        }
                    }
                }
            }

            if (editorState.metadata != null) {
                item {
                    VideoMetadataSummaryCard(
                        metadata = editorState.metadata,
                        primaryActionLabel = "Analyze With Gemini AI",
                        onPrimaryAction = onOpenAnalysis
                    )
                }

                item {
                    StudioCard {
                        Text("Quick Workflow Actions", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = onOpenOneClick,
                                modifier = Modifier.weight(1f).testTag("picker_one_click_btn")
                            ) {
                                Text("One-Click 9:16")
                            }
                            OutlinedButton(
                                onClick = onOpenEditor,
                                modifier = Modifier.weight(1f).testTag("picker_editor_btn")
                            ) {
                                Text("Full Editor")
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = onOpenSubtitles,
                                modifier = Modifier.weight(1f).testTag("picker_subtitles_btn")
                            ) {
                                Text("Subtitles")
                            }
                            OutlinedButton(
                                onClick = onOpenWatermark,
                                modifier = Modifier.weight(1f).testTag("picker_watermark_btn")
                            ) {
                                Text("Watermark")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VideoAnalysisScreen(
    editorState: ActiveEditorState,
    aiState: AiAnalysisState,
    onSelectDuration: (Int) -> Unit,
    onAnalyzeClick: () -> Unit,
    onViewClipResults: () -> Unit,
    onPickVideo: () -> Unit,
    onBack: () -> Unit
) {
    val meta = editorState.metadata

    Scaffold(
        containerColor = StudioBackground,
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            StudioTopBar(
                title = "AI Video Analysis",
                subtitle = "Gemini multimodal moment detection",
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
            if (meta == null) {
                item {
                    EmptyStateCard(
                        title = "No Video Selected",
                        description = "Import a local video or paste a direct video URL first to analyze highlights.",
                        actionLabel = "Select Video",
                        onAction = onPickVideo
                    )
                }
            } else {
                item {
                    VideoPlayerStage(
                        uriString = meta.uriString,
                        showVerticalStage = false
                    )
                }

                item {
                    StudioCard {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Filled.PrivacyTip, contentDescription = null, tint = AccentLavender, modifier = Modifier.size(18.dp))
                            Text(
                                text = "Privacy Notice: Audio segments and sampled keyframes are processed securely via Firebase AI Logic to detect moments and are not stored permanently.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        Spacer(Modifier.height(14.dp))
                        Text("Preferred Short Length", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(15, 30, 45).forEach { sec ->
                                FilterChip(
                                    selected = editorState.targetClipDurationSec == sec,
                                    onClick = { onSelectDuration(sec) },
                                    label = { Text("${sec}s") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = PrimaryPurple,
                                        selectedLabelColor = TextPrimary
                                    )
                                )
                            }
                        }
                        Spacer(Modifier.height(14.dp))

                        Button(
                            onClick = onAnalyzeClick,
                            enabled = aiState !is AiAnalysisState.Analyzing,
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("analyze_video_button")
                        ) {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(if (aiState is AiAnalysisState.Analyzing) "Analyzing..." else "Analyze Video")
                        }
                    }
                }

                when (aiState) {
                    is AiAnalysisState.Analyzing -> {
                        item {
                            StudioCard(highlighted = true) {
                                Text(
                                    text = aiState.stage,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = AccentLavender
                                )
                                Spacer(Modifier.height(10.dp))
                                LinearProgressIndicator(
                                    progress = { aiState.progressFraction.coerceIn(0.05f, 1f) },
                                    color = PrimaryPurple,
                                    trackColor = StudioCardElevated,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(999.dp))
                                )
                            }
                        }
                    }

                    is AiAnalysisState.Error -> {
                        item {
                            ErrorBanner(message = aiState.message)
                        }
                    }

                    is AiAnalysisState.Success -> {
                        item {
                            StudioCard(highlighted = true) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        StatusPill(label = "${aiState.clips.size} Moments Found", color = StatusSuccess)
                                        Spacer(Modifier.height(6.dp))
                                        Text(
                                            text = "AI highlights validated against source timestamps.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary
                                        )
                                    }
                                    Button(
                                        onClick = onViewClipResults,
                                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                                        modifier = Modifier.testTag("view_clips_button")
                                    ) {
                                        Text("View Clips")
                                    }
                                }
                            }
                        }
                    }

                    AiAnalysisState.Idle -> Unit
                }
            }
        }
    }
}

@Composable
fun ClipSelectionScreen(
    editorState: ActiveEditorState,
    onToggleSelect: (String) -> Unit,
    onSelectAll: (Boolean) -> Unit,
    onPreviewClip: (SuggestedClip) -> Unit,
    onEditClip: (SuggestedClip) -> Unit,
    onGenerateOne: (SuggestedClip) -> Unit,
    onGenerateSelected: (List<SuggestedClip>) -> Unit,
    onGenerateAll: (List<SuggestedClip>) -> Unit,
    onRunAnalysis: () -> Unit,
    onBack: () -> Unit
) {
    val clips = editorState.suggestedClips
    val selectedClips = clips.filter { it.isSelected }

    Scaffold(
        containerColor = StudioBackground,
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            StudioTopBar(
                title = "Suggested AI Clips",
                subtitle = "${clips.size} highlights • ${selectedClips.size} selected",
                onBack = onBack
            )
        }
    ) { padding ->
        if (clips.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                EmptyStateCard(
                    title = "No Clips Suggested Yet",
                    description = "Run Gemini AI video analysis to discover engaging moments automatically.",
                    icon = Icons.Filled.ContentCut,
                    actionLabel = "Go to Video Analysis",
                    onAction = onRunAnalysis
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .navigationBarsPadding(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    StudioCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { onSelectAll(true) }) {
                                    Text("Select All")
                                }
                                OutlinedButton(onClick = { onSelectAll(false) }) {
                                    Text("Clear")
                                }
                            }
                            Text(
                                text = "${selectedClips.size}/${clips.size}",
                                style = MaterialTheme.typography.labelMedium,
                                color = AccentLavender
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { onGenerateSelected(selectedClips) },
                                enabled = selectedClips.isNotEmpty(),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("generate_selected_clips_btn")
                            ) {
                                Text("Generate Selected")
                            }
                            OutlinedButton(
                                onClick = { onGenerateAll(clips) },
                                border = BorderStroke(1.dp, AccentLavender),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("generate_all_clips_btn")
                            ) {
                                Text("Generate All", color = AccentLavender)
                            }
                        }
                    }
                }

                items(clips, key = { it.id }) { clip ->
                    StudioCard(highlighted = clip.isSelected) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            // Thumbnail
                            Box(
                                modifier = Modifier
                                    .width(78.dp)
                                    .height(104.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(StudioCardElevated)
                                    .clickable { onPreviewClip(clip) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (!clip.thumbnailPath.isNullOrBlank() && File(clip.thumbnailPath).exists()) {
                                    AsyncImage(
                                        model = File(clip.thumbnailPath),
                                        contentDescription = clip.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Icon(Icons.Filled.Movie, contentDescription = null, tint = TextSecondary)
                                }
                                Surface(
                                    color = StudioBackground.copy(alpha = 0.72f),
                                    shape = RoundedCornerShape(999.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.PlayArrow,
                                        contentDescription = "Preview clip",
                                        tint = TextPrimary,
                                        modifier = Modifier.padding(4.dp).size(20.dp)
                                    )
                                }
                            }

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = clip.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextPrimary,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Checkbox(
                                        checked = clip.isSelected,
                                        onCheckedChange = { onToggleSelect(clip.id) },
                                        colors = CheckboxDefaults.colors(checkedColor = PrimaryPurple)
                                    )
                                }
                                Text(
                                    text = "${formatTimestampMs(clip.startMs)} - ${formatTimestampMs(clip.endMs)}  (${clip.durationSec.toInt()}s)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = AccentLavender
                                )
                                Text(
                                    text = clip.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextPrimary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Why it works: ${clip.reason}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = StatusSuccess,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onPreviewClip(clip) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Preview")
                            }
                            OutlinedButton(
                                onClick = { onEditClip(clip) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Edit")
                            }
                            Button(
                                onClick = { onGenerateOne(clip) },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Export")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VideoMetadataSummaryCard(
    metadata: VideoMetadata,
    primaryActionLabel: String,
    onPrimaryAction: () -> Unit
) {
    StudioCard(highlighted = true) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(StudioCardElevated),
                contentAlignment = Alignment.Center
            ) {
                if (!metadata.thumbnailPath.isNullOrBlank() && File(metadata.thumbnailPath).exists()) {
                    AsyncImage(
                        model = File(metadata.thumbnailPath),
                        contentDescription = metadata.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(Icons.Filled.Movie, contentDescription = null, tint = AccentLavender)
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = StatusSuccess, modifier = Modifier.size(16.dp))
                    Text("Source Video Loaded", style = MaterialTheme.typography.labelSmall, color = StatusSuccess)
                }
                Text(
                    text = metadata.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${metadata.effectiveWidth}x${metadata.effectiveHeight} (${metadata.aspectRatioLabel}) • ${formatTimestampMs(metadata.durationMs)} • ${metadata.fps.toInt()} fps",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                if (metadata.fileSizeBytes > 0L) {
                    Text(
                        text = "Size: ${formatFileSize(metadata.fileSizeBytes)} • Audio: ${if (metadata.hasAudio) "Yes" else "Silent"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Button(
            onClick = onPrimaryAction,
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("metadata_primary_action_button")
        ) {
            Text(primaryActionLabel, fontWeight = FontWeight.SemiBold)
        }
    }
}
