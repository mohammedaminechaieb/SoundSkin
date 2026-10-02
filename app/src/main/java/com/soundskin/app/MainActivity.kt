package com.soundskin.app

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.soundskin.app.data.PrefsStore
import com.soundskin.app.ui.HomeScreen

class MainActivity : ComponentActivity() {

    private lateinit var prefs: PrefsStore

    /** Re-checked in onResume so the status updates the moment the user
     *  comes back from Accessibility settings. */
    private var accessibilityEnabled by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        prefs = PrefsStore(applicationContext)

        setContent {
            val dark = isSystemInDarkTheme()
            MaterialTheme(
                colorScheme = if (dark) {
                    darkColorScheme(primary = Color(0xFF8AB4FF), secondary = Color(0xFF7C4DFF), background = Color(0xFF0F1014), surface = Color(0xFF0F1014))
                } else {
                    lightColorScheme(primary = Color(0xFF3557D6), secondary = Color(0xFF6A3DE8))
                }
            ) {
                HomeScreen(
                    prefs = prefs,
                    accessibilityEnabled = accessibilityEnabled,
                    onRequestAccessibility = { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        accessibilityEnabled = isAccessibilityServiceEnabled()
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val expected = "$packageName/.accessibility.VolumeKeyAccessibilityService"
        val expectedFull = "$packageName/$packageName.accessibility.VolumeKeyAccessibilityService"
        val enabledServices = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
        val splitter = TextUtils.SimpleStringSplitter(':')
        splitter.setString(enabledServices)
        while (splitter.hasNext()) {
            val s = splitter.next()
            if (s.equals(expected, ignoreCase = true) || s.equals(expectedFull, ignoreCase = true)) return true
        }
        return false
    }
}
