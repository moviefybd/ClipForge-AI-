package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import com.example.data.local.ExportRecordEntity
import com.example.data.local.ProjectEntity
import com.example.model.AiAnalysisState
import com.example.model.CropMode
import com.example.model.CropSettings
import com.example.model.ExportUiState
import com.example.model.ExportedVideoSummary
import com.example.model.SubtitleCue
import com.example.model.SubtitleGenState
import com.example.model.SubtitleLanguage
import com.example.model.SubtitleStyle
import com.example.model.SuggestedClip
import com.example.model.VideoMetadata
import com.example.model.WatermarkConfig
import com.example.ui.screens.ClipSelectionScreen
import com.example.ui.screens.ExportHistoryScreen
import com.example.ui.screens.ExportProgressScreen
import com.example.ui.screens.MyProjectsScreen
import com.example.ui.screens.OneClickConvertScreen
import com.example.ui.screens.SubtitleEditorScreen
import com.example.ui.screens.VideoAnalysisScreen
import com.example.ui.screens.VideoEditorScreen
import com.example.ui.screens.VideoPreviewScreen
import com.example.ui.screens.WatermarkEditorScreen
import com.example.ui.theme.ClipForgeTheme
import com.example.ui.viewmodel.ActiveEditorState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [24, 34, 36], qualifiers = "w412dp-h915dp-xxhdpi")
class ActiveScreensRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val sampleMetadata = VideoMetadata(
        uriString = "file:///tmp/sample_clip.mp4",
        title = "sample_talk.mp4",
        durationMs = 45_000L,
        width = 1920,
        height = 1080,
        rotationDegrees = 0,
        fps = 30f,
        fileSizeBytes = 12_500_000L,
        hasAudio = true
    )

    private val sampleClips = listOf(
        SuggestedClip(
            id = "clip_1",
            startTimeSec = 2.0,
            endTimeSec = 28.0,
            durationSec = 26.0,
            title = "Opening Hook",
            description = "High-energy intro hook",
            reason = "Strong opening question",
            isSelected = true
        )
    )

    private val activeEditorState = ActiveEditorState(
        projectId = 1L,
        projectName = "Demo Short",
        metadata = sampleMetadata,
        clipStartMs = 1_000L,
        clipEndMs = 31_000L,
        targetClipDurationSec = 30,
        cropSettings = CropSettings(mode = CropMode.CENTER_CROP),
        subtitlesEnabled = true,
        subtitleCues = listOf(
            SubtitleCue("c1", 1_000L, 4_000L, "Hello ClipForge AI"),
            SubtitleCue("c2", 4_200L, 7_500L, "বাংলা সাবটাইটেল")
        ),
        subtitleStyle = SubtitleStyle(),
        watermarkConfig = WatermarkConfig(enabled = true, text = "ClipForge AI"),
        suggestedClips = sampleClips
    )

    @Test
    fun `main activity launches cleanly across minSdk 24, 34, and 36`() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity = controller.get()
        assert(activity != null)
        controller.pause().stop().destroy()
    }

    @Test
    fun `active video editor renders and scrolls safely`() {
        composeRule.setContent {
            ClipForgeTheme {
                VideoEditorScreen(
                    editorState = activeEditorState,
                    onUpdateTrimRange = { _, _ -> },
                    onSelectCropMode = {},
                    onUpdateManualCrop = { _, _, _ -> },
                    onToggleMuteAudio = {},
                    onOpenSubtitles = {},
                    onOpenWatermark = {},
                    onSaveProject = {},
                    onExportClick = {},
                    onPickVideo = {},
                    onBack = {}
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("video_editor_list")
            .performScrollToNode(hasTestTag("editor_export_button"))
        composeRule.onNodeWithTag("editor_export_button").assertIsDisplayed()
    }

    @Test
    fun `one click convert renders with active video`() {
        composeRule.setContent {
            ClipForgeTheme {
                OneClickConvertScreen(
                    editorState = activeEditorState,
                    onPickVideo = {},
                    onSelectCropMode = {},
                    onUpdateManualCrop = { _, _, _ -> },
                    onConvertClick = {},
                    onOpenFullEditor = {},
                    onBack = {}
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("one_click_lazy_column")
            .performScrollToNode(hasTestTag("one_click_convert_button"))
        composeRule.onNodeWithTag("one_click_convert_button").assertIsDisplayed()
    }

    @Test
    fun `subtitle studio and watermark studio render with active video`() {
        composeRule.setContent {
            ClipForgeTheme {
                SubtitleEditorScreen(
                    editorState = activeEditorState,
                    subtitleGenState = SubtitleGenState.Idle,
                    defaultLanguage = SubtitleLanguage.ENGLISH,
                    onToggleSubtitles = {},
                    onGenerateSubtitles = {},
                    onAddCue = { _, _, _ -> },
                    onUpdateCue = { _, _, _, _ -> },
                    onDeleteCue = {},
                    onUpdateStyle = {},
                    onImportSrt = {},
                    onExportSrt = {},
                    onPickVideo = {},
                    onExportVideo = {},
                    onBack = {}
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("subtitle_editor_list")
            .performScrollToNode(hasTestTag("generate_auto_subtitles_btn"))
        composeRule.onNodeWithTag("generate_auto_subtitles_btn").assertIsDisplayed()
    }

    @Test
    fun `watermark editor renders with active video`() {
        composeRule.setContent {
            ClipForgeTheme {
                WatermarkEditorScreen(
                    editorState = activeEditorState,
                    onUpdateWatermark = {},
                    onPickVideo = {},
                    onExportVideo = {},
                    onBack = {}
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("watermark_editor_list")
            .performScrollToNode(hasTestTag("watermark_export_video_btn"))
        composeRule.onNodeWithTag("watermark_export_video_btn").assertIsDisplayed()
    }

    @Test
    fun `video analysis and preview screens render with active video`() {
        composeRule.setContent {
            ClipForgeTheme {
                VideoPreviewScreen(
                    uriString = "file:///tmp/sample_clip.mp4",
                    title = "Preview Clip",
                    startMs = 0L,
                    endMs = 15_000L,
                    showOverlays = true,
                    editorState = activeEditorState,
                    onEditInStudio = {},
                    onBack = {}
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("video_preview_lazy_column")
            .performScrollToNode(hasTestTag("preview_share_btn"))
        composeRule.onNodeWithTag("preview_share_btn").assertIsDisplayed()
    }

    @Test
    fun `video analysis screen renders with active video`() {
        composeRule.setContent {
            ClipForgeTheme {
                VideoAnalysisScreen(
                    editorState = activeEditorState,
                    aiState = AiAnalysisState.Idle,
                    onSelectDuration = {},
                    onAnalyzeClick = {},
                    onViewClipResults = {},
                    onPickVideo = {},
                    onBack = {}
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("analyze_video_button").assertIsDisplayed()
    }

    @Test
    fun `clip selection screen renders with suggested clips`() {
        composeRule.setContent {
            ClipForgeTheme {
                ClipSelectionScreen(
                    editorState = activeEditorState,
                    onToggleSelect = {},
                    onSelectAll = {},
                    onPreviewClip = {},
                    onEditClip = {},
                    onGenerateOne = {},
                    onGenerateSelected = {},
                    onGenerateAll = {},
                    onRunAnalysis = {},
                    onBack = {}
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Opening Hook").assertIsDisplayed()
    }

    @Test
    fun `projects screen renders with saved projects`() {
        val proj = ProjectEntity(
            id = 1L,
            name = "Sample Project",
            sourceUri = "file:///tmp/none.mp4",
            sourceTitle = "none.mp4",
            sourceDurationMs = 30_000L,
            sourceWidth = 1920,
            sourceHeight = 1080,
            sourceRotationDegrees = 0,
            sourceFps = 30f,
            sourceFileSizeBytes = 1024L,
            thumbnailPath = null,
            clipStartMs = 0L,
            clipEndMs = 30_000L,
            cropSettingsJson = "{}",
            subtitlesEnabled = false,
            subtitleCuesJson = "[]",
            subtitleStyleJson = "{}",
            watermarkConfigJson = "{}",
            suggestedClipsJson = "[]",
            latestExportUri = null
        )
        composeRule.setContent {
            ClipForgeTheme {
                MyProjectsScreen(
                    projects = listOf(proj),
                    isUriAccessible = { false },
                    onOpenProject = {},
                    onRenameProject = { _, _ -> },
                    onDeleteProject = {},
                    onPreviewVideo = { _, _ -> },
                    onCreateNewProject = {},
                    onBack = {}
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Sample Project").assertIsDisplayed()
    }

    @Test
    fun `export history and progress screens render with data`() {
        val rec = ExportRecordEntity(
            id = 1L,
            projectId = 1L,
            title = "Export 1",
            outputUri = "file:///tmp/out.mp4",
            filePath = "Movies/ClipForge/out.mp4",
            sourceUri = "file:///tmp/none.mp4",
            durationMs = 30_000L,
            width = 1080,
            height = 1920,
            fileSizeBytes = 2048L,
            cropMode = "Center Crop",
            burnedSubtitles = true,
            burnedWatermark = true
        )
        composeRule.setContent {
            ClipForgeTheme {
                ExportHistoryScreen(
                    exports = listOf(rec),
                    isUriAccessible = { false },
                    onPreviewExport = {},
                    onDeleteExport = {},
                    onStartOneClick = {},
                    onBack = {}
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Export 1").assertIsDisplayed()
    }

    @Test
    fun `export progress completed state renders cleanly`() {
        composeRule.setContent {
            ClipForgeTheme {
                ExportProgressScreen(
                    exportState = ExportUiState.Completed(
                        listOf(
                            ExportedVideoSummary(
                                title = "Export 1",
                                outputUriString = "file:///tmp/out.mp4",
                                filePath = "Movies/ClipForge/out.mp4",
                                durationMs = 30_000L,
                                fileSizeBytes = 2048L,
                                width = 1080,
                                height = 1920
                            )
                        )
                    ),
                    onCancelExport = {},
                    onRetryExport = {},
                    onPreviewExported = {},
                    onViewHistory = {},
                    onBack = {}
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Export Complete").assertIsDisplayed()
    }
}
