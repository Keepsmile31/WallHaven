package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import com.example.data.model.WallpaperDto
import com.example.ui.MainViewModel
import com.example.ui.components.DisclaimerDialog
import com.example.ui.components.WallpaperDetailSheet
import com.example.ui.screens.ExploreScreen
import com.example.ui.screens.FavoritesScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.WallhavenTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        // Global System Bar Edge-to-Edge Specification
        if (Build.VERSION.SDK_INT >= 35) {
            enableEdgeToEdge()
        } else {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            window.statusBarColor = android.graphics.Color.TRANSPARENT
        }
        // Set solid background so splash icon never renders behind page content
        window.setBackgroundDrawableResource(R.color.launch_background)

        setContent {
            val settings by viewModel.settings.collectAsState()
            val isSettingsLoaded by viewModel.isSettingsLoaded.collectAsState()

            // Notification permission request for Android 13+
            val context = LocalContext.current
            val notifPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { /* Handled */ }

            LaunchedEffect(Unit) {
                if (ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            WallhavenTheme(
                darkModePreference = settings.darkMode,
                dynamicColor = settings.dynamicColor,
                themeColor = settings.themeColor,
                colorStyle = settings.colorStyle,
                amoledBlack = settings.amoledBlack
            ) {
                // First-launch mandatory Disclaimer and Terms of Service pop-up
                if (isSettingsLoaded && !settings.disclaimerAccepted) {
                    DisclaimerDialog(
                        onAgree = {
                            viewModel.acceptDisclaimer()
                        },
                        onExit = {
                            finishAffinity()
                        }
                    )
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val currentTab by viewModel.currentTab.collectAsState()
                val selectedWallpaper by viewModel.selectedWallpaper.collectAsState()
                val favoriteIds by viewModel.favoriteIds.collectAsState()
                val downloadStatusMap by viewModel.downloadStatusMap.collectAsState()
                val isBatchDownloading by viewModel.isBatchDownloading.collectAsState()
                val tagTranslations by viewModel.tagTranslations.collectAsState()

                var isNavBarVisible by remember { mutableStateOf(true) }

                LaunchedEffect(currentTab) {
                    isNavBarVisible = true
                }

                // Smooth, unhindered onPostScroll nested scroll connection:
                // Only activated on content feed tabs (Explore and Search) when hideNavOnScroll is enabled.
                // Uses onPostScroll so child scroll containers experience ZERO drag resistance.
                val nestedScrollConnection = remember(settings.hideNavOnScroll, currentTab) {
                    if (!settings.hideNavOnScroll || (currentTab != 0 && currentTab != 1)) {
                        null
                    } else {
                        object : NestedScrollConnection {
                            private var accumulatedScroll = 0f

                            override fun onPostScroll(
                                consumed: Offset,
                                available: Offset,
                                source: NestedScrollSource
                            ): Offset {
                                val dy = consumed.y
                                if (dy < -10f) {
                                    // User dragging up (content moving up/scrolling deeper)
                                    if (accumulatedScroll > 0f) accumulatedScroll = 0f
                                    accumulatedScroll += dy
                                    if (accumulatedScroll < -75f && isNavBarVisible) {
                                        isNavBarVisible = false
                                    }
                                } else if (dy > 10f) {
                                    // User dragging down (content moving down/scrolling towards top)
                                    if (accumulatedScroll < 0f) accumulatedScroll = 0f
                                    accumulatedScroll += dy
                                    if (accumulatedScroll > 60f && !isNavBarVisible) {
                                        isNavBarVisible = true
                                    }
                                }
                                return Offset.Zero
                            }
                        }
                    }
                }

                val nestedScrollModifier = if (nestedScrollConnection != null) {
                    Modifier.nestedScroll(nestedScrollConnection)
                } else {
                    Modifier
                }

                // System Back button handling
                BackHandler(enabled = selectedWallpaper != null || currentTab != 0) {
                    if (selectedWallpaper != null) {
                        viewModel.dismissDetailSheet()
                    } else if (currentTab != 0) {
                        viewModel.selectTab(0)
                    }
                }

                Scaffold(
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    modifier = Modifier
                        .fillMaxSize()
                        .then(nestedScrollModifier),
                    bottomBar = {
                        AnimatedBottomBar(
                            currentTab = currentTab,
                            isNavBarVisible = isNavBarVisible,
                            hideNavOnScroll = settings.hideNavOnScroll,
                            selectedWallpaper = selectedWallpaper,
                            onTabSelected = { tab ->
                                viewModel.hapticHelper.vibrateTick(settings.hapticFeedback)
                                viewModel.selectTab(tab)
                            }
                        )
                    }
                ) { _ ->
                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        when (currentTab) {
                            0 -> {
                                val exploreWallpapers by viewModel.exploreWallpapers.collectAsState()
                                val isExploreLoading by viewModel.isExploreLoading.collectAsState()
                                val exploreError by viewModel.exploreError.collectAsState()
                                val currentSubTab by viewModel.exploreSubTab.collectAsState()
                                val currentFilter by viewModel.currentExploreFilter.collectAsState()

                                ExploreScreen(
                                    wallpapers = exploreWallpapers,
                                    isLoading = isExploreLoading,
                                    isEndReached = false,
                                    errorMessage = exploreError,
                                    favoriteIds = favoriteIds,
                                    currentSubTab = currentSubTab,
                                    filterState = currentFilter,
                                    hasApiKey = settings.apiKey.isNotBlank(),
                                    onSelectSubTab = { viewModel.selectExploreSubTab(it) },
                                    onApplyFilter = { viewModel.applyExploreFilter(it) },
                                    onResetFilter = { viewModel.resetExploreFilter() },
                                    onRemoveSingleFilter = { viewModel.removeSingleExploreFilter(it) },
                                    onNavigateToSettings = { viewModel.selectTab(4) },
                                    onLoadMore = { viewModel.loadExploreWallpapers(reset = false) },
                                    onRetry = { viewModel.loadExploreWallpapers(reset = true) },
                                    onWallpaperClick = { viewModel.selectWallpaper(it) },
                                    onFavoriteToggle = { viewModel.toggleFavorite(it) },
                                    hapticHelper = viewModel.hapticHelper,
                                    showFavoritesCount = settings.showFavoritesCount
                                )
                            }

                            1 -> {
                                val searchQuery by viewModel.searchQuery.collectAsState()
                                val searchResults by viewModel.searchResults.collectAsState()
                                val isSearchLoading by viewModel.isSearchLoading.collectAsState()
                                val searchError by viewModel.searchError.collectAsState()
                                val searchFilterState by viewModel.searchFilterState.collectAsState()
                                val selectedColor by viewModel.selectedSearchColor.collectAsState()
                                val tagSuggestions by viewModel.tagSuggestions.collectAsState()

                                SearchScreen(
                                    searchQuery = searchQuery,
                                    onQueryChange = { viewModel.onSearchQueryChange(it) },
                                    onPerformSearch = { viewModel.performSearch(reset = true) },
                                    wallpapers = searchResults,
                                    isLoading = isSearchLoading,
                                    isEndReached = false,
                                    errorMessage = searchError,
                                    favoriteIds = favoriteIds,
                                    filterState = searchFilterState,
                                    selectedColor = selectedColor,
                                    hasApiKey = settings.apiKey.isNotBlank(),
                                    onApplyFilter = { viewModel.applySearchFilter(it) },
                                    onResetFilter = { viewModel.resetSearchFilter() },
                                    onRemoveSingleFilter = { viewModel.removeSingleSearchFilter(it) },
                                    onSelectColor = { viewModel.setSearchColor(it) },
                                    onLoadMore = { viewModel.performSearch(reset = false) },
                                    onWallpaperClick = { viewModel.selectWallpaper(it) },
                                    onFavoriteToggle = { viewModel.toggleFavorite(it) },
                                    onNavigateToSettings = { viewModel.selectTab(4) },
                                    hapticHelper = viewModel.hapticHelper,
                                    showFavoritesCount = settings.showFavoritesCount,
                                    suggestions = tagSuggestions,
                                    onSelectSuggestion = { viewModel.selectTagSuggestion(it) },
                                    onClearSuggestions = { viewModel.clearTagSuggestions() },
                                    isTagLocalizationEnabled = settings.tagLocalization
                                )
                            }

                            2 -> {
                                val favorites by viewModel.favorites.collectAsState()
                                FavoritesScreen(
                                    favorites = favorites,
                                    onWallpaperClick = { viewModel.selectWallpaper(it) },
                                    onFavoriteToggle = { viewModel.toggleFavorite(it) },
                                    onBatchDownload = { list, callback ->
                                        viewModel.batchDownloadWallpapers(list, callback)
                                    },
                                    onBatchUnfavorite = { ids ->
                                        viewModel.batchUnfavorite(ids)
                                    },
                                    isBatchDownloading = isBatchDownloading,
                                    hapticHelper = viewModel.hapticHelper
                                )
                            }

                            3 -> {
                                val browseHistory by viewModel.browseHistory.collectAsState()
                                val downloadHistory by viewModel.downloadHistory.collectAsState()

                                HistoryScreen(
                                    browseHistory = browseHistory,
                                    downloadHistory = downloadHistory,
                                    onClearHistory = { viewModel.clearHistory() },
                                    onClearDownloads = { viewModel.clearDownloads() },
                                    onWallpaperClick = { viewModel.selectWallpaper(it) }
                                )
                            }

                            4 -> {
                                SettingsScreen(
                                    settings = settings,
                                    preferencesManager = viewModel.preferencesManager,
                                    backupManager = viewModel.backupManager,
                                    hapticHelper = viewModel.hapticHelper,
                                    onClearCacheNow = { onComplete ->
                                        viewModel.clearCacheNow(onComplete)
                                    },
                                    onClearHistory = { viewModel.clearHistory() },
                                    onClearDownloads = { viewModel.clearDownloads() }
                                )
                            }
                        }



                        // Wallpaper Detail Bottom Sheet
                        selectedWallpaper?.let { wp ->
                            val currentStatus = downloadStatusMap[wp.id]
                            val isFav = favoriteIds.contains(wp.id)
                            WallpaperDetailSheet(
                                wallpaper = wp,
                                isFavorite = isFav,
                                downloadStatus = currentStatus,
                                onFavoriteToggle = { viewModel.toggleFavorite(wp) },
                                onDownload = { viewModel.downloadWallpaper(wp) },
                                onSearchTag = { tag ->
                                    viewModel.dismissDetailSheet()
                                    viewModel.selectTab(1)
                                    viewModel.onSearchQueryChange(tag)
                                    viewModel.performSearch(reset = true)
                                },
                                onSearchColor = { hex ->
                                    viewModel.dismissDetailSheet()
                                    viewModel.selectTab(1)
                                    viewModel.setSearchColor(hex)
                                    viewModel.performSearch(reset = true)
                                },
                                onDismiss = { viewModel.dismissDetailSheet() },
                                hapticHelper = viewModel.hapticHelper,
                                wallpaperSetHelper = viewModel.wallpaperSetHelper,
                                tagTranslator = { viewModel.translateTag(it) },
                                tagTranslations = tagTranslations,
                                isTagLocalizationEnabled = settings.tagLocalization
                            )
                        }
                    }
                }
            }
        }
    }
}

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            val s = viewModel.settings.value
            if (s.autoClearCacheMode == "on_exit") {
                com.example.util.CacheManager.clearCache(applicationContext)
            }
        }
    }
}

@Composable
private fun AnimatedBottomBar(
    currentTab: Int,
    isNavBarVisible: Boolean,
    hideNavOnScroll: Boolean,
    selectedWallpaper: WallpaperDto?,
    onTabSelected: (Int) -> Unit
) {
    val navBarTranslationY by animateFloatAsState(
        targetValue = if (selectedWallpaper == null && (!hideNavOnScroll || isNavBarVisible)) 0f else 300f,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "nav_bar_slide"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { translationY = navBarTranslationY }
    ) {
        NavigationBar(
            windowInsets = WindowInsets.navigationBars,
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            modifier = Modifier.testTag("main_navigation_bar")
        ) {
            NavigationBarItem(
                selected = currentTab == 0,
                onClick = { onTabSelected(0) },
                icon = {
                    Icon(
                        imageVector = if (currentTab == 0) Icons.Filled.Explore else Icons.Outlined.Explore,
                        contentDescription = stringResource(R.string.nav_explore)
                    )
                },
                label = { Text(stringResource(R.string.nav_explore), fontSize = 11.sp) },
                modifier = Modifier.testTag("nav_explore_item")
            )

            NavigationBarItem(
                selected = currentTab == 1,
                onClick = { onTabSelected(1) },
                icon = {
                    Icon(
                        imageVector = if (currentTab == 1) Icons.Filled.Search else Icons.Outlined.Search,
                        contentDescription = stringResource(R.string.search_title)
                    )
                },
                label = { Text(stringResource(R.string.search_title), fontSize = 11.sp) },
                modifier = Modifier.testTag("nav_search_item")
            )

            NavigationBarItem(
                selected = currentTab == 2,
                onClick = { onTabSelected(2) },
                icon = {
                    Icon(
                        imageVector = if (currentTab == 2) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = stringResource(R.string.nav_favorites)
                    )
                },
                label = { Text(stringResource(R.string.nav_favorites), fontSize = 11.sp) },
                modifier = Modifier.testTag("nav_favorites_item")
            )

            NavigationBarItem(
                selected = currentTab == 3,
                onClick = { onTabSelected(3) },
                icon = {
                    Icon(
                        imageVector = if (currentTab == 3) Icons.Filled.History else Icons.Outlined.History,
                        contentDescription = stringResource(R.string.nav_history)
                    )
                },
                label = { Text(stringResource(R.string.nav_history), fontSize = 11.sp) },
                modifier = Modifier.testTag("nav_history_item")
            )

            NavigationBarItem(
                selected = currentTab == 4,
                onClick = { onTabSelected(4) },
                icon = {
                    Icon(
                        imageVector = if (currentTab == 4) Icons.Filled.Settings else Icons.Outlined.Settings,
                        contentDescription = stringResource(R.string.nav_settings)
                    )
                },
                label = { Text(stringResource(R.string.nav_settings), fontSize = 11.sp) },
                modifier = Modifier.testTag("nav_settings_item")
            )
        }
    }
}
