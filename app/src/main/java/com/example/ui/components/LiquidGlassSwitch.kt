package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * FlClash-inspired Liquid Glass Switch:
 * - Compact capsule track + elongated pill thumb at rest.
 * - On press, tap toggle, or horizontal drag, the thumb lifts into an enlarged
 *   translucent Liquid Glass droplet with AGSL edge refraction and prismatic dispersion.
 */
@Composable
fun LiquidGlassSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    liquidGlassMode: String = LocalLiquidGlassMode.current,
    backdropColor: Color = MaterialTheme.colorScheme.surfaceVariant
        .copy(alpha = 0.45f)
        .compositeOver(MaterialTheme.colorScheme.background)
) {
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val isGlassActive = liquidGlassMode != "none"
    val isUltimate = liquidGlassMode == "ultimate"
    val density = LocalDensity.current
    val viewConfiguration = LocalViewConfiguration.current
    val haptic = LocalHapticFeedback.current

    val currentCheckedState = rememberUpdatedState(checked)
    val currentOnCheckedChange = rememberUpdatedState(onCheckedChange)
    val currentEnabled = rememberUpdatedState(enabled)

    var isPressed by remember { mutableStateOf(false) }
    var isDragging by remember { mutableStateOf(false) }
    var isTapPulsing by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(if (checked) 1f else 0f) }
    var lastCheckedValue by remember { mutableStateOf(checked) }

    LaunchedEffect(checked) {
        if (!isDragging) {
            dragFraction = if (checked) 1f else 0f
        }
        if (checked != lastCheckedValue) {
            lastCheckedValue = checked
            if (!isDragging) {
                isTapPulsing = true
                delay(320)
                isTapPulsing = false
            }
        }
    }

    val targetFraction = if (isDragging) dragFraction else if (checked) 1f else 0f
    val animatedFraction by animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = if (isDragging) {
            snap()
        } else {
            spring(
                dampingRatio = 0.74f,
                stiffness = Spring.StiffnessMediumLow
            )
        },
        label = "liquid_switch_fraction"
    )

    val isLensElevated = (isPressed || isDragging || isTapPulsing) && enabled
    val lensProgress by animateFloatAsState(
        targetValue = if (isLensElevated) 1f else 0f,
        animationSpec = spring(
            dampingRatio = 0.72f,
            stiffness = Spring.StiffnessMedium
        ),
        label = "liquid_switch_lens"
    )

    val containerWidthDp = 62.dp
    val containerHeightDp = 48.dp
    val trackWidthDp = 54.dp
    val trackHeightDp = 28.dp
    val thumbRestWidthDp = 30.dp
    val thumbRestHeightDp = 22.dp
    val thumbExpandedWidthDp = when (liquidGlassMode) {
        "ultimate" -> 40.dp
        "standard" -> 36.dp
        else -> 32.dp
    }
    val thumbExpandedHeightDp = when (liquidGlassMode) {
        "ultimate" -> 34.dp
        "standard" -> 29.dp
        else -> 22.dp
    }
    val thumbPadDp = 3.dp

    val containerWidthPx = with(density) { containerWidthDp.toPx() }
    val containerHeightPx = with(density) { containerHeightDp.toPx() }
    val trackWidthPx = with(density) { trackWidthDp.toPx() }
    val trackHeightPx = with(density) { trackHeightDp.toPx() }
    val thumbRestWidthPx = with(density) { thumbRestWidthDp.toPx() }
    val thumbRestHeightPx = with(density) { thumbRestHeightDp.toPx() }
    val thumbExpandedWidthPx = with(density) { thumbExpandedWidthDp.toPx() }
    val thumbExpandedHeightPx = with(density) { thumbExpandedHeightDp.toPx() }
    val thumbPadPx = with(density) { thumbPadDp.toPx() }

    val trackLeftPx = (containerWidthPx - trackWidthPx) * 0.5f
    val trackTopPx = (containerHeightPx - trackHeightPx) * 0.5f
    val travelPx = (trackWidthPx - thumbRestWidthPx - thumbPadPx * 2f).coerceAtLeast(1f)

    val offTrackColor = if (isDarkTheme) {
        Color(0xFF3E424E)
    } else {
        Color(0xFFB5BAC7)
    }
    val primaryColor = MaterialTheme.colorScheme.primary
    val inversePrimaryColor = MaterialTheme.colorScheme.inversePrimary
    val onTrackColor = if (isDarkTheme) {
        if (inversePrimaryColor.luminance() < primaryColor.luminance()) {
            lerp(inversePrimaryColor, primaryColor, 0.20f)
        } else {
            primaryColor
        }
    } else {
        primaryColor
    }
    val trackColor = lerp(offTrackColor, onTrackColor, animatedFraction)
        .let { if (enabled) it else it.copy(alpha = 0.45f) }

    val trackLayer = rememberGraphicsLayer()
    val lensShader = remember { LiquidGlassShaders.createLensShader() }

    val curThumbWidthPx = lerp(thumbRestWidthPx, thumbExpandedWidthPx, lensProgress)
    val curThumbHeightPx = lerp(thumbRestHeightPx, thumbExpandedHeightPx, lensProgress)
    val thumbCenterX = trackLeftPx + thumbPadPx + thumbRestWidthPx * 0.5f + animatedFraction * travelPx
    val thumbCenterY = trackTopPx + trackHeightPx * 0.5f
    val thumbLeftPx = thumbCenterX - curThumbWidthPx * 0.5f
    val thumbTopPx = thumbCenterY - curThumbHeightPx * 0.5f

    val lensRenderEffect = remember(
        liquidGlassMode,
        curThumbWidthPx,
        curThumbHeightPx,
        lensProgress,
        isDarkTheme,
        backdropColor
    ) {
        if (!isGlassActive || lensProgress <= 0.02f) {
            null
        } else {
            when (liquidGlassMode) {
                "standard" -> {
                    LiquidGlassShaders.buildStandardBlurEffect(with(density) { (6.dp * lensProgress).toPx() })
                }
                "ultimate" -> {
                    val refractionPx = with(density) { (12.dp * lensProgress).toPx() }
                    val edgeBandPx = with(density) { 11.dp.toPx() }
                    LiquidGlassShaders.buildUltimateCapsuleEffect(
                        shader = lensShader,
                        widthPx = curThumbWidthPx,
                        heightPx = curThumbHeightPx,
                        cornerRadiusPx = curThumbHeightPx * 0.5f,
                        blurRadiusPx = 0f,
                        refractionPx = refractionPx,
                        dispersion = 0.72f * lensProgress,
                        zoom = 0.09f * lensProgress,
                        edgeBandPx = edgeBandPx,
                        specular = if (isDarkTheme) 0.28f else 0.42f,
                        fallbackColorR = backdropColor.red,
                        fallbackColorG = backdropColor.green,
                        fallbackColorB = backdropColor.blue
                    )
                }
                else -> null
            }
        }
    }

    val lensBorderBrush = remember(isDarkTheme, isUltimate) {
        if (isUltimate) {
            Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = if (isDarkTheme) 0.65f else 0.96f),
                    Color(0xFF7DD3FC).copy(alpha = if (isDarkTheme) 0.38f else 0.45f),
                    Color(0xFFFDA4AF).copy(alpha = if (isDarkTheme) 0.30f else 0.36f),
                    Color.White.copy(alpha = if (isDarkTheme) 0.50f else 0.92f)
                )
            )
        } else {
            Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = if (isDarkTheme) 0.65f else 0.95f),
                    Color.White.copy(alpha = if (isDarkTheme) 0.25f else 0.65f)
                )
            )
        }
    }

    Box(
        modifier = modifier
            .width(containerWidthDp)
            .height(containerHeightDp)
            .semantics {
                role = Role.Switch
                toggleableState = ToggleableState(checked)
            }
            .pointerInput(travelPx) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    if (!currentEnabled.value) return@awaitEachGesture

                    isPressed = true
                    val touchSlop = viewConfiguration.touchSlop
                    val startFraction = if (currentCheckedState.value) 1f else 0f
                    var totalDx = 0f
                    var dragStarted = false
                    var lastMidpointCrossed = currentCheckedState.value

                    var releaseCanceled = false
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        val change = event.changes.firstOrNull { it.id == down.id }
                            ?: event.changes.firstOrNull()
                        if (change == null) {
                            releaseCanceled = true
                            break
                        }
                        if (!change.pressed) {
                            if (change.isConsumed) {
                                releaseCanceled = true
                            } else if (!dragStarted) {
                                // Consume tap release so parent M3SettingsItemRow clickable doesn't double-toggle
                                change.consume()
                            }
                            break
                        }

                        val dx = change.positionChange().x
                        totalDx += dx

                        if (!dragStarted && abs(totalDx) > touchSlop * 0.65f) {
                            dragStarted = true
                            isDragging = true
                        }

                        if (dragStarted) {
                            change.consume()
                            val nextFraction = (startFraction + totalDx / travelPx).coerceIn(0f, 1f)
                            dragFraction = nextFraction
                            val nowHalf = nextFraction >= 0.5f
                            if (nowHalf != lastMidpointCrossed) {
                                lastMidpointCrossed = nowHalf
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        }
                    }

                    isPressed = false
                    if (dragStarted) {
                        val finalChecked = dragFraction >= 0.5f
                        isDragging = false
                        if (finalChecked != currentCheckedState.value) {
                            currentOnCheckedChange.value(finalChecked)
                        }
                    } else {
                        isDragging = false
                        if (!releaseCanceled) {
                            currentOnCheckedChange.value(!currentCheckedState.value)
                        }
                    }
                }
            }
    ) {
        // Track pill recorded into trackLayer for glass refraction
        Box(
            modifier = Modifier
                .layout { measurable, _ ->
                    val w = trackWidthPx.roundToInt().coerceAtLeast(1)
                    val h = trackHeightPx.roundToInt().coerceAtLeast(1)
                    val placeable = measurable.measure(Constraints.fixed(w, h))
                    layout(0, 0) {
                        placeable.place(
                            x = trackLeftPx.roundToInt(),
                            y = trackTopPx.roundToInt()
                        )
                    }
                }
                .drawWithContent {
                    if (isGlassActive) {
                        trackLayer.record {
                            this@drawWithContent.drawContent()
                        }
                        drawLayer(trackLayer)
                    } else {
                        drawContent()
                    }
                }
                .clip(CircleShape)
                .background(trackColor)
                .border(
                    width = 0.8.dp,
                    color = if (isDarkTheme) {
                        Color.White.copy(alpha = 0.12f)
                    } else {
                        Color.Black.copy(alpha = 0.06f)
                    },
                    shape = CircleShape
                )
        )

        // Thumb / Elevated Liquid Glass Droplet Pill
        Box(
            modifier = Modifier
                .layout { measurable, _ ->
                    val w = curThumbWidthPx.roundToInt().coerceAtLeast(1)
                    val h = curThumbHeightPx.roundToInt().coerceAtLeast(1)
                    val placeable = measurable.measure(Constraints.fixed(w, h))
                    layout(0, 0) {
                        placeable.place(
                            x = thumbLeftPx.roundToInt(),
                            y = thumbTopPx.roundToInt()
                        )
                    }
                }
                .shadow(
                    elevation = lerp(3.dp.value, 8.dp.value, lensProgress).dp,
                    shape = CircleShape,
                    ambientColor = Color.Black.copy(alpha = if (isDarkTheme) 0.45f else 0.10f),
                    spotColor = Color.Black.copy(alpha = if (isDarkTheme) 0.55f else 0.16f)
                )
                .clip(CircleShape)
        ) {
            // Refracted / Frosted Liquid Glass lens interior when pressed / dragging / toggling
            if (isGlassActive && lensProgress > 0.02f) {
                Canvas(
                    modifier = Modifier
                        .matchParentSize()
                        .graphicsLayer {
                            alpha = lensProgress
                            renderEffect = lensRenderEffect
                            clip = true
                            shape = CircleShape
                        }
                ) {
                    drawRect(color = backdropColor)
                    translate(
                        left = trackLeftPx - thumbLeftPx,
                        top = trackTopPx - thumbTopPx
                    ) {
                        drawLayer(trackLayer)
                    }
                    drawRect(
                        color = if (isDarkTheme) {
                            Color.White.copy(alpha = if (isUltimate) 0.14f else 0.26f)
                        } else {
                            Color.White.copy(alpha = if (isUltimate) 0.18f else 0.34f)
                        }
                    )
                }

                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .graphicsLayer { alpha = lensProgress }
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = if (isDarkTheme) 0.16f else 0.28f),
                                    Color.Transparent,
                                    Color.White.copy(alpha = if (isDarkTheme) 0.08f else 0.14f)
                                )
                            )
                        )
                        .border(
                            width = 1.1.dp,
                            brush = lensBorderBrush,
                            shape = CircleShape
                        )
                )
            }

            // Crisp resting white capsule thumb
            val restAlpha = if (isGlassActive) (1f - lensProgress).coerceIn(0f, 1f) else 1f
            if (restAlpha > 0.01f) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .graphicsLayer { alpha = restAlpha }
                        .background(
                            color = if (enabled) Color.White else Color.White.copy(alpha = 0.72f),
                            shape = CircleShape
                        )
                        .border(
                            width = 0.75.dp,
                            color = Color.White.copy(alpha = 0.9f),
                            shape = CircleShape
                        )
                )
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.translate(
    left: Float,
    top: Float,
    block: androidx.compose.ui.graphics.drawscope.DrawScope.() -> Unit
) {
    drawContext.transform.translate(left, top)
    try {
        block()
    } finally {
        drawContext.transform.translate(-left, -top)
    }
}
