package com.example.data.preferences

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "wallhaven_settings")

enum class FilterMode {
    GLOBAL_SHARED,
    TAB_INDEPENDENT
}

@Immutable
data class AppSettings(
    val dynamicColor: Boolean = true,
    val darkMode: String = "system", // "system", "light", "dark"
    val themeColor: String = "orange", // "orange", "blue", "green", "purple", "pink", "teal", "amber", "red", "slate"
    val colorStyle: String = "vibrant", // "vibrant", "expressive", "neutral", "high_contrast"
    val amoledBlack: Boolean = false,
    val hapticFeedback: Boolean = true,
    val categories: String = "111", // General + Anime + People (all selected by default)
    val purity: String = "110",    // SFW + Sketchy default (NSFW when API key entered)
    val apiKey: String = "",
    val webDavUrl: String = "",
    val webDavUsername: String = "",
    val webDavPassword: String = "",
    val webDavPath: String = "/wallhaven_backup/",
    val webDavAutoBackup: Boolean = false,
    val webDavLastBackupTime: Long = 0L,
    val webDavLastStatus: String = "",
    val hideNavOnScroll: Boolean = false,
    val useFloatingCapsuleNav: Boolean = true,
    val downloadDirectory: String = "/storage/emulated/0/Download/WallHaven",
    val concurrentDownloads: Int = 3,
    val showFavoritesCount: Boolean = true,
    val filterMode: FilterMode = FilterMode.TAB_INDEPENDENT,
    val tagLocalization: Boolean = false,
    val disclaimerAccepted: Boolean = false,
    val autoClearCacheMode: String = "none", // "none", "every_3_days", "on_exit"
    val lastCacheClearTime: Long = 0L,
    val liquidGlassMode: String = "ultimate" // "none", "standard", "ultimate"
)

class UserPreferencesManager(private val context: Context) {

    private object PreferencesKeys {
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val DARK_MODE = stringPreferencesKey("dark_mode")
        val THEME_COLOR = stringPreferencesKey("theme_color")
        val COLOR_STYLE = stringPreferencesKey("color_style")
        val AMOLED_BLACK = booleanPreferencesKey("amoled_black")
        val HAPTIC_FEEDBACK = booleanPreferencesKey("haptic_feedback")
        val CATEGORIES = stringPreferencesKey("categories")
        val PURITY = stringPreferencesKey("purity")
        val API_KEY = stringPreferencesKey("api_key")
        val WEBDAV_URL = stringPreferencesKey("webdav_url")
        val WEBDAV_USERNAME = stringPreferencesKey("webdav_username")
        val WEBDAV_PASSWORD = stringPreferencesKey("webdav_password")
        val WEBDAV_PATH = stringPreferencesKey("webdav_path")
        val WEBDAV_AUTO_BACKUP = booleanPreferencesKey("webdav_auto_backup")
        val WEBDAV_LAST_BACKUP_TIME = longPreferencesKey("webdav_last_backup_time")
        val WEBDAV_LAST_STATUS = stringPreferencesKey("webdav_last_status")
        val HIDE_NAV_ON_SCROLL = booleanPreferencesKey("hide_nav_on_scroll")
        val USE_FLOATING_CAPSULE_NAV = booleanPreferencesKey("use_floating_capsule_nav")
        val DOWNLOAD_DIRECTORY = stringPreferencesKey("download_directory")
        val CONCURRENT_DOWNLOADS = androidx.datastore.preferences.core.intPreferencesKey("concurrent_downloads")
        val SHOW_FAVORITES_COUNT = booleanPreferencesKey("show_favorites_count")
        val FILTER_MODE = stringPreferencesKey("filter_mode")
        val TAG_LOCALIZATION = booleanPreferencesKey("tag_localization")
        val DISCLAIMER_ACCEPTED = booleanPreferencesKey("disclaimer_accepted")
        val AUTO_CLEAR_CACHE_MODE = stringPreferencesKey("auto_clear_cache_mode")
        val LAST_CACHE_CLEAR_TIME = longPreferencesKey("last_cache_clear_time")
        val LIQUID_GLASS_MODE = stringPreferencesKey("liquid_glass_mode")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        val rawFilterMode = prefs[PreferencesKeys.FILTER_MODE]
        val mode = if (rawFilterMode != null) {
            try {
                FilterMode.valueOf(rawFilterMode)
            } catch (_: Exception) {
                FilterMode.TAB_INDEPENDENT
            }
        } else {
            FilterMode.TAB_INDEPENDENT
        }

        val currentApiKey = prefs[PreferencesKeys.API_KEY] ?: ""
        val hasApiKey = currentApiKey.isNotBlank()
        val rawPurity = prefs[PreferencesKeys.PURITY]
        val effectivePurity = when {
            rawPurity == null || rawPurity == "100" -> if (hasApiKey) "111" else "110"
            !hasApiKey && rawPurity.length >= 3 && rawPurity[2] == '1' -> rawPurity.substring(0, 2) + "0"
            else -> rawPurity
        }

        AppSettings(
            dynamicColor = prefs[PreferencesKeys.DYNAMIC_COLOR] ?: true,
            darkMode = prefs[PreferencesKeys.DARK_MODE] ?: "system",
            themeColor = prefs[PreferencesKeys.THEME_COLOR] ?: "orange",
            colorStyle = prefs[PreferencesKeys.COLOR_STYLE] ?: "vibrant",
            amoledBlack = prefs[PreferencesKeys.AMOLED_BLACK] ?: false,
            hapticFeedback = prefs[PreferencesKeys.HAPTIC_FEEDBACK] ?: true,
            categories = prefs[PreferencesKeys.CATEGORIES] ?: "111",
            purity = effectivePurity,
            apiKey = currentApiKey,
            webDavUrl = prefs[PreferencesKeys.WEBDAV_URL] ?: "",
            webDavUsername = prefs[PreferencesKeys.WEBDAV_USERNAME] ?: "",
            webDavPassword = prefs[PreferencesKeys.WEBDAV_PASSWORD] ?: "",
            webDavPath = prefs[PreferencesKeys.WEBDAV_PATH] ?: "/wallhaven_backup/",
            webDavAutoBackup = prefs[PreferencesKeys.WEBDAV_AUTO_BACKUP] ?: false,
            webDavLastBackupTime = prefs[PreferencesKeys.WEBDAV_LAST_BACKUP_TIME] ?: 0L,
            webDavLastStatus = prefs[PreferencesKeys.WEBDAV_LAST_STATUS] ?: "",
            hideNavOnScroll = prefs[PreferencesKeys.HIDE_NAV_ON_SCROLL] ?: false,
            useFloatingCapsuleNav = prefs[PreferencesKeys.USE_FLOATING_CAPSULE_NAV] ?: true,
            downloadDirectory = prefs[PreferencesKeys.DOWNLOAD_DIRECTORY] ?: "/storage/emulated/0/Download/WallHaven",
            concurrentDownloads = prefs[PreferencesKeys.CONCURRENT_DOWNLOADS] ?: 3,
            showFavoritesCount = prefs[PreferencesKeys.SHOW_FAVORITES_COUNT] ?: true,
            filterMode = mode,
            tagLocalization = prefs[PreferencesKeys.TAG_LOCALIZATION] ?: false,
            disclaimerAccepted = prefs[PreferencesKeys.DISCLAIMER_ACCEPTED] ?: false,
            autoClearCacheMode = prefs[PreferencesKeys.AUTO_CLEAR_CACHE_MODE] ?: "none",
            lastCacheClearTime = prefs[PreferencesKeys.LAST_CACHE_CLEAR_TIME] ?: 0L,
            liquidGlassMode = when (prefs[PreferencesKeys.LIQUID_GLASS_MODE]) {
                "none", "standard", "ultimate" -> prefs[PreferencesKeys.LIQUID_GLASS_MODE]!!
                else -> "ultimate"
            }
        )
    }

    suspend fun getSettings(): AppSettings = settingsFlow.first()

    suspend fun setDisclaimerAccepted(accepted: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.DISCLAIMER_ACCEPTED] = accepted }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.DYNAMIC_COLOR] = enabled }
    }

    suspend fun setDarkMode(mode: String) {
        context.dataStore.edit { it[PreferencesKeys.DARK_MODE] = mode }
    }

    suspend fun setThemeColor(color: String) {
        context.dataStore.edit { it[PreferencesKeys.THEME_COLOR] = color }
    }

    suspend fun setColorStyle(style: String) {
        context.dataStore.edit { it[PreferencesKeys.COLOR_STYLE] = style }
    }

    suspend fun setAmoledBlack(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.AMOLED_BLACK] = enabled }
    }

    suspend fun setHapticFeedback(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.HAPTIC_FEEDBACK] = enabled }
    }

    suspend fun setCategories(categories: String) {
        context.dataStore.edit { it[PreferencesKeys.CATEGORIES] = categories }
    }

    suspend fun setPurity(purity: String) {
        context.dataStore.edit { it[PreferencesKeys.PURITY] = purity }
    }

    suspend fun setApiKey(key: String) {
        context.dataStore.edit { it[PreferencesKeys.API_KEY] = key.trim() }
    }

    suspend fun setWebDavConfig(
        url: String,
        user: String,
        pass: String,
        path: String,
        autoBackup: Boolean
    ) {
        context.dataStore.edit {
            it[PreferencesKeys.WEBDAV_URL] = url.trim()
            it[PreferencesKeys.WEBDAV_USERNAME] = user.trim()
            it[PreferencesKeys.WEBDAV_PASSWORD] = pass
            it[PreferencesKeys.WEBDAV_PATH] = if (path.isBlank()) "/wallhaven_backup/" else path.trim()
            it[PreferencesKeys.WEBDAV_AUTO_BACKUP] = autoBackup
        }
    }

    suspend fun setWebDavAutoBackup(enabled: Boolean) {
        context.dataStore.edit {
            it[PreferencesKeys.WEBDAV_AUTO_BACKUP] = enabled
        }
    }

    suspend fun updateWebDavBackupStatus(timestamp: Long, status: String) {
        context.dataStore.edit {
            it[PreferencesKeys.WEBDAV_LAST_BACKUP_TIME] = timestamp
            it[PreferencesKeys.WEBDAV_LAST_STATUS] = status
        }
    }

    suspend fun setHideNavOnScroll(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.HIDE_NAV_ON_SCROLL] = enabled }
    }

    suspend fun setUseFloatingCapsuleNav(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.USE_FLOATING_CAPSULE_NAV] = enabled }
    }

    suspend fun setDownloadDirectory(directory: String) {
        context.dataStore.edit { it[PreferencesKeys.DOWNLOAD_DIRECTORY] = directory.trim() }
    }

    suspend fun setConcurrentDownloads(count: Int) {
        context.dataStore.edit { it[PreferencesKeys.CONCURRENT_DOWNLOADS] = count.coerceIn(1, 6) }
    }

    suspend fun setShowFavoritesCount(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.SHOW_FAVORITES_COUNT] = enabled }
    }

    suspend fun setFilterMode(mode: FilterMode) {
        context.dataStore.edit { it[PreferencesKeys.FILTER_MODE] = mode.name }
    }

    suspend fun setTagLocalization(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.TAG_LOCALIZATION] = enabled }
    }

    suspend fun setAutoClearCacheMode(mode: String) {
        context.dataStore.edit { it[PreferencesKeys.AUTO_CLEAR_CACHE_MODE] = mode }
    }

    suspend fun setLastCacheClearTime(timestamp: Long) {
        context.dataStore.edit { it[PreferencesKeys.LAST_CACHE_CLEAR_TIME] = timestamp }
    }

    suspend fun setLiquidGlassMode(mode: String) {
        val normalized = if (mode in listOf("none", "standard", "ultimate")) mode else "ultimate"
        context.dataStore.edit { it[PreferencesKeys.LIQUID_GLASS_MODE] = normalized }
    }
}
