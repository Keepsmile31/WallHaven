package com.example

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.example.data.api.GitHubApiService
import com.example.data.api.GitHubReleaseAssetDto
import com.example.data.api.GitHubReleaseDto
import com.example.data.preferences.AppSettings
import com.example.data.preferences.UpdatePrefs
import com.example.data.repository.AppUpdateRepository
import com.example.data.repository.UpdateCheckResult
import com.example.ui.MainViewModel
import com.example.ui.screens.SettingsAboutScreen
import com.example.ui.theme.WallhavenTheme
import com.example.util.HapticHelper
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.net.SocketTimeoutException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp")
class GitHubUpdateTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("wallhaven_update_prefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun `UpdatePrefs throttles cold start to once per day and resumes next day`() {
        var currentDay = "2026-11-16"
        val prefs = UpdatePrefs(context, todayProvider = { currentDay })

        assertTrue(prefs.shouldAutoCheckOnColdStart())
        assertNull(prefs.getLastCheckDate())

        prefs.markCheckedToday()
        assertEquals("2026-11-16", prefs.getLastCheckDate())
        assertFalse(prefs.shouldAutoCheckOnColdStart())

        // Tomorrow resumes cold-start check
        currentDay = "2026-11-17"
        assertTrue(prefs.shouldAutoCheckOnColdStart())
    }

    @Test
    fun `UpdatePrefs version comparator and smart ignore auto-resume on newer version`() {
        assertTrue(UpdatePrefs.isNewerVersion("v2.6.2", "2.6.1"))
        assertTrue(UpdatePrefs.isNewerVersion("2.6.10", "2.6.2"))
        assertFalse(UpdatePrefs.isNewerVersion("v2.6.1", "2.6.1"))
        assertFalse(UpdatePrefs.isNewerVersion("2.6.0", "2.6.1"))

        val prefs = UpdatePrefs(context, todayProvider = { "2026-11-16" })
        prefs.setIgnoredVersion("v2.6.2")

        assertTrue(prefs.isVersionIgnored("v2.6.2"))
        assertTrue(prefs.isVersionIgnored("2.6.2"))
        // Newer release automatically un-mutes prompt
        assertFalse(prefs.isVersionIgnored("v2.6.3"))
    }

    @Test
    fun `AppUpdateRepository enforces cold-start throttle and smart ignore while manual check always queries`() = runBlocking {
        var currentDay = "2026-11-16"
        var remoteTag = "v2.6.2"
        var callCount = 0

        val fakeApi = object : GitHubApiService {
            override suspend fun getLatestRelease(): GitHubReleaseDto {
                callCount++
                return GitHubReleaseDto(
                    tagName = remoteTag,
                    name = "WallHaven $remoteTag",
                    body = "- Release notes for $remoteTag",
                    htmlUrl = "https://github.com/Keepsmile31/WallHaven/releases/tag/$remoteTag",
                    assets = listOf(
                        GitHubReleaseAssetDto(
                            name = "WallHaven-$remoteTag-arm64-v8a.apk",
                            browserDownloadUrl = "https://github.com/Keepsmile31/WallHaven/releases/download/$remoteTag/app.apk",
                            size = 15_728_640L
                        )
                    )
                )
            }
        }

        val prefs = UpdatePrefs(context, todayProvider = { currentDay })
        val repo = AppUpdateRepository(
            updatePrefs = prefs,
            apiService = fakeApi,
            currentVersionProvider = { "2.6.1" },
            ioDispatcher = Dispatchers.Unconfined
        )

        // First cold start today queries API and returns update
        val first = repo.checkForUpdate(isColdStart = true)
        assertTrue(first is UpdateCheckResult.UpdateAvailable)
        assertEquals(1, callCount)
        assertEquals(
            "https://github.com/Keepsmile31/WallHaven/releases/download/v2.6.2/app.apk",
            (first as UpdateCheckResult.UpdateAvailable).info.apkDownloadUrl
        )

        // Second cold start on same day is skipped without network call
        val secondSameDay = repo.checkForUpdate(isColdStart = true)
        assertNull(secondSameDay)
        assertEquals(1, callCount)

        // User chooses "Don't remind for this version"
        repo.ignoreVersion("v2.6.2")

        // Next day cold start calls API, sees v2.6.2 is still ignored -> returns null
        currentDay = "2026-11-17"
        val nextDaySameTag = repo.checkForUpdate(isColdStart = true)
        assertNull(nextDaySameTag)
        assertEquals(2, callCount)

        // Manual check ignores both throttle & muted version
        val manual = repo.checkForUpdate(isColdStart = false)
        assertTrue(manual is UpdateCheckResult.UpdateAvailable)
        assertEquals(3, callCount)

        // Following day a newer version v2.6.3 ships -> cold start automatically resumes!
        currentDay = "2026-11-18"
        remoteTag = "v2.6.3"
        val newerRelease = repo.checkForUpdate(isColdStart = true)
        assertTrue(newerRelease is UpdateCheckResult.UpdateAvailable)
        assertEquals("v2.6.3", (newerRelease as UpdateCheckResult.UpdateAvailable).info.tagName)
        assertEquals(4, callCount)
    }

    @Test
    fun `SettingsAboutScreen Check Updates shows GitHub release dialog and Telegram opens t_me link`() {
        var currentDay = "2026-11-16"
        val prefs = UpdatePrefs(context, todayProvider = { currentDay })
        val fakeApi = object : GitHubApiService {
            override suspend fun getLatestRelease(): GitHubReleaseDto {
                return GitHubReleaseDto(
                    tagName = "v2.6.2",
                    name = "Release v2.6.2",
                    body = "Optimized GitHub release updater",
                    htmlUrl = "https://github.com/Keepsmile31/WallHaven/releases/tag/v2.6.2",
                    assets = listOf(
                        GitHubReleaseAssetDto(
                            name = "WallHaven-2.6.2.apk",
                            browserDownloadUrl = "https://github.com/Keepsmile31/WallHaven/releases/download/v2.6.2/WallHaven-2.6.2.apk",
                            size = 12_000_000L
                        )
                    )
                )
            }
        }
        val repo = AppUpdateRepository(
            updatePrefs = prefs,
            apiService = fakeApi,
            currentVersionProvider = { "2.6.1" },
            ioDispatcher = Dispatchers.Unconfined
        )

        composeTestRule.setContent {
            WallhavenTheme {
                SettingsAboutScreen(
                    settings = AppSettings(
                        disclaimerAccepted = true,
                        disclaimerAcceptedVersion = 2,
                        disclaimerAcceptedAt = 1763200000000L
                    ),
                    hapticHelper = HapticHelper(context),
                    appUpdateRepository = repo,
                    onClearHistory = null,
                    onClearDownloads = null,
                    onBack = {}
                )
            }
        }

        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.waitForIdle()

        // Tap Check Updates -> opens AppUpdateDialog with all 3 actions
        composeTestRule.onNodeWithTag("check_update_button").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("发现新版本 v2.6.2").assertIsDisplayed()
        composeTestRule.onNodeWithTag("app_update_release_notes").assertIsDisplayed()
        composeTestRule.onNodeWithTag("update_now_btn").assertIsDisplayed()
        composeTestRule.onNodeWithTag("update_later_btn").assertIsDisplayed()
        composeTestRule.onNodeWithTag("update_ignore_btn").assertIsDisplayed()

        // Tap "不再提示" -> dismisses and persists ignored version
        composeTestRule.onNodeWithTag("update_ignore_btn").performClick()
        composeTestRule.waitForIdle()
        assertEquals("v2.6.2", prefs.getIgnoredVersion())

        // Tap Telegram Channel row -> dispatches ACTION_VIEW for https://t.me/WallHaven8
        composeTestRule.onNodeWithTag("settings_telegram_channel_row").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        val launchedIntent = shadowOf(ApplicationProvider.getApplicationContext<Application>()).nextStartedActivity
        assertEquals(Intent.ACTION_VIEW, launchedIntent?.action)
        assertEquals("https://t.me/WallHaven8", launchedIntent?.dataString)

        // Tap 123Pan Version Update button -> dispatches ACTION_VIEW for 123Pan update/history link
        composeTestRule.onNodeWithTag("update_123pan_button").performScrollTo().assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()
        val updatePanIntent = shadowOf(ApplicationProvider.getApplicationContext<Application>()).nextStartedActivity
        assertEquals(Intent.ACTION_VIEW, updatePanIntent?.action)
        assertEquals("https://1815318986.share.123pan.cn/123pan/ngLtVv-8xfm3", updatePanIntent?.dataString)
    }

    @Test
    fun `MainViewModel checkForUpdatesOnColdStart runs once per session and populates pendingUpdateInfo`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = MainViewModel(app)
        var callCount = 0
        val fakeApi = object : GitHubApiService {
            override suspend fun getLatestRelease(): GitHubReleaseDto {
                callCount++
                return GitHubReleaseDto(
                    tagName = "v2.7.0",
                    name = null,
                    body = null,
                    htmlUrl = "https://github.com/Keepsmile31/WallHaven/releases/tag/v2.7.0"
                )
            }
        }
        val prefs = UpdatePrefs(context, todayProvider = { "2026-11-16" })
        val repo = AppUpdateRepository(
            updatePrefs = prefs,
            apiService = fakeApi,
            currentVersionProvider = { "2.6.2" },
            ioDispatcher = Dispatchers.Unconfined
        )

        // Trigger multiple times to simulate recomposition/rotation/tab switching
        viewModel.checkForUpdatesOnColdStart(repo)
        viewModel.checkForUpdatesOnColdStart(repo)
        viewModel.checkForUpdatesOnColdStart(repo)
        shadowOf(android.os.Looper.getMainLooper()).idle()

        assertEquals(1, callCount)
        val pending = viewModel.pendingUpdateInfo.value
        assertNotNull(pending)
        assertEquals("v2.7.0", pending?.tagName)
        assertEquals("本次更新包含功能优化与稳定性改进。", pending?.releaseNotes)

        viewModel.dismissUpdateLater()
        assertNull(viewModel.pendingUpdateInfo.value)
    }

    @Test
    fun `MainViewModel checkForUpdatesOnColdStart fails silently on timeout or up-to-date`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val timeoutViewModel = MainViewModel(app)
        val timeoutApi = object : GitHubApiService {
            override suspend fun getLatestRelease(): GitHubReleaseDto {
                throw SocketTimeoutException("timeout")
            }
        }
        val prefs = UpdatePrefs(context, todayProvider = { "2026-11-16" })
        val timeoutRepo = AppUpdateRepository(
            updatePrefs = prefs,
            apiService = timeoutApi,
            currentVersionProvider = { "2.6.2" },
            ioDispatcher = Dispatchers.Unconfined
        )

        timeoutViewModel.checkForUpdatesOnColdStart(timeoutRepo)
        shadowOf(android.os.Looper.getMainLooper()).idle()
        assertNull(timeoutViewModel.pendingUpdateInfo.value)

        // Failed cold-start check should NOT mark today as checked so next launch can retry
        assertTrue(prefs.shouldAutoCheckOnColdStart())

        val upToDateViewModel = MainViewModel(app)
        val upToDateApi = object : GitHubApiService {
            override suspend fun getLatestRelease(): GitHubReleaseDto {
                return GitHubReleaseDto(tagName = "v2.6.2")
            }
        }
        val upToDateRepo = AppUpdateRepository(
            updatePrefs = prefs,
            apiService = upToDateApi,
            currentVersionProvider = { "2.6.2" },
            ioDispatcher = Dispatchers.Unconfined
        )
        upToDateViewModel.checkForUpdatesOnColdStart(upToDateRepo)
        shadowOf(android.os.Looper.getMainLooper()).idle()
        assertNull(upToDateViewModel.pendingUpdateInfo.value)
    }

    @Test
    fun `GitHubReleaseDto deserializes JSON with null name, body, and published_at safely`() {
        val moshi = Moshi.Builder().build()
        val adapter = moshi.adapter(GitHubReleaseDto::class.java)
        val json = """
            {
              "tag_name": "v2.7.0",
              "name": null,
              "body": null,
              "html_url": "https://github.com/Keepsmile31/WallHaven/releases/tag/v2.7.0",
              "published_at": null,
              "prerelease": false,
              "draft": false,
              "assets": []
            }
        """.trimIndent()
        val dto = adapter.fromJson(json)
        assertNotNull(dto)
        assertEquals("v2.7.0", dto?.tagName)
        assertNull(dto?.name)
        assertNull(dto?.body)
    }
}
