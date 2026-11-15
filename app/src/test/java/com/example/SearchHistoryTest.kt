package com.example

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.backup.BackupManager
import com.example.data.db.AppDatabase
import com.example.data.db.FavoriteWallpaperEntity
import com.example.data.db.SearchHistoryEntity
import com.example.data.model.TagDto
import com.example.data.model.WallpaperDto
import com.example.data.preferences.AppSettings
import com.example.data.preferences.UserPreferencesManager
import com.example.data.repository.WallpaperRepository
import com.example.ui.MainViewModel
import com.example.ui.components.FilterState
import com.example.ui.components.WallpaperDetailSheet
import com.example.ui.screens.SearchHistoryScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.WallhavenTheme
import com.example.util.HapticHelper
import com.example.util.WallpaperSetHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SearchHistoryTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var repository: WallpaperRepository
    private lateinit var preferencesManager: UserPreferencesManager
    private lateinit var backupManager: BackupManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = WallpaperRepository(database)
        preferencesManager = UserPreferencesManager(context)
        backupManager = BackupManager(context, preferencesManager, database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `repository stores deduplicates batch deletes and clears full search history`() = runBlocking {
        repository.recordSearchQuery("   ")
        assertEquals(emptyList<SearchHistoryEntity>(), repository.getSearchHistory().first())

        for (i in 1..12) {
            database.searchHistoryDao().insertSearch(
                SearchHistoryEntity(query = "tag_$i", searchedAt = i * 100L)
            )
        }
        // Re-insert existing item with newer timestamp to verify deduplication and ordering
        database.searchHistoryDao().insertSearch(
            SearchHistoryEntity(query = "tag_2", searchedAt = 5000L)
        )

        val allEntries = repository.getSearchHistory().first()
        assertEquals(12, allEntries.size)
        assertEquals("tag_2", allEntries.first().query)

        repository.deleteSearchHistoryItem("tag_1")
        assertEquals(11, repository.getSearchHistory().first().size)

        repository.batchDeleteSearchHistory(listOf("tag_3", "tag_4", "tag_5"))
        val afterBatch = repository.getSearchHistory().first().map { it.query }
        assertEquals(8, afterBatch.size)
        assertFalse(afterBatch.contains("tag_3"))
        assertFalse(afterBatch.contains("tag_4"))
        assertFalse(afterBatch.contains("tag_5"))

        repository.clearSearchHistory()
        assertEquals(emptyList<SearchHistoryEntity>(), repository.getSearchHistory().first())
    }

    @Test
    fun `search screen displays up to 10 inline history entries plus all button and supports chinese input clear`() {
        var selectedQuery: String? = null
        var deletedQuery: String? = null
        var openedAll = false
        var clearedSearch = false
        var latestQuery = ""

        val history = (12 downTo 1).map { idx ->
            SearchHistoryEntity(query = "kw_$idx", searchedAt = idx * 100L)
        }

        composeTestRule.setContent {
            WallhavenTheme {
                SearchScreen(
                    searchQuery = latestQuery,
                    onQueryChange = { latestQuery = it },
                    onClearSearch = { clearedSearch = true },
                    onPerformSearch = {},
                    wallpapers = emptyList(),
                    isLoading = false,
                    isEndReached = false,
                    errorMessage = null,
                    favoriteIds = emptySet(),
                    filterState = FilterState(),
                    selectedColor = "",
                    hasApiKey = false,
                    onApplyFilter = {},
                    onResetFilter = {},
                    onRemoveSingleFilter = {},
                    onSelectColor = {},
                    onLoadMore = {},
                    onWallpaperClick = {},
                    onFavoriteToggle = {},
                    onNavigateToSettings = {},
                    hapticHelper = HapticHelper(context),
                    searchHistory = history,
                    onSelectSearchHistory = { selectedQuery = it },
                    onDeleteSearchHistory = { deletedQuery = it },
                    onOpenAllSearchHistory = { openedAll = true }
                )
            }
        }

        // First 10 entries (kw_12 .. kw_3) are displayed inline; 11th & 12th (kw_2, kw_1) are omitted inline
        composeTestRule.onNodeWithTag("search_history_inline_section").assertExists()
        composeTestRule.onNodeWithTag("search_history_chip_kw_12").assertExists()
        composeTestRule.onNodeWithTag("search_history_chip_kw_3").assertExists()
        composeTestRule.onNodeWithTag("search_history_chip_kw_2").assertDoesNotExist()
        composeTestRule.onNodeWithTag("search_history_chip_kw_1").assertDoesNotExist()
        composeTestRule.onNodeWithTag("search_history_all_btn").assertExists()

        // Selecting and deleting inline history chips
        composeTestRule.onNodeWithTag("delete_inline_search_history_kw_11").performClick()
        composeTestRule.waitForIdle()
        assertEquals("kw_11", deletedQuery)

        composeTestRule.onNodeWithText("kw_12").performClick()
        composeTestRule.waitForIdle()
        assertEquals("kw_12", selectedQuery)

        // Clicking "All" opens secondary search history screen
        composeTestRule.onNodeWithTag("search_history_all_btn").performClick()
        composeTestRule.waitForIdle()
        assertTrue(openedAll)

        // Chinese input + clear search
        composeTestRule.onNodeWithTag("search_text_input").performClick()
        composeTestRule.onNodeWithTag("search_text_input").performTextInput("猫")
        composeTestRule.waitForIdle()
        assertEquals("猫", latestQuery)

        composeTestRule.onNodeWithTag("search_clear_btn").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()
        assertTrue(clearedSearch)
    }

    @Test
    fun `search history secondary screen supports individual batch delete and clear all`() {
        var singleDeleted: String? = null
        var batchDeleted: List<String> = emptyList()
        var clearedAll = false
        var backed = false

        val fullHistory = (12 downTo 1).map { idx ->
            SearchHistoryEntity(query = "history_$idx", searchedAt = idx * 1000L)
        }

        composeTestRule.setContent {
            WallhavenTheme {
                SearchHistoryScreen(
                    searchHistory = fullHistory,
                    hapticHelper = HapticHelper(context),
                    onBack = { backed = true },
                    onSelectQuery = {},
                    onDeleteQuery = { singleDeleted = it },
                    onBatchDeleteQueries = { batchDeleted = it },
                    onClearAll = { clearedAll = true }
                )
            }
        }

        // Standalone backup entry point is no longer displayed on Search History screen
        composeTestRule.onNodeWithTag("search_history_backup_btn").assertDoesNotExist()
        composeTestRule.onNodeWithTag("empty_search_history_restore_btn").assertDoesNotExist()

        // Complete list includes entries beyond the first 10
        composeTestRule.onNodeWithTag("all_search_history_card_history_12").assertExists()
        composeTestRule.onNodeWithTag("all_search_history_list")
            .performScrollToNode(hasTestTag("all_search_history_card_history_1"))
        composeTestRule.onNodeWithTag("all_search_history_card_history_1").assertExists()

        // Individual delete
        composeTestRule.onNodeWithTag("all_search_history_list").performScrollToIndex(0)
        composeTestRule.onNodeWithTag("delete_all_search_history_history_12").performClick()
        composeTestRule.waitForIdle()
        assertEquals("history_12", singleDeleted)

        // Enter batch mode, select 2 top items, and confirm batch delete
        composeTestRule.onNodeWithTag("search_history_batch_btn").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("all_search_history_card_history_12").performClick()
        composeTestRule.onNodeWithTag("all_search_history_card_history_11").performClick()
        composeTestRule.onNodeWithTag("search_history_batch_delete_btn").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(context.getString(R.string.action_confirm)).performClick()
        composeTestRule.waitForIdle()
        assertEquals(setOf("history_12", "history_11"), batchDeleted.toSet())

        // Clear all
        composeTestRule.onNodeWithTag("search_history_clear_all_btn").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(context.getString(R.string.action_confirm)).performClick()
        composeTestRule.waitForIdle()
        assertTrue(clearedAll)

        // Back button
        composeTestRule.onNodeWithTag("search_history_back_btn").performClick()
        composeTestRule.waitForIdle()
        assertTrue(backed)
    }

    @Test
    fun `full backup bundles search history with favorites and appends keeping newer timestamps`() = runBlocking {
        database.favoriteDao().insertFavorite(
            FavoriteWallpaperEntity(
                id = "wp_1",
                url = "https://wallhaven.cc/w/wp_1",
                resolution = "1920x1080",
                dimensionX = 1920,
                dimensionY = 1080,
                ratio = "1.78",
                fileSize = 102400L,
                fileType = "image/jpeg",
                category = "general",
                purity = "sfw",
                path = "https://w.wallhaven.cc/full/wp_1.jpg",
                thumbUrl = "https://th.wallhaven.cc/small/wp_1.jpg",
                views = 100,
                favorites = 10,
                colorsString = "#000000"
            )
        )
        database.searchHistoryDao().insertSearch(SearchHistoryEntity(query = "猫", searchedAt = 1000L))
        database.searchHistoryDao().insertSearch(SearchHistoryEntity(query = "cyberpunk", searchedAt = 2000L))

        val exportedJson = backupManager.createExportJson()
        assertTrue(exportedJson.contains("wp_1"))
        assertTrue(exportedJson.contains("cyberpunk"))
        assertTrue(exportedJson.contains("猫"))

        // Clear search history and leave an older timestamp for "猫" + a local-only entry to verify append/merge
        database.favoriteDao().deleteFavoriteById("wp_1")
        database.searchHistoryDao().clearAll()
        database.searchHistoryDao().insertSearch(SearchHistoryEntity(query = "猫", searchedAt = 500L))
        database.searchHistoryDao().insertSearch(SearchHistoryEntity(query = "local_only", searchedAt = 3000L))

        val restoreResult = backupManager.restoreFromJson(exportedJson)
        assertTrue(restoreResult.isSuccess)
        val summary = restoreResult.getOrNull()!!
        assertEquals(1, summary.favoritesCount)
        assertEquals(2, summary.searchHistoryCount)

        assertEquals(1, database.favoriteDao().getAllFavoritesList().size)
        val merged = database.searchHistoryDao().getAllSearchesList().associateBy { it.query }
        assertEquals(3, merged.size)
        assertEquals(1000L, merged["猫"]?.searchedAt)
        assertEquals(2000L, merged["cyberpunk"]?.searchedAt)
        assertEquals(3000L, merged["local_only"]?.searchedAt)
    }

    @Test
    fun `settings webdav cloud server config persists url username and password across reopen`() {
        composeTestRule.setContent {
            val liveSettings by preferencesManager.settingsFlow.collectAsStateWithLifecycle(
                initialValue = AppSettings()
            )
            WallhavenTheme {
                SettingsScreen(
                    settings = liveSettings,
                    preferencesManager = preferencesManager,
                    backupManager = backupManager,
                    hapticHelper = HapticHelper(context)
                )
            }
        }

        // Navigate to Settings > Backup & Restore > WebDAV Cloud Server
        composeTestRule.onNodeWithTag("settings_backup_card").performScrollTo().performClick()
        composeTestRule.mainClock.advanceTimeBy(500L)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("settings_webdav_row").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("webdav_url_input").performTextInput("https://dav.example.com/dav/")
        composeTestRule.onNodeWithTag("webdav_user_input").performTextInput("test_user")
        composeTestRule.onNodeWithTag("webdav_pass_input").performTextInput("secret_pass")
        composeTestRule.onNodeWithTag("webdav_save_btn").performClick()

        composeTestRule.waitUntil(timeoutMillis = 5_000L) {
            runBlocking { preferencesManager.getSettings().webDavUrl == "https://dav.example.com/dav/" }
        }
        composeTestRule.waitForIdle()

        val savedSettings = runBlocking { preferencesManager.getSettings() }
        assertEquals("https://dav.example.com/dav/", savedSettings.webDavUrl)
        assertEquals("test_user", savedSettings.webDavUsername)
        assertEquals("secret_pass", savedSettings.webDavPassword)

        // Reopen WebDAV Cloud Server dialog and verify values are retained
        composeTestRule.onNodeWithTag("settings_webdav_row")
            .assertTextContains("https://dav.example.com/dav/")
            .performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("webdav_url_input").assertTextContains("https://dav.example.com/dav/")
        composeTestRule.onNodeWithTag("webdav_user_input").assertTextContains("test_user")
    }

    @Test
    fun `clearing search query resets search state and clears error or results`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = MainViewModel(app)

        viewModel.onSearchQueryChange("猫")
        assertEquals("猫", viewModel.searchQuery.value)

        viewModel.onSearchQueryChange("")
        assertEquals("", viewModel.searchQuery.value)
        assertEquals(emptyList<Any>(), viewModel.searchResults.value)
        assertNull(viewModel.searchError.value)
        assertFalse(viewModel.isSearchLoading.value)

        viewModel.onSearchQueryChange("anime")
        viewModel.clearSearch()
        assertEquals("", viewModel.searchQuery.value)
        assertEquals(emptyList<Any>(), viewModel.searchResults.value)
        assertNull(viewModel.searchError.value)
        assertFalse(viewModel.isSearchLoading.value)
    }

    @Test
    fun `clicking localized detail tag updates search box and history in chinese and back restores detail sheet and scroll`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = MainViewModel(app)

        runBlocking {
            viewModel.database.searchHistoryDao().clearAll()
            viewModel.preferencesManager.setTagLocalization(true)
        }
        val settingsDeadline = System.currentTimeMillis() + 5_000L
        while (!viewModel.settings.value.tagLocalization && System.currentTimeMillis() < settingsDeadline) {
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            Thread.sleep(20)
        }
        assertTrue(viewModel.settings.value.tagLocalization)

        val sampleWallpaper = WallpaperDto(
            id = "wp_detail_1",
            resolution = "1920x1080",
            dimensionX = 1920,
            dimensionY = 1080,
            ratio = "1.78",
            tags = listOf(
                TagDto(id = 1, name = "cat"),
                TagDto(id = 2, name = "custom rare motif")
            )
        )

        viewModel.selectTab(2)
        viewModel.selectWallpaper(sampleWallpaper)
        viewModel.searchFromDetailTag(
            englishTag = "custom rare motif",
            localizedTag = "罕见自定义主题",
            detailScrollOffset = 640
        )

        assertEquals(1, viewModel.currentTab.value)
        assertNull(viewModel.selectedWallpaper.value)
        assertEquals("罕见自定义主题", viewModel.searchQuery.value)
        assertEquals(1, viewModel.detailReturnStack.value.size)
        assertEquals("custom rare motif", viewModel.tagTranslationRepository.resolveSearchQuery("罕见自定义主题"))

        val historyDeadline = System.currentTimeMillis() + 5_000L
        var historyQueries: List<String> = emptyList()
        while (System.currentTimeMillis() < historyDeadline) {
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            historyQueries = runBlocking {
                viewModel.database.searchHistoryDao().getAllSearchesList().map { it.query }
            }
            if (historyQueries.contains("罕见自定义主题")) break
            Thread.sleep(20)
        }
        assertTrue(historyQueries.contains("罕见自定义主题"))
        assertFalse(historyQueries.contains("custom rare motif"))

        viewModel.navigateBackFromDetailReturn()

        assertEquals(2, viewModel.currentTab.value)
        assertEquals("wp_detail_1", viewModel.selectedWallpaper.value?.id)
        assertEquals(640, viewModel.pendingDetailScrollOffset.value)
        assertTrue(viewModel.isRestoredFromDetailReturn.value)
        assertTrue(viewModel.detailReturnStack.value.isEmpty())

        viewModel.dismissDetailSheet()
        assertFalse(viewModel.isRestoredFromDetailReturn.value)

        viewModel.consumePendingDetailScroll()
        assertEquals(0, viewModel.pendingDetailScrollOffset.value)
    }

    @Test
    fun `exiting search returns to main search page before navigating back to discover`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = MainViewModel(app)

        viewModel.selectTab(1)
        assertFalse(viewModel.isSearchActive.value)

        viewModel.onSearchQueryChange("cyberpunk")
        val deadline = System.currentTimeMillis() + 3_000L
        while (!viewModel.isSearchActive.value && System.currentTimeMillis() < deadline) {
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            Thread.sleep(15)
        }
        assertTrue(viewModel.isSearchActive.value)

        viewModel.exitSearchToSearchHome()
        val clearDeadline = System.currentTimeMillis() + 3_000L
        while (viewModel.isSearchActive.value && System.currentTimeMillis() < clearDeadline) {
            org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
            Thread.sleep(15)
        }
        assertEquals(1, viewModel.currentTab.value)
        assertEquals("", viewModel.searchQuery.value)
        assertFalse(viewModel.isSearchActive.value)
    }

    @Test
    fun `wallpaper detail sheet passes localized tag and scroll offset on click and restores scroll offset`() {
        var clickedEnglish: String? = null
        var clickedLocalized: String? = null
        var clickedOffset: Int = -1
        var scrollRestored = false

        val sampleWallpaper = WallpaperDto(
            id = "wp_sheet_1",
            resolution = "1920x1080",
            dimensionX = 1920,
            dimensionY = 1080,
            ratio = "1.78",
            colors = listOf("#663399"),
            tags = listOf(TagDto(id = 10, name = "cat"))
        )

        composeTestRule.setContent {
            WallhavenTheme {
                WallpaperDetailSheet(
                    wallpaper = sampleWallpaper,
                    isFavorite = false,
                    downloadStatus = null,
                    onFavoriteToggle = {},
                    onDownload = {},
                    onSearchTag = { en, zh, offset ->
                        clickedEnglish = en
                        clickedLocalized = zh
                        clickedOffset = offset
                    },
                    onSearchColor = { _, _ -> },
                    onDismiss = {},
                    hapticHelper = HapticHelper(context),
                    wallpaperSetHelper = WallpaperSetHelper(context),
                    tagTranslations = mapOf("cat" to "猫咪"),
                    isTagLocalizationEnabled = true,
                    initialScrollOffset = 80,
                    onScrollRestored = { scrollRestored = true },
                    isRestoredFromSearch = true
                )
            }
        }

        composeTestRule.onNodeWithTag("restored_detail_surface").assertExists()
        composeTestRule.waitUntil(timeoutMillis = 5_000L) { scrollRestored }
        composeTestRule.onNodeWithTag("detail_tag_cat").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        assertEquals("cat", clickedEnglish)
        assertEquals("猫咪", clickedLocalized)
        assertTrue(clickedOffset >= 0)
    }
}
