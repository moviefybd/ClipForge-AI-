package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.BrandingWatermark
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.CropPortrait
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.formatTimestampMs
import com.example.ui.components.ClipForgeLogoMark
import com.example.ui.components.FeatureDashboardCard
import com.example.ui.components.StatusPill
import com.example.ui.components.StudioCard
import com.example.ui.theme.AccentLavender
import com.example.ui.theme.PrimaryPurple
import com.example.ui.theme.PrimaryPurpleContainer
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StudioBackground
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCardElevated
import com.example.ui.theme.StudioSecondaryBg
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.ActiveEditorState

@Composable
fun HomeScreen(
    editorState: ActiveEditorState,
    projectCount: Int,
    exportCount: Int,
    onAiShortsClick: () -> Unit,
    onOneClickConvertClick: () -> Unit,
    onSubtitleStudioClick: () -> Unit,
    onWatermarkStudioClick: () -> Unit,
    onMyProjectsClick: () -> Unit,
    onExportHistoryClick: () -> Unit,
    onResumeEditorClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onPrivacyClick: () -> Unit
) {
    val cardRows = listOf(
        listOf(
            CardSpec(
                title = stringResource(R.string.card_ai_shorts_title),
                description = stringResource(R.string.card_ai_shorts_desc),
                icon = Icons.Filled.ContentCut,
                badge = "AI",
                accent = PrimaryPurple,
                testTag = "card_ai_shorts",
                onClick = onAiShortsClick
            ),
            CardSpec(
                title = stringResource(R.string.card_one_click_title),
                description = stringResource(R.string.card_one_click_desc),
                icon = Icons.Filled.CropPortrait,
                badge = "1080p",
                accent = StatusSuccess,
                testTag = "card_one_click",
                onClick = onOneClickConvertClick
            )
        ),
        listOf(
            CardSpec(
                title = stringResource(R.string.card_subtitle_title),
                description = stringResource(R.string.card_subtitle_desc),
                icon = Icons.Filled.ClosedCaption,
                badge = "EN / বাংলা",
                accent = AccentLavender,
                testTag = "card_subtitles",
                onClick = onSubtitleStudioClick
            ),
            CardSpec(
                title = stringResource(R.string.card_watermark_title),
                description = stringResource(R.string.card_watermark_desc),
                icon = Icons.AutoMirrored.Filled.BrandingWatermark,
                badge = "Brand",
                accent = Color(0xFFFFB547),
                testTag = "card_watermark",
                onClick = onWatermarkStudioClick
            )
        ),
        listOf(
            CardSpec(
                title = stringResource(R.string.card_projects_title),
                description = stringResource(R.string.card_projects_desc),
                icon = Icons.Filled.Folder,
                badge = "$projectCount saved",
                accent = AccentLavender,
                testTag = "card_projects",
                onClick = onMyProjectsClick
            ),
            CardSpec(
                title = stringResource(R.string.card_history_title),
                description = stringResource(R.string.card_history_desc),
                icon = Icons.Filled.Download,
                badge = "$exportCount MP4",
                accent = StatusSuccess,
                testTag = "card_history",
                onClick = onExportHistoryClick
            )
        )
    )

    Scaffold(
        containerColor = StudioBackground,
        contentWindowInsets = WindowInsets.statusBars
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ClipForgeLogoMark()
                        Column {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.headlineSmall,
                                color = TextPrimary
                            )
                            Text(
                                text = stringResource(R.string.app_tagline),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onPrivacyClick,
                            modifier = Modifier.testTag("home_privacy_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Info,
                                contentDescription = "About and Privacy",
                                tint = TextSecondary
                            )
                        }
                        IconButton(
                            onClick = onSettingsClick,
                            modifier = Modifier.testTag("home_settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = "Settings",
                                tint = TextPrimary
                            )
                        }
                    }
                }
            }

            // Welcome Hero Banner with Studio Waveform Graphic
            item {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = StudioSecondaryBg,
                    border = BorderStroke(1.dp, StudioBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        // Subtle studio glow + waveform bars drawn in Compose Canvas
                        Canvas(
                            modifier = Modifier
                                .matchParentSize()
                        ) {
                            drawRect(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        PrimaryPurple.copy(alpha = 0.24f),
                                        Color.Transparent,
                                        AccentLavender.copy(alpha = 0.12f)
                                    )
                                )
                            )
                            val barCount = 26
                            val barW = size.width / (barCount * 2f)
                            for (i in 0 until barCount) {
                                val norm = kotlin.math.abs(kotlin.math.sin(i * 0.65)).toFloat()
                                val barH = size.height * (0.12f + norm * 0.42f)
                                val x = size.width * 0.48f + i * barW * 1.4f
                                if (x < size.width - 12f) {
                                    drawRoundRect(
                                        color = PrimaryPurple.copy(alpha = 0.16f),
                                        topLeft = Offset(x, size.height - barH - 12f),
                                        size = Size(barW, barH),
                                        cornerRadius = CornerRadius(6f, 6f)
                                    )
                                }
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            StatusPill(label = "9:16 VERTICAL STUDIO • GEMINI AI", color = AccentLavender)
                            Text(
                                text = stringResource(R.string.welcome_title),
                                style = MaterialTheme.typography.displaySmall,
                                color = TextPrimary
                            )
                            Text(
                                text = stringResource(R.string.welcome_subtitle),
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextSecondary
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = onAiShortsClick,
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("hero_ai_shorts_cta")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.AutoAwesome,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text("AI Shorts")
                                }
                                Button(
                                    onClick = onOneClickConvertClick,
                                    colors = ButtonDefaults.buttonColors(containerColor = StudioCardElevated),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("hero_one_click_cta")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.CropPortrait,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = AccentLavender
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text("One-Click 9:16")
                                }
                            }
                        }
                    }
                }
            }

            // Active workspace quick resume banner
            if (editorState.metadata != null) {
                item {
                    StudioCard(
                        highlighted = true,
                        onClick = onResumeEditorClick,
                        modifier = Modifier.testTag("resume_active_project_card")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(PrimaryPurpleContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.PlayCircle,
                                        contentDescription = "Active video",
                                        tint = AccentLavender
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Active Workspace",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = StatusSuccess
                                    )
                                    Text(
                                        text = editorState.projectName,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${editorState.metadata.aspectRatioLabel} • ${formatTimestampMs(editorState.metadata.durationMs)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                            }
                            Text(
                                text = "Open",
                                style = MaterialTheme.typography.labelLarge,
                                color = AccentLavender,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // 6 Main Feature Cards
            item {
                Text(
                    text = "Studio Tools",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
            }

            cardRows.forEachIndexed { rowIdx, rowItems ->
                item(key = "studio_row_$rowIdx") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowItems.forEach { card ->
                            FeatureDashboardCard(
                                title = card.title,
                                description = card.description,
                                icon = card.icon,
                                badge = card.badge,
                                accentColor = card.accent,
                                testTag = card.testTag,
                                onClick = card.onClick,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

private data class CardSpec(
    val title: String,
    val description: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val badge: String,
    val accent: Color,
    val testTag: String,
    val onClick: () -> Unit
)
