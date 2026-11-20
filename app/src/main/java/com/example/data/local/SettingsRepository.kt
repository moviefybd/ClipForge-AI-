package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.AppSettings
import com.example.model.CropMode
import com.example.model.SubtitleLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "clipforge_settings")

class SettingsRepository(private val context: Context) {
    private object Keys {
        val DEFAULT_CLIP_DURATION = intPreferencesKey("default_clip_duration")
        val DEFAULT_CROP_MODE = stringPreferencesKey("default_crop_mode")
        val DEFAULT_SUBTITLE_LANG = stringPreferencesKey("default_subtitle_lang")
        val DEFAULT_WATERMARK_TEXT = stringPreferencesKey("default_watermark_text")
        val NOTIFY_EXPORT_COMPLETE = booleanPreferencesKey("notify_export_complete")
        val AUTO_CLEANUP_TEMP = booleanPreferencesKey("auto_cleanup_temp")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            defaultClipDurationSec = prefs[Keys.DEFAULT_CLIP_DURATION] ?: 30,
            defaultCropMode = prefs[Keys.DEFAULT_CROP_MODE]?.let { name ->
                CropMode.entries.firstOrNull { it.name == name }
            } ?: CropMode.CENTER_CROP,
            defaultSubtitleLanguage = prefs[Keys.DEFAULT_SUBTITLE_LANG]?.let { name ->
                SubtitleLanguage.entries.firstOrNull { it.name == name }
            } ?: SubtitleLanguage.ENGLISH,
            defaultWatermarkText = prefs[Keys.DEFAULT_WATERMARK_TEXT] ?: "ClipForge AI",
            notifyOnExportComplete = prefs[Keys.NOTIFY_EXPORT_COMPLETE] ?: true,
            autoCleanupTempFiles = prefs[Keys.AUTO_CLEANUP_TEMP] ?: true
        )
    }

    suspend fun updateDefaultClipDuration(seconds: Int) {
        context.dataStore.edit { it[Keys.DEFAULT_CLIP_DURATION] = seconds.coerceIn(5, 180) }
    }

    suspend fun updateDefaultCropMode(mode: CropMode) {
        context.dataStore.edit { it[Keys.DEFAULT_CROP_MODE] = mode.name }
    }

    suspend fun updateDefaultSubtitleLanguage(lang: SubtitleLanguage) {
        context.dataStore.edit { it[Keys.DEFAULT_SUBTITLE_LANG] = lang.name }
    }

    suspend fun updateDefaultWatermarkText(text: String) {
        context.dataStore.edit { it[Keys.DEFAULT_WATERMARK_TEXT] = text.ifBlank { "ClipForge AI" } }
    }

    suspend fun updateNotifyOnExport(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NOTIFY_EXPORT_COMPLETE] = enabled }
    }

    suspend fun updateAutoCleanupTemp(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_CLEANUP_TEMP] = enabled }
    }

    suspend fun resetDefaults() {
        context.dataStore.edit { it.clear() }
    }
}
