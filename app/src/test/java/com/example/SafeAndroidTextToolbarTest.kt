package com.example

import android.app.Activity
import android.view.ActionMode
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.FrameLayout
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.ui.components.SafeAndroidTextToolbar
import com.example.ui.components.SafeTextToolbarProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SafeAndroidTextToolbarTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private class FakeFloatingActionMode(
        private val callback: ActionMode.Callback2
    ) : ActionMode() {
        var finishCalls: Int = 0
        var invalidateCalls: Int = 0
        var invalidateRectCalls: Int = 0

        override fun finish() {
            finishCalls++
            callback.onDestroyActionMode(this)
        }

        override fun invalidate() {
            invalidateCalls++
        }

        override fun invalidateContentRect() {
            invalidateRectCalls++
        }

        override fun setTitle(title: CharSequence?) = Unit
        override fun setTitle(resId: Int) = Unit
        override fun setSubtitle(subtitle: CharSequence?) = Unit
        override fun setSubtitle(resId: Int) = Unit
        override fun setCustomView(view: View?) = Unit
        override fun getMenu(): Menu? = null
        override fun getTitle(): CharSequence? = null
        override fun getSubtitle(): CharSequence? = null
        override fun getCustomView(): View? = null
        override fun getMenuInflater(): MenuInflater? = null
    }

    private fun createAttachedHostView(
        onStartMode: (ActionMode.Callback2) -> FakeFloatingActionMode
    ): Pair<View, MutableList<FakeFloatingActionMode>> {
        val controller = Robolectric.buildActivity(Activity::class.java).setup()
        val activity = controller.get()
        val createdModes = mutableListOf<FakeFloatingActionMode>()
        val hostView = object : FrameLayout(activity) {
            override fun startActionMode(callback: ActionMode.Callback?, type: Int): ActionMode? {
                val cb2 = callback as ActionMode.Callback2
                val mode = onStartMode(cb2)
                createdModes += mode
                return mode
            }
        }
        activity.setContentView(hostView)
        return hostView to createdModes
    }

    @Test
    fun `clicking Copy or Paste when callback calls hide finishes ActionMode only once`() {
        var capturedCallback: ActionMode.Callback2? = null
        val (hostView, createdModes) = createAttachedHostView { cb ->
            capturedCallback = cb
            FakeFloatingActionMode(cb)
        }
        val toolbar = SafeAndroidTextToolbar(hostView)

        toolbar.showMenu(
            rect = Rect(0f, 0f, 100f, 40f),
            onCopyRequested = {
                // Simulates Compose SelectionManager.copy() which calls textToolbar.hide()
                toolbar.hide()
            },
            onPasteRequested = null,
            onCutRequested = null,
            onSelectAllRequested = null
        )

        assertEquals(TextToolbarStatus.Shown, toolbar.status)
        assertEquals(1, createdModes.size)
        val mode = createdModes.single()

        // Invoke Copy item click directly via ActionMode.Callback2
        val fakeCopyItem = Robolectric.buildActivity(Activity::class.java).setup().get()
            .let { activity ->
                val menu = android.widget.PopupMenu(activity, hostView).menu
                capturedCallback!!.onCreateActionMode(mode, menu)
                menu.findItem(0)
            }

        assertTrue(capturedCallback!!.onActionItemClicked(mode, fakeCopyItem))
        assertEquals(1, mode.finishCalls)
        assertEquals(TextToolbarStatus.Hidden, toolbar.status)

        // Calling hide() repeatedly must never double-finish
        toolbar.hide()
        toolbar.hide()
        assertEquals(1, mode.finishCalls)
    }

    @Test
    fun `detaching host view skips finish on already cleaned DecorView mode`() {
        val (hostView, createdModes) = createAttachedHostView { cb ->
            FakeFloatingActionMode(cb)
        }
        val toolbar = SafeAndroidTextToolbar(hostView)

        toolbar.showMenu(
            rect = Rect(10f, 10f, 80f, 50f),
            onCopyRequested = null,
            onPasteRequested = {},
            onCutRequested = null,
            onSelectAllRequested = null
        )
        assertEquals(1, createdModes.size)
        val mode = createdModes.single()

        // Host detaches before dispose calls hide()
        toolbar.onHostDetached()
        toolbar.hide()

        assertEquals(0, mode.finishCalls)
        assertEquals(TextToolbarStatus.Hidden, toolbar.status)
    }

    @Test
    fun `SafeTextToolbarProvider overrides LocalTextToolbar inside composition`() {
        var isSafeToolbarProvided = false
        composeTestRule.setContent {
            SafeTextToolbarProvider {
                isSafeToolbarProvided = LocalTextToolbar.current is SafeAndroidTextToolbar
            }
        }
        assertTrue(isSafeToolbarProvided)
    }
}
