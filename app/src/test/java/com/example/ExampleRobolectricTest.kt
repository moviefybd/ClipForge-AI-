package com.example

import android.content.Context
import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.ClipForgeDatabase
import com.example.data.local.ProjectRepository
import com.example.data.local.ProjectWorkspaceData
import com.example.media.MediaStoreExporter
import com.example.media.OverlayRenderer
import com.example.media.SrtParser
import com.example.media.VideoUrlDownloader
import com.example.media.YouTubeLicenseChecker
import com.example.media.YouTubeLicenseStatus
import com.example.model.CropMode
import com.example.model.CropSettings
import com.example.model.SubtitleCue
import com.example.model.SubtitleStyle
import com.example.model.VideoMetadata
import com.example.model.WatermarkConfig
import com.example.model.WatermarkPosition
import com.example.ui.viewmodel.ClipForgeViewModel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w412dp-h915dp-xxhdpi")
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun `read app_name string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("ClipForge AI", appName)
    }

    @Test
    fun `launcher icons, drawables, and custom fonts load without error`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertNotNull(ContextCompat.getDrawable(context, R.mipmap.ic_launcher))
        assertNotNull(ContextCompat.getDrawable(context, R.mipmap.ic_launcher_round))
        assertNotNull(ContextCompat.getDrawable(context, R.drawable.ic_launcher_logo))
        assertNotNull(ContextCompat.getDrawable(context, R.drawable.developer_avatar))
        assertNotNull(ContextCompat.getDrawable(context, R.drawable.ic_launcher_foreground))
        assertNotNull(ContextCompat.getDrawable(context, R.drawable.ic_launcher_background))
        assertNotNull(ResourcesCompat.getFont(context, R.font.space_grotesk))
        assertNotNull(ResourcesCompat.getFont(context, R.font.plus_jakarta_sans))
        assertNotNull(ResourcesCompat.getFont(context, R.font.jetbrains_mono))
    }

    @Test
    fun `home dashboard renders all 6 studio cards and navigates across all primary routes`() {
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Create Amazing Shorts").assertIsDisplayed()
        composeTestRule.onNodeWithTag("card_ai_shorts").assertIsDisplayed()
        composeTestRule.onNodeWithTag("card_one_click").assertIsDisplayed()
        composeTestRule.onNodeWithTag("card_subtitles").assertIsDisplayed()
        composeTestRule.onNodeWithTag("card_watermark").assertIsDisplayed()
        composeTestRule.onNodeWithTag("card_projects").assertIsDisplayed()
        composeTestRule.onNodeWithTag("card_history").assertIsDisplayed()

        // Navigate to AI Shorts Generator -> URL Input -> Back -> Local Picker -> Back -> Home
        composeTestRule.onNodeWithTag("card_ai_shorts").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("ai_shorts_local_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("ai_shorts_url_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("video_url_textfield").assertIsDisplayed()
        composeTestRule.onNodeWithTag("nav_back_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("ai_shorts_local_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("launch_photo_picker_video").assertIsDisplayed()
        composeTestRule.onNodeWithTag("nav_back_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("nav_back_button").performClick()
        composeTestRule.waitForIdle()

        // One-Click 9:16
        composeTestRule.onNodeWithTag("card_one_click").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("nav_back_button").performClick()
        composeTestRule.waitForIdle()

        // Subtitle Studio
        composeTestRule.onNodeWithTag("card_subtitles").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("nav_back_button").performClick()
        composeTestRule.waitForIdle()

        // Watermark Studio
        composeTestRule.onNodeWithTag("card_watermark").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("watermark_enable_switch").assertIsDisplayed()
        composeTestRule.onNodeWithTag("nav_back_button").performClick()
        composeTestRule.waitForIdle()

        // My Projects
        composeTestRule.onNodeWithTag("card_projects").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("nav_back_button").performClick()
        composeTestRule.waitForIdle()

        // Export History
        composeTestRule.onNodeWithTag("card_history").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("nav_back_button").performClick()
        composeTestRule.waitForIdle()

        // About & Privacy
        composeTestRule.onNodeWithTag("home_privacy_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("privacy_delete_data_btn").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("nav_back_button").performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun `updateTrimRange handles edge-case and sub-second durations without IllegalArgumentException`() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val vm = ClipForgeViewModel(app)
        vm.updateTrimRange(500L, 1200L)
        vm.updateTrimRange(90_000L, 100L)
        val state = vm.editorState.value
        assertTrue(state.clipEndMs >= state.clipStartMs)
    }

    @Test
    fun `buildShareIntent handles content and external file URIs safely`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val contentChooser = MediaStoreExporter.buildShareIntent(
            context,
            "content://media/external/video/media/42",
            "My Short"
        )
        assertEquals(Intent.ACTION_CHOOSER, contentChooser.action)

        val unmappedFileChooser = MediaStoreExporter.buildShareIntent(
            context,
            "file:///sdcard/Download/outside_root.mp4",
            "Fallback Share"
        )
        assertEquals(Intent.ACTION_CHOOSER, unmappedFileChooser.action)
    }

    @Test
    fun `srt parser round-trips English and Bengali subtitles accurately`() {
        val cues = listOf(
            SubtitleCue("1", 1200L, 3800L, "Turn long videos into Shorts"),
            SubtitleCue("2", 4200L, 7500L, "বাংলায় অটোমেটিক সাবটাইটেল")
        )
        val formatted = SrtParser.format(cues)
        val parsed = SrtParser.parse(formatted)

        assertEquals(2, parsed.size)
        assertEquals(1200L, parsed[0].startMs)
        assertEquals(3800L, parsed[0].endMs)
        assertEquals("Turn long videos into Shorts", parsed[0].text)
        assertEquals("বাংলায় অটোমেটিক সাবটাইটেল", parsed[1].text)
    }

    @Test
    fun `url validator accepts direct mp4, routes youtube urls, and blocks other streaming platforms`() {
        val valid = VideoUrlDownloader.validateUrl("https://cdn.example.com/media/demo_video.mp4")
        assertTrue(valid is VideoUrlDownloader.ValidationResult.Valid)

        val youtube = VideoUrlDownloader.validateUrl("https://www.youtube.com/watch?v=dQw4w9WgXcQ")
        assertTrue(youtube is VideoUrlDownloader.ValidationResult.YouTube)
        assertEquals("dQw4w9WgXcQ", (youtube as VideoUrlDownloader.ValidationResult.YouTube).videoId)

        val shorts = VideoUrlDownloader.validateUrl("https://youtube.com/shorts/abcdefghijk")
        assertTrue(shorts is VideoUrlDownloader.ValidationResult.YouTube)
        assertEquals("abcdefghijk", (shorts as VideoUrlDownloader.ValidationResult.YouTube).videoId)

        val tiktok = VideoUrlDownloader.validateUrl("https://www.tiktok.com/@creator/video/123456")
        assertTrue(tiktok is VideoUrlDownloader.ValidationResult.Invalid)

        val empty = VideoUrlDownloader.validateUrl("   ")
        assertTrue(empty is VideoUrlDownloader.ValidationResult.Invalid)
    }

    @Test
    fun `youtube license checker verifies creativeCommon and warns on standard or unclear license`() {
        val ccJson = """
            {
              "items": [{
                "snippet": {"title": "CC Nature Clip", "channelTitle": "Open Media"},
                "status": {"license": "creativeCommon", "privacyStatus": "public"}
              }]
            }
        """.trimIndent()
        val ccRes = YouTubeLicenseChecker.parseYouTubeApiResponse(
            videoId = "abcdefghijk",
            watchUrl = "https://www.youtube.com/watch?v=abcdefghijk",
            jsonBody = ccJson
        )
        assertEquals(YouTubeLicenseStatus.VERIFIED_CREATIVE_COMMONS, ccRes.licenseStatus)
        assertEquals(YouTubeLicenseChecker.VERIFIED_MESSAGE, ccRes.message)

        val stdJson = """
            {
              "items": [{
                "snippet": {"title": "Standard Clip", "channelTitle": "Creator"},
                "status": {"license": "youtube", "privacyStatus": "public"}
              }]
            }
        """.trimIndent()
        val stdRes = YouTubeLicenseChecker.parseYouTubeApiResponse(
            videoId = "abcdefghijk",
            watchUrl = "https://www.youtube.com/watch?v=abcdefghijk",
            jsonBody = stdJson
        )
        assertEquals(YouTubeLicenseStatus.UNVERIFIED_OR_STANDARD, stdRes.licenseStatus)
        assertEquals(YouTubeLicenseChecker.WARNING_MESSAGE, stdRes.message)
    }

    @Test
    fun `settings screen renders developer section with profile and social links`() {
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("home_settings_button").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("settings_developer_section").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("MD Ibrahim Raj").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Junior Developer").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("developer_facebook_button").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("developer_instagram_button").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("developer_email_button").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `overlay renderer creates valid bitmaps for watermark and Bengali subtitles`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val wmBitmap = OverlayRenderer.renderWatermarkBitmap(
            context = context,
            config = WatermarkConfig(enabled = true, text = "ClipForge AI", position = WatermarkPosition.TOP_RIGHT),
            outputWidth = 1080
        )
        assertNotNull(wmBitmap)
        assertTrue(wmBitmap!!.width > 0 && wmBitmap.height > 0)

        val subBitmap = OverlayRenderer.renderSubtitleOverlayBitmap(
            context = context,
            text = "নতুন শর্ট ভিডিও তৈরি করুন",
            style = SubtitleStyle(),
            frameWidth = 1080,
            frameHeight = 1920
        )
        assertEquals(1080, subBitmap.width)
        assertEquals(1920, subBitmap.height)
    }

    @Test
    fun `room database persists and reloads project workspace`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = ClipForgeDatabase.getInstance(context)
        val repo = ProjectRepository(db.projectDao(), db.exportRecordDao())
        repo.deleteAllData()

        val id = repo.saveWorkspace(
            ProjectWorkspaceData(
                name = "Test Short Project",
                metadata = VideoMetadata(
                    uriString = "content://media/external/video/123",
                    title = "lecture.mp4",
                    durationMs = 90_000L,
                    width = 1920,
                    height = 1080
                ),
                clipStartMs = 5_000L,
                clipEndMs = 35_000L,
                cropSettings = CropSettings(mode = CropMode.BLUR_BACKGROUND),
                subtitlesEnabled = true,
                subtitleCues = listOf(SubtitleCue("c1", 5_000L, 8_000L, "Hook line")),
                watermarkConfig = WatermarkConfig(enabled = true, text = "MyBrand")
            )
        )

        val loaded = repo.loadWorkspace(id)
        assertNotNull(loaded)
        assertEquals("Test Short Project", loaded!!.name)
        assertEquals(CropMode.BLUR_BACKGROUND, loaded.cropSettings.mode)
        assertEquals(1, loaded.subtitleCues.size)
        assertEquals("MyBrand", loaded.watermarkConfig.text)
    }
}
