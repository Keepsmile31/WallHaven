package com.example.data.repository

import androidx.compose.runtime.Immutable
import com.example.BuildConfig
import com.example.data.api.GitHubApiClient
import com.example.data.api.GitHubApiService
import com.example.data.api.GitHubReleaseDto
import com.example.data.preferences.UpdatePrefs
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Immutable
data class AppUpdateInfo(
    val tagName: String,
    val versionName: String,
    val releaseTitle: String,
    val releaseNotes: String,
    val htmlUrl: String,
    val apkDownloadUrl: String,
    val apkFileName: String,
    val apkSizeBytes: Long,
    val publishedAt: String
)

sealed interface UpdateCheckResult {
    data class UpdateAvailable(val info: AppUpdateInfo) : UpdateCheckResult
    data class UpToDate(val currentVersion: String, val latestTag: String) : UpdateCheckResult
    data class Error(val message: String) : UpdateCheckResult
}

class AppUpdateRepository(
    private val updatePrefs: UpdatePrefs,
    private val apiService: GitHubApiService = GitHubApiClient.service,
    private val currentVersionProvider: () -> String = { BuildConfig.VERSION_NAME },
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    /**
     * Checks GitHub Releases for a newer release.
     *
     * When [isColdStart] is true:
     * - Enforces the once-per-day throttle via [UpdatePrefs.shouldAutoCheckOnColdStart].
     * - Respects [UpdatePrefs.isVersionIgnored] so muted versions are skipped until a newer release ships.
     * - Returns null if throttled, ignored, or failed silently.
     *
     * When [isColdStart] is false (manual check):
     * - Always queries GitHub Releases regardless of daily throttle or ignored version tag.
     */
    suspend fun checkForUpdate(isColdStart: Boolean): UpdateCheckResult? {
        if (isColdStart && !updatePrefs.shouldAutoCheckOnColdStart()) {
            return null
        }

        return withContext(ioDispatcher) {
            try {
                val release = apiService.getLatestRelease()
                if (isColdStart) {
                    updatePrefs.markCheckedToday()
                }
                if (release.draft || release.prerelease || release.tagName.isBlank()) {
                    return@withContext if (isColdStart) null else UpdateCheckResult.UpToDate(
                        currentVersion = currentVersionProvider(),
                        latestTag = currentVersionProvider()
                    )
                }

                val currentVersion = currentVersionProvider()
                if (!UpdatePrefs.isNewerVersion(release.tagName, currentVersion)) {
                    return@withContext UpdateCheckResult.UpToDate(
                        currentVersion = currentVersion,
                        latestTag = release.tagName
                    )
                }

                if (isColdStart && updatePrefs.isVersionIgnored(release.tagName)) {
                    return@withContext null
                }

                UpdateCheckResult.UpdateAvailable(release.toAppUpdateInfo())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (isColdStart) {
                    null
                } else {
                    UpdateCheckResult.Error(e.localizedMessage ?: "无法连接 GitHub 发布源")
                }
            }
        }
    }

    fun remindLaterToday() {
        updatePrefs.markCheckedToday()
    }

    fun ignoreVersion(tagName: String) {
        updatePrefs.setIgnoredVersion(tagName)
        updatePrefs.markCheckedToday()
    }

    companion object {
        internal fun GitHubReleaseDto.toAppUpdateInfo(): AppUpdateInfo {
            val apkAsset = assets.firstOrNull { asset ->
                asset.name.endsWith(".apk", ignoreCase = true) && asset.browserDownloadUrl.isNotBlank()
            }
            val resolvedDownloadUrl = apkAsset?.browserDownloadUrl?.takeIf { it.isNotBlank() }
                ?: htmlUrl.ifBlank { "https://github.com/Keepsmile31/WallHaven/releases" }
            val normalizedVer = UpdatePrefs.normalizeVersionTag(tagName)
            val safeTitle = name.orEmpty().trim()
            val safeBody = body.orEmpty().trim()
            return AppUpdateInfo(
                tagName = tagName.trim(),
                versionName = normalizedVer.ifBlank { tagName.trim() },
                releaseTitle = safeTitle.ifBlank { tagName.trim() },
                releaseNotes = safeBody.ifBlank { "本次更新包含功能优化与稳定性改进。" },
                htmlUrl = htmlUrl.ifBlank { "https://github.com/Keepsmile31/WallHaven/releases" },
                apkDownloadUrl = resolvedDownloadUrl,
                apkFileName = apkAsset?.name.orEmpty(),
                apkSizeBytes = apkAsset?.size ?: 0L,
                publishedAt = publishedAt.orEmpty()
            )
        }
    }
}
