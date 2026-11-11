package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign

/**
 * Liquid Glass Segmented Tab Capsule for Discovery page ("最新", "热门", "随机", "榜单").
 * Features real AGSL backdrop refraction, smooth sliding active capsule, and lifted
 * refractive droplet lens when swiped horizontally across tabs.
 */
@Composable
fun ExploreLiquidGlassTabs(
    tabs: List<Pair<Int, String>>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    backdropLayer: GraphicsLayer?,
    modifier: Modifier = Modifier,
    liquidGlassMode: String = LocalLiquidGlassMode.current,
    testTagForTab: (Int) -> String = { "explore_tab_$it" }
) {
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val backdropBaseColor = MaterialTheme.colorScheme.background
    val density = LocalDensity.current
    val viewConfiguration = LocalViewConfiguration.current
    val haptic = LocalHapticFeedback.current

    val safeCount = tabs.size.coerceAtLeast(1)
    val clampedIndex = selectedIndex.coerceIn(0, safeCount - 1)
    val currentSelectedState = rememberUpdatedState(clampedIndex)
    val currentOnSelectState = rememberUpdatedState(onSelect)

    var isDragging by remember { mutableStateOf(false) }
    var isActiveSlotPressed by remember { mutableStateOf(false) }
    var isTapPulsing by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(clampedIndex.toFloat()) }
    var lastHapticTab by remember { mutableIntStateOf(clampedIndex) }
    var lastSelectedTab by remember { mutableIntStateOf(clampedIndex) }
    var barPosInRoot by remember { mutableStateOf(Offset.Zero) }
    var barSizePx by remember { mutableStateOf(IntSize.Zero) }

    LaunchedEffect(clampedIndex) {
        if (!isDragging) {
            dragFraction = clampedIndex.toFloat()
            lastHapticTab = clampedIndex
        }
        if (clampedIndex != lastSelectedTab) {
            lastSelectedTab = clampedIndex
            if (!isDragging) {
                isTapPulsing = true
                kotlinx.coroutines.delay(260)
                isTapPulsing = false
            }
        }
    }

    val targetFraction = if (isDragging) dragFraction else clampedIndex.toFloat()
    val animatedFraction by animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = if (isDragging) {
            snap()
        } else {
            spring(
                dampingRatio = 0.78f,
                stiffness = Spring.StiffnessMediumLow
            )
        },
        label = "explore_tabs_fraction"
    )

    val isLensElevated = isDragging || isActiveSlotPressed || isTapPulsing
    val dragProgress by animateFloatAsState(
        targetValue = if (isLensElevated) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.74f,
            stiffness = Spring.StiffnessMedium
        ),
        label = "explore_tabs_drag_progress"
    )

    val isGlassActive = liquidGlassMode != "none"
    val isUltimate = liquidGlassMode == "ultimate"
    val lensScaleY = when (liquidGlassMode) {
        "ultimate" -> 1f + 0.22f * dragProgress
        "standard" -> 1f + 0.08f * dragProgress
        else -> 1f + 0.03f * dragProgress
    }
    val lensScaleX = when (liquidGlassMode) {
        "ultimate" -> 1f + 0.18f * dragProgress
        "standard" -> 1f + 0.08f * dragProgress
        else -> 1f + 0.03f * dragProgress
    }
    val lensElevation by animateDpAsState(
        targetValue = if (isLensElevated) (if (isDarkTheme) 12.dp else 8.dp) else 0.dp,
        animationSpec = spring(
            dampingRatio = 0.80f,
            stiffness = Spring.StiffnessMedium
        ),
        label = "explore_tabs_lens_elevation"
    )

    val hasValidBackdrop = isGlassActive &&
        backdropLayer != null &&
        backdropLayer.size.width > 0 &&
        backdropLayer.size.height > 0

    val barShader = remember { LiquidGlassShaders.createBarShader() }
    val lensShader = remember { LiquidGlassShaders.createLensShader() }
    val tabsContentLayer = rememberGraphicsLayer()

    val barRenderEffect = remember(
        liquidGlassMode,
        barSizePx.width,
        barSizePx.height,
        hasValidBackdrop,
        backdropBaseColor,
        isDarkTheme
    ) {
        if (!hasValidBackdrop || barSizePx.width <= 0 || barSizePx.height <= 0) {
            null
        } else {
            when (liquidGlassMode) {
                "standard" -> {
                    LiquidGlassShaders.buildStandardBlurEffect(with(density) { 12.dp.toPx() })
                }
                "ultimate" -> {
                    val w = barSizePx.width.toFloat()
                    val h = barSizePx.height.toFloat()
                    LiquidGlassShaders.buildUltimateCapsuleEffect(
                        shader = barShader,
                        widthPx = w,
                        heightPx = h,
                        cornerRadiusPx = h * 0.5f,
                        blurRadiusPx = with(density) { 8.dp.toPx() },
                        refractionPx = with(density) { 10.dp.toPx() },
                        dispersion = 0.20f,
                        zoom = 0.02f,
                        edgeBandPx = with(density) { 14.dp.toPx() },
                        specular = if (isDarkTheme) 0.14f else 0.24f,
                        fallbackColorR = backdropBaseColor.red,
                        fallbackColorG = backdropBaseColor.green,
                        fallbackColorB = backdropBaseColor.blue
                    )
                }
                else -> null
            }
        }
    }

    val surfaceContainerHigh = MaterialTheme.colorScheme.surfaceContainerHigh
    val surfaceContainer = MaterialTheme.colorScheme.surfaceContainer
    val primaryColor = MaterialTheme.colorScheme.primary

    val containerAlpha = when (liquidGlassMode) {
        "none" -> if (isDarkTheme) 0.92f else 0.96f
        "standard" -> if (isDarkTheme) 0.68f else 0.82f
        else -> if (isDarkTheme) 0.58f else 0.78f
    }

    val containerBrush = remember(isDarkTheme, containerAlpha, surfaceContainerHigh, surfaceContainer) {
        if (isDarkTheme) {
            Brush.verticalGradient(
                colors = listOf(
                    surfaceContainerHigh.copy(alpha = containerAlpha),
                    surfaceContainer.copy(alpha = (containerAlpha - 0.08f).coerceAtLeast(0.32f))
                )
            )
        } else {
            Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = containerAlpha),
                    Color(0xFFF6F8FC).copy(alpha = (containerAlpha - 0.08f).coerceAtLeast(0.62f))
                )
            )
        }
    }

    val containerBorderBrush = remember(isDarkTheme, isGlassActive) {
        if (isDarkTheme) {
            Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = if (isGlassActive) 0.24f else 0.15f),
                    Color.White.copy(alpha = 0.06f)
                )
            )
        } else {
            Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.96f),
                    Color.Black.copy(alpha = if (isGlassActive) 0.06f else 0.09f)
                )
            )
        }
    }

    val restingIndicatorBrush = remember(isDarkTheme, primaryColor) {
        if (isDarkTheme) {
            Brush.verticalGradient(
                colors = listOf(
                    primaryColor.copy(alpha = 0.22f),
                    primaryColor.copy(alpha = 0.14f)
                )
            )
        } else {
            Brush.verticalGradient(
                colors = listOf(
                    primaryColor.copy(alpha = 0.14f),
                    primaryColor.copy(alpha = 0.10f)
                )
            )
        }
    }

    val restingIndicatorBorderBrush = remember(isDarkTheme) {
        if (isDarkTheme) {
            Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.10f),
                    Color.Transparent
                )
            )
        } else {
            Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.45f),
                    Color.Transparent
                )
            )
        }
    }

    val draggingBorderBrush = remember(isDarkTheme) {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = if (isDarkTheme) 0.55f else 0.96f),
                Color(0xFF7DD3FC).copy(alpha = if (isDarkTheme) 0.42f else 0.50f),
                Color(0xFFFDA4AF).copy(alpha = if (isDarkTheme) 0.34f else 0.42f),
                Color.White.copy(alpha = if (isDarkTheme) 0.45f else 0.92f)
            )
        )
    }

    val draggingGlossBrush = remember(isDarkTheme) {
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = if (isDarkTheme) 0.12f else 0.18f),
                Color.Transparent,
                Color.White.copy(alpha = if (isDarkTheme) 0.06f else 0.08f)
            )
        )
    }

    Box(
        modifier = modifier
            .height(42.dp)
            .onGloballyPositioned { coords ->
                barPosInRoot = coords.positionInRoot()
                barSizePx = coords.size
            }
    ) {
        // Layer 1: Liquid Glass Capsule Track
        if (hasValidBackdrop && backdropLayer != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .shadow(
                        elevation = if (isDarkTheme) 8.dp else 4.dp,
                        shape = CircleShape,
                        ambientColor = Color.Black.copy(alpha = if (isDarkTheme) 0.40f else 0.05f),
                        spotColor = Color.Black.copy(alpha = if (isDarkTheme) 0.45f else 0.09f)
                    )
                    .clip(CircleShape)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            renderEffect = barRenderEffect
                            clip = true
                            shape = CircleShape
                        }
                ) {
                    drawRect(color = backdropBaseColor)
                    translate(left = -barPosInRoot.x, top = -barPosInRoot.y) {
                        drawLayer(backdropLayer)
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(containerBrush)
                        .border(width = 1.dp, brush = containerBorderBrush, shape = CircleShape)
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .shadow(
                        elevation = if (isDarkTheme) 8.dp else 4.dp,
                        shape = CircleShape,
                        ambientColor = Color.Black.copy(alpha = if (isDarkTheme) 0.40f else 0.05f),
                        spotColor = Color.Black.copy(alpha = if (isDarkTheme) 0.45f else 0.09f)
                    )
                    .clip(CircleShape)
                    .background(containerBrush)
                    .border(width = 1.dp, brush = containerBorderBrush, shape = CircleShape)
            )
        }

        // Layer 2: Tab Interaction, Sliding Indicator & Dragging Refractive Lens
        val innerPadDp = 3.dp
        val innerPadPx = with(density) { innerPadDp.toPx() }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadDp)
        ) {
            val itemWidthDp = maxWidth / safeCount
            val itemWidthPx = with(density) { itemWidthDp.toPx() }.coerceAtLeast(1f)
            val trackHeightPx = with(density) { maxHeight.toPx() }.coerceAtLeast(1f)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(safeCount, itemWidthPx) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val velocityTracker = VelocityTracker()
                            velocityTracker.addPosition(down.uptimeMillis, down.position)

                            val touchSlop = viewConfiguration.touchSlop
                            val activeIndexAtDown = currentSelectedState.value
                            val pointerTabAtDown =
                                (down.position.x / itemWidthPx - 0.5f).coerceIn(0f, (safeCount - 1).toFloat())
                            val nearActiveSlot = abs(pointerTabAtDown - activeIndexAtDown) <= 0.65f
                            if (nearActiveSlot) {
                                isActiveSlotPressed = true
                            }
                            val startAnchor = if (nearActiveSlot) {
                                activeIndexAtDown.toFloat()
                            } else {
                                pointerTabAtDown
                            }

                            var totalDx = 0f
                            var dragStarted = false
                            var lastPosition = down.position

                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Main)
                                val change = event.changes.firstOrNull { it.id == down.id }
                                    ?: event.changes.firstOrNull()
                                if (change == null || !change.pressed) {
                                    break
                                }

                                velocityTracker.addPosition(change.uptimeMillis, change.position)
                                val dx = change.positionChange().x
                                totalDx += dx
                                lastPosition = change.position

                                if (!dragStarted && abs(totalDx) > touchSlop) {
                                    dragStarted = true
                                    isDragging = true
                                    lastHapticTab = activeIndexAtDown
                                }

                                if (dragStarted) {
                                    change.consume()
                                    val nextFraction = (startAnchor + totalDx / itemWidthPx)
                                        .coerceIn(0f, (safeCount - 1).toFloat())
                                    dragFraction = nextFraction
                                    val hoveredTab = nextFraction.roundToInt().coerceIn(0, safeCount - 1)
                                    if (hoveredTab != lastHapticTab) {
                                        lastHapticTab = hoveredTab
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                }
                            }

                            isActiveSlotPressed = false
                            if (dragStarted) {
                                val velocityX = velocityTracker.calculateVelocity().x
                                val velocityBias = if (abs(velocityX) > 650f) {
                                    sign(velocityX) * 0.35f
                                } else {
                                    0f
                                }
                                val targetTab = (dragFraction + velocityBias)
                                    .roundToInt()
                                    .coerceIn(0, safeCount - 1)
                                isDragging = false
                                if (targetTab != currentSelectedState.value) {
                                    currentOnSelectState.value(targetTab)
                                }
                            } else {
                                val tappedTab = (lastPosition.x / itemWidthPx)
                                    .toInt()
                                    .coerceIn(0, safeCount - 1)
                                isDragging = false
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                currentOnSelectState.value(tappedTab)
                            }
                        }
                    }
            ) {
                // Resting / non-ultimate sliding capsule indicator
                if (!isUltimate || dragProgress < 0.99f) {
                    val restAlpha = if (isUltimate) (1f - dragProgress).coerceIn(0f, 1f) else 1f
                    Box(
                        modifier = Modifier
                            .offset { IntOffset((animatedFraction * itemWidthPx).roundToInt(), 0) }
                            .width(itemWidthDp)
                            .fillMaxHeight()
                            .graphicsLayer {
                                alpha = restAlpha
                                if (!isUltimate) {
                                    scaleX = lensScaleX
                                    scaleY = lensScaleY
                                }
                            }
                            .clip(CircleShape)
                            .background(restingIndicatorBrush)
                            .border(width = 1.dp, brush = restingIndicatorBorderBrush, shape = CircleShape)
                    )
                }

                // Tab labels row
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawWithContent {
                            if (isUltimate && (isLensElevated || dragProgress > 0.01f)) {
                                tabsContentLayer.record {
                                    this@drawWithContent.drawContent()
                                }
                                drawLayer(tabsContentLayer)
                            } else {
                                drawContent()
                            }
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    tabs.forEach { (index, title) ->
                        val proximity = (1f - abs(animatedFraction - index)).coerceIn(0f, 1f)
                        val isSelected = clampedIndex == index
                        val isActiveVisual = proximity > 0.5f

                        val textColor = lerp(
                            MaterialTheme.colorScheme.onSurfaceVariant,
                            MaterialTheme.colorScheme.primary,
                            proximity
                        )
                        val itemScale = 1f + (if (isLensElevated) 0.06f else 0.02f) * proximity

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .graphicsLayer {
                                    scaleX = itemScale
                                    scaleY = itemScale
                                }
                                .semantics {
                                    role = Role.Tab
                                    selected = isSelected
                                }
                                .testTag(testTagForTab(index))
                        ) {
                            Text(
                                text = title,
                                color = textColor,
                                fontSize = 13.sp,
                                fontWeight = if (isActiveVisual) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1
                            )
                        }
                    }
                }

                // Dragging / Elevated Ultimate Liquid Glass Droplet Lens
                if (isUltimate && (isLensElevated || dragProgress > 0.01f)) {
                    val lensHeightPx = trackHeightPx * lensScaleY
                    val lensWidthPx = maxOf(itemWidthPx * lensScaleX, lensHeightPx * (1f + 0.36f * dragProgress))
                    val lensLeftPx = animatedFraction * itemWidthPx - (lensWidthPx - itemWidthPx) * 0.5f
                    val lensTopPx = -(lensHeightPx - trackHeightPx) * 0.5f

                    val lensRenderEffect = remember(
                        lensWidthPx,
                        lensHeightPx,
                        dragProgress,
                        isDarkTheme,
                        backdropBaseColor
                    ) {
                        LiquidGlassShaders.buildUltimateCapsuleEffect(
                            shader = lensShader,
                            widthPx = lensWidthPx,
                            heightPx = lensHeightPx,
                            cornerRadiusPx = lensHeightPx * 0.5f,
                            blurRadiusPx = 0f,
                            refractionPx = with(density) { (16.dp * dragProgress).toPx() },
                            dispersion = 0.56f * dragProgress,
                            zoom = 0.08f * dragProgress,
                            edgeBandPx = with(density) { 13.dp.toPx() },
                            specular = if (isDarkTheme) 0.24f else 0.34f,
                            fallbackColorR = backdropBaseColor.red,
                            fallbackColorG = backdropBaseColor.green,
                            fallbackColorB = backdropBaseColor.blue
                        )
                    }

                    Box(
                        modifier = Modifier
                            .layout { measurable, _ ->
                                val w = lensWidthPx.roundToInt().coerceAtLeast(1)
                                val h = lensHeightPx.roundToInt().coerceAtLeast(1)
                                val placeable = measurable.measure(Constraints.fixed(w, h))
                                layout(0, 0) {
                                    placeable.place(
                                        x = lensLeftPx.roundToInt(),
                                        y = lensTopPx.roundToInt()
                                    )
                                }
                            }
                            .graphicsLayer { alpha = dragProgress.coerceIn(0f, 1f) }
                            .shadow(
                                elevation = lensElevation,
                                shape = CircleShape,
                                ambientColor = Color.Black.copy(alpha = if (isDarkTheme) 0.45f else 0.08f),
                                spotColor = Color.Black.copy(alpha = if (isDarkTheme) 0.55f else 0.14f)
                            )
                            .clip(CircleShape)
                    ) {
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    renderEffect = lensRenderEffect
                                    clip = true
                                    shape = CircleShape
                                }
                        ) {
                            drawRect(color = backdropBaseColor)
                            if (backdropLayer != null && backdropLayer.size.width > 0 && backdropLayer.size.height > 0) {
                                translate(
                                    left = -(barPosInRoot.x + innerPadPx + lensLeftPx),
                                    top = -(barPosInRoot.y + innerPadPx + lensTopPx)
                                ) {
                                    drawLayer(backdropLayer)
                                }
                            }
                            drawRect(
                                color = if (isDarkTheme) {
                                    Color(0xFF202229).copy(alpha = 0.22f)
                                } else {
                                    Color.White.copy(alpha = 0.18f)
                                }
                            )
                            if (tabsContentLayer.size.width > 0 && tabsContentLayer.size.height > 0) {
                                translate(left = -lensLeftPx, top = -lensTopPx) {
                                    drawLayer(tabsContentLayer)
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(draggingGlossBrush)
                                .border(
                                    width = 1.2.dp,
                                    brush = draggingBorderBrush,
                                    shape = CircleShape
                                )
                        )
                    }
                }
            }
        }
    }
}

/**
 * Generic circular Liquid Glass icon button (used for Filter & Clear History actions).
 */
@Composable
fun ExploreLiquidGlassCircleButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    tint: Color,
    backdropLayer: GraphicsLayer?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    showBadge: Boolean = false,
    liquidGlassMode: String = LocalLiquidGlassMode.current
) {
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val backdropBaseColor = MaterialTheme.colorScheme.background
    val density = LocalDensity.current

    var btnPosInRoot by remember { mutableStateOf(Offset.Zero) }
    var btnSizePx by remember { mutableStateOf(IntSize.Zero) }

    val isGlassActive = liquidGlassMode != "none"
    val hasValidBackdrop = isGlassActive &&
        backdropLayer != null &&
        backdropLayer.size.width > 0 &&
        backdropLayer.size.height > 0

    val orbShader = remember { LiquidGlassShaders.createBarShader() }
    val orbRenderEffect = remember(
        liquidGlassMode,
        btnSizePx.width,
        btnSizePx.height,
        hasValidBackdrop,
        backdropBaseColor,
        isDarkTheme
    ) {
        if (!hasValidBackdrop || btnSizePx.width <= 0 || btnSizePx.height <= 0) {
            null
        } else {
            when (liquidGlassMode) {
                "standard" -> {
                    LiquidGlassShaders.buildStandardBlurEffect(with(density) { 12.dp.toPx() })
                }
                "ultimate" -> {
                    val w = btnSizePx.width.toFloat()
                    val h = btnSizePx.height.toFloat()
                    LiquidGlassShaders.buildUltimateCapsuleEffect(
                        shader = orbShader,
                        widthPx = w,
                        heightPx = h,
                        cornerRadiusPx = h * 0.5f,
                        blurRadiusPx = with(density) { 8.dp.toPx() },
                        refractionPx = with(density) { 8.dp.toPx() },
                        dispersion = 0.22f,
                        zoom = 0.04f,
                        edgeBandPx = with(density) { 10.dp.toPx() },
                        specular = if (isDarkTheme) 0.18f else 0.28f,
                        fallbackColorR = backdropBaseColor.red,
                        fallbackColorG = backdropBaseColor.green,
                        fallbackColorB = backdropBaseColor.blue
                    )
                }
                else -> null
            }
        }
    }

    val surfaceContainerHigh = MaterialTheme.colorScheme.surfaceContainerHigh
    val surfaceContainer = MaterialTheme.colorScheme.surfaceContainer

    val orbAlpha = when (liquidGlassMode) {
        "none" -> if (isDarkTheme) 0.92f else 0.96f
        "standard" -> if (isDarkTheme) 0.68f else 0.82f
        else -> if (isDarkTheme) 0.58f else 0.78f
    }

    val containerBrush = remember(isDarkTheme, orbAlpha, surfaceContainerHigh, surfaceContainer) {
        if (isDarkTheme) {
            Brush.verticalGradient(
                colors = listOf(
                    surfaceContainerHigh.copy(alpha = orbAlpha),
                    surfaceContainer.copy(alpha = (orbAlpha - 0.08f).coerceAtLeast(0.32f))
                )
            )
        } else {
            Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = orbAlpha),
                    Color(0xFFF6F8FC).copy(alpha = (orbAlpha - 0.08f).coerceAtLeast(0.62f))
                )
            )
        }
    }

    val borderBrush = remember(isDarkTheme, isGlassActive) {
        if (isDarkTheme) {
            Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = if (isGlassActive) 0.26f else 0.16f),
                    Color.White.copy(alpha = 0.08f)
                )
            )
        } else {
            Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.96f),
                    Color.Black.copy(alpha = if (isGlassActive) 0.06f else 0.09f)
                )
            )
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(42.dp)
            .onGloballyPositioned { coords ->
                btnPosInRoot = coords.positionInRoot()
                btnSizePx = coords.size
            }
            .shadow(
                elevation = if (isDarkTheme) 8.dp else 4.dp,
                shape = CircleShape,
                ambientColor = Color.Black.copy(alpha = if (isDarkTheme) 0.40f else 0.05f),
                spotColor = Color.Black.copy(alpha = if (isDarkTheme) 0.45f else 0.09f)
            )
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        if (hasValidBackdrop && backdropLayer != null) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        renderEffect = orbRenderEffect
                        clip = true
                        shape = CircleShape
                    }
            ) {
                drawRect(color = backdropBaseColor)
                translate(left = -btnPosInRoot.x, top = -btnPosInRoot.y) {
                    drawLayer(backdropLayer)
                }
            }
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .background(containerBrush)
                .border(width = 1.dp, brush = borderBrush, shape = CircleShape)
        ) {
            BadgedBox(
                badge = {
                    if (showBadge) {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(8.dp)
                        )
                    }
                }
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    tint = tint,
                    modifier = Modifier.size(19.dp)
                )
            }
        }
    }
}

/**
 * Matching Liquid Glass circular filter button beside the Discovery page top tabs.
 */
@Composable
fun ExploreLiquidGlassFilterButton(
    hasActiveFilters: Boolean,
    backdropLayer: GraphicsLayer?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ExploreLiquidGlassCircleButton(
        icon = ImageVector.vectorResource(R.drawable.ic_filter_list),
        contentDescription = stringResource(R.string.action_filter),
        tint = if (hasActiveFilters) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        backdropLayer = backdropLayer,
        onClick = onClick,
        showBadge = hasActiveFilters,
        modifier = modifier.testTag("explore_filter_btn")
    )
}
