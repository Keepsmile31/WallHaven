package com.example.data.backup

import android.content.Context
import com.example.BuildConfig
import com.example.data.db.AppDatabase
import com.example.data.db.DownloadEntity
import com.example.data.db.FavoriteWallpaperEntity
import com.example.data.db.HistoryEntity
import com.example.data.db.SearchHistoryEntity
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
    val categories: String = "111",
    val purity: String = "110",
    val apiKey: String? = null,
    val webDavUrl: String? = null,
    val webDavUsername: String? = null,
    val webDavPassword: String? = null,
    val webDavPath: String? = null,
    val webDavAutoBackup: Boolean? = null,
    val hideNavOnScroll: Boolean = false,
    val useFloatingCapsuleNav: Boolean = true,
    val downloadDirectory: String? = null,
    val concurrentDownloads: Int? = null,
    val showFavoritesCount: Boolean = true,
    val filterMode: String = "TAB_INDEPENDENT",
    val tagLocalization: Boolean? = null,
    val autoClearCacheMode: String? = null,
    val liquidGlassMode: String = "ultimate"
)

@JsonClass(generateAdapter = true)
data class BackupPayload(
    val version: Int = CURRENT_BACKUP_VERSION,
    val timestamp: Long = System.currentTimeMillis(),
    val appVersion: String = BuildConfig.VERSION_NAME,
    val settings: BackupSettings,
    val favorites: List<FavoriteWallpaperEntity> = emptyList(),
    val browseHistory: List<HistoryEntity> = emptyList(),
    val downloadHistory: List<DownloadEntity> = emptyList(),
    val searchHistory: List<SearchHistoryEntity> = emptyList()
) {
    companion object {
        const val CURRENT_BACKUP_VERSION = 2
    }
}

data class BackupRestoreSummary(
    val favoritesCount: Int,
    val historyCount: Int,
    val downloadsCount: Int,
    val searchHistoryCount: Int = 0,
    val timestamp: Long
)

class BackupManager(
    private val context: Context,
    private val preferencesManager: UserPreferencesManager,
    private val database: AppDatabase,
    private val webDavClient: WebDavClient = WebDavClient()
) {
    private val moshi: Moshi = Moshi.Builder().build()
    private val payloadAdapter = moshi.adapter(BackupPayload::class.java).indent("  ")

    private suspend fun mergeSearchHistory(imported: List<SearchHistoryEntity>): Int {
        val cleaned = imported
            .mapNotNull { entity ->
                val q = entity.query.trim()
                if (q.isEmpty()) null else entity.copy(query = q)
            }
        if (cleaned.isEmpty()) return 0

        val existingMap = database.searchHistoryDao()
            .getAllSearchesList()
            .associateBy { it.query }
        val merged = cleaned
            .groupBy { it.query }
            .map { (query, entries) ->
                val maxImportedTime = entries.maxOf { it.searchedAt }
                val existingTime = existingMap[query]?.searchedAt ?: 0L
                SearchHistoryEntity(
                    query = query,
                    searchedAt = maxOf(existingTime, maxImportedTime)
                )
            }
        database.searchHistoryDao().insertAllSearches(merged)
        return merged.size
    }

    suspend fun createExportJson(): String = withContext(Dispatchers.IO) {
        val currentSettings = preferencesManager.getSettings()
        val favorites = database.favoriteDao().getAllFavoritesList()
        val history = database.historyDao().getAllHistoryList()
        val downloads = database.downloadDao().getAllDownloadsList()
        val searchHistory = database.searchHistoryDao().getAllSearchesList()

        val payload = BackupPayload(
            version = BackupPayload.CURRENT_BACKUP_VERSION,
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
                webDavUrl = currentSettings.webDavUrl,
                webDavUsername = currentSettings.webDavUsername,
                webDavPassword = currentSettings.webDavPassword,
                webDavPath = currentSettings.webDavPath,
                webDavAutoBackup = currentSettings.webDavAutoBackup,
                hideNavOnScroll = currentSettings.hideNavOnScroll,
                useFloatingCapsuleNav = currentSettings.useFloatingCapsuleNav,
                downloadDirectory = currentSettings.downloadDirectory,
                concurrentDownloads = currentSettings.concurrentDownloads,
                showFavoritesCount = currentSettings.showFavoritesCount,
                filterMode = currentSettings.filterMode.name,
                tagLocalization = currentSettings.tagLocalization,
                autoClearCacheMode = currentSettings.autoClearCacheMode,
                liquidGlassMode = currentSettings.liquidGlassMode
            ),
            favorites = favorites,
            browseHistory = history,
            downloadHistory = downloads,
            searchHistory = searchHistory
        )
        payloadAdapter.toJson(payload)
    }

    suspend fun restoreFromJson(json: String): Result<BackupRestoreSummary> =
        restoreFromJsonInternal(json = json, preserveConfiguredWebDavIfBlank = false)

    private suspend fun restoreFromJsonInternal(
        json: String,
        preserveConfiguredWebDavIfBlank: Boolean
    ): Result<BackupRestoreSummary> = withContext(Dispatchers.IO) {
        try {
            val payload = payloadAdapter.fromJson(json)
                ?: return@withContext Result.failure(IllegalArgumentException("解析备份文件失败，格式不正确"))

            // Restore settings atomically
            payload.settings.let { s ->
                val resolvedFilterMode = try {
                    com.example.data.preferences.FilterMode.valueOf(s.filterMode)
                } catch (_: Exception) {
                    null
                }
                val resolvedApiKey = when {
                    s.apiKey == null -> null
                    payload.version < 2 && s.apiKey.isBlank() -> null
                    else -> s.apiKey
                }
                val skipBlankWebDavCredentials =
                    preserveConfiguredWebDavIfBlank && s.webDavUrl.isNullOrBlank()

                preferencesManager.restoreSettings(
                    dynamicColor = s.dynamicColor,
                    darkMode = s.darkMode,
                    themeColor = s.themeColor,
                    colorStyle = s.colorStyle,
                    amoledBlack = s.amoledBlack,
                    hapticFeedback = s.hapticFeedback,
                    categories = s.categories,
                    purity = s.purity,
                    apiKey = resolvedApiKey,
                    webDavUrl = if (skipBlankWebDavCredentials) null else s.webDavUrl,
                    webDavUsername = if (skipBlankWebDavCredentials) null else s.webDavUsername,
                    webDavPassword = if (skipBlankWebDavCredentials) null else s.webDavPassword,
                    webDavPath = s.webDavPath,
                    webDavAutoBackup = s.webDavAutoBackup,
                    hideNavOnScroll = s.hideNavOnScroll,
                    useFloatingCapsuleNav = s.useFloatingCapsuleNav,
                    downloadDirectory = s.downloadDirectory,
                    concurrentDownloads = s.concurrentDownloads,
                    showFavoritesCount = s.showFavoritesCount,
                    filterMode = resolvedFilterMode,
                    tagLocalization = s.tagLocalization,
                    autoClearCacheMode = s.autoClearCacheMode,
                    liquidGlassMode = s.liquidGlassMode
                )
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
            val mergedSearchCount = mergeSearchHistory(payload.searchHistory)

            Result.success(
                BackupRestoreSummary(
                    favoritesCount = payload.favorites.size,
                    historyCount = payload.browseHistory.size,
                    downloadsCount = payload.downloadHistory.size,
                    searchHistoryCount = mergedSearchCount,
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
                restoreFromJsonInternal(json = json, preserveConfiguredWebDavIfBlank = true)
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
