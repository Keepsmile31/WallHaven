package com.example

import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.backup.BackupManager
import com.example.data.db.AppDatabase
import com.example.data.db.SearchHistoryEntity
import com.example.data.model.ThumbsDto
import com.example.data.model.WallpaperDto
import com.example.data.preferences.AppSettings
import com.example.data.preferences.UserPreferencesManager
import com.example.ui.components.FilterState
import com.example.ui.components.LocalDetailTransitionScope
import com.example.ui.components.LocalSearchHistoryTransitionScope
import com.example.ui.components.LocalSharedTransitionScope
import com.example.ui.components.WallpaperDetailSheet
import com.example.ui.screens.ExploreScreen
import com.example.ui.screens.SearchHistoryScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.WallhavenTheme
import com.example.util.HapticHelper
import com.example.util.WallpaperSetHelper
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SharedElementNavigationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var hapticHelper: HapticHelper
    private lateinit var wallpaperSetHelper: WallpaperSetHelper

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        hapticHelper = HapticHelper(context)
        wallpaperSetHelper = WallpaperSetHelper(context)
    }

    @OptIn(ExperimentalSharedTransitionApi::class)
    @Test
    fun `wallpaper card transitions smoothly to in-window detail sheet and back inside SharedTransitionLayout`() {
        val sample = WallpaperDto(
            id = "shared_wp_1",
            resolution = "3840x2160",
            dimensionX = 3840,
            dimensionY = 2160,
            ratio = "1.78",
            colors = listOf("#1E88E5"),
            thumbs = ThumbsDto(large = "https://example.com/thumb.jpg")
        )

        composeTestRule.setContent {
            WallhavenTheme {
                var selected by remember { mutableStateOf<WallpaperDto?>(null) }
                var cached by remember { mutableStateOf<WallpaperDto?>(null) }
                if (selected != null) cached = selected

                SharedTransitionLayout {
                    CompositionLocalProvider(LocalSharedTransitionScope provides this) {
                        AnimatedContent(
                            targetState = selected?.id,
                            transitionSpec = {
                                fadeIn(tween(240, easing = FastOutSlowInEasing))
                                    .togetherWith(fadeOut(tween(180, easing = FastOutSlowInEasing)))
                            },
                            label = "test_detail_transition"
                        ) { activeId ->
                            CompositionLocalProvider(LocalDetailTransitionScope provides this@AnimatedContent) {
                                if (activeId == null) {
                                    ExploreScreen(
                                        wallpapers = listOf(sample),
                                        isLoading = false,
                                        isEndReached = true,
                                        errorMessage = null,
                                        favoriteIds = emptySet(),
                                        currentSubTab = 0,
                                        filterState = FilterState(),
                                        hasApiKey = false,
                                        onSelectSubTab = {},
                                        onApplyFilter = {},
                                        onResetFilter = {},
                                        onRemoveSingleFilter = {},
                                        onNavigateToSettings = {},
                                        onLoadMore = {},
                                        onRetry = {},
                                        onWallpaperClick = { selected = it },
                                        onFavoriteToggle = {},
                                        hapticHelper = hapticHelper
                                    )
                                } else {
                                    val wp = selected ?: cached
                                    WallpaperDetailSheet(
                                        wallpaper = wp,
                                        isFavorite = false,
                                        downloadStatus = null,
                                        onFavoriteToggle = {},
                                        onDownload = {},
                                        onSearchTag = { _, _, _ -> },
                                        onSearchColor = { _, _ -> },
                                        onDismiss = { selected = null },
                                        hapticHelper = hapticHelper,
                                        wallpaperSetHelper = wallpaperSetHelper,
                                        sharedOrigin = "explore"
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("wallpaper_card_shared_wp_1").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("download_wallpaper_btn").assertIsDisplayed()
        composeTestRule.onNodeWithTag("close_sheet_btn").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("wallpaper_card_shared_wp_1").assertIsDisplayed()
    }

    @Test
    fun `settings main cards transition to sub-screens and back with shared elements`() {
        val prefsManager = UserPreferencesManager(context)
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val backupManager = BackupManager(context, prefsManager, db)

        try {
            composeTestRule.setContent {
                WallhavenTheme {
                    SettingsScreen(
                        settings = AppSettings(),
                        preferencesManager = prefsManager,
                        backupManager = backupManager,
                        hapticHelper = hapticHelper
                    )
                }
            }

            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag("settings_appearance_card").performScrollTo().performClick()
            composeTestRule.mainClock.advanceTimeBy(500L)
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithTag("dynamic_color_switch").assertIsDisplayed()
            composeTestRule.onNodeWithTag("settings_back_button").performClick()
            composeTestRule.mainClock.advanceTimeBy(500L)
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithTag("settings_appearance_card").assertIsDisplayed()
        } finally {
            db.close()
        }
    }

    @OptIn(ExperimentalSharedTransitionApi::class)
    @Test
    fun `search inline history transitions to SearchHistoryScreen and back inside SharedTransitionLayout`() {
        val history = listOf(
            SearchHistoryEntity(query = "cyberpunk", searchedAt = 1_700_000_000_000L),
            SearchHistoryEntity(query = "nature", searchedAt = 1_699_999_000_000L)
        )

        composeTestRule.setContent {
            WallhavenTheme {
                var showAll by remember { mutableStateOf(false) }

                SharedTransitionLayout {
                    CompositionLocalProvider(LocalSharedTransitionScope provides this) {
                        AnimatedContent(
                            targetState = showAll,
                            transitionSpec = {
                                fadeIn(tween(240, easing = FastOutSlowInEasing))
                                    .togetherWith(fadeOut(tween(180, easing = FastOutSlowInEasing)))
                            },
                            label = "test_search_history_transition"
                        ) { showingAll ->
                            CompositionLocalProvider(LocalSearchHistoryTransitionScope provides this@AnimatedContent) {
                                if (showingAll) {
                                    SearchHistoryScreen(
                                        searchHistory = history,
                                        hapticHelper = hapticHelper,
                                        onBack = { showAll = false },
                                        onSelectQuery = {},
                                        onDeleteQuery = {},
                                        onBatchDeleteQueries = {},
                                        onClearAll = {}
                                    )
                                } else {
                                    SearchScreen(
                                        searchQuery = "",
                                        onQueryChange = {},
                                        onPerformSearch = {},
                                        wallpapers = emptyList(),
                                        isLoading = false,
                                        isEndReached = true,
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
                                        hapticHelper = hapticHelper,
                                        searchHistory = history,
                                        onOpenAllSearchHistory = { showAll = true }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("search_history_all_btn").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("search_history_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("search_history_back_btn").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("search_history_inline_section").assertIsDisplayed()
    }
}
