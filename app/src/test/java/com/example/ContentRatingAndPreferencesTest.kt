package com.example

import android.content.Context
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.test.core.app.ApplicationProvider
import com.example.data.preferences.AppSettings
import com.example.data.preferences.UserPreferencesManager
import com.example.data.preferences.dataStore
import com.example.ui.components.FilterBottomSheet
import com.example.ui.components.FilterState
import com.example.ui.screens.SettingsContentScreen
import com.example.ui.theme.WallhavenTheme
import com.example.util.HapticHelper
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp")
class ContentRatingAndPreferencesTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var preferencesManager: UserPreferencesManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        preferencesManager = UserPreferencesManager(context)
        runBlocking {
            context.dataStore.edit { it.clear() }
        }
    }

    @Test
    fun `legacy disclaimer acceptance without current version forces disclaimer dialog to reappear after update`() = runBlocking {
        val legacyAcceptedKey = booleanPreferencesKey("disclaimer_accepted")
        val versionKey = intPreferencesKey("disclaimer_accepted_version")

        // Simulate existing user updating from an older app build where only boolean disclaimer_accepted=true existed
        context.dataStore.edit {
            it[legacyAcceptedKey] = true
            it.remove(versionKey)
        }

        val beforeReconfirm = preferencesManager.getSettings()
        assertFalse(beforeReconfirm.disclaimerAccepted)
        assertEquals(0, beforeReconfirm.disclaimerAcceptedVersion)
        assertEquals(0L, beforeReconfirm.disclaimerAcceptedAt)

        // Confirming records current version + timestamp
        val confirmedTimestamp = 1763201234567L
        preferencesManager.setDisclaimerAccepted(true, acceptedAt = confirmedTimestamp)

        val afterConfirm = preferencesManager.getSettings()
        assertTrue(afterConfirm.disclaimerAccepted)
        assertEquals(UserPreferencesManager.CURRENT_DISCLAIMER_VERSION, afterConfirm.disclaimerAcceptedVersion)
        assertEquals(confirmedTimestamp, afterConfirm.disclaimerAcceptedAt)
    }

    @Test
    fun `entering non-blank API key in Settings requires secondary confirmation before saving`() {
        composeTestRule.setContent {
            WallhavenTheme {
                SettingsContentScreen(
                    settings = AppSettings(),
                    preferencesManager = preferencesManager,
                    hapticHelper = HapticHelper(context),
                    onBack = {}
                )
            }
        }

        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.waitForIdle()

        // Open API Key dialog
        composeTestRule.onNodeWithTag("settings_api_key_row").performClick()
        composeTestRule.waitForIdle()

        // Enter API Key and click Save
        composeTestRule.onNodeWithTag("api_key_input").performTextInput("test_wallhaven_key_1234")
        composeTestRule.onNodeWithTag("api_key_save_btn").performClick()
        composeTestRule.waitForIdle()

        // Secondary warning prompt must appear and key is NOT saved yet
        composeTestRule.onNodeWithTag("content_rating_warning_msg").assertIsDisplayed()
        assertEquals("", runBlocking { preferencesManager.getSettings().apiKey })

        // Canceling keeps key unsaved
        composeTestRule.onNodeWithTag("content_rating_cancel_btn").performClick()
        composeTestRule.waitForIdle()
        assertEquals("", runBlocking { preferencesManager.getSettings().apiKey })

        // Clicking Save again -> Confirm and Assume Responsibility persists the key
        composeTestRule.onNodeWithTag("api_key_save_btn").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("content_rating_confirm_btn").performClick()
        composeTestRule.waitForIdle()

        // Wait briefly for coroutine DataStore write
        composeTestRule.waitUntil(timeoutMillis = 3000) {
            runBlocking { preferencesManager.getSettings().apiKey } == "test_wallhaven_key_1234"
        }
        assertEquals("test_wallhaven_key_1234", runBlocking { preferencesManager.getSettings().apiKey })
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun `enabling Sketchy or NSFW in FilterBottomSheet requires secondary confirmation before applying`() {
        var confirmedFilter: FilterState? = null

        composeTestRule.setContent {
            WallhavenTheme {
                val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                FilterBottomSheet(
                    sheetState = sheetState,
                    initialFilterState = FilterState(purity = "110"),
                    hasApiKey = true,
                    onDismissRequest = {},
                    onReset = {},
                    onConfirm = { confirmedFilter = it },
                    onRequireApiKey = {}
                )
            }
        }

        // Try enabling NSFW chip ("限制级", index = 2) -> warning dialog opens
        composeTestRule.onNodeWithTag("purity_chip_2").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("content_rating_warning_msg").assertIsDisplayed()

        // Canceling must NOT apply NSFW
        composeTestRule.onNodeWithTag("content_rating_cancel_btn").performClick()
        composeTestRule.waitForIdle()

        // Try enabling NSFW chip again and confirming -> enables NSFW ("111")
        composeTestRule.onNodeWithTag("purity_chip_2").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("content_rating_confirm_btn").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("filter_sheet_confirm_btn").performClick()
        composeTestRule.waitForIdle()
        assertEquals("111", confirmedFilter?.purity)
    }

    @Test
    fun `api key dialog includes 123pan link button that opens 123pan key url`() {
        composeTestRule.setContent {
            WallhavenTheme {
                SettingsContentScreen(
                    settings = AppSettings(),
                    preferencesManager = preferencesManager,
                    hapticHelper = HapticHelper(context),
                    onBack = {}
                )
            }
        }

        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("settings_api_key_row").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("api_key_official_btn").assertIsDisplayed()
        composeTestRule.onNodeWithText("前往官网获取 API Key").assertIsDisplayed()
        composeTestRule.onNodeWithTag("api_key_123pan_btn")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("前往 123网盘 获取 API Key").assertIsDisplayed()
        composeTestRule.onNodeWithTag("api_key_123pan_btn").performClick()
        composeTestRule.waitForIdle()

        val launchedIntent = org.robolectric.Shadows.shadowOf(
            ApplicationProvider.getApplicationContext<android.app.Application>()
        ).nextStartedActivity
        assertEquals(android.content.Intent.ACTION_VIEW, launchedIntent?.action)
        assertEquals(
            "https://1815318986.share.123pan.cn/123pan/ngLtVv-N3Fm3",
            launchedIntent?.dataString
        )
    }
}
