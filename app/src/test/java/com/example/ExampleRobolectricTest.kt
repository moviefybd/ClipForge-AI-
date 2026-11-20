package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.ClipForgeDatabase
import com.example.data.local.ProjectRepository
import com.example.data.local.ProjectWorkspaceData
import com.example.media.OverlayRenderer
import com.example.media.SrtParser
import com.example.media.VideoUrlDownloader
import com.example.model.CropMode
import com.example.model.CropSettings
import com.example.model.SubtitleCue
import com.example.model.SubtitleStyle
import com.example.model.VideoMetadata
import com.example.model.WatermarkConfig
import com.example.model.WatermarkPosition
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
    fun `home dashboard renders all 6 studio cards and navigates`() {
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Create Amazing Shorts").assertIsDisplayed()
        composeTestRule.onNodeWithTag("card_ai_shorts").assertIsDisplayed()
        composeTestRule.onNodeWithTag("card_one_click").assertIsDisplayed()
        composeTestRule.onNodeWithTag("card_subtitles").assertIsDisplayed()
        composeTestRule.onNodeWithTag("card_watermark").assertIsDisplayed()
        composeTestRule.onNodeWithTag("card_projects").assertIsDisplayed()
        composeTestRule.onNodeWithTag("card_history").assertIsDisplayed()

        // Navigate to AI Shorts Generator
        composeTestRule.onNodeWithTag("card_ai_shorts").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("ai_shorts_local_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("ai_shorts_url_button").assertIsDisplayed()

        // Navigate back and open Watermark Studio
        composeTestRule.onNodeWithTag("nav_back_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("card_watermark").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("watermark_enable_switch").assertIsDisplayed()
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
    fun `url validator accepts direct mp4 and rejects streaming platforms`() {
        val valid = VideoUrlDownloader.validateUrl("https://cdn.example.com/media/demo_video.mp4")
        assertTrue(valid is VideoUrlDownloader.ValidationResult.Valid)

        val youtube = VideoUrlDownloader.validateUrl("https://www.youtube.com/watch?v=dQw4w9WgXcQ")
        assertTrue(youtube is VideoUrlDownloader.ValidationResult.Invalid)

        val empty = VideoUrlDownloader.validateUrl("   ")
        assertTrue(empty is VideoUrlDownloader.ValidationResult.Invalid)
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
