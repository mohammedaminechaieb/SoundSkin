package com.soundskin.app.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import com.soundskin.app.data.PrefsStore
import com.soundskin.app.hud.HudOverlayService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * The only non-root way to beat the system volume dialog to the punch:
 * an AccessibilityService with canRequestFilterKeyEvents=true gets first
 * look at hardware key events. We adjust the stream ourselves with
 * FLAG_SHOW_UI omitted (so the stock dialog never appears) and consume
 * the event, then kick off our own animated HUD instead.
 *
 * Stream targeting: earlier versions of this always adjusted
 * STREAM_MUSIC. That meant pressing the rocker during a call or with
 * music stopped didn't actually control anything you'd expect. This
 * version mirrors what the stock volume rocker actually does — it picks
 * whichever stream is contextually "active" (music playing, an active
 * call, or ringer otherwise) via resolveActiveStream(), same as the
 * system's own default-stream resolution.
 */
class VolumeKeyAccessibilityService : AccessibilityService() {

    private lateinit var audioManager: AudioManager
    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onServiceConnected() {
        super.onServiceConnected()
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.action != KeyEvent.ACTION_DOWN) return false

        val direction = when (event.keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> AudioManager.ADJUST_RAISE
            KeyEvent.KEYCODE_VOLUME_DOWN -> AudioManager.ADJUST_LOWER
            else -> return false
        }

        scope.launch {
            val hudEnabled = PrefsStore(applicationContext).hudEnabled.first()
            if (!hudEnabled) return@launch // let the system dialog behave normally if HUD is off

            val stream = resolveActiveStream()
            audioManager.adjustStreamVolume(stream, direction, 0) // no FLAG_SHOW_UI
            val current = audioManager.getStreamVolume(stream)
            val max = audioManager.getStreamMaxVolume(stream)

            startForegroundHud(current, max)
        }

        return true // consume — stops the stock dialog from appearing
    }

    /**
     * Picks the stream the hardware rocker would sensibly control right
     * now: an active call takes priority, then music/media playback,
     * then ringtone/notification volume as the everyday default. Note:
     * there's no public API to detect "an alarm is currently sounding",
     * so STREAM_ALARM isn't specially targeted here — while an alarm is
     * ringing, the rocker still adjusts ring/media as usual, matching
     * most OEM defaults but not the exact alarm-priority behavior Pixel
     * stock ROMs have. Flag if you want a manual "always control X"
     * override added to Settings instead of the auto-detect.
     */
    private fun resolveActiveStream(): Int {
        return when {
            audioManager.mode == AudioManager.MODE_IN_CALL ||
                audioManager.mode == AudioManager.MODE_IN_COMMUNICATION -> AudioManager.STREAM_VOICE_CALL
            audioManager.isMusicActive -> AudioManager.STREAM_MUSIC
            else -> AudioManager.STREAM_RING
        }
    }

    private fun startForegroundHud(current: Int, max: Int) {
        val intent = Intent(this, HudOverlayService::class.java).apply {
            putExtra(HudOverlayService.EXTRA_LEVEL, current)
            putExtra(HudOverlayService.EXTRA_MAX, max)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent) else startService(intent)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
    override fun onInterrupt() = Unit
}
