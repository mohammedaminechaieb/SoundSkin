package com.soundskin.app.hud

import android.content.Context
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.WindowManager
import com.soundskin.app.data.HudPosition
import com.soundskin.app.data.HudSettings
import kotlin.math.min

/**
 * Owns the HUD window. It's added as TYPE_ACCESSIBILITY_OVERLAY from the
 * accessibility service's own context, which needs no "draw over other
 * apps" permission and no foreground-service notification — and it sits
 * above the status bar and other overlays, where a volume panel belongs.
 */
class HudController(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val handler = Handler(Looper.getMainLooper())
    private val dp = context.resources.displayMetrics.density

    private var view: VolumeHudView? = null
    private var attached = false
    private var currentPosition: HudPosition? = null

    private val hideRunnable = Runnable { hide() }

    fun show(level: Int, max: Int, streamLabel: String, settings: HudSettings) {
        val v = view ?: VolumeHudView(context).also { view = it }
        v.skin = settings.skin
        v.accent = settings.accent
        v.showPercent = settings.showPercent
        v.streamLabel = streamLabel

        if (attached && currentPosition != settings.position) detach()
        if (!attached) {
            runCatching {
                windowManager.addView(v, layoutParams(settings.position))
                attached = true
                currentPosition = settings.position
                v.setLevel(level, max, animate = false)
                v.alpha = 0f
                v.scaleX = 0.92f
                v.scaleY = 0.92f
                v.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(140).start()
            }
        }
        v.setLevel(level, max)

        handler.removeCallbacks(hideRunnable)
        handler.postDelayed(hideRunnable, settings.hideAfterMs)
    }

    fun hide() {
        val v = view ?: return
        if (!attached) return
        v.animate().alpha(0f).scaleX(0.92f).scaleY(0.92f).setDuration(160).withEndAction { detach() }.start()
    }

    fun release() {
        handler.removeCallbacks(hideRunnable)
        detach()
        view = null
    }

    private fun detach() {
        view?.let { v ->
            v.animate().cancel()
            if (attached) runCatching { windowManager.removeView(v) }
        }
        attached = false
    }

    private fun layoutParams(position: HudPosition): WindowManager.LayoutParams {
        val screenWidth = context.resources.displayMetrics.widthPixels
        val width = min((360 * dp).toInt(), screenWidth - (32 * dp).toInt())
        return WindowManager.LayoutParams(
            width,
            (92 * dp).toInt(),
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER_HORIZONTAL or when (position) {
                HudPosition.TOP -> Gravity.TOP
                HudPosition.CENTER -> Gravity.CENTER_VERTICAL
                HudPosition.BOTTOM -> Gravity.BOTTOM
            }
            y = when (position) {
                HudPosition.TOP -> (40 * dp).toInt()
                HudPosition.CENTER -> 0
                HudPosition.BOTTOM -> (96 * dp).toInt()
            }
        }
    }
}
