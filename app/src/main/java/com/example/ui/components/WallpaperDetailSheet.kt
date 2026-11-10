package com.example.ui.components

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Wallpaper
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.data.model.WallpaperDto
import com.example.util.DownloadStatus
import com.example.util.HapticHelper
import com.example.util.WallpaperSetHelper
import com.example.util.WallpaperTarget
import com.example.data.repository.DefaultTagsDictionary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun WallpaperDetailSheet(
    wallpaper: WallpaperDto?,
    isFavorite: Boolean,
    downloadStatus: DownloadStatus?,
    onFavoriteToggle: () -> Unit,
    onDownload: () -> Unit,
    onSearchTag: (String) -> Unit,
    onSearchColor: (String) -> Unit,
    onDismiss: () -> Unit,
    hapticHelper: HapticHelper,
    wallpaperSetHelper: WallpaperSetHelper,
    tagTranslator: ((String) -> String?)? = null,
    tagTranslations: Map<String, String> = emptyMap(),
    isTagLocalizationEnabled: Boolean = false
) {
    if (wallpaper == null) return

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showSetWallpaperDialog by remember { mutableStateOf(false) }
    var isSettingWallpaper by remember { mutableStateOf(false) }
    var showFullScreenPreview by remember { mutableStateOf(false) }
    var isFullScreenVisible by remember { mutableStateOf(false) }

    val parsedRatio = remember(wallpaper.ratio, wallpaper.dimensionX, wallpaper.dimensionY) {
        val ratioFromString = wallpaper.ratio.toFloatOrNull()?.takeIf { it in 0.15f..6.0f }
        val ratioFromDims = if (wallpaper.dimensionX > 0 && wallpaper.dimensionY > 0) {
            (wallpaper.dimensionX.toFloat() / wallpaper.dimensionY.toFloat()).takeIf { it in 0.15f..6.0f }
        } else null
        when {
            ratioFromDims != null && (ratioFromString == null || kotlin.math.abs(ratioFromDims - ratioFromString) < 0.05f) -> ratioFromDims
            ratioFromString != null -> ratioFromString
            ratioFromDims != null -> ratioFromDims
            else -> 1.33f
        }
    }
    val useOriginalThumb = parsedRatio < 1.26f || parsedRatio > 1.9f

    // Progressive loading with immediate full-size high-resolution image loading upon accessing page
    val cachedThumbUrl = remember(wallpaper.id, wallpaper.thumbs, useOriginalThumb) {
        wallpaper.thumbs.getThumbForRatio(useOriginalThumb).ifBlank { wallpaper.thumbs.bestThumb }
    }
    val fullSizeImageUrl = remember(wallpaper.id) {
        wallpaper.path.ifBlank { wallpaper.thumbs.original ?: wallpaper.thumbs.bestThumb }
    }
    val previewImageRequest = remember(wallpaper.id, fullSizeImageUrl, cachedThumbUrl) {
        ImageRequest.Builder(context)
            .data(fullSizeImageUrl)
            .placeholderMemoryCacheKey(cachedThumbUrl)
            .memoryCacheKey(fullSizeImageUrl)
            .diskCacheKey(fullSizeImageUrl)
            .crossfade(200)
            .build()
    }
    val fullScreenImageRequest = remember(wallpaper.id, fullSizeImageUrl, cachedThumbUrl) {
        ImageRequest.Builder(context)
            .data(fullSizeImageUrl)
            .placeholderMemoryCacheKey(cachedThumbUrl)
            .memoryCacheKey(fullSizeImageUrl)
            .diskCacheKey(fullSizeImageUrl)
            .crossfade(200)
            .build()
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxHeight()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Subtle drag indicator affordance
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 36.dp, height = 4.dp)
                        .background(
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(2.dp)
                        )
                )
            }

            // Header Bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 8.dp)
            ) {
                Text(
                    text = "WallHaven #${wallpaper.id}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Favorite Button in header
                    IconButton(
                        onClick = {
                            hapticHelper.vibrateSuccess()
                            onFavoriteToggle()
                        },
                        modifier = Modifier.testTag("detail_favorite_btn")
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (isFavorite) Color(0xFFFF4081) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Share Button in header
                    IconButton(
                        onClick = {
                            hapticHelper.vibrateClick()
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "WallHaven Wallpaper #${wallpaper.id}")
                                putExtra(Intent.EXTRA_TEXT, "分享来自 WallHaven 的精美壁纸: https://wallhaven.cc/w/${wallpaper.id}")
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "分享壁纸"))
                        },
                        modifier = Modifier.testTag("share_wallpaper_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Close Button
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_sheet_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }
            }

            // Image Preview (Uncropped) with fast progressive loading
            val fallbackColor = MaterialTheme.colorScheme.surfaceVariant
            val dominantColor = remember(wallpaper.colors) {
                val hex = wallpaper.colors.firstOrNull()?.removePrefix("#")
                if (!hex.isNullOrBlank()) {
                    try {
                        Color(android.graphics.Color.parseColor("#$hex"))
                    } catch (_: Exception) {
                        null
                    }
                } else null
            } ?: fallbackColor

            var isPreviewLoading by remember(wallpaper.id) { mutableStateOf(true) }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(dominantColor.copy(alpha = 0.2f))
                    .clickable {
                        hapticHelper.vibrateClick()
                        showFullScreenPreview = true
                        isFullScreenVisible = true
                    }
            ) {
                AsyncImage(
                    model = previewImageRequest,
                    contentDescription = "Wallpaper full preview",
                    contentScale = ContentScale.Fit, // Guarantees zero cropping!
                    onSuccess = { isPreviewLoading = false },
                    onError = { isPreviewLoading = false },
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(parsedRatio.coerceIn(0.35f, 2.4f))
                )

                if (isPreviewLoading) {
                    CircularProgressIndicator(
                        color = Color.White.copy(alpha = 0.7f),
                        strokeWidth = 2.dp,
                        modifier = Modifier
                            .size(36.dp)
                            .align(Alignment.Center)
                    )
                }

                Surface(
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "点击全屏查看",
                            color = Color.White,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Primary Action Buttons: Optimized Simplified Chinese layout for Download and Set as Wallpaper
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Download Button (下载原图)
                val isDownloading = downloadStatus?.isFinished == false && (downloadStatus.progress in 1..99)
                Button(
                    onClick = {
                        hapticHelper.vibrateClick()
                        Toast.makeText(context, "正在开始下载壁纸...", Toast.LENGTH_SHORT).show()
                        onDownload()
                    },
                    enabled = !isDownloading,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("download_wallpaper_btn")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (isDownloading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "下载中 ${downloadStatus?.progress ?: 0}%",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (downloadStatus?.isFinished == true) "再次下载" else stringResource(R.string.action_download),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                }

                // Set Wallpaper Button (设为壁纸)
                FilledTonalButton(
                    onClick = {
                        hapticHelper.vibrateClick()
                        showSetWallpaperDialog = true
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("set_wallpaper_btn")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Wallpaper,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.action_set_wallpaper),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }

            // Metadata Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.wallpaper_details),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        MetadataItem(label = stringResource(R.string.resolution), value = wallpaper.resolution)
                        MetadataItem(
                            label = stringResource(R.string.file_size),
                            value = formatFileSize(wallpaper.fileSize)
                        )
                        val localizedCategory = when (wallpaper.category.lowercase().trim()) {
                            "general" -> "常规"
                            "anime" -> "动漫"
                            "people" -> "人物"
                            else -> wallpaper.category.replaceFirstChar { it.uppercase() }
                        }
                        MetadataItem(label = stringResource(R.string.category), value = localizedCategory)
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        val localizedPurity = when (wallpaper.purity.lowercase().trim()) {
                            "sfw" -> "全年龄 (SFW)"
                            "sketchy" -> "性感 (Sketchy)"
                            "nsfw" -> "限制 (NSFW)"
                            else -> wallpaper.purity.uppercase()
                        }
                        MetadataItem(label = stringResource(R.string.purity), value = localizedPurity)
                        MetadataItem(label = stringResource(R.string.views_count), value = wallpaper.views.toString())
                        MetadataItem(label = stringResource(R.string.favorites_count), value = wallpaper.favorites.toString())
                    }

                    if (wallpaper.createdAt.isNotBlank()) {
                        MetadataItem(label = stringResource(R.string.upload_time), value = wallpaper.createdAt)
                    }
                }
            }

            // Extracted Colors Palette
            if (wallpaper.colors.isNotEmpty()) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    Text(
                        text = stringResource(R.string.color_palette),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        wallpaper.colors.forEach { hex ->
                            val color = try {
                                Color(android.graphics.Color.parseColor(if (hex.startsWith("#")) hex else "#$hex"))
                            } catch (_: Exception) {
                                Color.Gray
                            }
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), CircleShape)
                                    .clickable {
                                        hapticHelper.vibrateTick()
                                        onSearchColor(hex.removePrefix("#"))
                                    }
                            )
                        }
                    }
                }
            }

            // Tags Cloud
            if (wallpaper.tags.isNotEmpty()) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    Text(
                        text = stringResource(R.string.tags),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        wallpaper.tags.forEach { tag ->
                            val raw = tag.name.lowercase().trim()
                            val clean = raw.replace('_', ' ').replace('-', ' ')
                            val chineseTranslation = if (isTagLocalizationEnabled) {
                                (tagTranslations[raw]
                                    ?: tagTranslations[clean]
                                    ?: tagTranslator?.invoke(tag.name)
                                    ?: DefaultTagsDictionary.TAG_PAIRS.firstOrNull { 
                                        it.first == raw || it.first == clean 
                                    }?.second)
                                    ?.replace(Regex("""[0-9a-fA-F]{16,}"""), "")
                                    ?.trim()
                                    ?.ifBlank { null }
                            } else null

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.85f),
                                modifier = Modifier
                                    .widthIn(max = 280.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        hapticHelper.vibrateTick()
                                        // Retrieves original English tag and triggers search
                                        onSearchTag(tag.name)
                                    }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "#",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    if (chineseTranslation != null) {
                                        Text(
                                            text = chineseTranslation,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "(${tag.name})",
                                            fontSize = 10.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.65f),
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                    } else {
                                        Text(
                                            text = tag.name,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Set Wallpaper Dialog
    if (showSetWallpaperDialog) {
        AlertDialog(
            onDismissRequest = { showSetWallpaperDialog = false },
            title = { Text(text = stringResource(R.string.action_set_wallpaper)) },
            text = {
                if (isSettingWallpaper) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Text("正在设置壁纸...")
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(
                            onClick = {
                                isSettingWallpaper = true
                                scope.launch {
                                    val fullUrl = wallpaper.path.ifBlank { wallpaper.thumbs.bestThumb }
                                    val res = wallpaperSetHelper.setWallpaperFromUrl(fullUrl, WallpaperTarget.HOME)
                                    isSettingWallpaper = false
                                    showSetWallpaperDialog = false
                                    if (res.isSuccess) {
                                        hapticHelper.vibrateSuccess()
                                        Toast.makeText(context, R.string.setting_wallpaper_success, Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "设置失败: ${res.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("set_home_wallpaper_btn")
                        ) {
                            Text(stringResource(R.string.set_home_screen))
                        }

                        TextButton(
                            onClick = {
                                isSettingWallpaper = true
                                scope.launch {
                                    val fullUrl = wallpaper.path.ifBlank { wallpaper.thumbs.bestThumb }
                                    val res = wallpaperSetHelper.setWallpaperFromUrl(fullUrl, WallpaperTarget.LOCK)
                                    isSettingWallpaper = false
                                    showSetWallpaperDialog = false
                                    if (res.isSuccess) {
                                        hapticHelper.vibrateSuccess()
                                        Toast.makeText(context, R.string.setting_wallpaper_success, Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "设置失败: ${res.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("set_lock_wallpaper_btn")
                        ) {
                            Text(stringResource(R.string.set_lock_screen))
                        }

                        TextButton(
                            onClick = {
                                isSettingWallpaper = true
                                scope.launch {
                                    val fullUrl = wallpaper.path.ifBlank { wallpaper.thumbs.bestThumb }
                                    val res = wallpaperSetHelper.setWallpaperFromUrl(fullUrl, WallpaperTarget.BOTH)
                                    isSettingWallpaper = false
                                    showSetWallpaperDialog = false
                                    if (res.isSuccess) {
                                        hapticHelper.vibrateSuccess()
                                        Toast.makeText(context, R.string.setting_wallpaper_success, Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "设置失败: ${res.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("set_both_wallpaper_btn")
                        ) {
                            Text(stringResource(R.string.set_both_screens))
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                if (!isSettingWallpaper) {
                    TextButton(onClick = { showSetWallpaperDialog = false }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
            }
        )
    }

    // Full Screen Preview Dialog with Smooth Enter & Exit Animations & Progressive Fast Loading
    if (showFullScreenPreview) {
        Dialog(
            onDismissRequest = { isFullScreenVisible = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            BackHandler(enabled = true) {
                isFullScreenVisible = false
            }

            LaunchedEffect(isFullScreenVisible) {
                if (!isFullScreenVisible) {
                    delay(220)
                    showFullScreenPreview = false
                }
            }

            AnimatedVisibility(
                visible = isFullScreenVisible,
                enter = fadeIn(animationSpec = tween(260)) + scaleIn(initialScale = 0.88f, animationSpec = tween(260)),
                exit = fadeOut(animationSpec = tween(200)) + scaleOut(targetScale = 0.88f, animationSpec = tween(200))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                ) {
                    var isFullImageLoading by remember { mutableStateOf(true) }

                    AsyncImage(
                        model = fullScreenImageRequest,
                        contentDescription = "Full Screen Wallpaper",
                        contentScale = ContentScale.Fit, // Zero cropping
                        onSuccess = { isFullImageLoading = false },
                        onError = { isFullImageLoading = false },
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { isFullScreenVisible = false }
                    )

                    if (isFullImageLoading) {
                        CircularProgressIndicator(
                            color = Color.White.copy(alpha = 0.7f),
                            strokeWidth = 2.dp,
                            modifier = Modifier
                                .size(36.dp)
                                .align(Alignment.Center)
                        )
                    }

                    // Top Bar with Resolution / File Size Info and Close Button
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text(
                                text = "${wallpaper.resolution} • ${formatFileSize(wallpaper.fileSize)}",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                hapticHelper.vibrateClick()
                                isFullScreenVisible = false
                            },
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                .size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetadataItem(label: String, value: String) {
    Column {
        Text(text = label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "未知"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    return if (mb >= 1.0) {
        String.format("%.2f MB", mb)
    } else {
        String.format("%.1f KB", kb)
    }
}
