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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.FavoriteBorder
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.WallpaperDto
import com.example.ui.MainViewModel
import com.example.ui.components.DisclaimerDialog
import com.example.ui.components.FloatingCapsuleNavigationBar
import com.example.ui.components.LocalLiquidGlassMode
import com.example.ui.components.NavBarDestination
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
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            val isSettingsLoaded by viewModel.isSettingsLoaded.collectAsStateWithLifecycle()

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

                CompositionLocalProvider(LocalLiquidGlassMode provides settings.liquidGlassMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
                val selectedWallpaper by viewModel.selectedWallpaper.collectAsStateWithLifecycle()
                val favoriteIds by viewModel.favoriteIds.collectAsStateWithLifecycle()
                val downloadStatusMap by viewModel.downloadStatusMap.collectAsStateWithLifecycle()
                val isBatchDownloading by viewModel.isBatchDownloading.collectAsStateWithLifecycle()
                val tagTranslations by viewModel.tagTranslations.collectAsStateWithLifecycle()

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

                val backdropLayer = rememberGraphicsLayer()

                val onTabSelectedCallback = remember(viewModel, settings.hapticFeedback) {
                    { tab: Int ->
                        viewModel.hapticHelper.vibrateTick(settings.hapticFeedback)
                        viewModel.selectTab(tab)
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
                            useFloatingCapsuleNav = settings.useFloatingCapsuleNav,
                            liquidGlassMode = settings.liquidGlassMode,
                            backdropLayer = backdropLayer,
                            isNavBarVisible = isNavBarVisible,
                            hideNavOnScroll = settings.hideNavOnScroll,
                            selectedWallpaper = selectedWallpaper,
                            onTabSelected = onTabSelectedCallback
                        )
                    }
                ) { _ ->
                    val surfaceBackgroundColor = MaterialTheme.colorScheme.background
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .drawWithContent {
                                backdropLayer.record {
                                    drawRect(color = surfaceBackgroundColor)
                                    this@drawWithContent.drawContent()
                                }
                                drawLayer(backdropLayer)
                            }
                    ) {
                        when (currentTab) {
                            0 -> {
                                val exploreWallpapers by viewModel.exploreWallpapers.collectAsStateWithLifecycle()
                                val isExploreLoading by viewModel.isExploreLoading.collectAsStateWithLifecycle()
                                val exploreError by viewModel.exploreError.collectAsStateWithLifecycle()
                                val currentSubTab by viewModel.exploreSubTab.collectAsStateWithLifecycle()
                                val currentFilter by viewModel.currentExploreFilter.collectAsStateWithLifecycle()

                                ExploreScreen(
                                    wallpapers = exploreWallpapers,
                                    isLoading = isExploreLoading,
                                    isEndReached = false,
                                    errorMessage = exploreError,
                                    favoriteIds = favoriteIds,
                                    currentSubTab = currentSubTab,
                                    filterState = currentFilter,
                                    hasApiKey = settings.apiKey.isNotBlank(),
                                    onSelectSubTab = viewModel::selectExploreSubTab,
                                    onApplyFilter = viewModel::applyExploreFilter,
                                    onResetFilter = viewModel::resetExploreFilter,
                                    onRemoveSingleFilter = viewModel::removeSingleExploreFilter,
                                    onNavigateToSettings = remember(viewModel) { { viewModel.selectTab(4) } },
                                    onLoadMore = remember(viewModel) { { viewModel.loadExploreWallpapers(reset = false) } },
                                    onRetry = remember(viewModel) { { viewModel.loadExploreWallpapers(reset = true) } },
                                    onWallpaperClick = viewModel::selectWallpaper,
                                    onFavoriteToggle = viewModel::toggleFavorite,
                                    hapticHelper = viewModel.hapticHelper,
                                    showFavoritesCount = settings.showFavoritesCount
                                )
                            }

                            1 -> {
                                val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
                                val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
                                val isSearchLoading by viewModel.isSearchLoading.collectAsStateWithLifecycle()
                                val searchError by viewModel.searchError.collectAsStateWithLifecycle()
                                val searchFilterState by viewModel.searchFilterState.collectAsStateWithLifecycle()
                                val selectedColor by viewModel.selectedSearchColor.collectAsStateWithLifecycle()
                                val tagSuggestions by viewModel.tagSuggestions.collectAsStateWithLifecycle()

                                SearchScreen(
                                    searchQuery = searchQuery,
                                    onQueryChange = viewModel::onSearchQueryChange,
                                    onPerformSearch = remember(viewModel) { { viewModel.performSearch(reset = true) } },
                                    wallpapers = searchResults,
                                    isLoading = isSearchLoading,
                                    isEndReached = false,
                                    errorMessage = searchError,
                                    favoriteIds = favoriteIds,
                                    filterState = searchFilterState,
                                    selectedColor = selectedColor,
                                    hasApiKey = settings.apiKey.isNotBlank(),
                                    onApplyFilter = viewModel::applySearchFilter,
                                    onResetFilter = viewModel::resetSearchFilter,
                                    onRemoveSingleFilter = viewModel::removeSingleSearchFilter,
                                    onSelectColor = viewModel::setSearchColor,
                                    onLoadMore = remember(viewModel) { { viewModel.performSearch(reset = false) } },
                                    onWallpaperClick = viewModel::selectWallpaper,
                                    onFavoriteToggle = viewModel::toggleFavorite,
                                    onNavigateToSettings = remember(viewModel) { { viewModel.selectTab(4) } },
                                    hapticHelper = viewModel.hapticHelper,
                                    showFavoritesCount = settings.showFavoritesCount,
                                    suggestions = tagSuggestions,
                                    onSelectSuggestion = viewModel::selectTagSuggestion,
                                    onClearSuggestions = viewModel::clearTagSuggestions,
                                    isTagLocalizationEnabled = settings.tagLocalization
                                )
                            }

                            2 -> {
                                val favorites by viewModel.favorites.collectAsStateWithLifecycle()
                                FavoritesScreen(
                                    favorites = favorites,
                                    onWallpaperClick = viewModel::selectWallpaper,
                                    onFavoriteToggle = viewModel::toggleFavorite,
                                    onBatchDownload = viewModel::batchDownloadWallpapers,
                                    onBatchUnfavorite = viewModel::batchUnfavorite,
                                    isBatchDownloading = isBatchDownloading,
                                    hapticHelper = viewModel.hapticHelper
                                )
                            }

                            3 -> {
                                val browseHistory by viewModel.browseHistory.collectAsStateWithLifecycle()
                                val downloadHistory by viewModel.downloadHistory.collectAsStateWithLifecycle()

                                HistoryScreen(
                                    browseHistory = browseHistory,
                                    downloadHistory = downloadHistory,
                                    onDeleteHistoryItem = viewModel::deleteHistoryItem,
                                    onBatchDeleteHistory = viewModel::batchDeleteHistory,
                                    onClearHistory = viewModel::clearHistory,
                                    onDeleteDownloadItem = viewModel::deleteDownloadItem,
                                    onBatchDeleteDownloads = viewModel::batchDeleteDownloads,
                                    onClearDownloads = viewModel::clearDownloads,
                                    onWallpaperClick = viewModel::selectWallpaper,
                                    hapticHelper = viewModel.hapticHelper
                                )
                            }

                            4 -> {
                                SettingsScreen(
                                    settings = settings,
                                    preferencesManager = viewModel.preferencesManager,
                                    backupManager = viewModel.backupManager,
                                    hapticHelper = viewModel.hapticHelper,
                                    onClearCacheNow = viewModel::clearCacheNow,
                                    onClearHistory = viewModel::clearHistory,
                                    onClearDownloads = viewModel::clearDownloads
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
    useFloatingCapsuleNav: Boolean,
    liquidGlassMode: String,
    backdropLayer: GraphicsLayer,
    isNavBarVisible: Boolean,
    hideNavOnScroll: Boolean,
    selectedWallpaper: WallpaperDto?,
    onTabSelected: (Int) -> Unit
) {
    val navBarTranslationYState = animateFloatAsState(
        targetValue = if (selectedWallpaper == null && (!hideNavOnScroll || isNavBarVisible)) 0f else 520f,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "nav_bar_slide"
    )

    val exploreLabel = stringResource(R.string.nav_explore)
    val searchLabel = stringResource(R.string.search_title)
    val favoritesLabel = stringResource(R.string.nav_favorites)
    val historyLabel = stringResource(R.string.nav_history)
    val settingsLabel = stringResource(R.string.nav_settings)
    val exploreFilledIcon = ImageVector.vectorResource(R.drawable.ic_explore_filled)
    val exploreOutlinedIcon = ImageVector.vectorResource(R.drawable.ic_explore_outlined)
    val historyIcon = ImageVector.vectorResource(R.drawable.ic_history)

    val destinations = remember(
        exploreLabel,
        searchLabel,
        favoritesLabel,
        historyLabel,
        settingsLabel,
        exploreFilledIcon,
        exploreOutlinedIcon,
        historyIcon
    ) {
        listOf(
            NavBarDestination(
                index = 0,
                label = exploreLabel,
                selectedIcon = exploreFilledIcon,
                unselectedIcon = exploreOutlinedIcon,
                testTag = "nav_explore_item"
            ),
            NavBarDestination(
                index = 1,
                label = searchLabel,
                selectedIcon = Icons.Filled.Search,
                unselectedIcon = Icons.Outlined.Search,
                testTag = "nav_search_item"
            ),
            NavBarDestination(
                index = 2,
                label = favoritesLabel,
                selectedIcon = Icons.Filled.Favorite,
                unselectedIcon = Icons.Outlined.FavoriteBorder,
                testTag = "nav_favorites_item"
            ),
            NavBarDestination(
                index = 3,
                label = historyLabel,
                selectedIcon = historyIcon,
                unselectedIcon = historyIcon,
                testTag = "nav_history_item"
            ),
            NavBarDestination(
                index = 4,
                label = settingsLabel,
                selectedIcon = Icons.Filled.Settings,
                unselectedIcon = Icons.Outlined.Settings,
                testTag = "nav_settings_item"
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { translationY = navBarTranslationYState.value }
    ) {
        if (useFloatingCapsuleNav) {
            FloatingCapsuleNavigationBar(
                destinations = destinations,
                selectedIndex = currentTab,
                onSelect = onTabSelected,
                backdropLayer = backdropLayer,
                liquidGlassMode = liquidGlassMode
            )
        } else {
            NavigationBar(
                windowInsets = WindowInsets.navigationBars,
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                modifier = Modifier.testTag("main_navigation_bar")
            ) {
                destinations.forEach { dest ->
                    val isSelected = currentTab == dest.index
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { onTabSelected(dest.index) },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) dest.selectedIcon else dest.unselectedIcon,
                                contentDescription = dest.label
                            )
                        },
                        label = { Text(dest.label, fontSize = 11.sp) },
                        modifier = Modifier.testTag(dest.testTag)
                    )
                }
            }
        }
    }
}
