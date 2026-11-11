package com.example.data.backup

import android.content.Context
import com.example.data.db.AppDatabase
import com.example.data.db.DownloadEntity
import com.example.data.db.FavoriteWallpaperEntity
import com.example.data.db.HistoryEntity
import com.example.data.preferences.UserPreferencesManager
import com.example.data.webdav.WebDavClient
import com.example.util.logError
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@JsonClass(generateAdapter = true)
data class BackupSettings(
    val dynamicColor: Boolean = true,
    val darkMode: String = "system",
    val themeColor: String = "orange",
    val colorStyle: String = "vibrant",
    val amoledBlack: Boolean = false,
    val hapticFeedback: Boolean = true,
    val categories: String = "110",
    val purity: String = "100",
    val apiKey: String = "",
    val hideNavOnScroll: Boolean = false,
    val useFloatingCapsuleNav: Boolean = true,
    val showFavoritesCount: Boolean = true,
    val filterMode: String = "TAB_INDEPENDENT",
    val liquidGlassMode: String = "ultimate"
)

@JsonClass(generateAdapter = true)
data class BackupPayload(
    val version: Int = 1,
    val timestamp: Long = System.currentTimeMillis(),
    val appVersion: String = "1.0.0",
    val settings: BackupSettings,
    val favorites: List<FavoriteWallpaperEntity> = emptyList(),
    val browseHistory: List<HistoryEntity> = emptyList(),
    val downloadHistory: List<DownloadEntity> = emptyList()
)

data class BackupRestoreSummary(
    val favoritesCount: Int,
    val historyCount: Int,
    val downloadsCount: Int,
    val timestamp: Long
)

class BackupManager(
    private val context: Context,
    private val preferencesManager: UserPreferencesManager,
    private val database: AppDatabase
) {
    private val moshi: Moshi = Moshi.Builder().build()
    private val payloadAdapter = moshi.adapter(BackupPayload::class.java).indent("  ")
    private val webDavClient = WebDavClient()

    suspend fun createExportJson(): String = withContext(Dispatchers.IO) {
        val currentSettings = preferencesManager.getSettings()
        val favorites = database.favoriteDao().getAllFavoritesList()
        val history = database.historyDao().getAllHistoryList()
        val downloads = database.downloadDao().getAllDownloadsList()

        val payload = BackupPayload(
            timestamp = System.currentTimeMillis(),
            settings = BackupSettings(
                dynamicColor = currentSettings.dynamicColor,
                darkMode = currentSettings.darkMode,
                themeColor = currentSettings.themeColor,
                colorStyle = currentSettings.colorStyle,
                amoledBlack = currentSettings.amoledBlack,
                hapticFeedback = currentSettings.hapticFeedback,
                categories = currentSettings.categories,
                purity = currentSettings.purity,
                apiKey = currentSettings.apiKey,
                hideNavOnScroll = currentSettings.hideNavOnScroll,
                useFloatingCapsuleNav = currentSettings.useFloatingCapsuleNav,
                showFavoritesCount = currentSettings.showFavoritesCount,
                filterMode = currentSettings.filterMode.name,
                liquidGlassMode = currentSettings.liquidGlassMode
            ),
            favorites = favorites,
            browseHistory = history,
            downloadHistory = downloads
        )
        payloadAdapter.toJson(payload)
    }

    suspend fun restoreFromJson(json: String): Result<BackupRestoreSummary> = withContext(Dispatchers.IO) {
        try {
            val payload = payloadAdapter.fromJson(json)
                ?: return@withContext Result.failure(IllegalArgumentException("解析备份文件失败，格式不正确"))

            // Restore settings
            payload.settings.let { s ->
                preferencesManager.setDynamicColor(s.dynamicColor)
                preferencesManager.setDarkMode(s.darkMode)
                preferencesManager.setThemeColor(s.themeColor)
                preferencesManager.setColorStyle(s.colorStyle)
                preferencesManager.setAmoledBlack(s.amoledBlack)
                preferencesManager.setHapticFeedback(s.hapticFeedback)
                preferencesManager.setCategories(s.categories)
                preferencesManager.setPurity(s.purity)
                preferencesManager.setHideNavOnScroll(s.hideNavOnScroll)
                preferencesManager.setUseFloatingCapsuleNav(s.useFloatingCapsuleNav)
                preferencesManager.setShowFavoritesCount(s.showFavoritesCount)
                preferencesManager.setLiquidGlassMode(s.liquidGlassMode)
                try {
                    preferencesManager.setFilterMode(com.example.data.preferences.FilterMode.valueOf(s.filterMode))
                } catch (_: Exception) {}
                if (s.apiKey.isNotBlank()) {
                    preferencesManager.setApiKey(s.apiKey)
                }
            }

            // Restore database entities
            if (payload.favorites.isNotEmpty()) {
                database.favoriteDao().insertAllFavorites(payload.favorites)
            }
            if (payload.browseHistory.isNotEmpty()) {
                database.historyDao().insertAllHistory(payload.browseHistory)
            }
            if (payload.downloadHistory.isNotEmpty()) {
                database.downloadDao().insertAllDownloads(payload.downloadHistory)
            }

            Result.success(
                BackupRestoreSummary(
                    favoritesCount = payload.favorites.size,
                    historyCount = payload.browseHistory.size,
                    downloadsCount = payload.downloadHistory.size,
                    timestamp = payload.timestamp
                )
            )
        } catch (e: Exception) {
            logError(e, "BackupManager.restoreFromJson")
            Result.failure(e)
        }
    }

    suspend fun backupToWebDav(): Result<String> = withContext(Dispatchers.IO) {
        val settings = preferencesManager.getSettings()
        if (settings.webDavUrl.isBlank()) {
            return@withContext Result.failure(IllegalStateException("请先配置 WebDAV 服务器地址"))
        }

        try {
            val json = createExportJson()
            val fileName = "wallhaven_backup.json"
            val uploadRes = webDavClient.uploadFile(
                serverUrl = settings.webDavUrl,
                user = settings.webDavUsername,
                pass = settings.webDavPassword,
                folderPath = settings.webDavPath,
                fileName = fileName,
                content = json
            )

            if (uploadRes.isSuccess) {
                val now = System.currentTimeMillis()
                val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(now))
                preferencesManager.updateWebDavBackupStatus(now, "备份成功 ($dateStr)")
                Result.success("WebDAV 备份成功 ($dateStr)")
            } else {
                val errorMsg = uploadRes.exceptionOrNull()?.message ?: "未知上传错误"
                preferencesManager.updateWebDavBackupStatus(System.currentTimeMillis(), "备份失败: $errorMsg")
                Result.failure(uploadRes.exceptionOrNull() ?: Exception(errorMsg))
            }
        } catch (e: Exception) {
            logError(e, "BackupManager.backupToWebDav")
            preferencesManager.updateWebDavBackupStatus(System.currentTimeMillis(), "备份异常: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun restoreFromWebDav(): Result<BackupRestoreSummary> = withContext(Dispatchers.IO) {
        val settings = preferencesManager.getSettings()
        if (settings.webDavUrl.isBlank()) {
            return@withContext Result.failure(IllegalStateException("请先配置 WebDAV 服务器地址"))
        }

        try {
            val fileName = "wallhaven_backup.json"
            val downloadRes = webDavClient.downloadFile(
                serverUrl = settings.webDavUrl,
                user = settings.webDavUsername,
                pass = settings.webDavPassword,
                folderPath = settings.webDavPath,
                fileName = fileName
            )

            if (downloadRes.isSuccess) {
                val json = downloadRes.getOrThrow()
                restoreFromJson(json)
            } else {
                Result.failure(downloadRes.exceptionOrNull() ?: Exception("下载备份失败"))
            }
        } catch (e: Exception) {
            logError(e, "BackupManager.restoreFromWebDav")
            Result.failure(e)
        }
    }

    suspend fun checkAndRunDailyAutoBackup(): Result<Boolean> = withContext(Dispatchers.IO) {
        val settings = preferencesManager.getSettings()
        if (!settings.webDavAutoBackup || settings.webDavUrl.isBlank()) {
            return@withContext Result.success(false)
        }

        val oneDayMillis = 24 * 60 * 60 * 1000L
        val now = System.currentTimeMillis()
        if (now - settings.webDavLastBackupTime >= oneDayMillis) {
            val res = backupToWebDav()
            return@withContext Result.success(res.isSuccess)
        }
        Result.success(false)
    }
}
