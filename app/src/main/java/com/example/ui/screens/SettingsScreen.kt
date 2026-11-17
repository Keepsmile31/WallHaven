package com.example.ui.screens

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.BuildConfig
import com.example.R
import com.example.data.backup.BackupManager
import com.example.data.preferences.AppSettings
import com.example.data.preferences.UserPreferencesManager
import com.example.data.repository.AppUpdateRepository
import com.example.ui.components.AppHeaderBar
import com.example.util.HapticHelper

import androidx.compose.runtime.CompositionLocalProvider
import com.example.ui.components.EnsureSharedTransitionScope
import com.example.ui.components.LocalSettingsTransitionScope

internal object SettingsRoutes {
    const val MAIN = "settings_main"
    const val APPEARANCE = "settings_appearance"
    const val CONTENT = "settings_content"
    const val STORAGE = "settings_storage"
    const val BACKUP = "settings_backup"
    const val ABOUT = "settings_about"
}

/**
 * Two-level Jetpack Navigation settings container for WallHaven:
 * Level 1: Modular cards -> Level 2: Module sub-pages (with AlertDialogs for choices/config).
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    preferencesManager: UserPreferencesManager,
    backupManager: BackupManager,
    hapticHelper: HapticHelper,
    appUpdateRepository: AppUpdateRepository? = null,
    onClearCacheNow: ((() -> Unit) -> Unit)? = null,
    onClearHistory: (() -> Unit)? = null,
    onClearDownloads: (() -> Unit)? = null,
    mainScrollState: ScrollState = rememberScrollState()
) {
    val navController = rememberNavController()

    val settingsState = rememberUpdatedState(settings)
    val clearCacheNowState = rememberUpdatedState(onClearCacheNow)
    val clearHistoryState = rememberUpdatedState(onClearHistory)
    val clearDownloadsState = rememberUpdatedState(onClearDownloads)

    val navigateTo: (String) -> Unit = remember(navController, hapticHelper) {
        var lastClickTime = -1000L
        { route ->
            val now = android.os.SystemClock.elapsedRealtime()
            val currentRoute = navController.currentBackStackEntry?.destination?.route
            if (currentRoute == SettingsRoutes.MAIN && currentRoute != route && now - lastClickTime > 350L) {
                lastClickTime = now
                hapticHelper.vibrateTick(settingsState.value.hapticFeedback)
                navController.navigate(route) {
                    launchSingleTop = true
                }
            }
        }
    }

    val navigateBack: () -> Unit = remember(navController) {
        var lastBackTime = -1000L
        {
            val now = android.os.SystemClock.elapsedRealtime()
            if (navController.currentBackStackEntry?.destination?.route != SettingsRoutes.MAIN && now - lastBackTime > 300L) {
                lastBackTime = now
                navController.popBackStack()
            }
        }
    }

    EnsureSharedTransitionScope {
        NavHost(
            navController = navController,
            startDestination = SettingsRoutes.MAIN,
            modifier = Modifier.fillMaxSize(),
            enterTransition = {
                fadeIn(tween(240, easing = FastOutSlowInEasing))
            },
            exitTransition = {
                fadeOut(tween(180, easing = FastOutSlowInEasing))
            },
            popEnterTransition = {
                fadeIn(tween(240, easing = FastOutSlowInEasing))
            },
            popExitTransition = {
                fadeOut(tween(180, easing = FastOutSlowInEasing))
            }
        ) {
            composable(SettingsRoutes.MAIN) {
                CompositionLocalProvider(LocalSettingsTransitionScope provides this@composable) {
                    SettingsMainScreen(
                        scrollState = mainScrollState,
                        onNavigate = navigateTo
                    )
                }
            }

            composable(SettingsRoutes.APPEARANCE) {
                CompositionLocalProvider(LocalSettingsTransitionScope provides this@composable) {
                    SettingsAppearanceScreen(
                        settings = settingsState.value,
                        preferencesManager = preferencesManager,
                        hapticHelper = hapticHelper,
                        onBack = navigateBack
                    )
                }
            }

            composable(SettingsRoutes.CONTENT) {
                CompositionLocalProvider(LocalSettingsTransitionScope provides this@composable) {
                    SettingsContentScreen(
                        settings = settingsState.value,
                        preferencesManager = preferencesManager,
                        hapticHelper = hapticHelper,
                        onBack = navigateBack
                    )
                }
            }

            composable(SettingsRoutes.STORAGE) {
                CompositionLocalProvider(LocalSettingsTransitionScope provides this@composable) {
                    SettingsStorageScreen(
                        settings = settingsState.value,
                        preferencesManager = preferencesManager,
                        hapticHelper = hapticHelper,
                        onClearCacheNow = clearCacheNowState.value,
                        onBack = navigateBack
                    )
                }
            }

            composable(SettingsRoutes.BACKUP) {
                CompositionLocalProvider(LocalSettingsTransitionScope provides this@composable) {
                    SettingsBackupScreen(
                        settings = settingsState.value,
                        preferencesManager = preferencesManager,
                        backupManager = backupManager,
                        hapticHelper = hapticHelper,
                        onBack = navigateBack
                    )
                }
            }

            composable(SettingsRoutes.ABOUT) {
                CompositionLocalProvider(LocalSettingsTransitionScope provides this@composable) {
                    SettingsAboutScreen(
                        settings = settingsState.value,
                        hapticHelper = hapticHelper,
                        appUpdateRepository = appUpdateRepository,
                        onClearHistory = clearHistoryState.value,
                        onClearDownloads = clearDownloadsState.value,
                        onBack = navigateBack
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsMainScreen(
    scrollState: ScrollState,
    onNavigate: (String) -> Unit
) {
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val cutoutPadding = WindowInsets.displayCutout.asPaddingValues().calculateTopPadding()
    val topInset = maxOf(statusBarPadding, cutoutPadding)
    val navBottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(
                top = topInset + 6.dp,
                bottom = navBottomInset + 88.dp,
                start = 16.dp,
                end = 16.dp
            ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        AppHeaderBar(
            title = stringResource(R.string.nav_settings),
            modifier = Modifier.padding(vertical = 6.dp)
        )

        Spacer(modifier = Modifier.height(2.dp))

        M3ModuleCard(
            icon = ImageVector.vectorResource(R.drawable.ic_palette),
            iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
            iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
            title = stringResource(R.string.settings_appearance),
            subtitle = "动态取色、自定义主题配色、深色模式与液态玻璃",
            testTag = "settings_appearance_card",
            sharedRouteKey = SettingsRoutes.APPEARANCE,
            onClick = { onNavigate(SettingsRoutes.APPEARANCE) }
        )

        M3ModuleCard(
            icon = ImageVector.vectorResource(R.drawable.ic_tune),
            iconContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            iconTint = MaterialTheme.colorScheme.onSecondaryContainer,
            title = "内容与浏览",
            subtitle = "WallHaven API Key、多标签筛选模式与标签中文本地化",
            testTag = "settings_content_card",
            sharedRouteKey = SettingsRoutes.CONTENT,
            onClick = { onNavigate(SettingsRoutes.CONTENT) }
        )

        M3ModuleCard(
            icon = ImageVector.vectorResource(R.drawable.ic_storage),
            iconContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
            iconTint = MaterialTheme.colorScheme.onTertiaryContainer,
            title = stringResource(R.string.settings_storage),
            subtitle = "本地缓存深度清理、自动清理策略、保存目录与并发下载",
            testTag = "settings_storage_card",
            sharedRouteKey = SettingsRoutes.STORAGE,
            onClick = { onNavigate(SettingsRoutes.STORAGE) }
        )

        M3ModuleCard(
            icon = ImageVector.vectorResource(R.drawable.ic_cloud_sync),
            iconContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            iconTint = MaterialTheme.colorScheme.primary,
            title = "备份与恢复",
            subtitle = "WebDAV 云端远程同步、定时每日备份与本地 JSON 导入导出",
            testTag = "settings_backup_card",
            sharedRouteKey = SettingsRoutes.BACKUP,
            onClick = { onNavigate(SettingsRoutes.BACKUP) }
        )

        M3ModuleCard(
            icon = ImageVector.vectorResource(R.drawable.ic_info),
            iconContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            iconTint = MaterialTheme.colorScheme.secondary,
            title = stringResource(R.string.settings_about),
            subtitle = "版本 ${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})、更新检查、调试日志导出与记录清空",
            testTag = "settings_about_card",
            sharedRouteKey = SettingsRoutes.ABOUT,
            onClick = { onNavigate(SettingsRoutes.ABOUT) }
        )
    }
}
