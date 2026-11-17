package com.example

import android.content.Context
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pinch
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.unit.IntSize
import androidx.test.core.app.ApplicationProvider
import coil.request.ImageRequest
import com.example.ui.components.DOUBLE_TAP_ZOOM_SCALE
import com.example.ui.components.MAX_FULLSCREEN_ZOOM
import com.example.ui.components.MIN_FULLSCREEN_ZOOM
import com.example.ui.components.ZoomableFullscreenViewer
import com.example.ui.components.calculateZoomedOffset
import com.example.ui.components.clampZoomOffset
import com.example.ui.components.computeSubsampleSize
import com.example.ui.components.computeVisibleTiles
import com.example.ui.theme.WallhavenTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FullscreenZoomTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `calculateZoomedOffset anchors scaling at pinch centroid and double tap point`() {
        val containerCenter = Offset(500f, 1000f)
        val focalPoint = Offset(750f, 1400f)

        // Double tap 1.0f -> 3.0f at (750, 1400): image point under (750, 1400) must stay at (750, 1400)
        val newOffset = calculateZoomedOffset(
            currentScale = 1.0f,
            targetScale = 3.0f,
            currentOffset = Offset.Zero,
            centroid = focalPoint,
            pan = Offset.Zero,
            containerCenter = containerCenter
        )
        val mappedPointAfterZoom = containerCenter + (focalPoint - containerCenter) * 3.0f + newOffset
        assertEquals(focalPoint.x, mappedPointAfterZoom.x, 0.01f)
        assertEquals(focalPoint.y, mappedPointAfterZoom.y, 0.01f)

        // Incremental pinch 2.0f -> 2.5f with pan delta (20, -15) around (300, 600)
        val startOffset = Offset(-120f, 80f)
        val pinchCentroid = Offset(300f, 600f)
        val panDelta = Offset(20f, -15f)
        val localOffsetFromCenter = (pinchCentroid - containerCenter - startOffset) / 2.0f
        val pinchedOffset = calculateZoomedOffset(
            currentScale = 2.0f,
            targetScale = 2.5f,
            currentOffset = startOffset,
            centroid = pinchCentroid,
            pan = panDelta,
            containerCenter = containerCenter
        )
        val mappedPinchPoint = containerCenter + localOffsetFromCenter * 2.5f + pinchedOffset
        assertEquals(pinchCentroid.x + panDelta.x, mappedPinchPoint.x, 0.01f)
        assertEquals(pinchCentroid.y + panDelta.y, mappedPinchPoint.y, 0.01f)
    }

    @Test
    fun `clampZoomOffset enforces boundaries at min zoom and fitted image edges`() {
        val container = IntSize(1000, 2000) // aspect 0.5

        // At 1.0f scale, offset is always clamped to zero
        assertEquals(
            Offset.Zero,
            clampZoomOffset(Offset(250f, -300f), MIN_FULLSCREEN_ZOOM, container, imageAspectRatio = 1.0f)
        )

        // Square 1:1 image fitted in 1000x2000 is 1000x1000.
        // At 1.5x, scaled image is 1500x1500 -> maxOffsetX = 250, maxOffsetY = 0 (still shorter than 2000).
        val clamped15x = clampZoomOffset(
            offset = Offset(600f, 400f),
            scale = 1.5f,
            containerSize = container,
            imageAspectRatio = 1.0f
        )
        assertEquals(250f, clamped15x.x, 0.01f)
        assertEquals(0f, clamped15x.y, 0.01f)

        // At 3.0x, scaled image is 3000x3000 -> maxOffsetX = 1000, maxOffsetY = 500.
        val clamped3x = clampZoomOffset(
            offset = Offset(-1800f, -900f),
            scale = DOUBLE_TAP_ZOOM_SCALE,
            containerSize = container,
            imageAspectRatio = 1.0f
        )
        assertEquals(-1000f, clamped3x.x, 0.01f)
        assertEquals(-500f, clamped3x.y, 0.01f)
    }

    @Test
    fun `fullscreen viewer supports tap toggle double tap animatable zoom pinch and close`() {
        var closed = false
        var latestScale = 1.0f
        val dummyRequest = ImageRequest.Builder(context)
            .data("https://example.com/wallpaper.jpg")
            .build()

        composeTestRule.setContent {
            WallhavenTheme {
                ZoomableFullscreenViewer(
                    imageRequest = dummyRequest,
                    infoText = "1920x1080 • 1.20 MB",
                    fallbackAspectRatio = 1.78f,
                    onClose = { closed = true },
                    onScaleChanged = { latestScale = it }
                )
            }
        }

        // Top controls start visible
        composeTestRule.onNodeWithTag("fullscreen_top_controls").assertIsDisplayed()
        assertEquals(1.0f, latestScale, 0.01f)

        // Single tap toggles controls off, then back on
        composeTestRule.onNodeWithTag("fullscreen_image_viewer").performTouchInput { click() }
        composeTestRule.mainClock.advanceTimeBy(600L)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("fullscreen_top_controls").assertDoesNotExist()

        composeTestRule.onNodeWithTag("fullscreen_image_viewer").performTouchInput { click() }
        composeTestRule.mainClock.advanceTimeBy(600L)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("fullscreen_top_controls").assertIsDisplayed()

        // Double tap animates smoothly to 3.0f
        composeTestRule.onNodeWithTag("fullscreen_image_viewer").performTouchInput {
            doubleClick(position = Offset(width * 0.7f, height * 0.35f))
        }
        composeTestRule.mainClock.advanceTimeBy(450L)
        composeTestRule.waitForIdle()
        assertEquals(DOUBLE_TAP_ZOOM_SCALE, latestScale, 0.05f)

        // Dragging while zoomed pans smoothly within bounds
        composeTestRule.onNodeWithTag("fullscreen_image_viewer").performTouchInput {
            swipeLeft()
        }
        composeTestRule.waitForIdle()
        assertEquals(DOUBLE_TAP_ZOOM_SCALE, latestScale, 0.05f)

        // Double tap again animates back to 1.0f
        composeTestRule.onNodeWithTag("fullscreen_image_viewer").performTouchInput {
            doubleClick(center)
        }
        composeTestRule.mainClock.advanceTimeBy(450L)
        composeTestRule.waitForIdle()
        assertEquals(MIN_FULLSCREEN_ZOOM, latestScale, 0.05f)

        // Pinch gesture scales within [MIN_FULLSCREEN_ZOOM, MAX_FULLSCREEN_ZOOM] (up to 8f)
        assertEquals(8.0f, MAX_FULLSCREEN_ZOOM, 0.001f)
        composeTestRule.onNodeWithTag("fullscreen_image_viewer").performTouchInput {
            pinch(
                start0 = Offset(centerX - 20f, centerY),
                end0 = Offset(centerX - 420f, centerY),
                start1 = Offset(centerX + 20f, centerY),
                end1 = Offset(centerX + 420f, centerY)
            )
        }
        composeTestRule.waitForIdle()
        assertTrue(latestScale > 4.0f && latestScale <= MAX_FULLSCREEN_ZOOM)

        // Close button dismisses fullscreen viewer
        composeTestRule.onNodeWithTag("fullscreen_close_btn").performClick()
        composeTestRule.waitForIdle()
        assertTrue(closed)
    }

    @Test
    fun `downward swipe at 1_0f closes fullscreen viewer while downward swipe at 3_0f only pans`() {
        var closedCount = 0
        val dummyRequest = ImageRequest.Builder(context)
            .data("https://example.com/wallpaper.jpg")
            .build()

        composeTestRule.setContent {
            WallhavenTheme {
                ZoomableFullscreenViewer(
                    imageRequest = dummyRequest,
                    infoText = "1920x1080 • 1.20 MB",
                    fallbackAspectRatio = 1.78f,
                    onClose = { closedCount++ }
                )
            }
        }

        // Double tap to zoom to 3.0f, then swipe down -> must pan, NOT close
        composeTestRule.onNodeWithTag("fullscreen_image_viewer").performTouchInput {
            doubleClick(center)
        }
        composeTestRule.mainClock.advanceTimeBy(450L)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("fullscreen_image_viewer").performTouchInput {
            down(center)
            moveBy(Offset(0f, 350f))
            up()
        }
        composeTestRule.waitForIdle()
        assertEquals(0, closedCount)

        // Double tap back to 1.0f, then swipe down beyond threshold -> must close
        composeTestRule.onNodeWithTag("fullscreen_image_viewer").performTouchInput {
            doubleClick(center)
        }
        composeTestRule.mainClock.advanceTimeBy(450L)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("fullscreen_image_viewer").performTouchInput {
            down(Offset(centerX, height * 0.25f))
            moveBy(Offset(0f, 450f))
            up()
        }
        composeTestRule.waitForIdle()
        assertEquals(1, closedCount)
    }

    @Test
    fun `computeSubsampleSize activates native tiles only when zoomed beyond base bitmap resolution`() {
        // 1080p image decoded at full 1920x1080 needs no extra tile decoding
        assertNull(computeSubsampleSize(originalWidth = 1920, baseWidth = 1920, scaledWidthOnScreen = 3240f))

        // Ultra-HD 7717x4341 decoded with base 4096px:
        // At 1.0x (1080px) or 2.0x (2160px), 4096px base already supersamples screen
        assertNull(computeSubsampleSize(originalWidth = 7717, baseWidth = 4096, scaledWidthOnScreen = 1080f))
        assertNull(computeSubsampleSize(originalWidth = 7717, baseWidth = 4096, scaledWidthOnScreen = 2160f))

        // At 3.0x double-tap (3240px) and 8.0x (8640px), sampleSize=1 activates full 7717x4341 tiles
        assertEquals(1, computeSubsampleSize(originalWidth = 7717, baseWidth = 4096, scaledWidthOnScreen = 3240f))
        assertEquals(1, computeSubsampleSize(originalWidth = 7717, baseWidth = 4096, scaledWidthOnScreen = 8640f))

        // 16000px image uses sampleSize=2 at 3240px (8000px > 4096px) and sampleSize=1 at 6480px
        assertEquals(2, computeSubsampleSize(originalWidth = 16000, baseWidth = 4096, scaledWidthOnScreen = 3240f))
        assertEquals(1, computeSubsampleSize(originalWidth = 16000, baseWidth = 4096, scaledWidthOnScreen = 6480f))
    }

    @Test
    fun `computeVisibleTiles covers visible viewport contiguously within original bounds`() {
        val tiles = computeVisibleTiles(
            origW = 7717,
            origH = 4341,
            containerW = 1080f,
            containerH = 2400f,
            scaledW = 3240f,
            scaledH = 1822.8f,
            imgLeft = -1080f,
            imgTop = 288.6f,
            sampleSize = 1,
            tileOutputPx = 768
        )
        assertTrue(tiles.isNotEmpty())
        tiles.forEach { spec ->
            assertTrue(spec.srcRect.left >= 0)
            assertTrue(spec.srcRect.top >= 0)
            assertTrue(spec.srcRect.right <= 7717)
            assertTrue(spec.srcRect.bottom <= 4341)
            assertTrue(spec.srcRect.width() > 0)
            assertTrue(spec.srcRect.height() > 0)
        }
        // Center of 7717x4341 (~3858, ~2170) must be covered by the visible tiles
        assertTrue(tiles.any { it.srcRect.contains(3858, 2170) })
    }
}
