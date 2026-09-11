package com.soundskin.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.soundskin.app.data.PrefsStore
import com.soundskin.app.data.SkinStyle
import com.soundskin.app.hud.VolumeHudView
import kotlinx.coroutines.launch

private val skinLabels = mapOf(
    SkinStyle.MINIMAL to "Minimal — thin white bar",
    SkinStyle.NEON to "Neon — glowing cyan/magenta gradient",
    SkinStyle.RETRO_VU to "Retro VU — segmented LED meter with peak hold",
    SkinStyle.WAVE to "Wave — sine ripple, amplitude tracks level",
    SkinStyle.DOTS to "Dots — filling row of pulsing dots",
    SkinStyle.GRADIENT_RING to "Gradient Ring — circular fill, cyan→magenta sweep",
    SkinStyle.PULSE to "Pulse — concentric rings pulsing outward",
    SkinStyle.SPECTRUM_BARS to "Spectrum Bars — animated equalizer-style bars"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    prefs: PrefsStore,
    hasOverlayPermission: () -> Boolean,
    hasAccessibilityEnabled: () -> Boolean,
    onRequestOverlayPermission: () -> Unit,
    onRequestAccessibility: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val currentSkin by prefs.skinStyle.collectAsState(initial = SkinStyle.MINIMAL)
    val hudEnabled by prefs.hudEnabled.collectAsState(initial = true)

    Scaffold(topBar = { TopAppBar(title = { Text("SoundSkin") }) }) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(20.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (!hasOverlayPermission()) {
                Text("Step 1 — allow SoundSkin to draw over other apps:")
                Button(onClick = onRequestOverlayPermission) { Text("Grant \"draw over other apps\"") }
            } else {
                Text("✓ Overlay permission granted")
            }

            if (!hasAccessibilityEnabled()) {
                Text("Step 2 — enable the SoundSkin accessibility service (needed to intercept the volume keys before the system dialog shows):")
                Button(onClick = onRequestAccessibility) { Text("Open Accessibility settings") }
            } else {
                Text("✓ Accessibility service enabled")
            }

            Divider()

            Row {
                Text("Custom HUD enabled", modifier = Modifier.weight(1f))
                Switch(checked = hudEnabled, onCheckedChange = { scope.launch { prefs.setHudEnabled(it) } })
            }
            Text(
                "Auto-targets whichever stream is contextually active (call, music, or ringer) — same logic the hardware rocker normally follows.",
                style = MaterialTheme.typography.bodySmall
            )

            Divider()

            Text("Skin", style = MaterialTheme.typography.titleMedium)

            // Live preview — reflects the currently selected skin at ~70% volume.
            AndroidView(
                modifier = Modifier.fillMaxWidth().height(80.dp),
                factory = { ctx -> VolumeHudView(ctx).apply { skin = currentSkin; setLevel(7, 10) } },
                update = { view -> view.skin = currentSkin; view.setLevel(7, 10) }
            )

            skinLabels.forEach { (style, label) ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    RadioButton(
                        selected = currentSkin == style,
                        onClick = { scope.launch { prefs.setSkinStyle(style) } }
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(label, modifier = Modifier.align(androidx.compose.ui.Alignment.CenterVertically))
                }
            }

            Text(
                "Press a volume key on the device to see it live once both steps above are done.",
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(Modifier.height(8.dp))
        }
    }
}
