package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.db.SearchHistoryEntity
import com.example.data.model.WallpaperDto
import com.example.data.repository.TagSuggestion
import com.example.ui.components.ActiveFilterChip
import com.example.ui.components.AppHeaderBar
import com.example.ui.components.FilterBottomSheet
import com.example.ui.components.FilterState
import com.example.ui.components.RestrictedApiKeyDialog
import com.example.ui.components.WallpaperCard
import com.example.util.HapticHelper

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit = { onQueryChange("") },
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
    searchHistory: List<SearchHistoryEntity> = emptyList(),
    onSelectSearchHistory: (String) -> Unit = {},
    onDeleteSearchHistory: (String) -> Unit = {},
    onOpenAllSearchHistory: () -> Unit = {},
    isTagLocalizationEnabled: Boolean = false,
    gridState: LazyStaggeredGridState = rememberLazyStaggeredGridState()
) {
    val focusManager = LocalFocusManager.current
    var showFilterSheet by remember { mutableStateOf(false) }
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var isSearchFocused by remember { mutableStateOf(false) }
    var isDropdownExpanded by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val searchInteractionSource = remember { MutableInteractionSource() }

    // Maintain TextFieldValue locally so Android Pinyin IME composition state is never interrupted
    var textFieldValue by remember {
        mutableStateOf(TextFieldValue(text = searchQuery, selection = TextRange(searchQuery.length)))
    }
    LaunchedEffect(searchQuery) {
        if (textFieldValue.text != searchQuery) {
            textFieldValue = TextFieldValue(
                text = searchQuery,
                selection = TextRange(searchQuery.length)
            )
        }
    }

    val trimmedQuery = remember(searchQuery) { searchQuery.trim() }
    val recentTenHistory = remember(searchHistory) { searchHistory.take(10) }
    val matchingHistory = remember(searchHistory, trimmedQuery) {
        if (trimmedQuery.isEmpty()) {
            searchHistory.take(10)
        } else {
            searchHistory.filter {
                it.query.contains(trimmedQuery, ignoreCase = true) &&
                    !it.query.equals(trimmedQuery, ignoreCase = true)
            }.take(10)
        }
    }

    val hasSuggestions = isTagLocalizationEnabled && suggestions.isNotEmpty() && trimmedQuery.isNotEmpty()
    // Inline section below the search box shows recent 10 entries + "All" button in initial/cleared state;
    // Dropdown only needs to show while typing or when tapping the search box over existing search results.
    val showInlineHistorySection = wallpapers.isEmpty() && !isLoading
    val showHistoryInDropdown = matchingHistory.isNotEmpty() && (trimmedQuery.isNotEmpty() || !showInlineHistorySection)
    val hasDropdownItems = hasSuggestions || showHistoryInDropdown

    LaunchedEffect(isSearchFocused, hasSuggestions, hasDropdownItems, trimmedQuery, showInlineHistorySection) {
        isDropdownExpanded = when {
            !hasDropdownItems -> false
            isSearchFocused && (trimmedQuery.isNotEmpty() || !showInlineHistorySection) -> true
            else -> isDropdownExpanded && !showInlineHistorySection
        }
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

    LaunchedEffect(shouldLoadMore, isLoading, isEndReached) {
        if (shouldLoadMore && !isLoading && !isEndReached) {
            onLoadMore()
        }
    }

    // API Key Dialog
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
                top = topInset + 122.dp,
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
                item(span = StaggeredGridItemSpan.FullLine, contentType = "filter_chips") {
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

            // Recent search history + initial idle hint directly below search box
            if (!isLoading && wallpapers.isEmpty()) {
                if (recentTenHistory.isNotEmpty()) {
                    item(span = StaggeredGridItemSpan.FullLine, contentType = "inline_search_history") {
                        InlineSearchHistorySection(
                            recentHistory = recentTenHistory,
                            onSelectHistory = { query ->
                                hapticHelper.vibrateTick()
                                focusManager.clearFocus()
                                isDropdownExpanded = false
                                onSelectSearchHistory(query)
                            },
                            onDeleteHistory = { query ->
                                hapticHelper.vibrateTick()
                                onDeleteSearchHistory(query)
                            },
                            onOpenAll = {
                                hapticHelper.vibrateTick()
                                focusManager.clearFocus()
                                isDropdownExpanded = false
                                onOpenAllSearchHistory()
                            }
                        )
                    }
                }

                item(span = StaggeredGridItemSpan.FullLine, contentType = "empty_state") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = if (recentTenHistory.isNotEmpty()) 48.dp else 160.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = errorMessage ?: "输入关键词或通过右上角筛选查找壁纸",
                            color = if (errorMessage != null) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            },
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

        // Top Section Design
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

                // Row 2: Capsule Search Field + Circular Filter Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val textFieldColors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.outline,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    )

                    ExposedDropdownMenuBox(
                        expanded = isDropdownExpanded && hasDropdownItems,
                        onExpandedChange = { expanded ->
                            isDropdownExpanded = if (expanded) hasDropdownItems else false
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        BasicTextField(
                            value = textFieldValue,
                            onValueChange = { updated ->
                                textFieldValue = updated
                                if (updated.text != searchQuery) {
                                    onQueryChange(updated.text)
                                }
                            },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 15.sp,
                                lineHeight = 22.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = {
                                focusManager.clearFocus()
                                isDropdownExpanded = false
                                onPerformSearch()
                            }),
                            interactionSource = searchInteractionSource,
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryEditable, enabled = true)
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp)
                                .onFocusChanged { focusState ->
                                    isSearchFocused = focusState.isFocused
                                }
                                .testTag("search_text_input")
                        ) { innerTextField ->
                            OutlinedTextFieldDefaults.DecorationBox(
                                value = textFieldValue.text,
                                innerTextField = innerTextField,
                                enabled = true,
                                singleLine = true,
                                visualTransformation = VisualTransformation.None,
                                interactionSource = searchInteractionSource,
                                placeholder = {
                                    Text(
                                        text = "搜索壁纸、标签或关键词...",
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = ImageVector.vectorResource(R.drawable.ic_search),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                trailingIcon = {
                                    if (textFieldValue.text.isNotEmpty() || isLoading) {
                                        IconButton(
                                            onClick = {
                                                hapticHelper.vibrateTick()
                                                textFieldValue = TextFieldValue("")
                                                onClearSuggestions()
                                                onClearSearch()
                                                isDropdownExpanded = false
                                                focusManager.clearFocus()
                                            },
                                            modifier = Modifier
                                                .size(48.dp)
                                                .testTag("search_clear_btn")
                                        ) {
                                            Icon(
                                                imageVector = ImageVector.vectorResource(R.drawable.ic_close),
                                                contentDescription = stringResource(R.string.action_cancel),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                },
                                colors = textFieldColors,
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 10.dp),
                                container = {
                                    OutlinedTextFieldDefaults.Container(
                                        enabled = true,
                                        isError = false,
                                        interactionSource = searchInteractionSource,
                                        colors = textFieldColors,
                                        shape = RoundedCornerShape(50)
                                    )
                                }
                            )
                        }

                        ExposedDropdownMenu(
                            expanded = isDropdownExpanded && hasDropdownItems,
                            onDismissRequest = { isDropdownExpanded = false },
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                .clip(RoundedCornerShape(16.dp))
                                .testTag("search_dropdown_menu")
                        ) {
                            if (showHistoryInDropdown) {
                                Text(
                                    text = stringResource(R.string.search_history_title),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                                )

                                matchingHistory.forEach { historyItem ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = historyItem.query,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = ImageVector.vectorResource(R.drawable.ic_history),
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        },
                                        trailingIcon = {
                                            IconButton(
                                                onClick = {
                                                    hapticHelper.vibrateTick()
                                                    onDeleteSearchHistory(historyItem.query)
                                                },
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .testTag("delete_search_history_${historyItem.query}")
                                            ) {
                                                Icon(
                                                    imageVector = ImageVector.vectorResource(R.drawable.ic_close),
                                                    contentDescription = stringResource(R.string.search_history_delete),
                                                    modifier = Modifier.size(16.dp),
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        },
                                        onClick = {
                                            hapticHelper.vibrateTick()
                                            onSelectSearchHistory(historyItem.query)
                                            isDropdownExpanded = false
                                            focusManager.clearFocus()
                                        },
                                        modifier = Modifier.testTag("search_history_item_${historyItem.query}")
                                    )
                                }

                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = stringResource(R.string.search_history_all),
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.SemiBold
                                            ),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    },
                                    trailingIcon = {
                                        Icon(
                                            imageVector = ImageVector.vectorResource(R.drawable.ic_keyboard_arrow_right),
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    },
                                    onClick = {
                                        hapticHelper.vibrateTick()
                                        isDropdownExpanded = false
                                        focusManager.clearFocus()
                                        onOpenAllSearchHistory()
                                    },
                                    modifier = Modifier.testTag("dropdown_search_history_all_btn")
                                )
                            }

                            if (showHistoryInDropdown && hasSuggestions) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                                Text(
                                    text = stringResource(R.string.search_tag_suggestions_title),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                                )
                            }

                            if (hasSuggestions) {
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
                                                imageVector = ImageVector.vectorResource(R.drawable.ic_search),
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
                    }

                    // Circular Filter Button
                    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
                    val hasFilters = filterState.hasActiveFilters(isSearch = true)
                    Surface(
                        shape = CircleShape,
                        color = if (isDarkTheme) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        modifier = Modifier
                            .size(48.dp)
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
                                    imageVector = ImageVector.vectorResource(R.drawable.ic_filter_list),
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
                        trackColor = Color.Transparent
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InlineSearchHistorySection(
    recentHistory: List<SearchHistoryEntity>,
    onSelectHistory: (String) -> Unit,
    onDeleteHistory: (String) -> Unit,
    onOpenAll: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 10.dp)
            .testTag("search_history_inline_section"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.ic_history),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = stringResource(R.string.search_history_title),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_history_inline_list"),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            recentHistory.forEach { item ->
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    border = BorderStroke(
                        width = 0.8.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                    ),
                    modifier = Modifier.heightIn(min = 40.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            contentAlignment = Alignment.CenterStart,
                            modifier = Modifier
                                .defaultMinSize(minHeight = 40.dp)
                                .clip(RoundedCornerShape(topStart = 50.dp, bottomStart = 50.dp))
                                .clickable { onSelectHistory(item.query) }
                                .padding(start = 12.dp, end = 6.dp)
                                .testTag("search_history_chip_${item.query}")
                        ) {
                            Text(
                                text = item.query,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .size(32.dp)
                                .clip(CircleShape)
                                .clickable { onDeleteHistory(item.query) }
                                .testTag("delete_inline_search_history_${item.query}")
                        ) {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_close),
                                contentDescription = stringResource(R.string.search_history_delete),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            // "All" button immediately following the 10th (or final) history entry
            Surface(
                onClick = onOpenAll,
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                modifier = Modifier
                    .heightIn(min = 40.dp)
                    .testTag("search_history_all_btn")
            ) {
                Row(
                    modifier = Modifier.padding(start = 14.dp, end = 10.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = stringResource(R.string.search_history_all),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_keyboard_arrow_right),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(16.dp)
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
            ActiveFilterChip(label = "色彩: #$activeColor", onRemove = { onRemove("color") })
        }
        if (filterState.sorting != "relevance") {
            ActiveFilterChip(label = "排序: ${filterState.sorting}", onRemove = { onRemove("sorting") })
        }
        if (filterState.order != "desc") {
            ActiveFilterChip(label = "排序: 正序", onRemove = { onRemove("order") })
        }
        if (filterState.sorting == "toplist" && filterState.topRange != "1M") {
            ActiveFilterChip(label = "周期: ${filterState.topRange}", onRemove = { onRemove("topRange") })
        }
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
        if (filterState.categories != "111") {
            ActiveFilterChip(label = "类别定制", onRemove = { onRemove("categories") })
        }
        if (filterState.purity != "100") {
            ActiveFilterChip(label = "分级定制", onRemove = { onRemove("purity") })
        }
    }
}

