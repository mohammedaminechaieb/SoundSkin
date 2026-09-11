package com.soundskin.app.hud

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.soundskin.app.data.PrefsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Shows the animated volume HUD for ~1.5s then removes itself. Each call
 * to onStartCommand (one per volume key press, see
 * VolumeKeyAccessibilityService) resets that timer and updates the level,
 * so holding the key down keeps the HUD visible continuously.
 */
class HudOverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var hudView: VolumeHudView? = null
    private val hideHandler = Handler(Looper.getMainLooper())
    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.Main + job)

    private val hideRunnable = Runnable { stopSelf() }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        startForeground(NOTIF_ID, buildNotification())
        addHudView()
        scope.launch {
            PrefsStore(applicationContext).skinStyle.collect { style ->
                hudView?.skin = style
                hudView?.invalidate()
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val level = intent?.getIntExtra(EXTRA_LEVEL, 0) ?: 0
        val max = intent?.getIntExtra(EXTRA_MAX, 1) ?: 1
        hudView?.setLevel(level, max)

        hideHandler.removeCallbacks(hideRunnable)
        hideHandler.postDelayed(hideRunnable, AUTO_HIDE_MS)
        return START_NOT_STICKY
    }

    private fun addHudView() {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            140,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.CENTER; y = -300 } // sits a bit above center, phone-native feel

        hudView = VolumeHudView(this)
        windowManager.addView(hudView, params)
    }

    private fun buildNotification(): android.app.Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "SoundSkin HUD", NotificationManager.IMPORTANCE_MIN)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SoundSkin")
            .setContentText("Volume HUD active")
            .setSmallIcon(android.R.drawable.ic_lock_silent_mode_off)
            .setOngoing(false)
            .build()
    }

    override fun onDestroy() {
        hideHandler.removeCallbacks(hideRunnable)
        hudView?.let { runCatching { windowManager.removeView(it) } }
        job.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val EXTRA_LEVEL = "extra_level"
        const val EXTRA_MAX = "extra_max"
        private const val CHANNEL_ID = "soundskin_hud"
        private const val NOTIF_ID = 77
        private const val AUTO_HIDE_MS = 1500L
    }
}
