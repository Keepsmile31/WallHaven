package com.example

import android.content.Context
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.ThumbsDto
import com.example.data.model.WallpaperDto
import com.example.ui.components.FilterState
import com.example.ui.screens.ExploreScreen
import com.example.ui.theme.WallhavenTheme
import com.example.util.HapticHelper
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExploreTabsInteractionTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var hapticHelper: HapticHelper

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        hapticHelper = HapticHelper(context)
    }

    private fun sampleWallpapers(count: Int = 30): List<WallpaperDto> {
        return List(count) { idx ->
            WallpaperDto(
                id = "wp_$idx",
                resolution = "1920x1080",
                ratio = "1.78",
                dimensionX = 1920,
                dimensionY = 1080,
                thumbs = ThumbsDto(large = "https://example.com/thumb_$idx.jpg")
            )
        }
    }

    @Test
    fun `tapping different tab switches category while tapping active tab smoothly scrolls to top`() {
        val selectedTabs = mutableListOf<Int>()
        var retryCount = 0
        val gridState = LazyStaggeredGridState()
        val wallpapers = sampleWallpapers(30)

        composeTestRule.setContent {
            WallhavenTheme {
                ExploreScreen(
                    wallpapers = wallpapers,
                    isLoading = false,
                    isEndReached = true,
                    errorMessage = null,
                    favoriteIds = emptySet(),
                    currentSubTab = 0,
                    filterState = FilterState(),
                    hasApiKey = false,
                    onSelectSubTab = { selectedTabs.add(it) },
                    onApplyFilter = {},
                    onResetFilter = {},
                    onRemoveSingleFilter = {},
                    onNavigateToSettings = {},
                    onLoadMore = {},
                    onRetry = { retryCount++ },
                    onWallpaperClick = {},
                    onFavoriteToggle = {},
                    hapticHelper = hapticHelper,
                    gridState = gridState
                )
            }
        }

        composeTestRule.waitForIdle()

        // Scroll down away from top
        composeTestRule.runOnUiThread {
            runBlocking {
                gridState.scrollToItem(16)
            }
        }
        composeTestRule.waitForIdle()
        assertTrue(gridState.firstVisibleItemIndex > 0)

        // Repeated tap on already selected tab (0 = 最新):
        // must NOT trigger category switch, must smoothly scroll back to index 0
        composeTestRule.onNodeWithTag("explore_tab_0").performClick()
        composeTestRule.mainClock.advanceTimeBy(1200L)
        composeTestRule.waitForIdle()

        assertTrue("Category switch should not be triggered on repeated tap", selectedTabs.isEmpty())
        assertEquals(0, retryCount)
        assertEquals(0, gridState.firstVisibleItemIndex)
        assertEquals(0, gridState.firstVisibleItemScrollOffset)

        // Repeated tap while already at index 0 triggers optional refresh without category switch
        composeTestRule.onNodeWithTag("explore_tab_0").performClick()
        composeTestRule.waitForIdle()
        assertTrue("Category switch should not be triggered on repeated tap at top", selectedTabs.isEmpty())
        assertEquals(1, retryCount)

        // Tapping a different tab (1 = 热门) triggers standard category switch
        composeTestRule.onNodeWithTag("explore_tab_1").performClick()
        composeTestRule.waitForIdle()
        assertEquals(listOf(1), selectedTabs)
    }

    @Test
    fun `double clicking Random tab refreshes wallpapers even when scrolled down`() {
        val selectedTabs = mutableListOf<Int>()
        var retryCount = 0
        val gridState = LazyStaggeredGridState()
        val wallpapers = sampleWallpapers(30)

        composeTestRule.setContent {
            WallhavenTheme {
                ExploreScreen(
                    wallpapers = wallpapers,
                    isLoading = false,
                    isEndReached = true,
                    errorMessage = null,
                    favoriteIds = emptySet(),
                    currentSubTab = 2, // Random tab active
                    filterState = FilterState(),
                    hasApiKey = false,
                    onSelectSubTab = { selectedTabs.add(it) },
                    onApplyFilter = {},
                    onResetFilter = {},
                    onRemoveSingleFilter = {},
                    onNavigateToSettings = {},
                    onLoadMore = {},
                    onRetry = { retryCount++ },
                    onWallpaperClick = {},
                    onFavoriteToggle = {},
                    hapticHelper = hapticHelper,
                    gridState = gridState
                )
            }
        }

        composeTestRule.waitForIdle()

        // Single tap at top of Random tab should NOT accidentally refresh
        composeTestRule.onNodeWithTag("explore_tab_2").performClick()
        composeTestRule.mainClock.advanceTimeBy(500L)
        composeTestRule.waitForIdle()
        assertEquals(0, retryCount)

        // Scroll down away from top, then double-click Random tab -> scrolls to top AND refreshes
        composeTestRule.runOnUiThread {
            runBlocking {
                gridState.scrollToItem(16)
            }
        }
        composeTestRule.waitForIdle()
        assertTrue(gridState.firstVisibleItemIndex > 0)

        composeTestRule.onNodeWithTag("explore_tab_2").performTouchInput { doubleClick() }
        composeTestRule.mainClock.advanceTimeBy(1200L)
        composeTestRule.waitForIdle()

        assertTrue("Category switch should not be triggered while already on Random", selectedTabs.isEmpty())
        assertEquals(1, retryCount)
        assertEquals(0, gridState.firstVisibleItemIndex)
        assertEquals(0, gridState.firstVisibleItemScrollOffset)
    }
}
