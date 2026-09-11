package com.soundskin.app.hud

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.DecelerateInterpolator
import com.soundskin.app.data.SkinStyle
import kotlin.math.sin
import kotlin.random.Random

class VolumeHudView(context: Context) : View(context) {

    var skin: SkinStyle = SkinStyle.MINIMAL
    private var targetFraction: Float = 0f
    private var animatedFraction: Float = 0f
    private var peakHold: Float = 0f
    private var peakHoldFrames = 0

    // Continuous animation clock for skins that move even while the level
    // holds steady (WAVE, PULSE, SPECTRUM_BARS). Other skins ignore this.
    private var phase = 0f
    private val handler = Handler(Looper.getMainLooper())
    private val continuousTick = object : Runnable {
        override fun run() {
            phase += 0.15f
            invalidate()
            handler.postDelayed(this, 32) // ~30fps, plenty smooth for a HUD
        }
    }

    private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 220
        interpolator = DecelerateInterpolator()
        addUpdateListener {
            val t = it.animatedValue as Float
            this@VolumeHudView.animatedFraction += (targetFraction - this@VolumeHudView.animatedFraction) * t
            invalidate()
        }
    }

    fun setLevel(current: Int, max: Int) {
        targetFraction = if (max > 0) current.toFloat() / max else 0f
        if (targetFraction > peakHold) {
            peakHold = targetFraction
            peakHoldFrames = 30 // ~0.5s hold before it starts decaying
        }
        animator.cancel()
        animator.start()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        handler.post(continuousTick)
    }

    override fun onDetachedFromWindow() {
        handler.removeCallbacks(continuousTick)
        super.onDetachedFromWindow()
    }

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(60, 255, 255, 255) }
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        when (skin) {
            SkinStyle.MINIMAL -> drawMinimal(canvas)
            SkinStyle.NEON -> drawNeon(canvas)
            SkinStyle.RETRO_VU -> drawRetroVu(canvas)
            SkinStyle.WAVE -> drawWave(canvas)
            SkinStyle.DOTS -> drawDots(canvas)
            SkinStyle.GRADIENT_RING -> drawGradientRing(canvas)
            SkinStyle.PULSE -> drawPulse(canvas)
            SkinStyle.SPECTRUM_BARS -> drawSpectrumBars(canvas)
        }

        if (peakHoldFrames > 0) {
            peakHoldFrames--
        } else if (peakHold > animatedFraction) {
            peakHold -= 0.01f
        }
    }

    private fun drawMinimal(canvas: Canvas) {
        val barHeight = 10f
        val top = height / 2f - barHeight / 2f
        val cornerRadius = barHeight / 2f

        val track = RectF(40f, top, width - 40f, top + barHeight)
        canvas.drawRoundRect(track, cornerRadius, cornerRadius, trackPaint)

        barPaint.shader = null
        barPaint.color = Color.WHITE
        val fill = RectF(40f, top, 40f + (width - 80f) * animatedFraction, top + barHeight)
        canvas.drawRoundRect(fill, cornerRadius, cornerRadius, barPaint)
    }

    private fun drawNeon(canvas: Canvas) {
        val barHeight = 16f
        val top = height / 2f - barHeight / 2f
        val cornerRadius = barHeight / 2f
        val track = RectF(40f, top, width - 40f, top + barHeight)
        canvas.drawRoundRect(track, cornerRadius, cornerRadius, trackPaint)

        val fillWidth = (width - 80f) * animatedFraction
        val fill = RectF(40f, top, 40f + fillWidth, top + barHeight)

        glowPaint.shader = LinearGradient(
            40f, 0f, 40f + fillWidth, 0f,
            intArrayOf(Color.CYAN, Color.MAGENTA), null, Shader.TileMode.CLAMP
        )
        glowPaint.maskFilter = BlurMaskFilter(24f, BlurMaskFilter.Blur.NORMAL)
        canvas.drawRoundRect(fill, cornerRadius, cornerRadius, glowPaint)

        barPaint.maskFilter = null
        barPaint.shader = LinearGradient(
            40f, 0f, 40f + fillWidth, 0f,
            intArrayOf(Color.CYAN, Color.MAGENTA), null, Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(fill, cornerRadius, cornerRadius, barPaint)
    }

    private fun drawRetroVu(canvas: Canvas) {
        val segments = 16
        val gap = 6f
        val totalWidth = width - 80f
        val segWidth = (totalWidth - gap * (segments - 1)) / segments
        val segHeight = 28f
        val top = height / 2f - segHeight / 2f
        val litCount = (segments * animatedFraction).toInt()
        val peakIndex = (segments * peakHold).toInt().coerceIn(0, segments - 1)

        barPaint.shader = null
        barPaint.maskFilter = null

        for (i in 0 until segments) {
            val x = 40f + i * (segWidth + gap)
            val lit = i < litCount
            barPaint.color = when {
                i == peakIndex && peakHold > animatedFraction -> Color.WHITE
                !lit -> Color.argb(50, 255, 255, 255)
                i < segments * 0.6 -> Color.GREEN
                i < segments * 0.85 -> Color.YELLOW
                else -> Color.RED
            }
            canvas.drawRect(x, top, x + segWidth, top + segHeight, barPaint)
        }
    }

    /** A sine wave whose amplitude scales with the level — flat line at 0, full ripple at max. */
    private fun drawWave(canvas: Canvas) {
        val path = Path()
        val amplitude = 20f * animatedFraction
        val midY = height / 2f
        val left = 40f
        val right = width - 40f
        val waveLength = 60f

        path.moveTo(left, midY)
        var x = left
        while (x <= right) {
            val y = midY + amplitude * sin((x - left) / waveLength + phase)
            path.lineTo(x, y.toFloat())
            x += 4f
        }

        strokePaint.shader = null
        strokePaint.color = Color.WHITE
        strokePaint.strokeWidth = 5f
        canvas.drawPath(path, strokePaint)
    }

    /** A row of dots that fill in left-to-right, each pulsing slightly via phase. */
    private fun drawDots(canvas: Canvas) {
        val dotCount = 24
        val spacing = (width - 80f) / dotCount
        val litCount = (dotCount * animatedFraction).toInt()

        barPaint.shader = null
        for (i in 0 until dotCount) {
            val cx = 40f + i * spacing + spacing / 2f
            val lit = i < litCount
            val wobble = if (lit) 1f + 0.15f * sin(phase + i * 0.5f) else 1f
            barPaint.color = if (lit) Color.WHITE else Color.argb(50, 255, 255, 255)
            canvas.drawCircle(cx, height / 2f, 6f * wobble, barPaint)
        }
    }

    /** A circular ring that fills clockwise from the top — a round alternative to the linear bar. */
    private fun drawGradientRing(canvas: Canvas) {
        val cx = width / 2f
        val cy = height / 2f
        val radius = (height / 2f - 12f).coerceAtMost(40f)

        strokePaint.shader = null
        strokePaint.color = Color.argb(60, 255, 255, 255)
        strokePaint.strokeWidth = 8f
        canvas.drawCircle(cx, cy, radius, strokePaint)

        strokePaint.shader = SweepGradient(cx, cy, intArrayOf(Color.CYAN, Color.MAGENTA, Color.CYAN), null)
        val sweep = 360f * animatedFraction
        canvas.drawArc(cx - radius, cy - radius, cx + radius, cy + radius, -90f, sweep, false, strokePaint)
    }

    /** Concentric circles that pulse outward continuously, sized by the level. */
    private fun drawPulse(canvas: Canvas) {
        val cx = width / 2f
        val cy = height / 2f
        val baseRadius = 10f + 30f * animatedFraction

        strokePaint.shader = null
        strokePaint.strokeWidth = 3f
        for (ring in 0 until 3) {
            val ringPhase = (phase + ring * 2f) % 6f
            val radius = baseRadius + ringPhase * 8f
            val alpha = (255 * (1f - ringPhase / 6f)).toInt().coerceIn(0, 255)
            strokePaint.color = Color.argb(alpha, 255, 255, 255)
            canvas.drawCircle(cx, cy, radius, strokePaint)
        }
        barPaint.shader = null
        barPaint.color = Color.WHITE
        canvas.drawCircle(cx, cy, baseRadius * 0.4f, barPaint)
    }

    /** Fake spectrum-analyzer bars — not real audio FFT data (no access to what's actually
     *  playing), randomized heights scaled by the level so it reads as "activity" rather
     *  than a literal frequency readout. Cosmetic, same spirit as a classic media-player skin. */
    private val spectrumSeed = Random(0)
    private fun drawSpectrumBars(canvas: Canvas) {
        val bars = 20
        val gap = 4f
        val totalWidth = width - 80f
        val barWidth = (totalWidth - gap * (bars - 1)) / bars
        val maxBarHeight = 36f

        barPaint.shader = null
        barPaint.color = Color.WHITE
        for (i in 0 until bars) {
            val x = 40f + i * (barWidth + gap)
            val noise = 0.5f + 0.5f * sin(phase * 2f + i * 1.3f)
            val barHeight = maxBarHeight * animatedFraction * noise.coerceIn(0.15f, 1f)
            canvas.drawRect(x, height / 2f - barHeight / 2f, x + barWidth, height / 2f + barHeight / 2f, barPaint)
        }
    }
}
