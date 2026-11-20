package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.ui.navigation.ClipForgeRoute
import com.example.ui.screens.AboutPrivacyScreen
import com.example.ui.screens.AiShortsGeneratorScreen
import com.example.ui.screens.ClipSelectionScreen
import com.example.ui.screens.ExportHistoryScreen
import com.example.ui.screens.ExportProgressScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LocalVideoPickerScreen
import com.example.ui.screens.MyProjectsScreen
import com.example.ui.screens.OneClickConvertScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SubtitleEditorScreen
import com.example.ui.screens.VideoAnalysisScreen
import com.example.ui.screens.VideoEditorScreen
import com.example.ui.screens.VideoPreviewScreen
import com.example.ui.screens.VideoUrlInputScreen
import com.example.ui.screens.WatermarkEditorScreen
import com.example.ui.theme.ClipForgeTheme
import com.example.ui.theme.StudioBackground
import com.example.ui.viewmodel.ClipForgeViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure Firebase App Check with launch intent BEFORE Compose UI, ViewModels, or Firebase calls.
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        setContent {
            ClipForgeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = StudioBackground
                ) {
                    ClipForgeApp()
                }
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun ClipForgeApp(
    viewModel: ClipForgeViewModel = viewModel()
) {
    val navController = rememberNavController()
    val editorState by viewModel.editorState.collectAsStateWithLifecycle()
    val aiState by viewModel.aiState.collectAsStateWithLifecycle()
    val subtitleGenState by viewModel.subtitleGenState.collectAsStateWithLifecycle()
    val urlDownloadState by viewModel.urlDownloadState.collectAsStateWithLifecycle()
    val exportState by viewModel.exportState.collectAsStateWithLifecycle()
    val projects by viewModel.projects.collectAsStateWithLifecycle()
    val exportHistory by viewModel.exportHistory.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val cacheSizeBytes by viewModel.cacheSizeBytes.collectAsStateWithLifecycle()

    NavHost(
        navController = navController,
        startDestination = ClipForgeRoute.Home
    ) {
        composable<ClipForgeRoute.Home> {
            HomeScreen(
                editorState = editorState,
                projectCount = projects.size,
                exportCount = exportHistory.size,
                onAiShortsClick = { navController.navigate(ClipForgeRoute.AiShortsGenerator) },
                onOneClickConvertClick = { navController.navigate(ClipForgeRoute.OneClickConvert) },
                onSubtitleStudioClick = { navController.navigate(ClipForgeRoute.SubtitleEditor) },
                onWatermarkStudioClick = { navController.navigate(ClipForgeRoute.WatermarkEditor) },
                onMyProjectsClick = { navController.navigate(ClipForgeRoute.MyProjects) },
                onExportHistoryClick = { navController.navigate(ClipForgeRoute.ExportHistory) },
                onResumeEditorClick = { navController.navigate(ClipForgeRoute.VideoEditor) },
                onSettingsClick = { navController.navigate(ClipForgeRoute.Settings) },
                onPrivacyClick = { navController.navigate(ClipForgeRoute.AboutPrivacy) }
            )
        }

        composable<ClipForgeRoute.AiShortsGenerator> {
            AiShortsGeneratorScreen(
                editorState = editorState,
                onSelectDuration = { viewModel.setTargetClipDuration(it) },
                onOpenUrlInput = { navController.navigate(ClipForgeRoute.VideoUrlInput("analysis")) },
                onOpenLocalPicker = { navController.navigate(ClipForgeRoute.LocalVideoPicker("analysis")) },
                onGoToAnalysis = { navController.navigate(ClipForgeRoute.VideoAnalysis) },
                onBack = { navController.popBackStack() }
            )
        }

        composable<ClipForgeRoute.VideoUrlInput> { backStackEntry ->
            val route = backStackEntry.toRoute<ClipForgeRoute.VideoUrlInput>()
            VideoUrlInputScreen(
                urlState = urlDownloadState,
                onStartDownload = { viewModel.downloadVideoFromUrl(it) },
                onCancelDownload = { viewModel.cancelUrlDownload() },
                onResetState = { viewModel.resetUrlDownloadState() },
                onContinueToTarget = {
                    if (route.nextTarget == "oneclick") {
                        navController.navigate(ClipForgeRoute.OneClickConvert)
                    } else {
                        navController.navigate(ClipForgeRoute.VideoAnalysis)
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable<ClipForgeRoute.LocalVideoPicker> {
            LocalVideoPickerScreen(
                editorState = editorState,
                onVideoPicked = { uri, persistable -> viewModel.selectLocalVideo(uri, persistable) },
                onDismissError = { viewModel.clearEditorMessages() },
                onOpenAnalysis = { navController.navigate(ClipForgeRoute.VideoAnalysis) },
                onOpenOneClick = { navController.navigate(ClipForgeRoute.OneClickConvert) },
                onOpenEditor = { navController.navigate(ClipForgeRoute.VideoEditor) },
                onOpenSubtitles = { navController.navigate(ClipForgeRoute.SubtitleEditor) },
                onOpenWatermark = { navController.navigate(ClipForgeRoute.WatermarkEditor) },
                onBack = { navController.popBackStack() }
            )
        }

        composable<ClipForgeRoute.VideoAnalysis> {
            VideoAnalysisScreen(
                editorState = editorState,
                aiState = aiState,
                onSelectDuration = { viewModel.setTargetClipDuration(it) },
                onAnalyzeClick = { viewModel.analyzeActiveVideo() },
                onViewClipResults = { navController.navigate(ClipForgeRoute.ClipSelection) },
                onPickVideo = { navController.navigate(ClipForgeRoute.LocalVideoPicker("analysis")) },
                onBack = { navController.popBackStack() }
            )
        }

        composable<ClipForgeRoute.ClipSelection> {
            ClipSelectionScreen(
                editorState = editorState,
                onToggleSelect = { viewModel.toggleClipSelection(it) },
                onSelectAll = { viewModel.selectAllClips(it) },
                onPreviewClip = { clip ->
                    val uri = editorState.metadata?.uriString ?: return@ClipSelectionScreen
                    navController.navigate(
                        ClipForgeRoute.VideoPreview(
                            uriString = uri,
                            title = clip.title,
                            startMs = clip.startMs,
                            endMs = clip.endMs,
                            showOverlays = true
                        )
                    )
                },
                onEditClip = { clip ->
                    viewModel.applyClipRangeToEditor(clip)
                    navController.navigate(ClipForgeRoute.VideoEditor)
                },
                onGenerateOne = { clip ->
                    viewModel.exportClips(listOf(clip))
                    navController.navigate(ClipForgeRoute.ExportProgress)
                },
                onGenerateSelected = { selected ->
                    viewModel.exportClips(selected)
                    navController.navigate(ClipForgeRoute.ExportProgress)
                },
                onGenerateAll = { all ->
                    viewModel.exportClips(all)
                    navController.navigate(ClipForgeRoute.ExportProgress)
                },
                onRunAnalysis = { navController.navigate(ClipForgeRoute.VideoAnalysis) },
                onBack = { navController.popBackStack() }
            )
        }

        composable<ClipForgeRoute.VideoEditor> {
            VideoEditorScreen(
                editorState = editorState,
                onUpdateTrimRange = { s, e -> viewModel.updateTrimRange(s, e) },
                onSelectCropMode = { viewModel.updateCropMode(it) },
                onUpdateManualCrop = { px, py, z -> viewModel.updateManualCrop(px, py, z) },
                onToggleMuteAudio = { viewModel.updateMuteAudio(it) },
                onOpenSubtitles = { navController.navigate(ClipForgeRoute.SubtitleEditor) },
                onOpenWatermark = { navController.navigate(ClipForgeRoute.WatermarkEditor) },
                onSaveProject = { viewModel.saveCurrentProject() },
                onExportClick = {
                    viewModel.exportActiveVideo()
                    navController.navigate(ClipForgeRoute.ExportProgress)
                },
                onPickVideo = { navController.navigate(ClipForgeRoute.LocalVideoPicker("editor")) },
                onBack = { navController.popBackStack() }
            )
        }

        composable<ClipForgeRoute.SubtitleEditor> {
            SubtitleEditorScreen(
                editorState = editorState,
                subtitleGenState = subtitleGenState,
                defaultLanguage = settings.defaultSubtitleLanguage,
                onToggleSubtitles = { viewModel.setSubtitlesEnabled(it) },
                onGenerateSubtitles = { viewModel.generateAutomaticSubtitles(it) },
                onAddCue = { s, e, t -> viewModel.addManualSubtitleCue(s, e, t) },
                onUpdateCue = { id, s, e, t -> viewModel.updateSubtitleCue(id, s, e, t) },
                onDeleteCue = { viewModel.deleteSubtitleCue(it) },
                onUpdateStyle = { viewModel.updateSubtitleStyle(it) },
                onImportSrt = { viewModel.importSrtFromUri(it) },
                onExportSrt = { viewModel.exportSrtToUri(it) },
                onPickVideo = { navController.navigate(ClipForgeRoute.LocalVideoPicker("subtitles")) },
                onExportVideo = {
                    viewModel.exportActiveVideo()
                    navController.navigate(ClipForgeRoute.ExportProgress)
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable<ClipForgeRoute.WatermarkEditor> {
            WatermarkEditorScreen(
                editorState = editorState,
                onUpdateWatermark = { viewModel.updateWatermarkConfig(it) },
                onPickVideo = { navController.navigate(ClipForgeRoute.LocalVideoPicker("watermark")) },
                onExportVideo = {
                    viewModel.exportActiveVideo()
                    navController.navigate(ClipForgeRoute.ExportProgress)
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable<ClipForgeRoute.OneClickConvert> {
            OneClickConvertScreen(
                editorState = editorState,
                onPickVideo = { navController.navigate(ClipForgeRoute.LocalVideoPicker("oneclick")) },
                onSelectCropMode = { viewModel.updateCropMode(it) },
                onUpdateManualCrop = { px, py, z -> viewModel.updateManualCrop(px, py, z) },
                onConvertClick = {
                    viewModel.exportActiveVideo()
                    navController.navigate(ClipForgeRoute.ExportProgress)
                },
                onOpenFullEditor = { navController.navigate(ClipForgeRoute.VideoEditor) },
                onBack = { navController.popBackStack() }
            )
        }

        composable<ClipForgeRoute.VideoPreview> { backStackEntry ->
            val route = backStackEntry.toRoute<ClipForgeRoute.VideoPreview>()
            VideoPreviewScreen(
                uriString = route.uriString,
                title = route.title,
                startMs = route.startMs,
                endMs = route.endMs,
                showOverlays = route.showOverlays,
                editorState = editorState,
                onEditInStudio = { navController.navigate(ClipForgeRoute.VideoEditor) },
                onBack = { navController.popBackStack() }
            )
        }

        composable<ClipForgeRoute.ExportProgress> {
            ExportProgressScreen(
                exportState = exportState,
                onCancelExport = { viewModel.cancelExport() },
                onRetryExport = { viewModel.retryLastExport() },
                onPreviewExported = { item ->
                    navController.navigate(
                        ClipForgeRoute.VideoPreview(
                            uriString = item.outputUriString,
                            title = item.title,
                            startMs = 0L,
                            endMs = -1L,
                            showOverlays = false
                        )
                    )
                },
                onViewHistory = { navController.navigate(ClipForgeRoute.ExportHistory) },
                onBack = { navController.popBackStack() }
            )
        }

        composable<ClipForgeRoute.MyProjects> {
            MyProjectsScreen(
                projects = projects,
                isUriAccessible = { viewModel.isMediaAccessible(it) },
                onOpenProject = { entity ->
                    viewModel.openProject(entity) {
                        navController.navigate(ClipForgeRoute.VideoEditor)
                    }
                },
                onRenameProject = { id, name -> viewModel.renameProject(id, name) },
                onDeleteProject = { viewModel.deleteProject(it) },
                onPreviewVideo = { uri, title ->
                    navController.navigate(
                        ClipForgeRoute.VideoPreview(
                            uriString = uri,
                            title = title
                        )
                    )
                },
                onCreateNewProject = { navController.navigate(ClipForgeRoute.LocalVideoPicker("editor")) },
                onBack = { navController.popBackStack() }
            )
        }

        composable<ClipForgeRoute.ExportHistory> {
            ExportHistoryScreen(
                exports = exportHistory,
                isUriAccessible = { viewModel.isMediaAccessible(it) },
                onPreviewExport = { rec ->
                    navController.navigate(
                        ClipForgeRoute.VideoPreview(
                            uriString = rec.outputUri,
                            title = rec.title
                        )
                    )
                },
                onDeleteExport = { viewModel.deleteExportRecord(it) },
                onStartOneClick = { navController.navigate(ClipForgeRoute.OneClickConvert) },
                onBack = { navController.popBackStack() }
            )
        }

        composable<ClipForgeRoute.Settings> {
            SettingsScreen(
                settings = settings,
                cacheSizeBytes = cacheSizeBytes,
                onUpdateClipDuration = { viewModel.updateDefaultClipDuration(it) },
                onUpdateCropMode = { viewModel.updateDefaultCropMode(it) },
                onUpdateSubtitleLanguage = { viewModel.updateDefaultSubtitleLanguage(it) },
                onUpdateWatermarkText = { viewModel.updateDefaultWatermarkText(it) },
                onUpdateNotify = { viewModel.updateNotifyOnExport(it) },
                onUpdateAutoCleanup = { viewModel.updateAutoCleanupTemp(it) },
                onClearCache = { viewModel.clearTemporaryCache() },
                onDeleteAllData = { viewModel.deleteAllProjectsAndData() },
                onOpenAboutPrivacy = { navController.navigate(ClipForgeRoute.AboutPrivacy) },
                onBack = { navController.popBackStack() }
            )
        }

        composable<ClipForgeRoute.AboutPrivacy> {
            AboutPrivacyScreen(
                onDeleteAllData = { viewModel.deleteAllProjectsAndData() },
                onBack = { navController.popBackStack() }
            )
        }
    }
}
