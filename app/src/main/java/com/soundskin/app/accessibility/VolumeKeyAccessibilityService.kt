package com.soundskin.app.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import com.soundskin.app.data.HudSettings
import com.soundskin.app.data.IdleStream
import com.soundskin.app.data.PrefsStore
import com.soundskin.app.hud.HudController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * The only non-root way to beat the system volume dialog to the punch:
 * an AccessibilityService with canRequestFilterKeyEvents=true gets first
 * look at hardware key events. We adjust the stream ourselves without
 * FLAG_SHOW_UI (so the stock dialog never appears), consume the event,
 * and show our own animated HUD instead.
 *
 * onKeyEvent must decide synchronously whether to consume a key, so the
 * settings are mirrored into [settings] by a collector rather than read
 * on demand — reading them asynchronously (the old approach) meant keys
 * were consumed even with the HUD switched off, killing the volume rocker.
 */
class VolumeKeyAccessibilityService : AccessibilityService() {

    private lateinit var audioManager: AudioManager
    private lateinit var hud: HudController
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    @Volatile private var settings = HudSettings()

    override fun onServiceConnected() {
        super.onServiceConnected()
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        hud = HudController(this)
        scope.launch { PrefsStore(applicationContext).settings.collect { settings = it } }
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        val direction = when (event.keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> AudioManager.ADJUST_RAISE
            KeyEvent.KEYCODE_VOLUME_DOWN -> AudioManager.ADJUST_LOWER
            else -> return false
        }
        val s = settings
        if (!s.enabled) return false // fall through to the stock volume dialog

        // Consume both DOWN and UP so the system never sees half a key press.
        if (event.action != KeyEvent.ACTION_DOWN) return true

        val stream = resolveActiveStream(s)
        val before = audioManager.getStreamVolume(stream)
        runCatching { audioManager.adjustStreamVolume(stream, direction, 0) } // no FLAG_SHOW_UI
        val current = audioManager.getStreamVolume(stream)
        val max = audioManager.getStreamMaxVolume(stream)

        if (s.hapticTick && current != before) tick()
        hud.show(current, max, streamLabel(stream), s)
        return true
    }

    /**
     * Picks the stream the rocker should control right now: an active call
     * first, then whatever is playing, then the user's chosen idle stream.
     */
    private fun resolveActiveStream(s: HudSettings): Int = when {
        audioManager.mode == AudioManager.MODE_IN_CALL ||
            audioManager.mode == AudioManager.MODE_IN_COMMUNICATION -> AudioManager.STREAM_VOICE_CALL
        audioManager.isMusicActive -> AudioManager.STREAM_MUSIC
        s.idleStream == IdleStream.RINGER -> AudioManager.STREAM_RING
        else -> AudioManager.STREAM_MUSIC
    }

    private fun streamLabel(stream: Int) = when (stream) {
        AudioManager.STREAM_VOICE_CALL -> "Call"
        AudioManager.STREAM_RING -> "Ringer"
        else -> "Media"
    }

    private fun tick() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION") getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
        } else {
            vibrator.vibrate(VibrationEffect.createOneShot(12, 120))
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
    override fun onInterrupt() = Unit

    override fun onDestroy() {
        if (::hud.isInitialized) hud.release()
        scope.cancel()
        super.onDestroy()
    }
}
