package com.example.ui.components

import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import kotlin.math.roundToInt

/**
 * Custom [TextToolbar] avoiding the Android DecorView double-finish FloatingActionMode bug
 * in Compose UI 1.7 when copying/pasting inside dialogs or disposing text selection hosts.
 *
 * Ensures:
 * - `actionMode` is cleared before calling `finish()`, preventing re-entrant double `finish()`
 *   that causes `DecorView: Destroying unexpected ActionMode instance of TYPE_FLOATING... Expected null`.
 * - `onActionItemClicked` does not call `finish()` a second time if the action callback already called `hide()`.
 * - `onDetachedFromWindow` clears stale `actionMode` references without calling `finish()` on a detached DecorView.
 */
internal class SafeAndroidTextToolbar(
    private val view: View
) : TextToolbar {

    override var status: TextToolbarStatus = TextToolbarStatus.Hidden
        private set

    internal var activeActionModeCountForTest: Int = 0
        private set

    private var actionMode: ActionMode? = null
    private var isModeClosing: Boolean = false
    private var modeGeneration: Long = 0L
    private var showSeq: Long = 0L
    private var contentRect: android.graphics.Rect = android.graphics.Rect()

    private var onCopyRequested: (() -> Unit)? = null
    private var onPasteRequested: (() -> Unit)? = null
    private var onCutRequested: (() -> Unit)? = null
    private var onSelectAllRequested: (() -> Unit)? = null

    private val actionModeCallback = object : ActionMode.Callback2() {
        override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            if (mode == null || menu == null) return false
            populateMenu(menu)
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            if (mode == null || menu == null) return false
            populateMenu(menu)
            return true
        }

        override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
            val callback: (() -> Unit)? = when (item?.itemId) {
                MENU_ID_COPY -> onCopyRequested
                MENU_ID_PASTE -> onPasteRequested
                MENU_ID_CUT -> onCutRequested
                MENU_ID_SELECT_ALL -> onSelectAllRequested
                else -> return false
            }
            val genBefore = modeGeneration
            val showBefore = showSeq
            callback?.invoke()
            // Only finish if the callback did not already hide() or refresh showMenu()
            if (actionMode === mode && !isModeClosing && modeGeneration == genBefore && showSeq == showBefore) {
                hide()
            }
            return true
        }

        override fun onDestroyActionMode(mode: ActionMode?) {
            if (actionMode === mode) {
                actionMode = null
                isModeClosing = true
                modeGeneration++
                status = TextToolbarStatus.Hidden
            }
        }

        override fun onGetContentRect(mode: ActionMode?, view: View?, outRect: android.graphics.Rect?) {
            outRect?.set(contentRect)
        }
    }

    private fun populateMenu(menu: Menu) {
        menu.clear()
        onCopyRequested?.let {
            menu.add(Menu.NONE, MENU_ID_COPY, 0, android.R.string.copy)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
        }
        onPasteRequested?.let {
            menu.add(Menu.NONE, MENU_ID_PASTE, 1, android.R.string.paste)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
        }
        onCutRequested?.let {
            menu.add(Menu.NONE, MENU_ID_CUT, 2, android.R.string.cut)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
        }
        onSelectAllRequested?.let {
            menu.add(Menu.NONE, MENU_ID_SELECT_ALL, 3, android.R.string.selectAll)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
        }
    }

    override fun showMenu(
        rect: Rect,
        onCopyRequested: (() -> Unit)?,
        onPasteRequested: (() -> Unit)?,
        onCutRequested: (() -> Unit)?,
        onSelectAllRequested: (() -> Unit)?
    ) {
        if (!view.isAttachedToWindow) return

        this.contentRect.set(
            rect.left.roundToInt(),
            rect.top.roundToInt(),
            rect.right.roundToInt(),
            rect.bottom.roundToInt()
        )
        this.onCopyRequested = onCopyRequested
        this.onPasteRequested = onPasteRequested
        this.onCutRequested = onCutRequested
        this.onSelectAllRequested = onSelectAllRequested
        showSeq++

        val existing = actionMode
        if (existing != null && !isModeClosing) {
            status = TextToolbarStatus.Shown
            existing.invalidate()
            existing.invalidateContentRect()
            return
        }

        actionMode = null
        isModeClosing = false
        modeGeneration++
        status = TextToolbarStatus.Shown
        val started = view.startActionMode(actionModeCallback, ActionMode.TYPE_FLOATING)
        actionMode = started
        if (started == null) {
            status = TextToolbarStatus.Hidden
        } else {
            activeActionModeCountForTest++
        }
    }

    override fun hide() {
        status = TextToolbarStatus.Hidden
        val current = actionMode ?: return
        actionMode = null
        modeGeneration++
        if (!isModeClosing) {
            isModeClosing = true
            if (view.isAttachedToWindow) {
                current.finish()
            }
        }
    }

    internal fun onHostDetached() {
        actionMode = null
        isModeClosing = true
        modeGeneration++
        status = TextToolbarStatus.Hidden
    }

    private companion object {
        const val MENU_ID_COPY = 0
        const val MENU_ID_PASTE = 1
        const val MENU_ID_CUT = 2
        const val MENU_ID_SELECT_ALL = 3
    }
}

@Composable
fun SafeTextToolbarProvider(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    val toolbar = remember(view) { SafeAndroidTextToolbar(view) }

    DisposableEffect(view, toolbar) {
        val attachListener = object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) = Unit
            override fun onViewDetachedFromWindow(v: View) {
                toolbar.onHostDetached()
            }
        }
        view.addOnAttachStateChangeListener(attachListener)
        onDispose {
            view.removeOnAttachStateChangeListener(attachListener)
            toolbar.hide()
        }
    }

    CompositionLocalProvider(LocalTextToolbar provides toolbar) {
        content()
    }
}
