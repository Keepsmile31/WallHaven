package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import com.example.R
import com.example.data.model.WallpaperDto
import com.example.data.repository.TagSuggestion
import com.example.ui.components.AppHeaderBar
import com.example.ui.components.FilterBottomSheet
import com.example.ui.components.FilterState
import com.example.ui.components.WallpaperCard
import com.example.util.HapticHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    onPerformSearch: () -> Unit,
    wallpapers: List<WallpaperDto>,
    isLoading: Boolean,
    isEndReached: Boolean,
    errorMessage: String?,
    favoriteIds: Set<String>,
    filterState: FilterState,
    selectedColor: String,
    hasApiKey: Boolean,
    onApplyFilter: (FilterState) -> Unit,
    onResetFilter: () -> Unit,
    onRemoveSingleFilter: (String) -> Unit,
    onSelectColor: (String) -> Unit,
    onLoadMore: () -> Unit,
    onWallpaperClick: (WallpaperDto) -> Unit,
    onFavoriteToggle: (WallpaperDto) -> Unit,
    onNavigateToSettings: () -> Unit,
    hapticHelper: HapticHelper,
    showFavoritesCount: Boolean = true,
    suggestions: List<TagSuggestion> = emptyList(),
    onSelectSuggestion: (TagSuggestion) -> Unit = {},
    onClearSuggestions: () -> Unit = {},
    isTagLocalizationEnabled: Boolean = false
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val gridState = rememberLazyStaggeredGridState()
    var showFilterSheet by remember { mutableStateOf(false) }
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var isDropdownExpanded by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(suggestions, searchQuery, isTagLocalizationEnabled) {
        isDropdownExpanded = isTagLocalizationEnabled && suggestions.isNotEmpty() && searchQuery.isNotBlank()
    }

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

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore && !isLoading && !isEndReached) {
            onLoadMore()
        }
    }

    // API Key Dialog
    if (showApiKeyDialog) {
        AlertDialog(
            onDismissRequest = { showApiKeyDialog = false },
            title = { Text(stringResource(R.string.need_api_key_for_restricted_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(R.string.need_api_key_for_restricted_msg), fontSize = 13.sp)
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://1815318986.share.123pan.cn/123pan/ngLtVv-N3Fm3"))
                            try {
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.channel_123pan), fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showApiKeyDialog = false
                        onNavigateToSettings()
                    }
                ) {
                    Text(stringResource(R.string.go_to_settings))
                }
            },
            dismissButton = {
                TextButton(onClick = { showApiKeyDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    // Filter Bottom Sheet
    if (showFilterSheet) {
        FilterBottomSheet(
            sheetState = sheetState,
            initialFilterState = filterState,
            isTopTab = false,
            isRandomTab = false,
            isSearchMode = true,
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

    Box(modifier = Modifier.fillMaxSize()) {
        // Wallpaper 2-column uncropped staggered grid
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(2),
            state = gridState,
            contentPadding = PaddingValues(
                start = 8.dp,
                end = 8.dp,
                top = topInset + 118.dp,
                bottom = navBottomInset + 88.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalItemSpacing = 8.dp,
            modifier = Modifier
                .fillMaxSize()
                .testTag("search_wallpaper_grid")
        ) {
            // Active search filter chips
            val hasActiveFilters = filterState.hasActiveFilters(isSearch = true) || selectedColor.isNotBlank()
            if (hasActiveFilters) {
                item(span = StaggeredGridItemSpan.FullLine) {
                    ActiveSearchChipsRow(
                        filterState = filterState,
                        selectedColor = selectedColor,
                        onRemove = onRemoveSingleFilter
                    )
                }
            }

            // Results
            items(
                items = wallpapers,
                key = { it.id }
            ) { wp ->
                WallpaperCard(
                    wallpaper = wp,
                    isFavorite = favoriteIds.contains(wp.id),
                    onFavoriteToggle = { onFavoriteToggle(wp) },
                    onClick = { onWallpaperClick(wp) },
                    showFavoritesCount = showFavoritesCount
                )
            }

            // Loading more spinner
            if (isLoading && wallpapers.isNotEmpty()) {
                item(span = StaggeredGridItemSpan.FullLine) {
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

            // Empty state matching Screenshot 2
            if (!isLoading && wallpapers.isEmpty()) {
                item(span = StaggeredGridItemSpan.FullLine) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "输入关键词或通过右上角筛选查找壁纸",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        // Dedicated prominent initial search loading overlay
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
                        text = "正在搜索壁纸...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }
            }
        }

        // Top Section Design matching Screenshot 2
        Surface(
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
            shadowElevation = 2.dp,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = topInset, bottom = 10.dp)
            ) {
                // Row 1: Refined Header Bar with App Icon + Title
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppHeaderBar(
                        title = stringResource(R.string.search_title)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Row 2: Capsule Search Field + Circular Filter Button (Screenshot 2)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ExposedDropdownMenuBox(
                        expanded = isTagLocalizationEnabled && isDropdownExpanded && suggestions.isNotEmpty(),
                        onExpandedChange = { if (isTagLocalizationEnabled) isDropdownExpanded = it },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = onQueryChange,
                            placeholder = {
                                Text(
                                    text = "搜索壁纸、标签或关键词...",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = {
                                        onQueryChange("")
                                        onClearSuggestions()
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Clear",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = {
                                focusManager.clearFocus()
                                isDropdownExpanded = false
                                onPerformSearch()
                            }),
                            shape = RoundedCornerShape(50),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.outline,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            ),
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("search_text_input")
                        )

                        ExposedDropdownMenu(
                            expanded = isTagLocalizationEnabled && isDropdownExpanded && suggestions.isNotEmpty(),
                            onDismissRequest = { isDropdownExpanded = false },
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                .clip(RoundedCornerShape(16.dp))
                        ) {
                            suggestions.forEach { suggestion ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${suggestion.chineseName} - ${suggestion.englishTag}",
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = FontWeight.Medium
                                                ),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            if (suggestion.category.isNotEmpty()) {
                                                Text(
                                                    text = suggestion.category,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Search,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    },
                                    onClick = {
                                        onSelectSuggestion(suggestion)
                                        isDropdownExpanded = false
                                        focusManager.clearFocus()
                                    },
                                    modifier = Modifier.testTag("tag_suggestion_${suggestion.englishTag}")
                                )
                            }
                        }
                    }

                    // Circular Filter Button matching Screenshot 2
                    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
                    val hasFilters = filterState.hasActiveFilters(isSearch = true)
                    Surface(
                        shape = CircleShape,
                        color = if (isDarkTheme) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .clickable {
                                hapticHelper.vibrateTick()
                                showFilterSheet = true
                            }
                            .testTag("search_filter_btn")
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            BadgedBox(
                                badge = {
                                    if (hasFilters) {
                                        Badge(containerColor = MaterialTheme.colorScheme.primary, modifier = Modifier.size(8.dp))
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FilterList,
                                    contentDescription = stringResource(R.string.action_filter),
                                    tint = if (hasFilters) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }

                if (isLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .height(2.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = androidx.compose.ui.graphics.Color.Transparent
                    )
                }
            }
        }
    }
}

@Composable
private fun ActiveSearchChipsRow(
    filterState: FilterState,
    selectedColor: String,
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
        val activeColor = filterState.color.ifBlank { selectedColor }
        if (activeColor.isNotBlank()) {
            ActiveChip(label = "色彩: #$activeColor", onRemove = { onRemove("color") })
        }
        if (filterState.sorting != "relevance") {
            ActiveChip(label = "排序: ${filterState.sorting}", onRemove = { onRemove("sorting") })
        }
        if (filterState.order != "desc") {
            ActiveChip(label = "排序: 正序", onRemove = { onRemove("order") })
        }
        if (filterState.sorting == "toplist" && filterState.topRange != "1M") {
            ActiveChip(label = "周期: ${filterState.topRange}", onRemove = { onRemove("topRange") })
        }
        if (filterState.scenes.isNotEmpty()) {
            ActiveChip(label = "风格: ${filterState.scenes.joinToString(",")}", onRemove = { onRemove("scenes") })
        }
        if (filterState.ratios.isNotEmpty()) {
            ActiveChip(label = "比例: ${filterState.ratios.joinToString(",")}", onRemove = { onRemove("ratios") })
        }
        if (filterState.atleast.isNotBlank()) {
            ActiveChip(label = "门槛: ≥${filterState.atleast}", onRemove = { onRemove("atleast") })
        }
        if (filterState.resolutions.isNotEmpty()) {
            ActiveChip(label = "分辨率: ${filterState.resolutions.joinToString(",")}", onRemove = { onRemove("resolutions") })
        }
        if (filterState.categories != "111") {
            ActiveChip(label = "类别定制", onRemove = { onRemove("categories") })
        }
        if (filterState.purity != "100") {
            ActiveChip(label = "分级定制", onRemove = { onRemove("purity") })
        }
    }
}

@Composable
private fun ActiveChip(label: String, onRemove: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
        modifier = Modifier.clip(RoundedCornerShape(50))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 10.dp, end = 6.dp, top = 4.dp, bottom = 4.dp)
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.Clear,
                contentDescription = "Remove filter",
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier
                    .size(14.dp)
                    .clickable { onRemove() }
            )
        }
    }
}
