package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    browseHistory: List<HistoryEntity>,
    downloadHistory: List<DownloadEntity>,
    onClearHistory: () -> Unit,
    onClearDownloads: () -> Unit,
    onWallpaperClick: (WallpaperDto) -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var showClearDialog by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()) }

    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val cutoutPadding = WindowInsets.displayCutout.asPaddingValues().calculateTopPadding()
    val topInset = maxOf(statusBarPadding, cutoutPadding)
    val navBottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    // Clear Confirmation Dialog
    if (showClearDialog) {
        val titleText = if (selectedTab == 0) "清空浏览历史" else "清空下载历史"
        val messageText = if (selectedTab == 0) "确定清空全部壁纸浏览历史吗？此操作无法撤销。" else "确定清空全部下载历史记录吗？（已下载的本地文件不会被删除）"
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(titleText) },
            text = { Text(messageText) },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (selectedTab == 0) onClearHistory() else onClearDownloads()
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

    Box(modifier = Modifier.fillMaxSize()) {
        // Content List
        if (selectedTab == 0) {
            if (browseHistory.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
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
                        top = topInset + 106.dp,
                        bottom = navBottomInset + 88.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize().testTag("browse_history_list")
                ) {
                    items(
                        items = browseHistory,
                        key = { it.id }
                    ) { item ->
                        HistoryCard(
                            id = item.id,
                            thumbUrl = item.thumbUrl,
                            resolution = item.resolution,
                            timestamp = item.viewedAt,
                            dateFormat = dateFormat,
                            onClick = {
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
                            imageVector = Icons.Default.Download,
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
                        top = topInset + 106.dp,
                        bottom = navBottomInset + 88.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize().testTag("download_history_list")
                ) {
                    items(
                        items = downloadHistory,
                        key = { it.id }
                    ) { item ->
                        DownloadHistoryCard(
                            item = item,
                            dateFormat = dateFormat,
                            onOpenLocal = {
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(Uri.parse(item.localUri), "image/*")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                try {
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            },
                            onClick = {
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
                        )
                    }
                }
            }
        }

        // Top Section Design matching Screenshot 3
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
                    .padding(top = topInset, bottom = 4.dp)
            ) {
                // Row 1: Refined Header Bar with App Icon + Title
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppHeaderBar(
                        title = stringResource(R.string.nav_history)
                    )
                }

                // Row 2: Tabs (浏览历史, 下载历史) + Red Delete Icon Button (Screenshot 3)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color.Transparent,
                        divider = {},
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                color = MaterialTheme.colorScheme.primary,
                                height = 3.dp
                            )
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = {
                                Text(
                                    text = stringResource(R.string.tab_browse_history),
                                    fontSize = 14.sp,
                                    fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedTab == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            modifier = Modifier.testTag("tab_browse_history")
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = {
                                Text(
                                    text = stringResource(R.string.tab_download_history),
                                    fontSize = 14.sp,
                                    fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedTab == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            modifier = Modifier.testTag("tab_download_history")
                        )
                    }

                    // Red Trash Can Icon Button matching Screenshot 3
                    IconButton(
                        onClick = { showClearDialog = true },
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("clear_history_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Clear",
                            tint = Color(0xFFD32F2F),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryCard(
    id: String,
    thumbUrl: String,
    resolution: String,
    timestamp: Long,
    dateFormat: SimpleDateFormat,
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

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
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
                    .background(MaterialTheme.colorScheme.surfaceVariant)
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
        }
    }
}

@Composable
private fun DownloadHistoryCard(
    item: DownloadEntity,
    dateFormat: SimpleDateFormat,
    onOpenLocal: () -> Unit,
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

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
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
                    .background(MaterialTheme.colorScheme.surfaceVariant)
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

            IconButton(onClick = onOpenLocal) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = "Open in Gallery",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
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
