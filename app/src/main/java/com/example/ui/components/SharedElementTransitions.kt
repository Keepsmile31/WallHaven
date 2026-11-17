package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedTransitionScope = staticCompositionLocalOf<SharedTransitionScope?> { null }

val LocalDetailTransitionScope = compositionLocalOf<AnimatedVisibilityScope?> { null }
val LocalSettingsTransitionScope = compositionLocalOf<AnimatedVisibilityScope?> { null }
val LocalSearchHistoryTransitionScope = compositionLocalOf<AnimatedVisibilityScope?> { null }

enum class WallpaperSharedElementPart {
    Container,
    Image
}

data class WallpaperSharedElementKey(
    val wallpaperId: String,
    val origin: String,
    val part: WallpaperSharedElementPart
)

enum class SettingsSharedElementPart {
    Container,
    Icon,
    Title
}

data class SettingsSharedElementKey(
    val route: String,
    val part: SettingsSharedElementPart
)

enum class SearchHistorySharedElementPart {
    Container,
    Icon,
    Title
}

data class SearchHistorySharedElementKey(
    val part: SearchHistorySharedElementPart
)

@OptIn(ExperimentalSharedTransitionApi::class)
val MaterialYouBoundsTransform = BoundsTransform { _, _ ->
    spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow
    )
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun EnsureSharedTransitionScope(
    content: @Composable (SharedTransitionScope) -> Unit
) {
    val existing = LocalSharedTransitionScope.current
    if (existing != null) {
        content(existing)
    } else {
        SharedTransitionLayout {
            CompositionLocalProvider(LocalSharedTransitionScope provides this) {
                content(this)
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
fun Modifier.wallhavenSharedBounds(
    key: Any,
    animatedVisibilityScope: AnimatedVisibilityScope?,
    clipShape: Shape = RoundedCornerShape(16.dp),
    resizeMode: SharedTransitionScope.ResizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
    zIndexInOverlay: Float = 0f,
    renderInOverlayDuringTransition: Boolean = true
): Modifier = composed {
    val sharedScope = LocalSharedTransitionScope.current
    if (sharedScope != null && animatedVisibilityScope != null) {
        with(sharedScope) {
            this@wallhavenSharedBounds.sharedBounds(
                rememberSharedContentState(key = key),
                animatedVisibilityScope = animatedVisibilityScope,
                boundsTransform = MaterialYouBoundsTransform,
                enter = fadeIn(tween(240, easing = FastOutSlowInEasing)),
                exit = fadeOut(tween(180, easing = FastOutSlowInEasing)),
                resizeMode = resizeMode,
                clipInOverlayDuringTransition = OverlayClip(clipShape),
                zIndexInOverlay = zIndexInOverlay,
                renderInOverlayDuringTransition = renderInOverlayDuringTransition
            )
        }
    } else {
        this
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
fun Modifier.wallhavenSharedElement(
    key: Any,
    animatedVisibilityScope: AnimatedVisibilityScope?,
    clipShape: Shape? = null,
    zIndexInOverlay: Float = 0f,
    renderInOverlayDuringTransition: Boolean = true
): Modifier = composed {
    val sharedScope = LocalSharedTransitionScope.current
    if (sharedScope != null && animatedVisibilityScope != null) {
        with(sharedScope) {
            val baseModifier = if (clipShape != null) {
                this@wallhavenSharedElement.sharedElement(
                    rememberSharedContentState(key = key),
                    animatedVisibilityScope = animatedVisibilityScope,
                    boundsTransform = MaterialYouBoundsTransform,
                    clipInOverlayDuringTransition = OverlayClip(clipShape),
                    zIndexInOverlay = zIndexInOverlay,
                    renderInOverlayDuringTransition = renderInOverlayDuringTransition
                )
            } else {
                this@wallhavenSharedElement.sharedElement(
                    rememberSharedContentState(key = key),
                    animatedVisibilityScope = animatedVisibilityScope,
                    boundsTransform = MaterialYouBoundsTransform,
                    zIndexInOverlay = zIndexInOverlay,
                    renderInOverlayDuringTransition = renderInOverlayDuringTransition
                )
            }
            baseModifier
        }
    } else {
        this
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
fun Modifier.wallhavenSkipToLookaheadSize(): Modifier = composed {
    val sharedScope = LocalSharedTransitionScope.current
    if (sharedScope != null) {
        with(sharedScope) {
            this@wallhavenSkipToLookaheadSize.skipToLookaheadSize()
        }
    } else {
        this
    }
}

fun Modifier.wallhavenAnimateEnterExit(
    animatedVisibilityScope: AnimatedVisibilityScope?,
    enter: EnterTransition = fadeIn(tween(220, delayMillis = 50, easing = FastOutSlowInEasing)),
    exit: ExitTransition = fadeOut(tween(120, easing = FastOutSlowInEasing))
): Modifier = if (animatedVisibilityScope != null) {
    with(animatedVisibilityScope) {
        this@wallhavenAnimateEnterExit.animateEnterExit(
            enter = enter,
            exit = exit
        )
    }
} else {
    this
}
