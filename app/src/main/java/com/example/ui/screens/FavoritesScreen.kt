package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.db.FavoriteWallpaperEntity
import com.example.data.model.WallpaperDto
import com.example.ui.components.AppHeaderBar
import com.example.ui.components.WallpaperCard
import com.example.util.BatchDownloadResult
import com.example.util.HapticHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    favorites: List<FavoriteWallpaperEntity>,
    onWallpaperClick: (WallpaperDto) -> Unit,
    onFavoriteToggle: (WallpaperDto) -> Unit,
    onBatchDownload: (List<WallpaperDto>, (BatchDownloadResult) -> Unit) -> Unit,
    onBatchUnfavorite: (List<String>) -> Unit,
    isBatchDownloading: Boolean,
    hapticHelper: HapticHelper
) {
    var isBatchMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    var showConfirmDeleteDialog by remember { mutableStateOf(false) }
    var bannerState by remember { mutableStateOf(CompletionBannerState()) }

    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val cutoutPadding = WindowInsets.displayCutout.asPaddingValues().calculateTopPadding()
    val topInset = maxOf(statusBarPadding, cutoutPadding)
    val navBottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    if (showConfirmDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDeleteDialog = false },
            title = { Text(stringResource(R.string.batch_unfavorite)) },
            text = { Text("确定将选中的 ${selectedIds.size} 张壁纸从收藏中移除吗？") },
            confirmButton = {
                Button(
                    onClick = {
                        val idsToRemove = selectedIds.toList()
                        onBatchUnfavorite(idsToRemove)
                        selectedIds = emptySet()
                        isBatchMode = false
                        showConfirmDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.action_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDeleteDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (favorites.isEmpty()) {
            // Empty state matching Screenshot 1
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FavoriteBorder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                        modifier = Modifier.size(76.dp)
                    )
                    Text(
                        text = "暂无收藏壁纸，快去发现心仪的壁纸吧！",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }
        } else {
            // Wallpaper 2-column staggered grid
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(2),
                contentPadding = PaddingValues(
                    start = 8.dp,
                    end = 8.dp,
                    top = topInset + 68.dp,
                    bottom = navBottomInset + (if (isBatchMode) 160.dp else 96.dp)
                ),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalItemSpacing = 8.dp,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("favorites_wallpaper_grid")
            ) {
                items(
                    items = favorites,
                    key = { it.id },
                    contentType = { "favorite_wallpaper" }
                ) { fav ->
                    val dto = remember(fav) { fav.toWallpaperDto() }
                    WallpaperCard(
                        wallpaper = dto,
                        isFavorite = true,
                        isSelectable = isBatchMode,
                        isSelected = selectedIds.contains(dto.id),
                        onSelectToggle = {
                            hapticHelper.vibrateTick()
                            selectedIds = if (selectedIds.contains(dto.id)) {
                                selectedIds - dto.id
                            } else {
                                selectedIds + dto.id
                            }
                        },
                        onFavoriteToggle = { onFavoriteToggle(dto) },
                        onClick = { onWallpaperClick(dto) }
                    )
                }
            }
        }

        // Top Section Design matching Screenshot 1
        Surface(
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
            shadowElevation = 2.dp,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = topInset + 6.dp, bottom = 10.dp, start = 16.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppHeaderBar(
                    title = stringResource(R.string.nav_favorites),
                    trailing = {
                        if (favorites.isNotEmpty()) {
                            FilledTonalButton(
                                onClick = {
                                    hapticHelper.vibrateTick()
                                    isBatchMode = !isBatchMode
                                    if (!isBatchMode) {
                                        selectedIds = emptySet()
                                    }
                                },
                                modifier = Modifier.testTag("batch_mode_toggle_btn")
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
        }

        // Completion Banner at Top
        AnimatedVisibility(
            visible = bannerState.isVisible,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(top = topInset + 68.dp, start = 16.dp, end = 16.dp)
        ) {
            val (bgColor, textColor) = when (bannerState.type) {
                BannerType.SUCCESS -> Color(0xFF2E7D32) to Color.White
                BannerType.PARTIAL -> Color(0xFFE65100) to Color.White
                BannerType.FAILED -> Color(0xFFC62828) to Color.White
            }

            Surface(
                color = bgColor,
                shape = RoundedCornerShape(12.dp),
                shadowElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { bannerState = bannerState.copy(isVisible = false) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = bannerState.message,
                        color = textColor,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close banner",
                        tint = textColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Floating Batch Operation Bar at Bottom
        AnimatedVisibility(
            visible = isBatchMode,
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
                modifier = Modifier.fillMaxWidth().testTag("batch_action_bar")
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

                        val allSelected = selectedIds.size == favorites.size && favorites.isNotEmpty()
                        TextButton(
                            onClick = {
                                hapticHelper.vibrateTick()
                                selectedIds = if (allSelected) {
                                    emptySet()
                                } else {
                                    favorites.map { it.id }.toSet()
                                }
                            },
                            modifier = Modifier.testTag("select_all_btn")
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
                        Button(
                            onClick = {
                                if (selectedIds.isEmpty()) return@Button
                                val dtosToDownload = favorites.filter { selectedIds.contains(it.id) }.map { it.toWallpaperDto() }
                                onBatchDownload(dtosToDownload) { res ->
                                    val (type, msg) = when {
                                        res.successCount == res.total -> BannerType.SUCCESS to "批量下载全部成功！共 ${res.total} 张"
                                        res.successCount > 0 -> BannerType.PARTIAL to "批量下载部分完成：${res.successCount} 成功，${res.failCount} 失败"
                                        else -> BannerType.FAILED to "批量下载失败，请检查网络设置"
                                    }
                                    bannerState = CompletionBannerState(isVisible = true, type = type, message = msg)
                                }
                            },
                            enabled = selectedIds.isNotEmpty() && !isBatchDownloading,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("batch_download_btn")
                        ) {
                            if (isBatchDownloading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("下载中...", fontSize = 12.sp)
                            } else {
                                Icon(imageVector = ImageVector.vectorResource(R.drawable.ic_download), contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(R.string.batch_download), fontSize = 12.sp)
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                if (selectedIds.isNotEmpty()) {
                                    showConfirmDeleteDialog = true
                                }
                            },
                            enabled = selectedIds.isNotEmpty(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("batch_unfavorite_btn")
                        ) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.batch_unfavorite), fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

enum class BannerType {
    SUCCESS,
    PARTIAL,
    FAILED
}

@Immutable
data class CompletionBannerState(
    val isVisible: Boolean = false,
    val type: BannerType = BannerType.SUCCESS,
    val message: String = ""
)
