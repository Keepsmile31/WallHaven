package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.data.model.WallpaperDto

@Composable
fun WallpaperCard(
    wallpaper: WallpaperDto,
    isFavorite: Boolean,
    onFavoriteToggle: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSelectable: Boolean = false,
    isSelected: Boolean = false,
    onSelectToggle: (() -> Unit)? = null,
    showFavoritesCount: Boolean = true
) {
    val context = LocalContext.current

    // Parse aspect ratio accurately (preferring exact pixel dimensions when consistent with ratio string)
    val parsedRatio = remember(wallpaper.id, wallpaper.ratio, wallpaper.dimensionX, wallpaper.dimensionY) {
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
    val cardRatio = parsedRatio.coerceIn(0.35f, 2.4f)
    // Wallhaven's `large` (300x200) and `small` (200x150) thumbnails are cropped to landscape ratios.
    // Use `thumbs.original` (uncropped proportional thumbnail) for portrait, square, near-square, and ultrawide wallpapers.
    val useOriginalThumb = parsedRatio < 1.26f || parsedRatio > 1.9f

    val fallbackColor = MaterialTheme.colorScheme.surfaceVariant
    val dominantColor = remember(wallpaper.colors) {
        val hex = wallpaper.colors.firstOrNull()?.removePrefix("#")
        if (!hex.isNullOrBlank() && (hex.length == 6 || hex.length == 8)) {
            hex.toLongOrNull(16)?.let { Color(it or 0xFF000000) }
        } else null
    } ?: fallbackColor

    var fallbackToLargeThumb by remember(wallpaper.id) { mutableStateOf(false) }
    val primaryThumbUrl = remember(wallpaper.id, wallpaper.thumbs, useOriginalThumb, wallpaper.path) {
        wallpaper.thumbs.getThumbForRatio(useOriginalThumb).ifBlank { wallpaper.path }
    }
    val fallbackThumbUrl = remember(wallpaper.id, wallpaper.thumbs, wallpaper.path) {
        wallpaper.thumbs.large?.takeIf { it.isNotBlank() }
            ?: wallpaper.thumbs.small?.takeIf { it.isNotBlank() }
            ?: wallpaper.path
    }
    val thumbUrl = if (fallbackToLargeThumb) fallbackThumbUrl else primaryThumbUrl
    val resolutionBadge = remember(wallpaper.resolution) {
        formatResolutionTag(wallpaper.resolution)
    }
    val favText = remember(wallpaper.favorites) {
        if (wallpaper.favorites >= 1000) {
            String.format(java.util.Locale.US, "%.1fk", wallpaper.favorites / 1000f)
        } else {
            "${wallpaper.favorites}"
        }
    }

    val imageRequest = remember(thumbUrl) {
        ImageRequest.Builder(context)
            .data(thumbUrl)
            .crossfade(80)
            .memoryCacheKey(thumbUrl)
            .diskCacheKey(thumbUrl)
            .build()
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = dominantColor.copy(alpha = 0.25f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable {
                if (isSelectable && onSelectToggle != null) {
                    onSelectToggle()
                } else {
                    onClick()
                }
            }
            .testTag("wallpaper_card_${wallpaper.id}")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(cardRatio)
                .background(dominantColor.copy(alpha = 0.35f))
        ) {
            // Uncropped image display with memoized request and fallback
            AsyncImage(
                model = imageRequest,
                contentDescription = "Wallpaper ${wallpaper.id}",
                contentScale = ContentScale.Fit, // ContentScale.Fit guarantees NO cropping
                onError = {
                    if (!fallbackToLargeThumb && primaryThumbUrl != fallbackThumbUrl) {
                        fallbackToLargeThumb = true
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.Center)
            )

            // Resolution Tag on top-left: translucent dark pill, hidden if empty
            if (resolutionBadge.isNotBlank()) {
                Surface(
                    color = Color.Black.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                ) {
                    Text(
                        text = resolutionBadge,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            // Purity/Category Tag on bottom-left
            if (wallpaper.purity != "sfw") {
                val isNsfw = wallpaper.purity == "nsfw"
                Surface(
                    color = if (isNsfw) Color(0xFFD32F2F).copy(alpha = 0.85f) else Color(0xFFF57C00).copy(alpha = 0.85f),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(6.dp)
                ) {
                    Text(
                        text = if (isNsfw) "NSFW" else "SKETCHY",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Quick Favorite Button or Batch Selection Checkbox on top-right
            if (isSelectable) {
                Surface(
                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.55f),
                    shape = CircleShape,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(32.dp)
                        .clickable { onSelectToggle?.invoke() }
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = ImageVector.vectorResource(
                                if (isSelected) R.drawable.ic_check_circle else R.drawable.ic_check_circle_outline
                            ),
                            contentDescription = "Select item",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            } else {
                Surface(
                    color = Color.Black.copy(alpha = 0.5f),
                    shape = CircleShape,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(32.dp)
                ) {
                    IconButton(
                        onClick = onFavoriteToggle,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("favorite_btn_${wallpaper.id}")
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (isFavorite) Color(0xFFFF4081) else Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Favorites Count display on bottom-right corner (toggleable via settings)
            if (showFavoritesCount) {
                Surface(
                    color = Color.Black.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = null,
                            tint = Color(0xFFFF5252),
                            modifier = Modifier.size(10.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = favText,
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Standard resolution tier parser:
 * 8K: >= 7680
 * 5K: >= 5120
 * 4K+: >= 3840
 * 2K: >= 2560
 * FHD: >= 1920
 * or WxH; hidden if empty
 */
private fun formatResolutionTag(resolution: String): String {
    if (resolution.isBlank()) return ""

    val parts = resolution.split("x", "X")
    if (parts.size == 2) {
        val w = parts[0].trim().toIntOrNull()
        val h = parts[1].trim().toIntOrNull()
        if (w != null && h != null) {
            val maxDim = maxOf(w, h)
            return when {
                maxDim >= 7680 -> "8K"
                maxDim >= 5120 -> "5K"
                maxDim >= 3840 -> "4K+"
                maxDim >= 2560 -> "2K"
                maxDim >= 1920 -> "FHD"
                else -> "${w}×${h}"
            }
        }
    }

    if (resolution.contains("7680")) return "8K"
    if (resolution.contains("5120")) return "5K"
    if (resolution.contains("3840") || resolution.contains("4096")) return "4K+"
    if (resolution.contains("2560")) return "2K"
    if (resolution.contains("1920")) return "FHD"
    return resolution
}
