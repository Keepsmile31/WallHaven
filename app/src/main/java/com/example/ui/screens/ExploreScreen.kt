package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.WallpaperDto
import com.example.ui.components.ActiveFilterChip
import com.example.ui.components.AppHeaderBar
import com.example.ui.components.ExploreLiquidGlassFilterButton
import com.example.ui.components.ExploreLiquidGlassTabs
import com.example.ui.components.FilterBottomSheet
import com.example.ui.components.FilterState
import com.example.ui.components.RestrictedApiKeyDialog
import com.example.ui.components.WallpaperCard
import com.example.util.HapticHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    wallpapers: List<WallpaperDto>,
    isLoading: Boolean,
    isEndReached: Boolean,
    errorMessage: String?,
    favoriteIds: Set<String>,
    currentSubTab: Int, // 0=Latest, 1=Hot, 2=Random, 3=Top
    filterState: FilterState,
    hasApiKey: Boolean,
    onSelectSubTab: (Int) -> Unit,
    onApplyFilter: (FilterState) -> Unit,
    onResetFilter: () -> Unit,
    onRemoveSingleFilter: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
    onWallpaperClick: (WallpaperDto) -> Unit,
    onFavoriteToggle: (WallpaperDto) -> Unit,
    hapticHelper: HapticHelper,
    showFavoritesCount: Boolean = true,
    gridState: LazyStaggeredGridState = rememberLazyStaggeredGridState()
) {
    var showFilterSheet by remember { mutableStateOf(false) }
    var showApiKeyDialog by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()

    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val cutoutPadding = WindowInsets.displayCutout.asPaddingValues().calculateTopPadding()
    val topInset = maxOf(statusBarPadding, cutoutPadding)
    val navBottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    val shouldLoadMore by remember {
        derivedStateOf {
            val totalItems = gridState.layoutInfo.totalItemsCount
            val lastVisibleIndex = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisibleIndex >= totalItems - 6
        }
    }

    LaunchedEffect(shouldLoadMore, isLoading, isEndReached) {
        if (shouldLoadMore && !isLoading && !isEndReached) {
            onLoadMore()
        }
    }

    var lastSubTab by rememberSaveable { mutableIntStateOf(currentSubTab) }
    LaunchedEffect(currentSubTab) {
        if (lastSubTab != currentSubTab) {
            lastSubTab = currentSubTab
            gridState.scrollToItem(0)
        }
    }

    // API Key Dialog for Restricted Content
    if (showApiKeyDialog) {
        RestrictedApiKeyDialog(
            onDismiss = { showApiKeyDialog = false },
            onNavigateToSettings = onNavigateToSettings
        )
    }

    // Filter Bottom Sheet
    if (showFilterSheet) {
        FilterBottomSheet(
            sheetState = sheetState,
            initialFilterState = filterState,
            isTopTab = currentSubTab == 3,
            isRandomTab = currentSubTab == 2,
            isSearchMode = false,
            hasApiKey = hasApiKey,
            onDismissRequest = { showFilterSheet = false },
            onReset = onResetFilter,
            onConfirm = { updated ->
                onApplyFilter(updated)
                showFilterSheet = false
            },
            onRequireApiKey = {
                showApiKeyDialog = true
            }
        )
    }

    val backdropLayer = rememberGraphicsLayer()
    val backdropBaseColor = MaterialTheme.colorScheme.background

    Box(modifier = Modifier.fillMaxSize()) {
        // Wallpaper 2-column staggered grid recorded into backdropLayer for top Liquid Glass refraction
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    backdropLayer.record {
                        drawRect(color = backdropBaseColor)
                        this@drawWithContent.drawContent()
                    }
                    drawLayer(backdropLayer)
                }
        ) {
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(2),
                state = gridState,
                contentPadding = PaddingValues(
                    start = 8.dp,
                    end = 8.dp,
                    top = topInset + 108.dp, // Space for 2-row top section
                    bottom = navBottomInset + 88.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalItemSpacing = 8.dp,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("explore_wallpaper_grid")
            ) {
            // Active Filter Chips Bar
            val hasActive = filterState.hasActiveFilters(isTopTab = currentSubTab == 3)
            if (hasActive) {
                item(span = StaggeredGridItemSpan.FullLine, contentType = "filter_chips") {
                    ActiveFilterChipsRow(
                        filterState = filterState,
                        isTopTab = currentSubTab == 3,
                        onRemove = onRemoveSingleFilter
                    )
                }
            }

            // Wallpapers
            items(
                items = wallpapers,
                key = { it.id },
                contentType = { "wallpaper" }
            ) { wp ->
                WallpaperCard(
                    wallpaper = wp,
                    isFavorite = favoriteIds.contains(wp.id),
                    onFavoriteToggle = onFavoriteToggle,
                    onClick = onWallpaperClick,
                    showFavoritesCount = showFavoritesCount
                )
            }

            // Loading more spinner
            if (isLoading && wallpapers.isNotEmpty()) {
                item(span = StaggeredGridItemSpan.FullLine, contentType = "loading_spinner") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Dedicated prominent initial loading overlay (prevents blank screen)
        if (isLoading && wallpapers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = topInset + 104.dp, bottom = navBottomInset + 88.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(40.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 3.dp
                    )
                    Text(
                        text = "正在探索精美壁纸...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }
            }
        }

            // Error display with retry button
            if (errorMessage != null && wallpapers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = topInset + 108.dp, bottom = navBottomInset + 88.dp, start = 24.dp, end = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 14.sp
                        )
                        Button(onClick = onRetry) {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_refresh),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.action_retry))
                        }
                    }
                }
            }
        }

        // Translucent Top Header with Liquid Glass Segmented Tabs & Circular Filter Orb
        val headerFadeBrush = remember(backdropBaseColor) {
            Brush.verticalGradient(
                0.0f to backdropBaseColor.copy(alpha = 0.88f),
                0.48f to backdropBaseColor.copy(alpha = 0.68f),
                0.84f to backdropBaseColor.copy(alpha = 0.24f),
                1.0f to Color.Transparent
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .background(headerFadeBrush)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = topInset, bottom = 14.dp)
            ) {
                // Row 1: Refined Header Bar with App Icon + Title
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppHeaderBar(
                        title = "WallHaven"
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Row 2: 4 Tabs ("最新", "热门", "随机", "榜单") + Filter Button
                // Uses weight(1f) to guarantee flawless proportional fit on 347dp (6.1-inch) and all screens!
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val tabs = remember {
                        listOf(
                            0 to "最新",
                            1 to "热门",
                            2 to "随机",
                            3 to "榜单"
                        )
                    }

                    ExploreLiquidGlassTabs(
                        tabs = tabs,
                        selectedIndex = currentSubTab,
                        onSelect = { index ->
                            hapticHelper.vibrateTick()
                            if (index == currentSubTab) {
                                val isAtTop = gridState.firstVisibleItemIndex == 0 &&
                                    gridState.firstVisibleItemScrollOffset == 0
                                coroutineScope.launch {
                                    gridState.animateScrollToItem(0)
                                }
                                if (isAtTop && index != 2 && !isLoading) {
                                    onRetry()
                                }
                            } else {
                                onSelectSubTab(index)
                            }
                        },
                        onDoubleTap = { index ->
                            hapticHelper.vibrateTick()
                            if (index == 2) {
                                coroutineScope.launch {
                                    gridState.animateScrollToItem(0)
                                }
                                if (currentSubTab != 2) {
                                    onSelectSubTab(2)
                                } else {
                                    onRetry()
                                }
                            } else if (index == currentSubTab) {
                                val isAtTop = gridState.firstVisibleItemIndex == 0 &&
                                    gridState.firstVisibleItemScrollOffset == 0
                                coroutineScope.launch {
                                    gridState.animateScrollToItem(0)
                                }
                                if (isAtTop && !isLoading) {
                                    onRetry()
                                }
                            } else {
                                onSelectSubTab(index)
                            }
                        },
                        backdropLayer = backdropLayer,
                        modifier = Modifier.weight(1f)
                    )

                    val hasActiveFilters = filterState.hasActiveFilters(isTopTab = currentSubTab == 3)
                    ExploreLiquidGlassFilterButton(
                        hasActiveFilters = hasActiveFilters,
                        backdropLayer = backdropLayer,
                        onClick = {
                            hapticHelper.vibrateTick()
                            showFilterSheet = true
                        }
                    )
                }

                if (isLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .height(2.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color.Transparent
                    )
                }
            }
        }
    }
}

@Composable
private fun ActiveFilterChipsRow(
    filterState: FilterState,
    isTopTab: Boolean,
    onRemove: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (filterState.scenes.isNotEmpty()) {
            ActiveFilterChip(label = "风格: ${filterState.scenes.joinToString(",")}", onRemove = { onRemove("scenes") })
        }
        if (filterState.ratios.isNotEmpty()) {
            ActiveFilterChip(label = "比例: ${filterState.ratios.joinToString(",")}", onRemove = { onRemove("ratios") })
        }
        if (filterState.atleast.isNotBlank()) {
            ActiveFilterChip(label = "门槛: ≥${filterState.atleast}", onRemove = { onRemove("atleast") })
        }
        if (filterState.resolutions.isNotEmpty()) {
            ActiveFilterChip(label = "分辨率: ${filterState.resolutions.joinToString(",")}", onRemove = { onRemove("resolutions") })
        }
        if (filterState.color.isNotBlank()) {
            ActiveFilterChip(label = "色彩: #${filterState.color}", onRemove = { onRemove("color") })
        }
        if (filterState.order != "desc") {
            ActiveFilterChip(label = "排序: 正序", onRemove = { onRemove("order") })
        }
        if (filterState.categories != "110") {
            ActiveFilterChip(label = "类别定制", onRemove = { onRemove("categories") })
        }
        if (filterState.purity != "100") {
            ActiveFilterChip(label = "分级定制", onRemove = { onRemove("purity") })
        }
        if (isTopTab && filterState.topRange != "1M") {
            ActiveFilterChip(label = "周期: ${filterState.topRange}", onRemove = { onRemove("topRange") })
        }
    }
}
