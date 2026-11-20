package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.BuildConfig
import com.example.R
import com.example.data.local.ExportRecordEntity
import com.example.data.local.ProjectEntity
import com.example.media.MediaStoreExporter
import com.example.model.AppSettings
import com.example.model.CropMode
import com.example.model.SubtitleLanguage
import com.example.model.formatFileSize
import com.example.model.formatTimestampMs
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.StatusPill
import com.example.ui.components.StudioCard
import com.example.ui.components.StudioTopBar
import com.example.ui.theme.AccentLavender
import com.example.ui.theme.PrimaryPurple
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.StudioBackground
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCardElevated
import com.example.ui.theme.StudioSecondaryBg
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.io.File
import java.text.DateFormat
import java.util.Date

@Composable
fun MyProjectsScreen(
    projects: List<ProjectEntity>,
    isUriAccessible: (String?) -> Boolean,
    onOpenProject: (ProjectEntity) -> Unit,
    onRenameProject: (Long, String) -> Unit,
    onDeleteProject: (Long) -> Unit,
    onPreviewVideo: (String, String) -> Unit,
    onCreateNewProject: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var renamingTarget by remember { mutableStateOf<ProjectEntity?>(null) }

    Scaffold(
        containerColor = StudioBackground,
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            StudioTopBar(
                title = "My Projects",
                subtitle = "${projects.size} saved local project(s)",
                onBack = onBack
            )
        }
    ) { padding ->
        if (projects.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                EmptyStateCard(
                    title = "No Saved Projects",
                    description = "Imported videos, AI clip selections, subtitles, and watermark settings are saved locally in Room.",
                    icon = Icons.Filled.Folder,
                    actionLabel = "Start New Project",
                    onAction = onCreateNewProject
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
                items(projects, key = { it.id }) { project ->
                    val sourceExists = remember(project.sourceUri) { isUriAccessible(project.sourceUri) }
                    val exportExists = remember(project.latestExportUri) { isUriAccessible(project.latestExportUri) }

                    StudioCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(StudioCardElevated),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!project.thumbnailPath.isNullOrBlank() && File(project.thumbnailPath).exists()) {
                                    AsyncImage(
                                        model = File(project.thumbnailPath),
                                        contentDescription = project.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Icon(Icons.Filled.Movie, contentDescription = null, tint = AccentLavender)
                                }
                            }

                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = project.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${project.sourceWidth}×${project.sourceHeight} • ${formatTimestampMs(project.sourceDurationMs)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                                Text(
                                    text = "Updated ${DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(project.updatedAt))}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                                if (!sourceExists) {
                                    StatusPill(label = "Source Media Missing", color = StatusWarning)
                                }
                            }

                            Row {
                                IconButton(onClick = { renamingTarget = project }) {
                                    Icon(Icons.Filled.Edit, contentDescription = "Rename project", tint = TextSecondary)
                                }
                                IconButton(onClick = { onDeleteProject(project.id) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete project", tint = StatusError)
                                }
                            }
                        }

                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = { onOpenProject(project) },
                                enabled = sourceExists,
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Open")
                            }
                            if (exportExists && project.latestExportUri != null) {
                                OutlinedButton(
                                    onClick = { onPreviewVideo(project.latestExportUri, project.name) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Preview")
                                }
                                OutlinedButton(
                                    onClick = {
                                        val share = MediaStoreExporter.buildShareIntent(
                                            context,
                                            project.latestExportUri,
                                            project.name
                                        )
                                        context.startActivity(share)
                                    }
                                ) {
                                    Icon(Icons.Filled.Share, contentDescription = "Share export", modifier = Modifier.size(16.dp))
                                }
                            } else if (sourceExists) {
                                OutlinedButton(
                                    onClick = { onPreviewVideo(project.sourceUri, project.name) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Preview")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    renamingTarget?.let { target ->
        var newName by remember(target.id) { mutableStateOf(target.name) }
        AlertDialog(
            onDismissRequest = { renamingTarget = null },
            containerColor = StudioCardElevated,
            title = { Text("Rename Project", color = TextPrimary) },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Project Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onRenameProject(target.id, newName)
                    renamingTarget = null
                }) {
                    Text("Save", color = AccentLavender)
                }
            },
            dismissButton = {
                TextButton(onClick = { renamingTarget = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
fun ExportHistoryScreen(
    exports: List<ExportRecordEntity>,
    isUriAccessible: (String?) -> Boolean,
    onPreviewExport: (ExportRecordEntity) -> Unit,
    onDeleteExport: (Long) -> Unit,
    onStartOneClick: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    Scaffold(
        containerColor = StudioBackground,
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            StudioTopBar(
                title = "Export History",
                subtitle = "Completed 9:16 MP4 files in Movies/ClipForge",
                onBack = onBack
            )
        }
    ) { padding ->
        if (exports.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                EmptyStateCard(
                    title = "No Completed Exports Yet",
                    description = "Exported 1080×1920 MP4 Shorts saved to Movies/ClipForge will appear here.",
                    icon = Icons.Filled.Download,
                    actionLabel = "Convert a Video Now",
                    onAction = onStartOneClick
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
                items(exports, key = { it.id }) { record ->
                    val fileAvailable = remember(record.outputUri) { isUriAccessible(record.outputUri) }
                    StudioCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(record.title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                Text(
                                    text = "${record.width}×${record.height} • ${formatTimestampMs(record.durationMs)} • ${formatFileSize(record.fileSizeBytes)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = AccentLavender
                                )
                                Text(
                                    text = "${record.cropMode} • Subtitles: ${if (record.burnedSubtitles) "Burned" else "Off"} • Watermark: ${if (record.burnedWatermark) "On" else "Off"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                                Text(
                                    text = record.filePath,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (!fileAvailable) {
                                    StatusPill(label = "File Deleted From Device", color = StatusWarning)
                                }
                            }
                            IconButton(onClick = { onDeleteExport(record.id) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Remove record", tint = TextSecondary)
                            }
                        }

                        if (fileAvailable) {
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                Button(
                                    onClick = { onPreviewExport(record) },
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Preview")
                                }
                                OutlinedButton(
                                    onClick = {
                                        val share = MediaStoreExporter.buildShareIntent(
                                            context,
                                            record.outputUri,
                                            record.title
                                        )
                                        context.startActivity(share)
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Share")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    cacheSizeBytes: Long,
    onUpdateClipDuration: (Int) -> Unit,
    onUpdateCropMode: (CropMode) -> Unit,
    onUpdateSubtitleLanguage: (SubtitleLanguage) -> Unit,
    onUpdateWatermarkText: (String) -> Unit,
    onUpdateNotify: (Boolean) -> Unit,
    onUpdateAutoCleanup: (Boolean) -> Unit,
    onClearCache: () -> Unit,
    onDeleteAllData: () -> Unit,
    onOpenAboutPrivacy: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var watermarkDraft by remember(settings.defaultWatermarkText) {
        mutableStateOf(settings.defaultWatermarkText)
    }
    var showConfirmDeleteAll by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = StudioBackground,
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            StudioTopBar(
                title = "Settings",
                subtitle = "Studio defaults, storage & privacy",
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
                    Text("Default AI Clip Duration", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(15, 30, 45).forEach { sec ->
                            FilterChip(
                                selected = settings.defaultClipDurationSec == sec,
                                onClick = { onUpdateClipDuration(sec) },
                                label = { Text("${sec}s") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryPurple,
                                    selectedLabelColor = TextPrimary
                                )
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    Text("Default 9:16 Crop Mode", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Spacer(Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        CropMode.entries.forEach { mode ->
                            FilterChip(
                                selected = settings.defaultCropMode == mode,
                                onClick = { onUpdateCropMode(mode) },
                                label = { Text(mode.displayName) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryPurple,
                                    selectedLabelColor = TextPrimary
                                )
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    Text("Default Subtitle Language", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Spacer(Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SubtitleLanguage.entries.forEach { lang ->
                            FilterChip(
                                selected = settings.defaultSubtitleLanguage == lang,
                                onClick = { onUpdateSubtitleLanguage(lang) },
                                label = { Text(lang.displayName) }
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    OutlinedTextField(
                        value = watermarkDraft,
                        onValueChange = {
                            watermarkDraft = it
                            onUpdateWatermarkText(it)
                        },
                        label = { Text("Default Brand Watermark") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            item {
                StudioCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Export Completion Notifications", style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                            Text("Notify when 9:16 MP4 finishes saving", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                        Switch(
                            checked = settings.notifyOnExportComplete,
                            onCheckedChange = onUpdateNotify,
                            colors = SwitchDefaults.colors(checkedTrackColor = PrimaryPurple)
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Auto-Clean Temp Files", style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                            Text("Delete staging MP4s after MediaStore copy", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                        Switch(
                            checked = settings.autoCleanupTempFiles,
                            onCheckedChange = onUpdateAutoCleanup,
                            colors = SwitchDefaults.colors(checkedTrackColor = PrimaryPurple)
                        )
                    }
                }
            }

            item {
                StudioCard {
                    Text("Storage & Privacy Controls", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Temporary Cache: ${formatFileSize(cacheSizeBytes)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = AccentLavender
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = onClearCache,
                        modifier = Modifier.fillMaxWidth().testTag("settings_clear_cache_btn")
                    ) {
                        Text("Clear Temporary Media Cache")
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { showConfirmDeleteAll = true },
                        modifier = Modifier.fillMaxWidth().testTag("settings_delete_all_btn")
                    ) {
                        Text("Delete All Local Projects & Data", color = StatusError)
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = onOpenAboutPrivacy,
                        colors = ButtonDefaults.buttonColors(containerColor = StudioCardElevated),
                        modifier = Modifier.fillMaxWidth().testTag("settings_about_privacy_btn")
                    ) {
                        Icon(Icons.Filled.PrivacyTip, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("About, Privacy Policy & Play Info")
                    }
                }
            }

            item {
                DeveloperProfileSectionCard(
                    onOpenUrl = { url ->
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        }
                    },
                    onOpenEmail = { email ->
                        runCatching {
                            val mailIntent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:$email")
                                putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
                                putExtra(Intent.EXTRA_SUBJECT, "ClipForge AI")
                            }
                            context.startActivity(mailIntent)
                        }
                    }
                )
            }
        }
    }

    if (showConfirmDeleteAll) {
        AlertDialog(
            onDismissRequest = { showConfirmDeleteAll = false },
            containerColor = StudioCardElevated,
            title = { Text("Delete All Projects & Data?", color = TextPrimary) },
            text = {
                Text(
                    "This clears all local Room project records, export history entries, thumbnails, and cached files. MP4 files already saved to Movies/ClipForge remain in your gallery.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteAllData()
                    showConfirmDeleteAll = false
                }) {
                    Text("Delete All", color = StatusError)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDeleteAll = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun DeveloperProfileSectionCard(
    onOpenUrl: (String) -> Unit,
    onOpenEmail: (String) -> Unit
) {
    val facebookUrl = "https://www.facebook.com/share/1DyNKzEBBW/"
    val instagramUrl = "https://www.instagram.com/md_ibrahim_kl?igsh=MXc5eWdrdHVxYXh6YQ=="
    val emailAddress = "ibrahimraj26@gmail.com"

    StudioCard(
        highlighted = true,
        modifier = Modifier.testTag("settings_developer_section")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Developer",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
            StatusPill(label = "CREATOR", color = AccentLavender)
        }

        Spacer(Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.developer_avatar),
                contentDescription = "Developer portrait of MD Ibrahim Raj",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .border(2.dp, PrimaryPurple, CircleShape)
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "MD Ibrahim Raj",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Junior Developer",
                    style = MaterialTheme.typography.labelLarge,
                    color = AccentLavender
                )
                Text(
                    text = "ClipForge AI Android App",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(
            text = "Social Media & Contact",
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary
        )
        Spacer(Modifier.height(8.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            DeveloperContactRow(
                label = "Facebook",
                subtitle = "Open Facebook profile",
                painter = painterResource(id = R.drawable.ic_social_facebook),
                badgeColor = Color(0xFF1877F2),
                testTag = "developer_facebook_button",
                onClick = { onOpenUrl(facebookUrl) }
            )

            DeveloperContactRow(
                label = "Instagram",
                subtitle = "@md_ibrahim_kl",
                painter = painterResource(id = R.drawable.ic_social_instagram),
                badgeColor = Color(0xFFE1306C),
                testTag = "developer_instagram_button",
                onClick = { onOpenUrl(instagramUrl) }
            )

            DeveloperContactRow(
                label = "Email",
                subtitle = emailAddress,
                iconVector = Icons.Filled.Email,
                badgeColor = PrimaryPurple,
                testTag = "developer_email_button",
                onClick = { onOpenEmail(emailAddress) }
            )
        }
    }
}

@Composable
private fun DeveloperContactRow(
    label: String,
    subtitle: String,
    badgeColor: Color,
    testTag: String,
    onClick: () -> Unit,
    painter: Painter? = null,
    iconVector: ImageVector? = null
) {
    Surface(
        color = StudioSecondaryBg,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, StudioBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(badgeColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                when {
                    painter != null -> Icon(
                        painter = painter,
                        contentDescription = label,
                        tint = badgeColor,
                        modifier = Modifier.size(22.dp)
                    )
                    iconVector != null -> Icon(
                        imageVector = iconVector,
                        contentDescription = label,
                        tint = AccentLavender,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = "Open $label",
                tint = TextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun AboutPrivacyScreen(
    onDeleteAllData: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        containerColor = StudioBackground,
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            StudioTopBar(
                title = "About & Privacy",
                subtitle = "ClipForge AI v${BuildConfig.VERSION_NAME}",
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                StudioCard(highlighted = true) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Filled.VerifiedUser, contentDescription = null, tint = StatusSuccess)
                        Text("Privacy-First Video Studio", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "ClipForge AI processes video encoding, 9:16 cropping, watermark rendering, and subtitle burn-in directly on your Android device using Jetpack Media3 Transformer.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }

            item {
                StudioCard {
                    Text("Privacy Policy & Data Handling", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "• Minimal Permissions: ClipForge AI uses the zero-permission Android Photo Picker and Storage Access Framework instead of broad storage access.\n" +
                            "• Ephemeral AI Analysis: When you tap 'Analyze Video' or 'Generate Automatic Subtitles', only necessary audio chunks and sampled keyframes are sent over encrypted TLS via Firebase AI Logic (protected by App Check) and are not retained in ClipForge cloud storage.\n" +
                            "• No Embedded API Keys: Zero private API credentials are stored in the APK.\n" +
                            "• Local Project Storage: Your projects, captions, and watermark preferences are stored locally in an on-device Room database and DataStore.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = onDeleteAllData,
                        modifier = Modifier.fillMaxWidth().testTag("privacy_delete_data_btn")
                    ) {
                        Text("Clear All Local Projects & Temp Media", color = StatusError)
                    }
                }
            }

            item {
                StudioCard {
                    Text("Google Play Compliance & Release Info", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Category: Video Players & Editors\n" +
                            "Content Rating: Everyone (PEGI 3 / ESRB E)\n" +
                            "Data Safety: On-device media processing; ephemeral encrypted AI requests initiated only by user action; zero third-party ad tracking SDKs.\n" +
                            "Package ID: ${BuildConfig.APPLICATION_ID}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}
