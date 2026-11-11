package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.data.db.DownloadEntity
import com.example.data.db.HistoryEntity
import com.example.data.model.ThumbsDto
import com.example.data.model.WallpaperDto
import com.example.ui.components.AppHeaderBar
import com.example.ui.components.ExploreLiquidGlassCircleButton
import com.example.ui.components.ExploreLiquidGlassTabs
import com.example.util.HapticHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryScreen(
    browseHistory: List<HistoryEntity>,
    downloadHistory: List<DownloadEntity>,
    onDeleteHistoryItem: (String) -> Unit,
    onBatchDeleteHistory: (List<String>) -> Unit,
    onClearHistory: () -> Unit,
    onDeleteDownloadItem: (String) -> Unit,
    onBatchDeleteDownloads: (List<String>) -> Unit,
    onClearDownloads: () -> Unit,
    onWallpaperClick: (WallpaperDto) -> Unit,
    hapticHelper: HapticHelper? = null
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var isBatchMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    var showClearDialog by remember { mutableStateOf(false) }
    var showBatchDeleteDialog by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()) }

    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val cutoutPadding = WindowInsets.displayCutout.asPaddingValues().calculateTopPadding()
    val topInset = maxOf(statusBarPadding, cutoutPadding)
    val navBottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    val currentIds = remember(selectedTab, browseHistory, downloadHistory) {
        if (selectedTab == 0) browseHistory.map { it.id } else downloadHistory.map { it.id }
    }
    val hasItemsInCurrentTab = currentIds.isNotEmpty()

    LaunchedEffect(selectedTab) {
        selectedIds = emptySet()
        if (!hasItemsInCurrentTab) {
            isBatchMode = false
        }
    }

    LaunchedEffect(currentIds) {
        if (currentIds.isEmpty()) {
            isBatchMode = false
            selectedIds = emptySet()
        } else {
            val validSet = currentIds.toSet()
            val pruned = selectedIds.intersect(validSet)
            if (pruned.size != selectedIds.size) {
                selectedIds = pruned
            }
        }
    }

    BackHandler(enabled = isBatchMode) {
        isBatchMode = false
        selectedIds = emptySet()
    }

    // Clear All Confirmation Dialog
    if (showClearDialog) {
        val titleText = if (selectedTab == 0) "清空浏览历史" else "清空下载历史"
        val messageText = if (selectedTab == 0) {
            "确定清空全部壁纸浏览历史吗？此操作无法撤销。"
        } else {
            "确定清空全部下载历史记录吗？（已下载的本地文件不会被删除）"
        }
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(titleText) },
            text = { Text(messageText) },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (selectedTab == 0) onClearHistory() else onClearDownloads()
                        selectedIds = emptySet()
                        isBatchMode = false
                        showClearDialog = false
                    }
                ) {
                    Text(stringResource(R.string.action_confirm), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    // Batch Delete Selected Confirmation Dialog
    if (showBatchDeleteDialog) {
        val titleText = if (selectedTab == 0) "批量删除浏览历史" else "批量删除下载历史"
        val messageText = if (selectedTab == 0) {
            "确定删除选中的 ${selectedIds.size} 条浏览历史记录吗？此操作无法撤销。"
        } else {
            "确定删除选中的 ${selectedIds.size} 条下载历史记录吗？（已下载的本地文件不会被删除）"
        }
        AlertDialog(
            onDismissRequest = { showBatchDeleteDialog = false },
            title = { Text(titleText) },
            text = { Text(messageText) },
            confirmButton = {
                Button(
                    onClick = {
                        val idsToDelete = selectedIds.toList()
                        if (selectedTab == 0) {
                            onBatchDeleteHistory(idsToDelete)
                        } else {
                            onBatchDeleteDownloads(idsToDelete)
                        }
                        selectedIds = emptySet()
                        isBatchMode = false
                        showBatchDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.action_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showBatchDeleteDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    val toggleSelection: (String) -> Unit = remember(hapticHelper) {
        { id ->
            hapticHelper?.vibrateTick()
            selectedIds = if (selectedIds.contains(id)) {
                selectedIds - id
            } else {
                selectedIds + id
            }
        }
    }

    val enterBatchWithItem: (String) -> Unit = remember(hapticHelper) {
        { id ->
            hapticHelper?.vibrateTick()
            isBatchMode = true
            selectedIds = selectedIds + id
        }
    }

    val listBottomPadding = navBottomInset + if (isBatchMode) 168.dp else 88.dp
    val backdropLayer = rememberGraphicsLayer()
    val backdropBaseColor = MaterialTheme.colorScheme.background

    Box(modifier = Modifier.fillMaxSize()) {
        // Content List recorded into backdropLayer for top Liquid Glass refraction
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
            if (selectedTab == 0) {
                if (browseHistory.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_history),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = stringResource(R.string.empty_history),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = topInset + 120.dp,
                        bottom = listBottomPadding
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize().testTag("browse_history_list")
                ) {
                    items(
                        items = browseHistory,
                        key = { it.id },
                        contentType = { "history_item" }
                    ) { item ->
                        val isSelected = selectedIds.contains(item.id)
                        HistoryCard(
                            id = item.id,
                            thumbUrl = item.thumbUrl,
                            resolution = item.resolution,
                            timestamp = item.viewedAt,
                            dateFormat = dateFormat,
                            isBatchMode = isBatchMode,
                            isSelected = isSelected,
                            onDeleteSingle = { onDeleteHistoryItem(item.id) },
                            onLongClick = {
                                if (!isBatchMode) {
                                    enterBatchWithItem(item.id)
                                } else {
                                    toggleSelection(item.id)
                                }
                            },
                            onClick = {
                                if (isBatchMode) {
                                    toggleSelection(item.id)
                                } else {
                                    onWallpaperClick(
                                        WallpaperDto(
                                            id = item.id,
                                            url = item.url,
                                            path = item.fullPath,
                                            resolution = item.resolution,
                                            category = item.category,
                                            ratio = item.ratio,
                                            thumbs = ThumbsDto(
                                                large = item.thumbUrl,
                                                original = item.fullPath,
                                                small = item.thumbUrl
                                            )
                                        )
                                    )
                                }
                            }
                        )
                    }
                }
            }
        } else {
            if (downloadHistory.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_download),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = stringResource(R.string.empty_downloads),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = topInset + 120.dp,
                        bottom = listBottomPadding
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize().testTag("download_history_list")
                ) {
                    items(
                        items = downloadHistory,
                        key = { it.id },
                        contentType = { "download_item" }
                    ) { item ->
                        val isSelected = selectedIds.contains(item.id)
                        DownloadHistoryCard(
                            item = item,
                            dateFormat = dateFormat,
                            isBatchMode = isBatchMode,
                            isSelected = isSelected,
                            onDeleteSingle = { onDeleteDownloadItem(item.id) },
                            onOpenLocal = {
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(Uri.parse(item.localUri), "image/*")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                try {
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            },
                            onLongClick = {
                                if (!isBatchMode) {
                                    enterBatchWithItem(item.id)
                                } else {
                                    toggleSelection(item.id)
                                }
                            },
                            onClick = {
                                if (isBatchMode) {
                                    toggleSelection(item.id)
                                } else {
                                    onWallpaperClick(
                                        WallpaperDto(
                                            id = item.id,
                                            url = "https://wallhaven.cc/w/${item.id}",
                                            path = item.fullPath,
                                            resolution = item.resolution,
                                            thumbs = ThumbsDto(large = item.thumbUrl)
                                        )
                                    )
                                }
                            }
                        )
                    }
                }
                }
            }
        }

        // Translucent Top Header with FlClash-inspired Liquid Glass Tabs & Clear Orb
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
                // Row 1: Header Bar with App Icon + Title + Batch Management Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppHeaderBar(
                        title = stringResource(R.string.nav_history),
                        trailing = {
                            if (hasItemsInCurrentTab) {
                                FilledTonalButton(
                                    onClick = {
                                        hapticHelper?.vibrateTick()
                                        isBatchMode = !isBatchMode
                                        if (!isBatchMode) {
                                            selectedIds = emptySet()
                                        }
                                    },
                                    modifier = Modifier.testTag("history_batch_mode_btn")
                                ) {
                                    Text(
                                        text = if (isBatchMode) stringResource(R.string.batch_exit) else stringResource(R.string.batch_select),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Row 2: Liquid Glass Tabs (浏览历史, 下载历史) + Liquid Glass Clear All Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val browseTabLabel = stringResource(R.string.tab_browse_history)
                    val downloadTabLabel = stringResource(R.string.tab_download_history)
                    val historyTabs = remember(browseTabLabel, downloadTabLabel) {
                        listOf(
                            0 to browseTabLabel,
                            1 to downloadTabLabel
                        )
                    }
                    ExploreLiquidGlassTabs(
                        tabs = historyTabs,
                        selectedIndex = selectedTab,
                        onSelect = { index ->
                            hapticHelper?.vibrateTick()
                            selectedTab = index
                        },
                        backdropLayer = backdropLayer,
                        testTagForTab = { index ->
                            if (index == 0) "tab_browse_history" else "tab_download_history"
                        },
                        modifier = Modifier.weight(1f)
                    )

                    ExploreLiquidGlassCircleButton(
                        icon = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.history_clear_all),
                        tint = if (hasItemsInCurrentTab) {
                            Color(0xFFD32F2F)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                        },
                        backdropLayer = backdropLayer,
                        enabled = hasItemsInCurrentTab,
                        onClick = {
                            hapticHelper?.vibrateTick()
                            showClearDialog = true
                        },
                        modifier = Modifier.testTag("clear_history_btn")
                    )
                }
            }
        }

        // Floating Batch Action Bar at Bottom
        AnimatedVisibility(
            visible = isBatchMode && hasItemsInCurrentTab,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = navBottomInset + 84.dp, start = 12.dp, end = 12.dp)
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("history_batch_action_bar")
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.batch_selected_count, selectedIds.size),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )

                        val allSelected = selectedIds.size == currentIds.size && currentIds.isNotEmpty()
                        TextButton(
                            onClick = {
                                hapticHelper?.vibrateTick()
                                selectedIds = if (allSelected) {
                                    emptySet()
                                } else {
                                    currentIds.toSet()
                                }
                            },
                            modifier = Modifier.testTag("history_select_all_btn")
                        ) {
                            Icon(
                                imageVector = if (allSelected) Icons.Default.Close else ImageVector.vectorResource(R.drawable.ic_select_all),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (allSelected) stringResource(R.string.batch_deselect_all) else stringResource(R.string.batch_select_all),
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showClearDialog = true },
                            enabled = hasItemsInCurrentTab,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("history_batch_clear_all_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.history_clear_all), fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                if (selectedIds.isNotEmpty()) {
                                    showBatchDeleteDialog = true
                                }
                            },
                            enabled = selectedIds.isNotEmpty(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("history_batch_delete_btn")
                        ) {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_delete_outline),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (selectedIds.isNotEmpty()) {
                                    "${stringResource(R.string.history_delete_selected)} (${selectedIds.size})"
                                } else {
                                    stringResource(R.string.history_delete_selected)
                                },
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HistoryCard(
    id: String,
    thumbUrl: String,
    resolution: String,
    timestamp: Long,
    dateFormat: SimpleDateFormat,
    isBatchMode: Boolean,
    isSelected: Boolean,
    onDeleteSingle: () -> Unit,
    onLongClick: () -> Unit,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val imageRequest = remember(thumbUrl) {
        ImageRequest.Builder(context)
            .data(thumbUrl)
            .crossfade(true)
            .memoryCacheKey(thumbUrl)
            .diskCacheKey(thumbUrl)
            .build()
    }
    val dateText = remember(timestamp) {
        dateFormat.format(Date(timestamp))
    }

    val baseCardColor = MaterialTheme.colorScheme.surfaceVariant
        .copy(alpha = 0.45f)
        .compositeOver(MaterialTheme.colorScheme.background)
    val selectedCardColor = MaterialTheme.colorScheme.primaryContainer
        .copy(alpha = 0.45f)
        .compositeOver(MaterialTheme.colorScheme.background)
    val cardContainerColor = if (isBatchMode && isSelected) selectedCardColor else baseCardColor
    val borderStroke = if (isBatchMode && isSelected) {
        BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
    } else {
        BorderStroke(1.dp, cardContainerColor)
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = borderStroke,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .testTag("browse_history_card_$id")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = imageRequest,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(68.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(baseCardColor)
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "WallHaven #$id",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = resolution,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = dateText,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            if (isBatchMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = null,
                    modifier = Modifier.padding(start = 4.dp)
                )
            } else {
                IconButton(
                    onClick = onDeleteSingle,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("delete_browse_item_$id")
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_delete_outline),
                        contentDescription = stringResource(R.string.history_delete_item),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DownloadHistoryCard(
    item: DownloadEntity,
    dateFormat: SimpleDateFormat,
    isBatchMode: Boolean,
    isSelected: Boolean,
    onDeleteSingle: () -> Unit,
    onOpenLocal: () -> Unit,
    onLongClick: () -> Unit,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val imageRequest = remember(item.thumbUrl) {
        ImageRequest.Builder(context)
            .data(item.thumbUrl)
            .crossfade(true)
            .memoryCacheKey(item.thumbUrl)
            .diskCacheKey(item.thumbUrl)
            .build()
    }
    val dateText = remember(item.downloadedAt) {
        dateFormat.format(Date(item.downloadedAt))
    }

    val baseCardColor = MaterialTheme.colorScheme.surfaceVariant
        .copy(alpha = 0.45f)
        .compositeOver(MaterialTheme.colorScheme.background)
    val selectedCardColor = MaterialTheme.colorScheme.primaryContainer
        .copy(alpha = 0.45f)
        .compositeOver(MaterialTheme.colorScheme.background)
    val cardContainerColor = if (isBatchMode && isSelected) selectedCardColor else baseCardColor
    val borderStroke = if (isBatchMode && isSelected) {
        BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
    } else {
        BorderStroke(1.dp, cardContainerColor)
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = borderStroke,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .testTag("download_history_card_${item.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = imageRequest,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(68.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(baseCardColor)
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "WallHaven #${item.id}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${item.resolution} · ${formatFileSize(item.fileSize)}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = dateText,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            if (isBatchMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = null,
                    modifier = Modifier.padding(start = 4.dp)
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onOpenLocal,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("open_download_item_${item.id}")
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_open_in_new),
                            contentDescription = "Open in Gallery",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = onDeleteSingle,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("delete_download_item_${item.id}")
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_delete_outline),
                            contentDescription = stringResource(R.string.history_delete_item),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "未知大小"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    return if (mb >= 1.0) {
        String.format(Locale.getDefault(), "%.1f MB", mb)
    } else {
        String.format(Locale.getDefault(), "%.0f KB", kb)
    }
}
