package com.example.ui.components

import android.net.Uri
import android.view.LayoutInflater
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.media.OverlayRenderer
import com.example.model.CropMode
import com.example.model.CropSettings
import com.example.model.SubtitleAlign
import com.example.model.SubtitleCue
import com.example.model.SubtitleFontFamily
import com.example.model.SubtitlePosition
import com.example.model.SubtitleStyle
import com.example.model.WatermarkConfig
import com.example.model.WatermarkPosition
import com.example.model.WatermarkType
import com.example.model.WatermarkWeight
import com.example.model.formatTimestampMs
import com.example.ui.theme.AccentLavender
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.PlusJakartaSansFontFamily
import com.example.ui.theme.PrimaryPurple
import com.example.ui.theme.PrimaryPurpleContainer
import com.example.ui.theme.SpaceGroteskFontFamily
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusErrorContainer
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StudioBackground
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCardBg
import com.example.ui.theme.StudioSecondaryBg
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay

@Composable
fun ClipForgeLogoMark(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(id = R.drawable.ic_launcher_logo),
        contentDescription = "ClipForge AI logo",
        contentScale = ContentScale.Crop,
        modifier = modifier
            .size(40.dp)
            .clip(RoundedCornerShape(10.dp))
    )
}

@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudioTopBar(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    onSettingsClick: (() -> Unit)? = null,
    actions: @Composable () -> Unit = {}
) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = StudioBackground,
            titleContentColor = TextPrimary
        ),
        navigationIcon = {
            if (onBack != null) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("nav_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Navigate back",
                        tint = TextPrimary
                    )
                }
            }
        },
        title = {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        actions = {
            actions()
            if (onSettingsClick != null) {
                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier.testTag("top_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "Settings",
                        tint = TextSecondary
                    )
                }
            }
        }
    )
}

@Composable
fun StudioCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    highlighted: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val borderColor = if (highlighted) PrimaryPurple else StudioBorder
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else Modifier
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = StudioCardBg),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

@Composable
fun FeatureDashboardCard(
    title: String,
    description: String,
    icon: ImageVector,
    badge: String? = null,
    accentColor: Color = PrimaryPurple,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag(testTag)
            .minimumInteractiveComponentSize()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = StudioCardBg),
        border = BorderStroke(1.dp, StudioBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(accentColor.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = accentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
                if (!badge.isNullOrBlank()) {
                    Surface(
                        color = PrimaryPurpleContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = badge,
                            style = MaterialTheme.typography.labelSmall,
                            color = AccentLavender,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ErrorBanner(
    message: String?,
    onDismiss: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(visible = !message.isNullOrBlank(), modifier = modifier) {
        Surface(
            color = StatusErrorContainer,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, StatusError.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.ErrorOutline,
                    contentDescription = "Error",
                    tint = StatusError
                )
                Text(
                    text = message.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                if (onDismiss != null) {
                    Text(
                        text = "Dismiss",
                        style = MaterialTheme.typography.labelLarge,
                        color = StatusError,
                        modifier = Modifier
                            .clickable(onClick = onDismiss)
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyStateCard(
    title: String,
    description: String,
    icon: ImageVector = Icons.Filled.VideoFile,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    StudioCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(PrimaryPurpleContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = AccentLavender,
                    modifier = Modifier.size(32.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(4.dp))
                androidx.compose.material3.Button(
                    onClick = onAction,
                    modifier = Modifier.testTag("empty_state_action_button")
                ) {
                    Text(actionLabel)
                }
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerStage(
    uriString: String,
    modifier: Modifier = Modifier,
    startMs: Long = 0L,
    endMs: Long = -1L,
    cropSettings: CropSettings = CropSettings(),
    showVerticalStage: Boolean = true,
    subtitlesEnabled: Boolean = false,
    subtitleCues: List<SubtitleCue> = emptyList(),
    subtitleStyle: SubtitleStyle = SubtitleStyle(),
    watermarkConfig: WatermarkConfig = WatermarkConfig(enabled = false),
    onPositionChanged: (Long) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosMs by remember { mutableLongStateOf(startMs.coerceAtLeast(0L)) }
    var totalDurationMs by remember { mutableLongStateOf(1000L) }
    var playerErrorMessage by remember(uriString) { mutableStateOf<String?>(null) }

    val exoPlayer = remember(uriString) {
        runCatching {
            ExoPlayer.Builder(context).build().apply {
                repeatMode = Player.REPEAT_MODE_ONE
            }
        }.onFailure { err ->
            playerErrorMessage = err.localizedMessage ?: "Video player unavailable on this device."
        }.getOrNull()
    }

    LaunchedEffect(exoPlayer, uriString, startMs, endMs) {
        val player = exoPlayer ?: return@LaunchedEffect
        runCatching {
            playerErrorMessage = null
            val safeStart = startMs.coerceAtLeast(0L)
            val mediaItemBuilder = MediaItem.Builder().setUri(Uri.parse(uriString))
            if (safeStart > 0L || endMs > safeStart) {
                val clipBuilder = MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(safeStart)
                if (endMs > safeStart) {
                    clipBuilder.setEndPositionMs(endMs)
                }
                mediaItemBuilder.setClippingConfiguration(clipBuilder.build())
            }
            player.setMediaItem(mediaItemBuilder.build())
            player.prepare()
            currentPosMs = 0L
        }.onFailure { err ->
            playerErrorMessage = err.localizedMessage ?: "Unable to load video preview."
        }
    }

    DisposableEffect(exoPlayer, lifecycleOwner) {
        val player = exoPlayer
        if (player == null) {
            return@DisposableEffect onDispose { }
        }
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    playerErrorMessage = null
                    totalDurationMs = player.duration.coerceAtLeast(1000L)
                }
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                isPlaying = false
                playerErrorMessage = error.localizedMessage ?: "Video preview failed to play."
            }
        }
        player.addListener(listener)

        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                runCatching { player.pause() }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            runCatching { player.removeListener(listener) }
            runCatching { player.release() }
        }
    }

    LaunchedEffect(exoPlayer, isPlaying) {
        val player = exoPlayer ?: return@LaunchedEffect
        val initialPos = runCatching { player.currentPosition.coerceAtLeast(0L) }.getOrDefault(0L)
        currentPosMs = initialPos
        onPositionChanged(initialPos + startMs.coerceAtLeast(0L))
        while (isPlaying) {
            val pos = runCatching { player.currentPosition.coerceAtLeast(0L) }.getOrDefault(0L)
            currentPosMs = pos
            onPositionChanged(pos + startMs.coerceAtLeast(0L))
            delay(120L)
        }
    }

    val activeSubtitle = remember(currentPosMs, startMs, subtitlesEnabled, subtitleCues, subtitleStyle) {
        if (!subtitlesEnabled || subtitleCues.isEmpty()) null
        else {
            val absTime = currentPosMs + startMs - subtitleStyle.timingOffsetMs
            subtitleCues.firstOrNull { absTime in it.startMs..it.endMs }
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .then(
                    if (showVerticalStage) {
                        Modifier
                            .fillMaxWidth(0.66f)
                            .aspectRatio(9f / 16f)
                    } else {
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                    }
                )
                .clip(RoundedCornerShape(20.dp))
                .background(
                    if (cropSettings.mode == CropMode.BLUR_BACKGROUND) {
                        Brush.verticalGradient(listOf(Color(0xFF231D3D), Color(0xFF10121A)))
                    } else {
                        Brush.verticalGradient(listOf(Color.Black, Color(0xFF0A0C12)))
                    }
                )
                .border(1.5.dp, StudioBorder, RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (exoPlayer != null) {
                AndroidView(
                    factory = { ctx ->
                        (LayoutInflater.from(ctx).inflate(
                            R.layout.clipforge_player_view,
                            null,
                            false
                        ) as PlayerView).apply {
                            player = exoPlayer
                            useController = false
                            resizeMode = when (cropSettings.mode) {
                                CropMode.FIT_ORIGINAL, CropMode.BLUR_BACKGROUND ->
                                    AspectRatioFrameLayout.RESIZE_MODE_FIT
                                CropMode.CENTER_CROP, CropMode.SMART_CROP ->
                                    AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                            }
                        }
                    },
                    update = { view ->
                        if (view.player !== exoPlayer) {
                            view.player = exoPlayer
                        }
                        view.resizeMode = when (cropSettings.mode) {
                            CropMode.FIT_ORIGINAL, CropMode.BLUR_BACKGROUND ->
                                AspectRatioFrameLayout.RESIZE_MODE_FIT
                            CropMode.CENTER_CROP, CropMode.SMART_CROP ->
                                AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        }
                    },
                    onRelease = { view ->
                        view.player = null
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Live Watermark Overlay Preview
            if (watermarkConfig.enabled) {
                val wmAlign = when (watermarkConfig.position) {
                    WatermarkPosition.TOP_LEFT -> Alignment.TopStart
                    WatermarkPosition.TOP_RIGHT -> Alignment.TopEnd
                    WatermarkPosition.BOTTOM_LEFT -> Alignment.BottomStart
                    WatermarkPosition.BOTTOM_RIGHT -> Alignment.BottomEnd
                    WatermarkPosition.CENTER -> Alignment.Center
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(watermarkConfig.paddingDp.coerceIn(4f, 40f).dp),
                    contentAlignment = wmAlign
                ) {
                    if (watermarkConfig.type == WatermarkType.TEXT && watermarkConfig.text.isNotBlank()) {
                        val parsedColor = Color(OverlayRenderer.parseColorSafe(watermarkConfig.textColorHex))
                        Text(
                            text = watermarkConfig.text,
                            color = parsedColor.copy(alpha = watermarkConfig.opacity.coerceIn(0.05f, 1f)),
                            fontSize = (watermarkConfig.fontSizeSp * 0.72f).sp,
                            fontFamily = SpaceGroteskFontFamily,
                            fontWeight = when (watermarkConfig.fontWeight) {
                                WatermarkWeight.NORMAL -> FontWeight.Normal
                                WatermarkWeight.MEDIUM -> FontWeight.Medium
                                WatermarkWeight.BOLD -> FontWeight.Bold
                            }
                        )
                    } else if (watermarkConfig.type == WatermarkType.IMAGE && watermarkConfig.imageUriString != null) {
                        val safeImageUri = remember(watermarkConfig.imageUriString) {
                            runCatching { Uri.parse(watermarkConfig.imageUriString) }.getOrNull()
                        }
                        if (safeImageUri != null) {
                            AsyncImage(
                                model = safeImageUri,
                                contentDescription = "Watermark preview",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .size((watermarkConfig.imageSizeDp * 0.6f).dp)
                                    .alpha(watermarkConfig.opacity.coerceIn(0.05f, 1f))
                            )
                        }
                    }
                }
            }

            // Live Subtitle Overlay Preview
            if (activeSubtitle != null) {
                val subAlign = when (subtitleStyle.position) {
                    SubtitlePosition.TOP -> Alignment.TopCenter
                    SubtitlePosition.CENTER -> Alignment.Center
                    SubtitlePosition.BOTTOM -> Alignment.BottomCenter
                }
                val textAlign = when (subtitleStyle.alignment) {
                    SubtitleAlign.LEFT -> TextAlign.Start
                    SubtitleAlign.CENTER -> TextAlign.Center
                    SubtitleAlign.RIGHT -> TextAlign.End
                }
                val fontFam = when (subtitleStyle.fontFamily) {
                    SubtitleFontFamily.SANS -> PlusJakartaSansFontFamily
                    SubtitleFontFamily.DISPLAY -> SpaceGroteskFontFamily
                    SubtitleFontFamily.MONO -> JetBrainsMonoFontFamily
                    SubtitleFontFamily.SERIF -> FontFamily.Serif
                }
                val fgColor = Color(OverlayRenderer.parseColorSafe(subtitleStyle.textColorHex))
                val bgColor = Color(OverlayRenderer.parseColorSafe(subtitleStyle.backgroundColorHex))
                    .copy(alpha = subtitleStyle.backgroundAlpha.coerceIn(0f, 1f))

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 28.dp),
                    contentAlignment = subAlign
                ) {
                    Surface(
                        color = bgColor,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = activeSubtitle.text,
                            color = fgColor,
                            fontSize = (subtitleStyle.fontSizeSp * 0.65f).sp,
                            fontFamily = fontFam,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = textAlign,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Center Play/Pause Tap Overlay or Error indicator
            if (playerErrorMessage != null) {
                Surface(
                    color = StatusErrorContainer.copy(alpha = 0.92f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, StatusError.copy(alpha = 0.6f)),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = playerErrorMessage.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPrimary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(StudioBackground.copy(alpha = 0.6f))
                        .clickable(enabled = exoPlayer != null) {
                            val player = exoPlayer ?: return@clickable
                            runCatching {
                                if (player.isPlaying) player.pause() else player.play()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "Pause preview" else "Play preview",
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
        }

        // Scrub bar + timecode
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = formatTimestampMs(currentPosMs),
                style = MaterialTheme.typography.labelSmall,
                color = AccentLavender
            )
            Slider(
                value = currentPosMs.toFloat().coerceIn(0f, totalDurationMs.toFloat().coerceAtLeast(1f)),
                onValueChange = { newVal ->
                    runCatching { exoPlayer?.seekTo(newVal.toLong()) }
                    currentPosMs = newVal.toLong()
                },
                valueRange = 0f..totalDurationMs.toFloat().coerceAtLeast(1f),
                colors = SliderDefaults.colors(
                    thumbColor = PrimaryPurple,
                    activeTrackColor = PrimaryPurple,
                    inactiveTrackColor = StudioCardBg
                ),
                modifier = Modifier.weight(1f)
            )
            Text(
                text = formatTimestampMs(totalDurationMs),
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
        }
    }
}

@Composable
fun StatusPill(
    label: String,
    color: Color = StatusSuccess,
    modifier: Modifier = Modifier
) {
    Surface(
        color = color.copy(alpha = 0.16f),
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.45f)),
        modifier = modifier
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}
