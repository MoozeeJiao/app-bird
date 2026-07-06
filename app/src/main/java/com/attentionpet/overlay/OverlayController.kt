package com.attentionpet.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.attentionpet.domain.RuleEvaluationResult

class OverlayController(private val context: Context) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val positionStore = OverlayPositionStore(context)
    private var capsuleView: View? = null
    private var panelView: View? = null
    private var sheetView: View? = null

    fun showCapsule(result: RuleEvaluationResult, onClick: () -> Unit = {}) {
        val view = capsuleView ?: composeView().also {
            capsuleView = it
            windowManager.addView(it, capsuleParams())
        }
        (view as ComposeView).setContent {
            CapsuleOverlay(result = result, onClick = onClick)
        }
        view.setOnClickListener { onClick() }
    }

    fun showPanel(result: RuleEvaluationResult, currentSessionText: String = "") {
        hidePanel()
        val view = composeView()
        panelView = view
        view.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_OUTSIDE) {
                hidePanel()
                true
            } else {
                false
            }
        }
        view.setContent {
            ExpandedPanelOverlay(result = result, currentSessionText = currentSessionText, onDismiss = ::hidePanel)
        }
        windowManager.addView(view, panelParams())
    }

    fun showTimeoutSheet(onRest: () -> Unit = {}, onExtend: () -> Unit = {}) {
        hidePanel()
        hideTimeoutSheet()
        val view = composeView()
        sheetView = view
        view.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_OUTSIDE) {
                hideTimeoutSheet()
                true
            } else {
                false
            }
        }
        view.setContent {
            TimeoutSheetOverlay(onRest = onRest, onExtend = onExtend)
        }
        windowManager.addView(view, timeoutSheetParams())
    }

    fun hidePanel() {
        panelView?.let { windowManager.removeView(it) }
        panelView = null
    }

    fun hideTimeoutSheet() {
        sheetView?.let { windowManager.removeView(it) }
        sheetView = null
    }

    fun hideAll() {
        hidePanel()
        hideTimeoutSheet()
        capsuleView?.let { windowManager.removeView(it) }
        capsuleView = null
    }

    private fun capsuleParams(): WindowManager.LayoutParams = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        x = 0
        y = 108
    }

    private fun panelParams(): WindowManager.LayoutParams = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        x = 0
        y = 108
    }

    private fun timeoutSheetParams(): WindowManager.LayoutParams = WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.BOTTOM
    }

    private fun composeView(): ComposeView {
        return ComposeView(context).apply {
            val owner = OverlayLifecycleOwner()
            setViewTreeLifecycleOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            owner.handleResume()
        }
    }
}

private class OverlayLifecycleOwner : LifecycleOwner, SavedStateRegistryOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateController = SavedStateRegistryController.create(this)

    init {
        savedStateController.performAttach()
        savedStateController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
    }

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateController.savedStateRegistry

    fun handleResume() {
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
    }
}
