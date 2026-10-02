package com.soundskin.app.hud

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.DecelerateInterpolator
import com.soundskin.app.data.SkinStyle
import kotlin.math.min
import kotlin.math.sin

/**
 * The volume HUD: a dark rounded card (readable over any app, light or
 * dark) with the stream name and percentage on top and the selected skin
 * drawn underneath in the accent color. Used both by the real overlay and
 * by the previews inside the app.
 */
class VolumeHudView(context: Context) : View(context) {

    var skin: SkinStyle = SkinStyle.MINIMAL
        set(value) { field = value; invalidate() }
    var accent: Int = Color.WHITE
        set(value) { field = value; invalidate() }
    var showPercent: Boolean = true
        set(value) { field = value; invalidate() }
    var streamLabel: String = "Media"
        set(value) { field = value; invalidate() }

    private val dp = resources.displayMetrics.density

    private var targetFraction = 0f
    private var shownFraction = 0f
    private var animStart = 0f
    private var peakHold = 0f
    private var peakHoldFrames = 0

    // Continuous clock for skins that move while the level holds steady.
    private var phase = 0f
    private val handler = Handler(Looper.getMainLooper())
    private val continuousTick = object : Runnable {
        override fun run() {
            phase += 0.15f
            if (peakHoldFrames > 0) peakHoldFrames-- else if (peakHold > shownFraction) peakHold = (peakHold - 0.01f).coerceAtLeast(shownFraction)
            invalidate()
            handler.postDelayed(this, 32)
        }
    }

    private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 220
        interpolator = DecelerateInterpolator()
        addUpdateListener {
            val t = it.animatedValue as Float
            shownFraction = animStart + (targetFraction - animStart) * t
            invalidate()
        }
    }

    val level: Float get() = targetFraction

    fun setLevel(current: Int, max: Int, animate: Boolean = true) {
        targetFraction = if (max > 0) (current.toFloat() / max).coerceIn(0f, 1f) else 0f
        if (targetFraction > peakHold) {
            peakHold = targetFraction
            peakHoldFrames = 30
        }
        animator.cancel()
        if (animate) {
            animStart = shownFraction
            animator.start()
        } else {
            shownFraction = targetFraction
            invalidate()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        handler.post(continuousTick)
    }

    override fun onDetachedFromWindow() {
        handler.removeCallbacks(continuousTick)
        animator.cancel()
        super.onDetachedFromWindow()
    }

    private val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(230, 22, 22, 26) }
    private val cardStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; color = Color.argb(40, 255, 255, 255)
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(200, 255, 255, 255); typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    private val percentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.RIGHT }
    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(50, 255, 255, 255) }
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
    private val wavePath = Path()

    /** Second gradient color derived from the accent (hue-shifted). */
    private val accent2: Int
        get() {
            val hsv = FloatArray(3)
            Color.colorToHSV(accent, hsv)
            return if (hsv[1] < 0.1f) Color.rgb(160, 160, 170) else Color.HSVToColor(floatArrayOf((hsv[0] + 60f) % 360f, hsv[1], hsv[2]))
        }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val pad = 4 * dp
        val card = RectF(pad, pad, width - pad, height - pad)
        val radius = min(card.height() / 2f, 24 * dp)
        canvas.drawRoundRect(card, radius, radius, cardPaint)
        cardStroke.strokeWidth = dp
        canvas.drawRoundRect(card, radius, radius, cardStroke)

        val inner = RectF(card.left + 18 * dp, card.top + 10 * dp, card.right - 18 * dp, card.bottom - 10 * dp)

        // Header row: stream name + percentage.
        val headerSize = (inner.height() * 0.26f).coerceIn(10 * dp, 15 * dp)
        labelPaint.textSize = headerSize
        val headerBaseline = inner.top + headerSize
        canvas.drawText(streamLabel.uppercase(), inner.left, headerBaseline, labelPaint)
        if (showPercent) {
            percentPaint.textSize = headerSize
            percentPaint.color = accent
            canvas.drawText("${(shownFraction * 100).toInt()}%", inner.right, headerBaseline, percentPaint)
        }

        val area = RectF(inner.left, headerBaseline + 6 * dp, inner.right, inner.bottom)
        when (skin) {
            SkinStyle.MINIMAL -> drawMinimal(canvas, area)
            SkinStyle.NEON -> drawNeon(canvas, area)
            SkinStyle.RETRO_VU -> drawRetroVu(canvas, area)
            SkinStyle.WAVE -> drawWave(canvas, area)
            SkinStyle.DOTS -> drawDots(canvas, area)
            SkinStyle.GRADIENT_RING -> drawGradientRing(canvas, area)
            SkinStyle.PULSE -> drawPulse(canvas, area)
            SkinStyle.SPECTRUM_BARS -> drawSpectrumBars(canvas, area)
        }
    }

    private fun drawMinimal(canvas: Canvas, a: RectF) {
        val h = 8 * dp
        val top = a.centerY() - h / 2
        canvas.drawRoundRect(a.left, top, a.right, top + h, h / 2, h / 2, trackPaint)
        barPaint.shader = null
        barPaint.color = accent
        val w = a.width() * shownFraction
        if (w > 0) canvas.drawRoundRect(a.left, top, a.left + w.coerceAtLeast(h), top + h, h / 2, h / 2, barPaint)
    }

    private fun drawNeon(canvas: Canvas, a: RectF) {
        val h = 10 * dp
        val top = a.centerY() - h / 2
        canvas.drawRoundRect(a.left, top, a.right, top + h, h / 2, h / 2, trackPaint)
        val w = a.width() * shownFraction
        if (w <= 0) return
        val fill = RectF(a.left, top, a.left + w.coerceAtLeast(h), top + h)
        val shader = LinearGradient(a.left, 0f, a.right, 0f, intArrayOf(accent, accent2), null, Shader.TileMode.CLAMP)
        glowPaint.shader = shader
        glowPaint.maskFilter = BlurMaskFilter(10 * dp, BlurMaskFilter.Blur.NORMAL)
        canvas.drawRoundRect(fill, h / 2, h / 2, glowPaint)
        barPaint.shader = shader
        canvas.drawRoundRect(fill, h / 2, h / 2, barPaint)
        barPaint.shader = null
    }

    private fun drawRetroVu(canvas: Canvas, a: RectF) {
        val segments = 16
        val gap = 3 * dp
        val segWidth = (a.width() - gap * (segments - 1)) / segments
        val segHeight = min(a.height(), 14 * dp)
        val top = a.centerY() - segHeight / 2
        val litCount = (segments * shownFraction).toInt()
        val peakIndex = (segments * peakHold).toInt().coerceIn(0, segments - 1)
        barPaint.shader = null
        for (i in 0 until segments) {
            val x = a.left + i * (segWidth + gap)
            barPaint.color = when {
                i == peakIndex && peakHold > shownFraction -> Color.WHITE
                i >= litCount -> Color.argb(45, 255, 255, 255)
                i < segments * 0.6 -> Color.rgb(105, 240, 174)
                i < segments * 0.85 -> Color.rgb(255, 215, 64)
                else -> Color.rgb(255, 82, 82)
            }
            canvas.drawRoundRect(x, top, x + segWidth, top + segHeight, 2 * dp, 2 * dp, barPaint)
        }
    }

    private fun drawWave(canvas: Canvas, a: RectF) {
        wavePath.reset()
        val amplitude = (a.height() / 2f - 2 * dp) * shownFraction
        val mid = a.centerY()
        val waveLength = 18 * dp
        var x = a.left
        wavePath.moveTo(x, mid)
        while (x <= a.right) {
            wavePath.lineTo(x, mid + amplitude * sin((x - a.left) / waveLength + phase))
            x += 3 * dp
        }
        strokePaint.shader = LinearGradient(a.left, 0f, a.right, 0f, intArrayOf(accent, accent2), null, Shader.TileMode.CLAMP)
        strokePaint.strokeWidth = 3 * dp
        canvas.drawPath(wavePath, strokePaint)
        strokePaint.shader = null
    }

    private fun drawDots(canvas: Canvas, a: RectF) {
        val count = 20
        val spacing = a.width() / count
        val lit = (count * shownFraction).toInt()
        barPaint.shader = null
        for (i in 0 until count) {
            val on = i < lit
            val wobble = if (on) 1f + 0.18f * sin(phase + i * 0.5f) else 1f
            barPaint.color = if (on) accent else Color.argb(45, 255, 255, 255)
            canvas.drawCircle(a.left + i * spacing + spacing / 2, a.centerY(), 3.5f * dp * wobble, barPaint)
        }
    }

    private fun drawGradientRing(canvas: Canvas, a: RectF) {
        // Ring on the left, a slim bar to its right so the card still reads
        // as a level meter at a glance.
        val r = a.height() / 2f - 2 * dp
        val cx = a.left + r + 2 * dp
        val cy = a.centerY()
        strokePaint.strokeWidth = 4 * dp
        strokePaint.shader = null
        strokePaint.color = Color.argb(50, 255, 255, 255)
        canvas.drawCircle(cx, cy, r, strokePaint)
        strokePaint.shader = SweepGradient(cx, cy, intArrayOf(accent, accent2, accent), null)
        canvas.drawArc(cx - r, cy - r, cx + r, cy + r, -90f, 360f * shownFraction, false, strokePaint)
        strokePaint.shader = null
        drawMinimal(canvas, RectF(cx + r + 14 * dp, a.top, a.right, a.bottom))
    }

    private fun drawPulse(canvas: Canvas, a: RectF) {
        val cx = a.left + a.height() / 2f
        val cy = a.centerY()
        val base = 3 * dp + (a.height() / 4f) * shownFraction
        strokePaint.shader = null
        strokePaint.strokeWidth = 2 * dp
        for (ring in 0 until 3) {
            val rp = (phase + ring * 2f) % 6f
            val alpha = (255 * (1f - rp / 6f)).toInt().coerceIn(0, 255)
            strokePaint.color = Color.argb(alpha, Color.red(accent), Color.green(accent), Color.blue(accent))
            canvas.drawCircle(cx, cy, base + rp * 1.6f * dp, strokePaint)
        }
        barPaint.shader = null
        barPaint.color = accent
        canvas.drawCircle(cx, cy, base * 0.6f, barPaint)
        drawMinimal(canvas, RectF(a.left + a.height() + 12 * dp, a.top, a.right, a.bottom))
    }

    private fun drawSpectrumBars(canvas: Canvas, a: RectF) {
        val bars = 24
        val gap = 2.5f * dp
        val barWidth = (a.width() - gap * (bars - 1)) / bars
        val shader = LinearGradient(0f, a.bottom, 0f, a.top, intArrayOf(accent, accent2), null, Shader.TileMode.CLAMP)
        for (i in 0 until bars) {
            val x = a.left + i * (barWidth + gap)
            val active = i.toFloat() / bars < shownFraction
            val noise = 0.5f + 0.5f * sin(phase * 2f + i * 1.3f)
            val h = if (active) (a.height() * noise).coerceAtLeast(3 * dp) else 3 * dp
            barPaint.shader = if (active) shader else null
            barPaint.color = if (active) Color.WHITE else Color.argb(45, 255, 255, 255)
            canvas.drawRoundRect(x, a.bottom - h, x + barWidth, a.bottom, dp, dp, barPaint)
        }
        barPaint.shader = null
    }
}
