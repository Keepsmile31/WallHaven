package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import com.example.data.preferences.AppSettings
import com.example.ui.components.DISCLAIMER_CONFIRM_STATEMENT_EN
import com.example.ui.components.DISCLAIMER_CONFIRM_STATEMENT_ZH
import com.example.ui.components.DisclaimerDialog
import com.example.ui.components.isDisclaimerConfirmationMatched
import com.example.ui.screens.SettingsAboutScreen
import com.example.ui.theme.WallhavenTheme
import com.example.util.HapticHelper
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
class DisclaimerDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `confirmation statement matcher ignores punctuation width and whitespace`() {
        assertFalse(isDisclaimerConfirmationMatched(""))
        assertFalse(isDisclaimerConfirmationMatched("   "))
        assertFalse(isDisclaimerConfirmationMatched("我承诺身处中国大陆以外地区"))
        assertFalse(isDisclaimerConfirmationMatched("I confirm that I am located outside of mainland China"))

        // Exact updated Chinese statement
        assertTrue(isDisclaimerConfirmationMatched(DISCLAIMER_CONFIRM_STATEMENT_ZH))
        // Half-width punctuation, spaces, and surrounding quotes
        assertTrue(
            isDisclaimerConfirmationMatched(
                "  \"我承诺身处中国大陆以外地区且已年满 18 周岁, 自愿承担使用本第三方客户端的一切法律责任.\"  "
            )
        )
        // Stripped punctuation completely
        assertTrue(
            isDisclaimerConfirmationMatched(
                "我承诺身处中国大陆以外地区且已年满18周岁自愿承担使用本第三方客户端的一切法律责任"
            )
        )
        // English fallback
        assertTrue(isDisclaimerConfirmationMatched(DISCLAIMER_CONFIRM_STATEMENT_EN))
        assertTrue(isDisclaimerConfirmationMatched("  \"$DISCLAIMER_CONFIRM_STATEMENT_EN\"  "))
    }

    @Test
    fun `first launch DisclaimerDialog requires scrolling to bottom then typing confirmation statement and records timestamp`() {
        var agreedCount = 0
        var recordedTimestamp = 0L
        var exitedCount = 0

        composeTestRule.setContent {
            WallhavenTheme {
                DisclaimerDialog(
                    onAgree = { ts ->
                        agreedCount++
                        recordedTimestamp = ts
                    },
                    onExit = { exitedCount++ }
                )
            }
        }

        // Initial Step 1: full terms shown, agree button disabled until scrolled to bottom
        composeTestRule.onNodeWithText("免责声明与服务条款").assertIsDisplayed()
        composeTestRule.onNodeWithText("WallHaven 用户协议与免责声明").assertIsDisplayed()
        composeTestRule.onNodeWithText("【重要地域与服务限制声明】").assertIsDisplayed()
        composeTestRule.onNodeWithText("拒绝并退出").assertIsDisplayed()
        composeTestRule.onNodeWithTag("disclaimer_agree_btn").assertIsNotEnabled()

        // Scroll to the bottom marker -> agree button becomes enabled
        composeTestRule.onNodeWithTag("disclaimer_bottom_marker").performScrollTo()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("disclaimer_agree_btn").assertIsEnabled()

        // Clicking agree opens Step 2 confirmation prompt without granting access yet
        composeTestRule.onNodeWithTag("disclaimer_agree_btn").performClick()
        composeTestRule.waitForIdle()
        assertEquals(0, agreedCount)

        composeTestRule.onNodeWithTag("disclaimer_confirm_statement").assertIsDisplayed()
        composeTestRule.onNodeWithText("提示：上方声明文字支持长按选中并复制").assertDoesNotExist()
        composeTestRule.onNodeWithText("您可以长按选中，并复制上方文字").assertDoesNotExist()
        composeTestRule.onNodeWithTag("disclaimer_confirm_submit_btn").assertIsNotEnabled()

        // Entering wrong text keeps Confirm disabled
        composeTestRule.onNodeWithTag("disclaimer_confirm_input").performTextInput("随便输入")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("disclaimer_confirm_submit_btn").assertIsNotEnabled()

        // Entering statement with half-width punctuation & spaces enables Confirm
        composeTestRule.onNodeWithTag("disclaimer_confirm_input")
            .performTextReplacement("我承诺身处中国大陆以外地区且已年满 18 周岁, 自愿承担使用本第三方客户端的一切法律责任.")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("disclaimer_confirm_submit_btn").assertIsEnabled()

        // Confirm grants access and records confirmation timestamp
        composeTestRule.onNodeWithTag("disclaimer_confirm_submit_btn").performClick()
        composeTestRule.waitForIdle()
        assertEquals(1, agreedCount)
        assertTrue(recordedTimestamp > 0L)
        assertEquals(0, exitedCount)
    }

    @Test
    fun `confirmation prompt also accepts English statement and exit triggers onExit`() {
        var agreedCount = 0
        var exitedCount = 0

        composeTestRule.setContent {
            WallhavenTheme {
                DisclaimerDialog(
                    onAgree = { agreedCount++ },
                    onExit = { exitedCount++ }
                )
            }
        }

        composeTestRule.onNodeWithTag("disclaimer_bottom_marker").performScrollTo()
        composeTestRule.onNodeWithTag("disclaimer_agree_btn").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("disclaimer_confirm_input")
            .performTextInput(DISCLAIMER_CONFIRM_STATEMENT_EN)
        composeTestRule.onNodeWithTag("disclaimer_confirm_submit_btn").assertIsEnabled()

        composeTestRule.onNodeWithTag("disclaimer_confirm_back_btn").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("disclaimer_exit_btn").performClick()
        composeTestRule.waitForIdle()

        assertEquals(0, agreedCount)
        assertEquals(1, exitedCount)
    }

    @Test
    fun `SettingsAboutScreen renames entry removes CoolApk link and shows full updated text`() {
        composeTestRule.setContent {
            WallhavenTheme {
                SettingsAboutScreen(
                    settings = AppSettings(
                        disclaimerAccepted = true,
                        disclaimerAcceptedVersion = 2,
                        disclaimerAcceptedAt = 1763200000000L
                    ),
                    hapticHelper = HapticHelper(context),
                    onClearHistory = null,
                    onClearDownloads = null,
                    onBack = {}
                )
            }
        }

        // Advance past SubPageScaffold's 350ms transition touch-guard delay
        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.waitForIdle()

        // CoolApk & 123pan removed; clean Check Updates button present
        composeTestRule.onNodeWithText("开发者主页 (酷安)").assertDoesNotExist()
        composeTestRule.onNodeWithText("检查更新 (123云盘发布源)").assertDoesNotExist()
        composeTestRule.onNodeWithText("用户服务协议").assertDoesNotExist()
        composeTestRule.onNodeWithText("检查更新").assertIsDisplayed()

        // Signature status row displays signed status, version, and timestamp
        composeTestRule.onNodeWithTag("settings_signature_status_row").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("协议签署状态").assertIsDisplayed()
        composeTestRule.onNodeWithText("已签署").assertIsDisplayed()

        // Telegram channel row present
        composeTestRule.onNodeWithTag("settings_telegram_channel_row").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Telegram 频道").assertIsDisplayed()
        composeTestRule.onNodeWithText("加入官方频道获取最新资讯与交流").assertIsDisplayed()

        composeTestRule.onNodeWithTag("settings_disclaimer_row").performScrollTo()
        composeTestRule.onNodeWithText("免责声明与服务条款").assertIsDisplayed()

        // Clicking opens dialog with the updated agreement
        composeTestRule.onNodeWithTag("settings_disclaimer_row").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("WallHaven 用户协议与免责声明").assertIsDisplayed()
        composeTestRule.onNodeWithText("更新日期： 2026年11月15日").assertIsDisplayed()
        composeTestRule.onNodeWithText("【重要地域与服务限制声明】").assertIsDisplayed()
        composeTestRule.onNodeWithText("第六条 协议修改与法律适用").performScrollTo().assertIsDisplayed()
    }
}
