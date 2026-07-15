package com.example.service

import android.accessibilityservice.AccessibilityService
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import androidx.compose.ui.platform.ComposeView
import com.example.state.DynamicIslandStateManager
import com.example.ui.DynamicIslandView
import com.example.ui.theme.MyApplicationTheme

class DynamicIslandAccessibilityService : AccessibilityService() {

    private var windowManager: WindowManager? = null
    private var composeView: ComposeView? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        composeView = ComposeView(this).apply {
            setContent {
                MyApplicationTheme {
                    DynamicIslandView(isPreviewMode = false)
                }
            }
        }

        val settings = DynamicIslandStateManager.settings.value
        val params = WindowManager.LayoutParams(
            settings.width.toInt(),
            settings.height.toInt(),
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            x = 0
            y = settings.yOffset.toInt()
        }

        windowManager?.addView(composeView, params)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Handle accessibility events here if needed
    }

    override fun onInterrupt() {
        // Handle service interruption
    }

    override fun onDestroy() {
        super.onDestroy()
        if (composeView != null && windowManager != null) {
            windowManager?.removeView(composeView)
        }
    }
}
