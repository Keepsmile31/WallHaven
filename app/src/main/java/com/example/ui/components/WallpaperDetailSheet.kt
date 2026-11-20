package com.example.ui.components

import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentDialog
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.findViewTreeViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.findViewTreeSavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Precision
import com.example.R
import com.example.data.model.WallpaperDto
import com.example.util.CacheManager
import com.example.util.DownloadStatus
import com.example.util.HapticHelper
import com.example.util.WallpaperSetHelper
import com.example.util.WallpaperTarget
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val HexHashRegex = Regex("""[0-9a-fA-F]{16,}""")

private fun parseWallhavenHexColor(hex: String): Color? {
    val clean = hex.removePrefix("#")
    return if (clean.length == 6 || clean.length == 8) {
        clean.toLongOrNull(16)?.let { Color(if (clean.length == 6) it or 0xFF000000 else it) }
    } else null
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WallpaperDetailSheet(
    wallpaper: WallpaperDto?,
    isFavorite: Boolean,
    downloadStatus: DownloadStatus?,
    onFavoriteToggle: () -> Unit,
    onDownload: () -> Unit,
    onSearchTag: (englishTag: String, localizedTag: String?, scrollOffset: Int) -> Unit,
    onSearchColor: (colorHex: String, scrollOffset: Int) -> Unit,
    onDismiss: () -> Unit,
    hapticHelper: HapticHelper,
    wallpaperSetHelper: WallpaperSetHelper,
    tagTranslator: ((String) -> String?)? = null,
    tagTranslations: Map<String, String> = emptyMap(),
    isTagLocalizationEnabled: Boolean = false,
    initialScrollOffset: Int = 0,
    onScrollRestored: () -> Unit = {}
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

    // Progressive loading with separate cache keys so the inline preview never downsamples the fullscreen cache
    val cachedThumbUrl = remember(wallpaper.id, wallpaper.thumbs, useOriginalThumb) {
        wallpaper.thumbs.getThumbForRatio(useOriginalThumb).ifBlank { wallpaper.thumbs.bestThumb }
    }
    val fullSizeImageUrl = remember(wallpaper.id, wallpaper.path, wallpaper.thumbs) {
        wallpaper.path.ifBlank { wallpaper.thumbs.original ?: wallpaper.thumbs.bestThumb }
    }
    val previewMemoryCacheKey = remember(fullSizeImageUrl) {
        "${fullSizeImageUrl}_sheet_preview"
    }
    val previewImageRequest = remember(wallpaper.id, fullSizeImageUrl, cachedThumbUrl, previewMemoryCacheKey) {
        ImageRequest.Builder(context)
            .data(fullSizeImageUrl)
            .placeholderMemoryCacheKey(cachedThumbUrl)
            .memoryCacheKey(previewMemoryCacheKey)
            .diskCacheKey(fullSizeImageUrl)
            .crossfade(200)
            .build()
    }
    var isPreviewLoading by remember(wallpaper.id) { mutableStateOf(true) }
    val fullscreenPlaceholderKey = if (!isPreviewLoading) previewMemoryCacheKey else cachedThumbUrl
    val fullscreenImageRequest = remember(wallpaper.id, fullSizeImageUrl, fullscreenPlaceholderKey) {
        ImageRequest.Builder(context)
            .data(fullSizeImageUrl)
            .placeholderMemoryCacheKey(fullscreenPlaceholderKey)
            .memoryCacheKey("${fullSizeImageUrl}_fullscreen_hd")
            .diskCacheKey(fullSizeImageUrl)
            .size(MAX_FULLSCREEN_BASE_DIMENSION, MAX_FULLSCREEN_BASE_DIMENSION)
            .precision(Precision.INEXACT)
            .crossfade(false)
            .build()
    }

    val detailScrollState = rememberScrollState(initial = initialScrollOffset.coerceAtLeast(0))

    LaunchedEffect(wallpaper.id, initialScrollOffset) {
        if (initialScrollOffset > 0) {
            snapshotFlow { detailScrollState.maxValue }
                .filter { it > 0 }
                .first()
            val clamped = initialScrollOffset.coerceAtMost(detailScrollState.maxValue)
            if (detailScrollState.value != clamped) {
                detailScrollState.scrollTo(clamped)
            }
            onScrollRestored()
        }
    }

    val renderDetailBody: @Composable (Modifier) -> Unit = { handleExtraModifier ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(detailScrollState)
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Subtle drag indicator affordance (supports swipe-down dismiss in shared-element view)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(handleExtraModifier)
                    .padding(top = 10.dp, bottom = 6.dp),
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
                            imageVector = ImageVector.vectorResource(
                                if (isFavorite) R.drawable.ic_favorite_filled else R.drawable.ic_favorite_border
                            ),
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
                            imageVector = ImageVector.vectorResource(R.drawable.ic_share),
                            contentDescription = "Share",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Close Button
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_sheet_btn")
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_close),
                            contentDescription = "Close"
                        )
                    }
                }
            }

            // Image Preview (Uncropped) with fast progressive loading
            val fallbackColor = MaterialTheme.colorScheme.surfaceVariant
            val dominantColor = remember(wallpaper.colors) {
                wallpaper.colors.firstOrNull()?.let { parseWallhavenHexColor(it) }
            } ?: fallbackColor

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
                            imageVector = ImageVector.vectorResource(R.drawable.ic_image),
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

            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
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
                                imageVector = ImageVector.vectorResource(R.drawable.ic_download),
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
                            imageVector = ImageVector.vectorResource(R.drawable.ic_wallpaper),
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
                            value = CacheManager.formatBytes(wallpaper.fileSize, zeroText = "未知", mbDecimals = 2, kbDecimals = 1)
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
                val parsedPalette = remember(wallpaper.colors) {
                    wallpaper.colors.map { hex ->
                        hex.removePrefix("#") to (parseWallhavenHexColor(hex) ?: Color.Gray)
                    }
                }
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
                        parsedPalette.forEach { (cleanHex, color) ->
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), CircleShape)
                                    .clickable {
                                        hapticHelper.vibrateTick()
                                        onSearchColor(cleanHex, detailScrollState.value)
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
                                    ?: tagTranslator?.invoke(tag.name))
                                    ?.replace(HexHashRegex, "")
                                    ?.trim()
                                    ?.ifBlank { null }
                            } else null

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.85f),
                                modifier = Modifier
                                    .widthIn(max = 280.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .testTag("detail_tag_${tag.name}")
                                    .clickable {
                                        hapticHelper.vibrateTick()
                                        // Pass both original English tag (for API) and localized Chinese tag (for display/history) + scroll offset
                                        onSearchTag(tag.name, chineseTranslation, detailScrollState.value)
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
    }

    BackHandler(enabled = !showFullScreenPreview) {
        onDismiss()
    }

    val dismissThresholdPx = with(LocalDensity.current) { 120.dp.toPx() }
    var dragOffsetY by remember(wallpaper.id) { mutableFloatStateOf(0f) }
    val animatedDragOffset by animateFloatAsState(
        targetValue = dragOffsetY,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "detail_drag_dismiss_offset"
    )

    val dragDismissModifier = Modifier.pointerInput(wallpaper.id) {
        detectVerticalDragGestures(
            onDragEnd = {
                if (dragOffsetY > dismissThresholdPx) {
                    onDismiss()
                }
                dragOffsetY = 0f
            },
            onDragCancel = { dragOffsetY = 0f },
            onVerticalDrag = { change, dragAmount ->
                if (dragAmount > 0f || dragOffsetY > 0f) {
                    change.consume()
                    dragOffsetY = (dragOffsetY + dragAmount).coerceAtLeast(0f)
                }
            }
        )
    }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(0.dp),
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                translationY = animatedDragOffset
                val scale = (1f - (animatedDragOffset / 2400f)).coerceIn(0.92f, 1f)
                scaleX = scale
                scaleY = scale
            }
    ) {
        renderDetailBody(dragDismissModifier)
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

    // Full Screen Preview Dialog with True Non-Floating Edge-to-Edge Translucent Immersive Window
    if (showFullScreenPreview) {
        ImmersiveFullScreenDialog(
            onDismissRequest = { isFullScreenVisible = false }
        ) {
            val transitionState = remember { MutableTransitionState(false) }
            LaunchedEffect(isFullScreenVisible) {
                if (isFullScreenVisible) {
                    transitionState.targetState = true
                } else {
                    transitionState.targetState = false
                    snapshotFlow {
                        transitionState.isIdle && !transitionState.currentState
                    }
                        .filter { it }
                        .first()
                    showFullScreenPreview = false
                }
            }

            val transition = rememberTransition(transitionState, label = "fullscreen_preview_transition")
            val backdropAlpha by transition.animateFloat(
                transitionSpec = {
                    if (targetState) tween(220, easing = FastOutSlowInEasing)
                    else tween(180, easing = FastOutSlowInEasing)
                },
                label = "fullscreen_backdrop_alpha"
            ) { visible -> if (visible) 1f else 0f }
            val imageScale by transition.animateFloat(
                transitionSpec = {
                    if (targetState) {
                        spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    } else {
                        tween(180, easing = FastOutSlowInEasing)
                    }
                },
                label = "fullscreen_image_scale"
            ) { visible -> if (visible) 1f else 0.92f }

            ZoomableFullscreenViewer(
                imageRequest = fullscreenImageRequest,
                infoText = "${wallpaper.resolution} • ${CacheManager.formatBytes(wallpaper.fileSize, zeroText = "未知", mbDecimals = 2, kbDecimals = 1)}",
                fallbackAspectRatio = parsedRatio,
                onClose = { isFullScreenVisible = false },
                onTapFeedback = { hapticHelper.vibrateClick() },
                backdropAlphaMultiplier = backdropAlpha,
                imageScaleMultiplier = imageScale,
                contentAlphaMultiplier = backdropAlpha
            )
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

@Composable
private fun ImmersiveFullScreenDialog(
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val parentView = LocalView.current
    val compositionContext = rememberCompositionContext()
    val currentContent by rememberUpdatedState(content)
    val currentOnDismiss by rememberUpdatedState(onDismissRequest)

    val dialog = remember {
        object : ComponentDialog(context, android.R.style.Theme_Translucent_NoTitleBar) {
            override fun onCreate(savedInstanceState: Bundle?) {
                super.onCreate(savedInstanceState)
                configureImmersiveWindow()
            }

            fun configureImmersiveWindow() {
                window?.let { win ->
                    WindowCompat.setDecorFitsSystemWindows(win, false)
                    win.setLayout(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    win.setFormat(PixelFormat.TRANSLUCENT)
                    win.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
                    win.setWindowAnimations(0)
                    win.setDimAmount(0f)
                    @Suppress("DEPRECATION")
                    win.clearFlags(
                        WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS or
                            WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION or
                            WindowManager.LayoutParams.FLAG_DIM_BEHIND
                    )
                    win.addFlags(
                        WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS or
                            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                    )
                    @Suppress("DEPRECATION")
                    win.statusBarColor = android.graphics.Color.TRANSPARENT
                    @Suppress("DEPRECATION")
                    win.navigationBarColor = android.graphics.Color.TRANSPARENT
                    win.isStatusBarContrastEnforced = false
                    win.isNavigationBarContrastEnforced = false
                    win.attributes = win.attributes.apply {
                        layoutInDisplayCutoutMode =
                            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                    }
                    WindowInsetsControllerCompat(win, win.decorView).apply {
                        isAppearanceLightStatusBars = false
                        isAppearanceLightNavigationBars = false
                    }
                }
            }
        }.apply {
            setCancelable(true)
            setCanceledOnTouchOutside(false)
            onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    currentOnDismiss()
                }
            })
            val composeView = ComposeView(context).apply {
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                setParentCompositionContext(compositionContext)
                setContent {
                    currentContent()
                }
            }
            setContentView(composeView)
            window?.decorView?.let { decor ->
                decor.setViewTreeLifecycleOwner(parentView.findViewTreeLifecycleOwner())
                decor.setViewTreeViewModelStoreOwner(parentView.findViewTreeViewModelStoreOwner())
                decor.setViewTreeSavedStateRegistryOwner(parentView.findViewTreeSavedStateRegistryOwner())
            }
        }
    }

    DisposableEffect(dialog) {
        dialog.configureImmersiveWindow()
        dialog.show()
        dialog.configureImmersiveWindow()
        onDispose {
            dialog.dismiss()
        }
    }
}

