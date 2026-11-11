package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign

@Immutable
data class NavBarDestination(
    val index: Int,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

/**
 * Floating capsule navigation bar ("Floating Widget") supporting continuous swipe/drag switching
 * and 3 experimental Liquid Glass modes: "none", "standard", and "ultimate".
 */
@Composable
fun FloatingCapsuleNavigationBar(
    destinations: List<NavBarDestination>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    backdropLayer: GraphicsLayer? = null,
    liquidGlassMode: String = LocalLiquidGlassMode.current,
    modifier: Modifier = Modifier
) {
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val density = LocalDensity.current
    val viewConfiguration = LocalViewConfiguration.current
    val haptic = LocalHapticFeedback.current

    val safeCount = destinations.size.coerceAtLeast(1)
    val clampedIndex = selectedIndex.coerceIn(0, safeCount - 1)
    val currentSelectedIndexState = rememberUpdatedState(clampedIndex)
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
        label = "capsule_indicator_fraction"
    )

    val isLensElevated = isDragging || isActiveSlotPressed || isTapPulsing
    val dragProgress by animateFloatAsState(
        targetValue = if (isLensElevated) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.74f,
            stiffness = Spring.StiffnessMedium
        ),
        label = "capsule_drag_progress"
    )

    val backdropBaseColor = MaterialTheme.colorScheme.background

    val lensScaleY = when (liquidGlassMode) {
        "ultimate" -> 1f + 0.22f * dragProgress
        "standard" -> 1f + 0.10f * dragProgress
        else -> 1f + 0.03f * dragProgress
    }
    val lensScaleX = when (liquidGlassMode) {
        "ultimate" -> 1f + 0.24f * dragProgress
        "standard" -> 1f + 0.12f * dragProgress
        else -> 1f + 0.04f * dragProgress
    }
    val lensElevation by animateDpAsState(
        targetValue = if (isLensElevated) (if (isDarkTheme) 14.dp else 9.dp) else 0.dp,
        animationSpec = spring(
            dampingRatio = 0.80f,
            stiffness = Spring.StiffnessMedium
        ),
        label = "capsule_lens_elevation"
    )

    val isGlassActive = liquidGlassMode != "none"
    val isUltimate = liquidGlassMode == "ultimate"
    val hasValidBackdrop = isGlassActive &&
        backdropLayer != null &&
        backdropLayer.size.width > 0 &&
        backdropLayer.size.height > 0

    val barShader = remember { LiquidGlassShaders.createBarShader() }
    val lensShader = remember { LiquidGlassShaders.createLensShader() }
    val barContentLayer = rememberGraphicsLayer()

    val barRenderEffect = remember(
        liquidGlassMode,
        barSizePx.width,
        barSizePx.height,
        hasValidBackdrop,
        backdropBaseColor
    ) {
        if (!hasValidBackdrop || barSizePx.width <= 0 || barSizePx.height <= 0) {
            null
        } else {
            when (liquidGlassMode) {
                "standard" -> {
                    val blurPx = with(density) { 14.dp.toPx() }
                    LiquidGlassShaders.buildStandardBlurEffect(blurPx)
                }
                "ultimate" -> {
                    val w = barSizePx.width.toFloat()
                    val h = barSizePx.height.toFloat()
                    val blurPx = with(density) { 8.dp.toPx() }
                    val refractionPx = with(density) { 14.dp.toPx() }
                    val edgeBandPx = with(density) { 18.dp.toPx() }
                    LiquidGlassShaders.buildUltimateCapsuleEffect(
                        shader = barShader,
                        widthPx = w,
                        heightPx = h,
                        cornerRadiusPx = h * 0.5f,
                        blurRadiusPx = blurPx,
                        refractionPx = refractionPx,
                        dispersion = 0.22f,
                        zoom = 0.02f,
                        edgeBandPx = edgeBandPx,
                        specular = if (isDarkTheme) 0.14f else 0.22f,
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
        "none" -> if (isDarkTheme) 0.94f else 0.96f
        "standard" -> if (isDarkTheme) 0.66f else 0.80f
        else -> if (isDarkTheme) 0.52f else 0.78f
    }

    val containerBrush = remember(isDarkTheme, containerAlpha, surfaceContainerHigh, surfaceContainer) {
        if (isDarkTheme) {
            Brush.verticalGradient(
                colors = listOf(
                    surfaceContainerHigh.copy(alpha = containerAlpha),
                    surfaceContainer.copy(alpha = (containerAlpha - 0.06f).coerceAtLeast(0.32f))
                )
            )
        } else {
            Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = containerAlpha),
                    Color(0xFFF6F8FC).copy(alpha = (containerAlpha - 0.06f).coerceAtLeast(0.62f))
                )
            )
        }
    }

    val containerBorderBrush = remember(isDarkTheme, isGlassActive) {
        if (isDarkTheme) {
            Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = if (isGlassActive) 0.24f else 0.16f),
                    Color.White.copy(alpha = 0.06f)
                )
            )
        } else {
            Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.96f),
                    Color.Black.copy(alpha = if (isGlassActive) 0.07f else 0.10f)
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

    val draggingTintOverlayBrush = remember(isUltimate, isDarkTheme, restingIndicatorBrush) {
        if (isUltimate) {
            if (isDarkTheme) {
                Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.12f),
                        Color.Transparent,
                        Color.White.copy(alpha = 0.06f)
                    )
                )
            } else {
                Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.18f),
                        Color.Transparent,
                        Color.White.copy(alpha = 0.08f)
                    )
                )
            }
        } else {
            restingIndicatorBrush
        }
    }

    val draggingBorderBrush = remember(isUltimate, isDarkTheme, restingIndicatorBorderBrush) {
        if (isUltimate) {
            Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = if (isDarkTheme) 0.55f else 0.96f),
                    Color(0xFF7DD3FC).copy(alpha = if (isDarkTheme) 0.42f else 0.52f),
                    Color(0xFFFDA4AF).copy(alpha = if (isDarkTheme) 0.34f else 0.44f),
                    Color.White.copy(alpha = if (isDarkTheme) 0.45f else 0.92f)
                )
            )
        } else {
            restingIndicatorBorderBrush
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        val isCompactScreen = maxWidth < 400.dp
        val outerHorizontalPad = if (isCompactScreen) 10.dp else 16.dp
        val barHeight = if (safeCount >= 5 && isCompactScreen) 56.dp else 62.dp
        val innerPadHDp = if (isCompactScreen) 3.dp else 4.dp
        val innerPadVDp = 4.dp
        val innerPadHPx = with(density) { innerPadHDp.toPx() }
        val innerPadVPx = with(density) { innerPadVDp.toPx() }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = outerHorizontalPad, end = outerHorizontalPad, top = 6.dp, bottom = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp)
                    .height(barHeight)
                    .onGloballyPositioned { coords ->
                        barPosInRoot = coords.positionInRoot()
                        barSizePx = coords.size
                    }
                    .testTag("main_navigation_bar")
            ) {
                // Layer 1: Backdrop refraction + blur (Standard / Ultimate) clipped to pill
                if (hasValidBackdrop && backdropLayer != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .shadow(
                                elevation = if (isDarkTheme) 16.dp else 10.dp,
                                shape = CircleShape,
                                ambientColor = Color.Black.copy(alpha = if (isDarkTheme) 0.50f else 0.06f),
                                spotColor = Color.Black.copy(alpha = if (isDarkTheme) 0.55f else 0.11f)
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

                        // Translucent glass tint & specular rim over refracted backdrop
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(containerBrush)
                                .border(width = 1.dp, brush = containerBorderBrush, shape = CircleShape)
                        )
                    }
                } else {
                    // "None" mode or fallback before first backdrop frame
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .shadow(
                                elevation = if (isDarkTheme) 16.dp else 10.dp,
                                shape = CircleShape,
                                ambientColor = Color.Black.copy(alpha = if (isDarkTheme) 0.50f else 0.06f),
                                spotColor = Color.Black.copy(alpha = if (isDarkTheme) 0.55f else 0.11f)
                            )
                            .clip(CircleShape)
                            .background(containerBrush)
                            .border(width = 1.dp, brush = containerBorderBrush, shape = CircleShape)
                    )
                }

                // Layer 2: Unclipped interaction, tab items & lifted Liquid Glass droplet lens
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = innerPadHDp, vertical = innerPadVDp)
                ) {
                    val itemWidthDp = maxWidth / safeCount
                    val trackHeightDp = maxHeight
                    val trackWidthPx = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
                    val itemWidthPx = with(density) { itemWidthDp.toPx() }.coerceAtLeast(1f)
                    val trackHeightPx = with(density) { trackHeightDp.toPx() }.coerceAtLeast(1f)

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(safeCount, itemWidthPx) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                val velocityTracker = VelocityTracker()
                                velocityTracker.addPosition(down.uptimeMillis, down.position)

                                val touchSlop = viewConfiguration.touchSlop
                                val activeIndexAtDown = currentSelectedIndexState.value
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
                                    if (targetTab != currentSelectedIndexState.value) {
                                        currentOnSelectState.value(targetTab)
                                    }
                                } else {
                                    val tappedTab = (lastPosition.x / itemWidthPx)
                                        .toInt()
                                        .coerceIn(0, safeCount - 1)
                                    isDragging = false
                                    currentOnSelectState.value(tappedTab)
                                }
                            }
                        }
                ) {
                        // Resting / non-ultimate sliding capsule indicator behind items
                        if (!isUltimate || dragProgress < 0.99f) {
                            val restAlpha = if (isUltimate) (1f - dragProgress).coerceIn(0f, 1f) else 1f
                            val indicatorWidthPx = maxOf(itemWidthPx, trackHeightPx * 1.32f).coerceAtMost(trackWidthPx)
                            val indicatorWidthDp = with(density) { indicatorWidthPx.toDp() }
                            val indicatorCenterPx = animatedFraction * itemWidthPx + itemWidthPx * 0.5f
                            val indicatorLeftPx = (indicatorCenterPx - indicatorWidthPx * 0.5f)
                                .coerceIn(0f, (trackWidthPx - indicatorWidthPx).coerceAtLeast(0f))

                            val indicatorGlowActive = !isUltimate && isGlassActive && isLensElevated
                            Box(
                                modifier = Modifier
                                    .offset { IntOffset(indicatorLeftPx.roundToInt(), 0) }
                                    .width(indicatorWidthDp)
                                    .fillMaxHeight()
                                    .graphicsLayer {
                                        alpha = restAlpha
                                        if (!isUltimate) {
                                            scaleX = lensScaleX
                                            scaleY = lensScaleY
                                        }
                                    }
                                    .clip(CircleShape)
                                    .background(if (indicatorGlowActive) draggingTintOverlayBrush else restingIndicatorBrush)
                                    .border(
                                        width = if (indicatorGlowActive) 1.2.dp else 1.dp,
                                        brush = if (indicatorGlowActive) draggingBorderBrush else restingIndicatorBorderBrush,
                                        shape = CircleShape
                                    )
                            )
                        }

                        // Tab items row (recorded into barContentLayer for Ultimate droplet refraction)
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .drawWithContent {
                                    if (isUltimate && (isLensElevated || dragProgress > 0.01f)) {
                                        barContentLayer.record {
                                            this@drawWithContent.drawContent()
                                        }
                                        drawLayer(barContentLayer)
                                    } else {
                                        drawContent()
                                    }
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            destinations.forEach { dest ->
                                val proximity = (1f - abs(animatedFraction - dest.index)).coerceIn(0f, 1f)
                                val isSelected = dest.index == clampedIndex
                                val isActiveVisual = proximity > 0.5f

                                val contentColor = lerp(
                                    MaterialTheme.colorScheme.onSurfaceVariant,
                                    MaterialTheme.colorScheme.primary,
                                    proximity
                                )
                                val itemScale = 1f + (if (isLensElevated) 0.08f else 0.02f) * proximity

                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .padding(horizontal = 2.dp)
                                        .graphicsLayer {
                                            scaleX = itemScale
                                            scaleY = itemScale
                                        }
                                        .semantics {
                                            role = Role.Tab
                                            selected = isSelected
                                        }
                                        .testTag(dest.testTag),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = if (isActiveVisual) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.label,
                                        tint = contentColor,
                                        modifier = Modifier.size(if (isCompactScreen) 20.dp else 22.dp)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = dest.label,
                                        fontSize = if (isCompactScreen) 10.sp else 10.5.sp,
                                        lineHeight = if (isCompactScreen) 11.sp else 12.sp,
                                        fontWeight = if (isActiveVisual) FontWeight.SemiBold else FontWeight.Medium,
                                        color = contentColor,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        // Ultimate Liquid Glass Droplet Lens Overlay while swiping/lifting
                        if (isUltimate && (isLensElevated || dragProgress > 0.01f)) {
                            val lensHeightPx = trackHeightPx * lensScaleY
                            val minCapsuleWidthPx = lensHeightPx * (1f + 0.44f * dragProgress)
                            val lensWidthPx = maxOf(itemWidthPx * lensScaleX, minCapsuleWidthPx)

                            // Center the expanded droplet capsule symmetrically over the animated slot
                            val lensLeftPx = animatedFraction * itemWidthPx - (lensWidthPx - itemWidthPx) * 0.5f
                            val lensTopPx = -(lensHeightPx - trackHeightPx) * 0.5f

                            val lensRenderEffect = remember(
                                lensWidthPx,
                                lensHeightPx,
                                dragProgress,
                                isDarkTheme,
                                backdropBaseColor
                            ) {
                                val refractionPx = with(density) { (18.dp * dragProgress).toPx() }
                                val edgeBandPx = with(density) { 15.dp.toPx() }
                                LiquidGlassShaders.buildUltimateCapsuleEffect(
                                    shader = lensShader,
                                    widthPx = lensWidthPx,
                                    heightPx = lensHeightPx,
                                    cornerRadiusPx = lensHeightPx * 0.5f,
                                    blurRadiusPx = 0f,
                                    refractionPx = refractionPx,
                                    dispersion = 0.58f * dragProgress,
                                    zoom = 0.08f * dragProgress,
                                    edgeBandPx = edgeBandPx,
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
                                    .graphicsLayer {
                                        alpha = dragProgress.coerceIn(0f, 1f)
                                    }
                                    .shadow(
                                        elevation = lensElevation,
                                        shape = CircleShape,
                                        ambientColor = Color.Black.copy(alpha = if (isDarkTheme) 0.50f else 0.08f),
                                        spotColor = Color.Black.copy(alpha = if (isDarkTheme) 0.58f else 0.14f)
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
                                    // 0. Opaque base color prevents dark/transparent shadow bleed in Light Mode
                                    drawRect(color = backdropBaseColor)

                                    // 1. Refracted screen content directly underneath the expanded lens
                                    if (backdropLayer != null && backdropLayer.size.width > 0 && backdropLayer.size.height > 0) {
                                        translate(
                                            left = -(barPosInRoot.x + innerPadHPx + lensLeftPx),
                                            top = -(barPosInRoot.y + innerPadVPx + lensTopPx)
                                        ) {
                                            drawLayer(backdropLayer)
                                        }
                                    }

                                    // 2. Subtle crystal-clear glass tint inside the lens so underlying bar & items refract sharply
                                    drawRect(
                                        color = if (isDarkTheme) {
                                            Color(0xFF202229).copy(alpha = 0.22f)
                                        } else {
                                            Color.White.copy(alpha = 0.18f)
                                        }
                                    )

                                    // 3. Bar icons & labels underneath the lens, refracted by the same droplet shader
                                    if (barContentLayer.size.width > 0 && barContentLayer.size.height > 0) {
                                        translate(left = -lensLeftPx, top = -lensTopPx) {
                                            drawLayer(barContentLayer)
                                        }
                                    }
                                }

                                // Subtle top gloss & iridescent glass rim
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(draggingTintOverlayBrush)
                                        .border(
                                            width = 1.4.dp,
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
    }
}
