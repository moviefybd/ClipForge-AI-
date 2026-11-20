package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.BrandingWatermark
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.media.OverlayRenderer
import com.example.model.CropMode
import com.example.model.SubtitleAlign
import com.example.model.SubtitleCue
import com.example.model.SubtitleFontFamily
import com.example.model.SubtitleGenState
import com.example.model.SubtitleLanguage
import com.example.model.SubtitlePosition
import com.example.model.SubtitleStyle
import com.example.model.WatermarkConfig
import com.example.model.WatermarkPosition
import com.example.model.WatermarkType
import com.example.model.WatermarkWeight
import com.example.model.formatTimestampMs
import com.example.model.formatTimestampWithMillis
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
fun VideoEditorScreen(
    editorState: ActiveEditorState,
    onUpdateTrimRange: (Long, Long) -> Unit,
    onSelectCropMode: (CropMode) -> Unit,
    onUpdateManualCrop: (Float, Float, Float) -> Unit,
    onToggleMuteAudio: (Boolean) -> Unit,
    onOpenSubtitles: () -> Unit,
    onOpenWatermark: () -> Unit,
    onSaveProject: () -> Unit,
    onExportClick: () -> Unit,
    onPickVideo: () -> Unit,
    onBack: () -> Unit
) {
    val meta = editorState.metadata

    Scaffold(
        containerColor = StudioBackground,
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            StudioTopBar(
                title = "Video Editor",
                subtitle = editorState.projectName,
                onBack = onBack,
                actions = {
                    if (meta != null) {
                        IconButton(
                            onClick = onSaveProject,
                            modifier = Modifier.testTag("editor_save_project_btn")
                        ) {
                            Icon(Icons.Filled.Save, contentDescription = "Save Project", tint = AccentLavender)
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (meta == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                EmptyStateCard(
                    title = "No Video Loaded",
                    description = "Pick a local video or load a direct URL to open the 9:16 Video Editor.",
                    actionLabel = "Select Video",
                    onAction = onPickVideo
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .navigationBarsPadding(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (!editorState.statusMessage.isNullOrBlank()) {
                    item {
                        StatusPill(label = editorState.statusMessage, color = StatusSuccess)
                    }
                }

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

                // Trim Range Card
                item {
                    StudioCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Trim Range", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            Text(
                                text = "${formatTimestampMs(editorState.clipStartMs)} - ${formatTimestampMs(editorState.clipEndMs)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = AccentLavender
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        val durFloat = meta.durationMs.toFloat().coerceAtLeast(2000f)
                        RangeSlider(
                            value = editorState.clipStartMs.toFloat()..editorState.clipEndMs.toFloat()
                                .coerceIn(editorState.clipStartMs.toFloat() + 1000f, durFloat),
                            onValueChange = { range ->
                                onUpdateTrimRange(range.start.toLong(), range.endInclusive.toLong())
                            },
                            valueRange = 0f..durFloat,
                            colors = SliderDefaults.colors(
                                thumbColor = PrimaryPurple,
                                activeTrackColor = PrimaryPurple
                            )
                        )
                    }
                }

                // Crop Modes Card
                item {
                    StudioCard {
                        Text("9:16 Aspect & Crop Mode", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
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
                                    modifier = Modifier.testTag("editor_crop_${mode.name}")
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = editorState.cropSettings.mode.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )

                        if (editorState.cropSettings.mode == CropMode.CENTER_CROP ||
                            editorState.cropSettings.mode == CropMode.SMART_CROP
                        ) {
                            Spacer(Modifier.height(12.dp))
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

                // Studio Overlays & Audio
                item {
                    StudioCard {
                        Text("Overlays & Audio", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = onOpenSubtitles,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("editor_open_subtitles_btn")
                            ) {
                                Icon(Icons.Filled.ClosedCaption, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Subtitles (${editorState.subtitleCues.size})")
                            }
                            OutlinedButton(
                                onClick = onOpenWatermark,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("editor_open_watermark_btn")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.BrandingWatermark, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(if (editorState.watermarkConfig.enabled) "Watermark On" else "Watermark Off")
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Preserve Original Audio", style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                                Text("AAC audio track encoded in MP4", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }
                            Switch(
                                checked = !editorState.muteAudio,
                                onCheckedChange = { onToggleMuteAudio(!it) },
                                colors = SwitchDefaults.colors(checkedTrackColor = PrimaryPurple)
                            )
                        }
                    }
                }

                // Export CTA
                item {
                    Button(
                        onClick = onExportClick,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("editor_export_button")
                    ) {
                        Text("Export 1080×1920 Short MP4", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SubtitleEditorScreen(
    editorState: ActiveEditorState,
    subtitleGenState: SubtitleGenState,
    defaultLanguage: SubtitleLanguage,
    onToggleSubtitles: (Boolean) -> Unit,
    onGenerateSubtitles: (SubtitleLanguage) -> Unit,
    onAddCue: (Long, Long, String) -> Unit,
    onUpdateCue: (String, Long, Long, String) -> Unit,
    onDeleteCue: (String) -> Unit,
    onUpdateStyle: ((SubtitleStyle) -> SubtitleStyle) -> Unit,
    onImportSrt: (Uri) -> Unit,
    onExportSrt: (Uri) -> Unit,
    onPickVideo: () -> Unit,
    onExportVideo: () -> Unit,
    onBack: () -> Unit
) {
    var selectedLanguage by remember(defaultLanguage) { mutableStateOf(defaultLanguage) }
    var currentPlaybackPosMs by remember { mutableLongStateOf(0L) }
    var editingCue by remember { mutableStateOf<SubtitleCue?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    val importSrtLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) onImportSrt(uri)
    }

    val exportSrtLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/x-subrip")
    ) { uri ->
        if (uri != null) onExportSrt(uri)
    }

    Scaffold(
        containerColor = StudioBackground,
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            StudioTopBar(
                title = "Subtitle Studio",
                subtitle = "Bengali & English captions • Burn-in & SRT",
                onBack = onBack
            )
        }
    ) { padding ->
        if (editorState.metadata == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                EmptyStateCard(
                    title = "Select a Video for Subtitles",
                    description = "Import a video first to generate automatic Bengali or English captions and preview them live.",
                    icon = Icons.Filled.ClosedCaption,
                    actionLabel = "Select Video",
                    onAction = onPickVideo
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .navigationBarsPadding(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    VideoPlayerStage(
                        uriString = editorState.metadata.uriString,
                        startMs = editorState.clipStartMs,
                        endMs = editorState.clipEndMs,
                        cropSettings = editorState.cropSettings,
                        showVerticalStage = true,
                        subtitlesEnabled = editorState.subtitlesEnabled,
                        subtitleCues = editorState.subtitleCues,
                        subtitleStyle = editorState.subtitleStyle,
                        watermarkConfig = editorState.watermarkConfig,
                        onPositionChanged = { currentPlaybackPosMs = it }
                    )
                }

                // Enable / Burn toggle + AI Transcription
                item {
                    StudioCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Enable & Burn Subtitles", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                Text("Preview live & burn captions into exported MP4", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }
                            Switch(
                                checked = editorState.subtitlesEnabled,
                                onCheckedChange = onToggleSubtitles,
                                colors = SwitchDefaults.colors(checkedTrackColor = PrimaryPurple),
                                modifier = Modifier.testTag("subtitles_enable_switch")
                            )
                        }

                        Spacer(Modifier.height(14.dp))
                        Text("Automatic AI Transcription Language", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                        Spacer(Modifier.height(8.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SubtitleLanguage.entries.forEach { lang ->
                                FilterChip(
                                    selected = selectedLanguage == lang,
                                    onClick = { selectedLanguage = lang },
                                    label = { Text(lang.displayName) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = PrimaryPurple,
                                        selectedLabelColor = TextPrimary
                                    ),
                                    modifier = Modifier.testTag("sub_lang_${lang.name}")
                                )
                            }
                        }

                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { onGenerateSubtitles(selectedLanguage) },
                            enabled = subtitleGenState !is SubtitleGenState.Generating,
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("generate_auto_subtitles_btn")
                        ) {
                            if (subtitleGenState is SubtitleGenState.Generating) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                                Text(subtitleGenState.stage)
                            } else {
                                Icon(Icons.Filled.AutoAwesome, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Generate Automatic Subtitles")
                            }
                        }

                        if (subtitleGenState is SubtitleGenState.Error) {
                            Spacer(Modifier.height(10.dp))
                            ErrorBanner(message = subtitleGenState.message)
                        }

                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { importSrtLauncher.launch(arrayOf("*/*")) },
                                modifier = Modifier.weight(1f).testTag("import_srt_btn")
                            ) {
                                Icon(Icons.Filled.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Import SRT")
                            }
                            OutlinedButton(
                                onClick = {
                                    val base = editorState.projectName.replace(" ", "_")
                                    exportSrtLauncher.launch("$base.srt")
                                },
                                enabled = editorState.subtitleCues.isNotEmpty(),
                                modifier = Modifier.weight(1f).testTag("export_srt_btn")
                            ) {
                                Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Export SRT")
                            }
                        }
                    }
                }

                // Style Settings Card
                item {
                    val style = editorState.subtitleStyle
                    StudioCard {
                        Text("Caption Style & Timing", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Spacer(Modifier.height(10.dp))

                        Text("Font Size: ${style.fontSizeSp.toInt()}sp", style = MaterialTheme.typography.labelSmall, color = AccentLavender)
                        Slider(
                            value = style.fontSizeSp,
                            onValueChange = { sz -> onUpdateStyle { it.copy(fontSizeSp = sz) } },
                            valueRange = 14f..36f
                        )

                        Text("Font Family", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Spacer(Modifier.height(6.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SubtitleFontFamily.entries.forEach { fam ->
                                FilterChip(
                                    selected = style.fontFamily == fam,
                                    onClick = { onUpdateStyle { it.copy(fontFamily = fam) } },
                                    label = { Text(fam.displayName) }
                                )
                            }
                        }

                        Spacer(Modifier.height(10.dp))
                        Text("Position & Alignment", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Spacer(Modifier.height(6.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SubtitlePosition.entries.forEach { pos ->
                                FilterChip(
                                    selected = style.position == pos,
                                    onClick = { onUpdateStyle { it.copy(position = pos) } },
                                    label = { Text(pos.displayName) }
                                )
                            }
                            SubtitleAlign.entries.forEach { al ->
                                FilterChip(
                                    selected = style.alignment == al,
                                    onClick = { onUpdateStyle { it.copy(alignment = al) } },
                                    label = { Text(al.displayName) }
                                )
                            }
                        }

                        Spacer(Modifier.height(10.dp))
                        Text("Text Color", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Spacer(Modifier.height(6.dp))
                        ColorSwatchRow(
                            selectedHex = style.textColorHex,
                            swatches = listOf("#FFFFFF", "#FFE14D", "#35C98A", "#9B83FF", "#FF5C70"),
                            onSelect = { hex -> onUpdateStyle { it.copy(textColorHex = hex) } }
                        )

                        Spacer(Modifier.height(10.dp))
                        Text("Background Color & Opacity (${(style.backgroundAlpha * 100).toInt()}%)", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Spacer(Modifier.height(6.dp))
                        ColorSwatchRow(
                            selectedHex = style.backgroundColorHex,
                            swatches = listOf("#10121A", "#000000", "#7957FF", "#191D29", "#222738"),
                            onSelect = { hex -> onUpdateStyle { it.copy(backgroundColorHex = hex) } }
                        )
                        Slider(
                            value = style.backgroundAlpha,
                            onValueChange = { a -> onUpdateStyle { it.copy(backgroundAlpha = a) } },
                            valueRange = 0f..1f
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Text Outline", color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                            Switch(
                                checked = style.outlineEnabled,
                                onCheckedChange = { on -> onUpdateStyle { it.copy(outlineEnabled = on) } }
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Drop Shadow", color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                            Switch(
                                checked = style.shadowEnabled,
                                onCheckedChange = { on -> onUpdateStyle { it.copy(shadowEnabled = on) } }
                            )
                        }

                        Text(
                            text = "Global Timing Shift: ${style.timingOffsetMs} ms",
                            style = MaterialTheme.typography.labelSmall,
                            color = AccentLavender
                        )
                        Slider(
                            value = style.timingOffsetMs.toFloat(),
                            onValueChange = { ms -> onUpdateStyle { it.copy(timingOffsetMs = ms.toLong()) } },
                            valueRange = -2000f..2000f
                        )
                    }
                }

                // Cue List Header + Manual Entry
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Subtitle Cues (${editorState.subtitleCues.size})",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Button(
                            onClick = { showAddDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                            modifier = Modifier.testTag("add_manual_subtitle_btn")
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Add Cue")
                        }
                    }
                }

                items(editorState.subtitleCues, key = { it.id }) { cue ->
                    StudioCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${formatTimestampWithMillis(cue.startMs)} → ${formatTimestampWithMillis(cue.endMs)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = AccentLavender
                            )
                            Row {
                                IconButton(onClick = { editingCue = cue }) {
                                    Icon(Icons.Filled.Edit, contentDescription = "Edit cue", tint = TextSecondary)
                                }
                                IconButton(onClick = { onDeleteCue(cue.id) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete cue", tint = TextSecondary)
                                }
                            }
                        }
                        Text(
                            text = cue.text,
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextPrimary
                        )
                    }
                }

                item {
                    Button(
                        onClick = onExportVideo,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("subtitle_export_video_btn")
                    ) {
                        Text("Export Video With Subtitles")
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        SubtitleCueEditDialog(
            initialStartMs = currentPlaybackPosMs,
            initialEndMs = (currentPlaybackPosMs + 2500L),
            initialText = "",
            title = "Add Subtitle Cue",
            onDismiss = { showAddDialog = false },
            onSave = { s, e, txt ->
                onAddCue(s, e, txt)
                showAddDialog = false
            }
        )
    }

    editingCue?.let { cue ->
        SubtitleCueEditDialog(
            initialStartMs = cue.startMs,
            initialEndMs = cue.endMs,
            initialText = cue.text,
            title = "Edit Subtitle Cue",
            onDismiss = { editingCue = null },
            onSave = { s, e, txt ->
                onUpdateCue(cue.id, s, e, txt)
                editingCue = null
            }
        )
    }
}

@Composable
private fun SubtitleCueEditDialog(
    initialStartMs: Long,
    initialEndMs: Long,
    initialText: String,
    title: String,
    onDismiss: () -> Unit,
    onSave: (Long, Long, String) -> Unit
) {
    var text by remember { mutableStateOf(initialText) }
    var startSec by remember { mutableStateOf(String.format("%.2f", initialStartMs / 1000.0)) }
    var endSec by remember { mutableStateOf(String.format("%.2f", initialEndMs / 1000.0)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = StudioCardElevated,
        title = { Text(title, color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Caption Text (English or বাংলা)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = startSec,
                        onValueChange = { startSec = it },
                        label = { Text("Start (sec)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = endSec,
                        onValueChange = { endSec = it },
                        label = { Text("End (sec)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val sMs = ((startSec.toDoubleOrNull() ?: (initialStartMs / 1000.0)) * 1000.0).toLong()
                val eMs = ((endSec.toDoubleOrNull() ?: (initialEndMs / 1000.0)) * 1000.0).toLong()
                onSave(sMs, eMs, text)
            }) {
                Text("Save", color = AccentLavender)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WatermarkEditorScreen(
    editorState: ActiveEditorState,
    onUpdateWatermark: ((WatermarkConfig) -> WatermarkConfig) -> Unit,
    onPickVideo: () -> Unit,
    onExportVideo: () -> Unit,
    onBack: () -> Unit
) {
    val wm = editorState.watermarkConfig
    val pngPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onUpdateWatermark {
                it.copy(
                    enabled = true,
                    type = WatermarkType.IMAGE,
                    imageUriString = uri.toString()
                )
            }
        }
    }

    Scaffold(
        containerColor = StudioBackground,
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            StudioTopBar(
                title = "Watermark Studio",
                subtitle = "Brand text or PNG overlay burned into MP4",
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
            if (editorState.metadata != null) {
                item {
                    VideoPlayerStage(
                        uriString = editorState.metadata.uriString,
                        startMs = editorState.clipStartMs,
                        endMs = editorState.clipEndMs,
                        cropSettings = editorState.cropSettings,
                        showVerticalStage = true,
                        subtitlesEnabled = editorState.subtitlesEnabled,
                        subtitleCues = editorState.subtitleCues,
                        subtitleStyle = editorState.subtitleStyle,
                        watermarkConfig = wm
                    )
                }
            } else {
                item {
                    StudioCard {
                        Text(
                            text = "Configure your watermark preset now, or pick a video to preview it live.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Spacer(Modifier.height(10.dp))
                        OutlinedButton(onClick = onPickVideo) {
                            Text("Select Video for Live Preview")
                        }
                    }
                }
            }

            item {
                StudioCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Enable Watermark", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            Text("Default brand mark: ClipForge AI", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                        Switch(
                            checked = wm.enabled,
                            onCheckedChange = { on -> onUpdateWatermark { it.copy(enabled = on) } },
                            colors = SwitchDefaults.colors(checkedTrackColor = PrimaryPurple),
                            modifier = Modifier.testTag("watermark_enable_switch")
                        )
                    }

                    Spacer(Modifier.height(12.dp))
                    Text("Watermark Type", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        WatermarkType.entries.forEach { t ->
                            FilterChip(
                                selected = wm.type == t,
                                onClick = { onUpdateWatermark { it.copy(type = t) } },
                                label = { Text(t.displayName) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryPurple,
                                    selectedLabelColor = TextPrimary
                                ),
                                modifier = Modifier.testTag("wm_type_${t.name}")
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    if (wm.type == WatermarkType.TEXT) {
                        OutlinedTextField(
                            value = wm.text,
                            onValueChange = { txt -> onUpdateWatermark { it.copy(text = txt) } },
                            label = { Text("Watermark Text") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("watermark_text_input")
                        )
                        Spacer(Modifier.height(10.dp))
                        Text("Font Size: ${wm.fontSizeSp.toInt()}sp", style = MaterialTheme.typography.labelSmall, color = AccentLavender)
                        Slider(
                            value = wm.fontSizeSp,
                            onValueChange = { sz -> onUpdateWatermark { it.copy(fontSizeSp = sz) } },
                            valueRange = 12f..36f
                        )
                        Text("Font Weight", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            WatermarkWeight.entries.forEach { w ->
                                FilterChip(
                                    selected = wm.fontWeight == w,
                                    onClick = { onUpdateWatermark { it.copy(fontWeight = w) } },
                                    label = { Text(w.displayName) }
                                )
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Text("Text Color", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Spacer(Modifier.height(6.dp))
                        ColorSwatchRow(
                            selectedHex = wm.textColorHex,
                            swatches = listOf("#FFFFFF", "#9B83FF", "#35C98A", "#FFE14D", "#FF5C70"),
                            onSelect = { hex -> onUpdateWatermark { it.copy(textColorHex = hex) } }
                        )
                    } else {
                        Button(
                            onClick = {
                                pngPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("pick_png_watermark_btn")
                        ) {
                            Icon(Icons.Filled.Image, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(if (wm.imageUriString != null) "Change PNG Watermark" else "Select PNG Image Watermark")
                        }
                        Spacer(Modifier.height(10.dp))
                        Text("Image Size: ${wm.imageSizeDp.toInt()}dp", style = MaterialTheme.typography.labelSmall, color = AccentLavender)
                        Slider(
                            value = wm.imageSizeDp,
                            onValueChange = { sz -> onUpdateWatermark { it.copy(imageSizeDp = sz) } },
                            valueRange = 36f..140f
                        )
                    }

                    Spacer(Modifier.height(12.dp))
                    Text("Position", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Spacer(Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        WatermarkPosition.entries.forEach { pos ->
                            FilterChip(
                                selected = wm.position == pos,
                                onClick = { onUpdateWatermark { it.copy(position = pos) } },
                                label = { Text(pos.displayName) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryPurple,
                                    selectedLabelColor = TextPrimary
                                ),
                                modifier = Modifier.testTag("wm_pos_${pos.name}")
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    Text("Opacity: ${(wm.opacity * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = AccentLavender)
                    Slider(
                        value = wm.opacity,
                        onValueChange = { op -> onUpdateWatermark { it.copy(opacity = op) } },
                        valueRange = 0.1f..1f
                    )

                    Text("Edge Padding: ${wm.paddingDp.toInt()}dp", style = MaterialTheme.typography.labelSmall, color = AccentLavender)
                    Slider(
                        value = wm.paddingDp,
                        onValueChange = { pad -> onUpdateWatermark { it.copy(paddingDp = pad) } },
                        valueRange = 6f..40f
                    )
                }
            }

            if (editorState.metadata != null) {
                item {
                    Button(
                        onClick = onExportVideo,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("watermark_export_video_btn")
                    ) {
                        Text("Export Video With Watermark")
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorSwatchRow(
    selectedHex: String,
    swatches: List<String>,
    onSelect: (String) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        swatches.forEach { hex ->
            val color = Color(OverlayRenderer.parseColorSafe(hex))
            val isSelected = selectedHex.equals(hex, ignoreCase = true)
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(
                        width = if (isSelected) 3.dp else 1.dp,
                        color = if (isSelected) PrimaryPurple else StudioBorder,
                        shape = CircleShape
                    )
                    .clickable { onSelect(hex) }
            )
        }
    }
}
