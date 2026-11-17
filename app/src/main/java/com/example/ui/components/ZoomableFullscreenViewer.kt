package com.example.ui.components

import android.graphics.drawable.BitmapDrawable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Precision
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

internal const val MIN_FULLSCREEN_ZOOM = 1.0f
internal const val MAX_FULLSCREEN_ZOOM = 8.0f
internal const val DOUBLE_TAP_ZOOM_SCALE = 3.0f
private const val DOUBLE_TAP_ZOOMED_THRESHOLD = 1.05f

internal fun calculateZoomedOffset(
    currentScale: Float,
    targetScale: Float,
    currentOffset: Offset,
    centroid: Offset,
    pan: Offset,
    containerCenter: Offset
): Offset {
    val safeOldScale = currentScale.coerceAtLeast(MIN_FULLSCREEN_ZOOM)
    val zoomFactor = targetScale / safeOldScale
    val focalRelative = centroid - containerCenter
    return focalRelative + pan - (focalRelative - currentOffset) * zoomFactor
}

internal fun clampZoomOffset(
    offset: Offset,
    scale: Float,
    containerSize: IntSize,
    imageAspectRatio: Float
): Offset {
    if (containerSize.width <= 0 || containerSize.height <= 0 || scale <= MIN_FULLSCREEN_ZOOM) {
        return Offset.Zero
    }
    val cw = containerSize.width.toFloat()
    val ch = containerSize.height.toFloat()
    val containerAspect = cw / ch
    val safeImageAspect = if (imageAspectRatio.isFinite() && imageAspectRatio > 0f) {
        imageAspectRatio
    } else {
        containerAspect
    }
    val (fittedWidth, fittedHeight) = if (safeImageAspect > containerAspect) {
        cw to (cw / safeImageAspect)
    } else {
        (ch * safeImageAspect) to ch
    }
    val maxOffsetX = ((fittedWidth * scale - cw) / 2f).coerceAtLeast(0f)
    val maxOffsetY = ((fittedHeight * scale - ch) / 2f).coerceAtLeast(0f)
    return Offset(
        x = offset.x.coerceIn(-maxOffsetX, maxOffsetX),
        y = offset.y.coerceIn(-maxOffsetY, maxOffsetY)
    )
}

@Composable
fun ZoomableFullscreenViewer(
    imageRequest: ImageRequest,
    infoText: String,
    fallbackAspectRatio: Float,
    onClose: () -> Unit,
    onTapFeedback: () -> Unit = {},
    onScaleChanged: ((Float) -> Unit)? = null,
    backdropAlphaMultiplier: Float = 1.0f,
    imageScaleMultiplier: Float = 1.0f,
    contentAlphaMultiplier: Float = 1.0f
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val density = LocalDensity.current
    val dismissThresholdPx = remember(density) { with(density) { 104.dp.toPx() } }

    val effectiveImageRequest = remember(imageRequest, context) {
        val baseMemKey = imageRequest.memoryCacheKey?.key ?: imageRequest.data.toString()
        val fsMemKey = if (baseMemKey.endsWith("_fullscreen_hd")) baseMemKey else "${baseMemKey}_fullscreen_hd"
        val fallbackPlaceholder = imageRequest.placeholderMemoryCacheKey ?: imageRequest.memoryCacheKey
        imageRequest.newBuilder(context)
            .memoryCacheKey(fsMemKey)
            .apply {
                if (fallbackPlaceholder != null) {
                    placeholderMemoryCacheKey(fallbackPlaceholder)
                }
            }
            .size(MAX_FULLSCREEN_BASE_DIMENSION, MAX_FULLSCREEN_BASE_DIMENSION)
            .precision(Precision.INEXACT)
            .build()
    }

    var isFullImageLoading by remember(effectiveImageRequest) { mutableStateOf(true) }
    var baseDecodedSize by remember(effectiveImageRequest) { mutableStateOf(IntSize.Zero) }
    var tileRenderer by remember(effectiveImageRequest) { mutableStateOf<SubsampledImageTileRenderer?>(null) }
    var tileRenderVersion by remember { mutableIntStateOf(0) }
    var showControls by remember { mutableStateOf(true) }
    var isClosingByDrag by remember { mutableStateOf(false) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var imageAspectRatio by remember(fallbackAspectRatio) {
        mutableFloatStateOf(fallbackAspectRatio.coerceIn(0.15f, 6.0f))
    }

    val scaleAnimatable = remember { Animatable(MIN_FULLSCREEN_ZOOM) }
    val offsetAnimatable = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    val dismissOffsetY = remember { Animatable(0f) }

    if (onScaleChanged != null) {
        LaunchedEffect(onScaleChanged) {
            snapshotFlow { scaleAnimatable.value }.collect { onScaleChanged(it) }
        }
    }

    val dismissProgress = if (containerSize.height > 0) {
        (dismissOffsetY.value / (containerSize.height * 0.45f)).coerceIn(0f, 1f)
    } else {
        (dismissOffsetY.value / (dismissThresholdPx * 3f)).coerceIn(0f, 1f)
    }
    val effectiveBackdropAlpha = ((1f - dismissProgress * 0.85f) * backdropAlphaMultiplier).coerceIn(0f, 1f)
    val dragScaleFactor = (1f - dismissProgress * 0.16f).coerceIn(0.84f, 1f)
    val dragControlsAlpha = if (dismissOffsetY.value > 0f || isClosingByDrag) {
        (1f - dismissProgress * 3f).coerceIn(0f, 1f)
    } else {
        1f
    }
    val currentDragScaleFactor by rememberUpdatedState(dragScaleFactor)
    val currentImageScaleMultiplier by rememberUpdatedState(imageScaleMultiplier)

    LaunchedEffect(effectiveImageRequest, baseDecodedSize) {
        if (baseDecodedSize.width > 0 && baseDecodedSize.height > 0 && tileRenderer == null) {
            val opened = openSubsampledTileRenderer(context, effectiveImageRequest, baseDecodedSize)
            if (opened != null) {
                tileRenderer = opened
                imageAspectRatio = opened.originalWidth.toFloat() / opened.originalHeight.toFloat()
            }
        }
    }

    DisposableEffect(tileRenderer) {
        val activeRenderer = tileRenderer
        onDispose {
            if (activeRenderer != null) {
                CoroutineScope(Dispatchers.IO).launch {
                    activeRenderer.close()
                }
            }
        }
    }

    LaunchedEffect(tileRenderer, containerSize, baseDecodedSize) {
        val renderer = tileRenderer ?: return@LaunchedEffect
        if (containerSize.width <= 0 || containerSize.height <= 0 || baseDecodedSize.width <= 0) {
            return@LaunchedEffect
        }
        snapshotFlow {
            val s = scaleAnimatable.value * currentDragScaleFactor * currentImageScaleMultiplier
            val tx = offsetAnimatable.value.x
            val ty = offsetAnimatable.value.y + dismissOffsetY.value
            val cw = containerSize.width.toFloat()
            val ch = containerSize.height.toFloat()
            val origAspect = renderer.originalWidth.toFloat() / renderer.originalHeight.toFloat()
            val containerAspect = cw / ch
            val (fittedW, fittedH) = if (origAspect > containerAspect) {
                cw to (cw / origAspect)
            } else {
                (ch * origAspect) to ch
            }
            val scaledW = fittedW * s
            val scaledH = fittedH * s
            val sampleSize = computeSubsampleSize(
                originalWidth = renderer.originalWidth,
                baseWidth = baseDecodedSize.width,
                scaledWidthOnScreen = scaledW
            )
            if (sampleSize == null) {
                emptyList()
            } else {
                val imgLeft = (cw - scaledW) * 0.5f + tx
                val imgTop = (ch - scaledH) * 0.5f + ty
                computeVisibleTiles(
                    origW = renderer.originalWidth,
                    origH = renderer.originalHeight,
                    containerW = cw,
                    containerH = ch,
                    scaledW = scaledW,
                    scaledH = scaledH,
                    imgLeft = imgLeft,
                    imgTop = imgTop,
                    sampleSize = sampleSize
                )
            }
        }.collectLatest { visibleTiles ->
            for (spec in visibleTiles) {
                if (renderer.decodeTileIfNeeded(spec)) {
                    tileRenderVersion++
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = effectiveBackdropAlpha))
            .clipToBounds()
            .onSizeChanged { newSize ->
                containerSize = newSize
                val clamped = clampZoomOffset(
                    offset = offsetAnimatable.value,
                    scale = scaleAnimatable.value,
                    containerSize = newSize,
                    imageAspectRatio = imageAspectRatio
                )
                if (clamped != offsetAnimatable.value) {
                    scope.launch(start = CoroutineStart.UNDISPATCHED) {
                        offsetAnimatable.snapTo(clamped)
                    }
                }
            }
            .pointerInput(dismissThresholdPx) {
                awaitEachGesture {
                    var zoom = 1f
                    var pan = Offset.Zero
                    var pastTouchSlop = false
                    var lockedToDismiss = false
                    var totalDismissY = dismissOffsetY.value
                    val touchSlop = viewConfiguration.touchSlop

                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        val canceled = event.changes.any { it.isConsumed }
                        if (!canceled) {
                            val pressedCount = event.changes.count { it.pressed }
                            val zoomChange = event.calculateZoom()
                            val panChange = event.calculatePan()

                            if (!pastTouchSlop) {
                                zoom *= zoomChange
                                pan += panChange
                                val centroidSize = event.calculateCentroidSize(useCurrent = false)
                                val zoomMotion = abs(1 - zoom) * centroidSize
                                val panMotion = pan.getDistance()
                                if (zoomMotion > touchSlop || panMotion > touchSlop) {
                                    pastTouchSlop = true
                                    lockedToDismiss = pressedCount == 1 &&
                                        scaleAnimatable.value <= DOUBLE_TAP_ZOOMED_THRESHOLD &&
                                        pan.y > 0f &&
                                        pan.y > abs(pan.x) * 1.2f
                                    if (lockedToDismiss) {
                                        totalDismissY = pan.y.coerceAtLeast(0f)
                                        scope.launch(start = CoroutineStart.UNDISPATCHED) {
                                            dismissOffsetY.snapTo(totalDismissY)
                                        }
                                        event.changes.forEach {
                                            if (it.positionChanged()) it.consume()
                                        }
                                    }
                                }
                            } else if (lockedToDismiss) {
                                if (pressedCount > 1) {
                                    lockedToDismiss = false
                                    scope.launch {
                                        dismissOffsetY.animateTo(0f, tween(150, easing = FastOutSlowInEasing))
                                    }
                                } else {
                                    totalDismissY = (totalDismissY + panChange.y).coerceAtLeast(0f)
                                    scope.launch(start = CoroutineStart.UNDISPATCHED) {
                                        dismissOffsetY.snapTo(totalDismissY)
                                    }
                                    event.changes.forEach {
                                        if (it.positionChanged()) it.consume()
                                    }
                                }
                            } else {
                                if (zoomChange.isFinite() && zoomChange > 0f &&
                                    (zoomChange != 1f || panChange != Offset.Zero)
                                ) {
                                    val centroid = event.calculateCentroid(useCurrent = false)
                                    val oldScale = scaleAnimatable.value
                                    val newScale = (oldScale * zoomChange).coerceIn(MIN_FULLSCREEN_ZOOM, MAX_FULLSCREEN_ZOOM)
                                    val containerCenter = Offset(
                                        x = containerSize.width / 2f,
                                        y = containerSize.height / 2f
                                    )
                                    val rawOffset = calculateZoomedOffset(
                                        currentScale = oldScale,
                                        targetScale = newScale,
                                        currentOffset = offsetAnimatable.value,
                                        centroid = centroid,
                                        pan = panChange,
                                        containerCenter = containerCenter
                                    )
                                    val clampedOffset = clampZoomOffset(
                                        offset = rawOffset,
                                        scale = newScale,
                                        containerSize = containerSize,
                                        imageAspectRatio = imageAspectRatio
                                    )
                                    scope.launch(start = CoroutineStart.UNDISPATCHED) {
                                        if (dismissOffsetY.value != 0f) {
                                            dismissOffsetY.snapTo(0f)
                                        }
                                        scaleAnimatable.snapTo(newScale)
                                        offsetAnimatable.snapTo(clampedOffset)
                                    }
                                    event.changes.forEach {
                                        if (it.positionChanged()) it.consume()
                                    }
                                }
                            }
                        }
                    } while (!canceled && event.changes.any { it.pressed })

                    if (lockedToDismiss) {
                        if (dismissOffsetY.value >= dismissThresholdPx) {
                            isClosingByDrag = true
                            onTapFeedback()
                            onClose()
                        } else {
                            scope.launch {
                                dismissOffsetY.animateTo(
                                    targetValue = 0f,
                                    animationSpec = tween(200, easing = FastOutSlowInEasing)
                                )
                            }
                        }
                    }
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        showControls = !showControls
                    },
                    onDoubleTap = { tapOffset ->
                        scope.launch {
                            val currentScale = scaleAnimatable.value
                            val targetScale = if (currentScale > DOUBLE_TAP_ZOOMED_THRESHOLD) {
                                MIN_FULLSCREEN_ZOOM
                            } else {
                                DOUBLE_TAP_ZOOM_SCALE
                            }
                            val containerCenter = Offset(
                                x = containerSize.width / 2f,
                                y = containerSize.height / 2f
                            )
                            val rawTargetOffset = if (targetScale <= MIN_FULLSCREEN_ZOOM) {
                                Offset.Zero
                            } else {
                                calculateZoomedOffset(
                                    currentScale = currentScale,
                                    targetScale = targetScale,
                                    currentOffset = offsetAnimatable.value,
                                    centroid = tapOffset,
                                    pan = Offset.Zero,
                                    containerCenter = containerCenter
                                )
                            }
                            val targetOffset = clampZoomOffset(
                                offset = rawTargetOffset,
                                scale = targetScale,
                                containerSize = containerSize,
                                imageAspectRatio = imageAspectRatio
                            )
                            val scaleSpec = tween<Float>(durationMillis = 280, easing = FastOutSlowInEasing)
                            val offsetSpec = tween<Offset>(durationMillis = 280, easing = FastOutSlowInEasing)
                            coroutineScope {
                                launch { scaleAnimatable.animateTo(targetScale, animationSpec = scaleSpec) }
                                launch { offsetAnimatable.animateTo(targetOffset, animationSpec = offsetSpec) }
                            }
                        }
                    }
                )
            }
            .testTag("fullscreen_image_viewer")
    ) {
        AsyncImage(
            model = effectiveImageRequest,
            contentDescription = "Full Screen Wallpaper",
            contentScale = ContentScale.Fit,
            filterQuality = FilterQuality.High,
            onSuccess = { state ->
                isFullImageLoading = false
                val drawable = state.result.drawable
                val bmp = (drawable as? BitmapDrawable)?.bitmap
                if (bmp != null && bmp.width > 0 && bmp.height > 0) {
                    baseDecodedSize = IntSize(bmp.width, bmp.height)
                    imageAspectRatio = bmp.width.toFloat() / bmp.height.toFloat()
                } else {
                    val intrinsic = state.painter.intrinsicSize
                    if (intrinsic.width.isFinite() && intrinsic.height.isFinite() &&
                        intrinsic.width > 0f && intrinsic.height > 0f
                    ) {
                        baseDecodedSize = IntSize(intrinsic.width.roundToInt(), intrinsic.height.roundToInt())
                        imageAspectRatio = intrinsic.width / intrinsic.height
                    }
                }
            },
            onError = { isFullImageLoading = false },
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val s = scaleAnimatable.value * dragScaleFactor * imageScaleMultiplier
                    val t = offsetAnimatable.value
                    transformOrigin = TransformOrigin.Center
                    scaleX = s
                    scaleY = s
                    translationX = t.x
                    translationY = t.y + dismissOffsetY.value
                    alpha = contentAlphaMultiplier
                }
                .testTag("fullscreen_zoomable_image")
        )

        val activeTileRenderer = tileRenderer
        if (activeTileRenderer != null && baseDecodedSize.width > 0 && containerSize.width > 0 && containerSize.height > 0) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = contentAlphaMultiplier }
            ) {
                @Suppress("UNUSED_VARIABLE")
                val version = tileRenderVersion
                val cw = size.width
                val ch = size.height
                if (cw <= 0f || ch <= 0f) return@Canvas

                val s = scaleAnimatable.value * dragScaleFactor * imageScaleMultiplier
                val tx = offsetAnimatable.value.x
                val ty = offsetAnimatable.value.y + dismissOffsetY.value
                val origW = activeTileRenderer.originalWidth
                val origH = activeTileRenderer.originalHeight
                val origAspect = origW.toFloat() / origH.toFloat()
                val containerAspect = cw / ch
                val (fittedW, fittedH) = if (origAspect > containerAspect) {
                    cw to (cw / origAspect)
                } else {
                    (ch * origAspect) to ch
                }
                val scaledW = fittedW * s
                val scaledH = fittedH * s
                val sampleSize = computeSubsampleSize(
                    originalWidth = origW,
                    baseWidth = baseDecodedSize.width,
                    scaledWidthOnScreen = scaledW
                ) ?: return@Canvas

                val imgLeft = (cw - scaledW) * 0.5f + tx
                val imgTop = (ch - scaledH) * 0.5f + ty
                val visibleTiles = computeVisibleTiles(
                    origW = origW,
                    origH = origH,
                    containerW = cw,
                    containerH = ch,
                    scaledW = scaledW,
                    scaledH = scaledH,
                    imgLeft = imgLeft,
                    imgTop = imgTop,
                    sampleSize = sampleSize
                )
                for (spec in visibleTiles) {
                    val tileBmp = activeTileRenderer.getCachedTile(spec.key) ?: continue
                    val dstLeft = (imgLeft + (spec.srcRect.left.toFloat() / origW) * scaledW).roundToInt()
                    val dstTop = (imgTop + (spec.srcRect.top.toFloat() / origH) * scaledH).roundToInt()
                    val dstRight = (imgLeft + (spec.srcRect.right.toFloat() / origW) * scaledW).roundToInt()
                    val dstBottom = (imgTop + (spec.srcRect.bottom.toFloat() / origH) * scaledH).roundToInt()
                    val dstWidth = (dstRight - dstLeft).coerceAtLeast(1)
                    val dstHeight = (dstBottom - dstTop).coerceAtLeast(1)
                    drawImage(
                        image = tileBmp,
                        dstOffset = IntOffset(dstLeft, dstTop),
                        dstSize = IntSize(dstWidth, dstHeight),
                        filterQuality = FilterQuality.Medium
                    )
                }
            }
        }

        if (isFullImageLoading) {
            CircularProgressIndicator(
                color = Color.White.copy(alpha = 0.7f),
                strokeWidth = 2.dp,
                modifier = Modifier
                    .size(36.dp)
                    .align(Alignment.Center)
            )
        }

        // Top Bar with Resolution / File Size Info and Close Button (toggled on single tap)
        AnimatedVisibility(
            visible = showControls && !isClosingByDrag,
            enter = fadeIn(tween(180)) + slideInVertically(tween(200)) { -it / 2 },
            exit = fadeOut(tween(180)) + slideOutVertically(tween(200)) { -it / 2 },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .graphicsLayer { alpha = dragControlsAlpha * contentAlphaMultiplier }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .testTag("fullscreen_top_controls"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = infoText,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                IconButton(
                    onClick = {
                        onTapFeedback()
                        onClose()
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("fullscreen_close_btn")
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_close),
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
