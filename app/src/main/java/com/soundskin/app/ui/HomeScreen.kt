package com.soundskin.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.soundskin.app.data.*
import com.soundskin.app.hud.VolumeHudView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    prefs: PrefsStore,
    accessibilityEnabled: Boolean,
    onRequestAccessibility: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val settings by prefs.settings.collectAsState(initial = HudSettings())
    fun update(transform: (HudSettings) -> HudSettings) { scope.launch { prefs.update(transform) } }

    // Drives the big preview: "Test" animates it up and down like real key presses.
    var previewLevel by remember { mutableIntStateOf(10) }
    var testing by remember { mutableStateOf(false) }
    LaunchedEffect(testing) {
        if (!testing) return@LaunchedEffect
        for (l in (previewLevel..15) + (15 downTo 3) + (3..10)) {
            previewLevel = l
            delay(90)
        }
        testing = false
    }

    Scaffold(
        topBar = { LargeTopAppBar(title = { Text("SoundSkin") }) }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(160.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = padding.calculateTopPadding(), bottom = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            fullWidth { SetupCard(accessibilityEnabled, settings.enabled, onRequestAccessibility) { on -> update { it.copy(enabled = on) } } }

            fullWidth {
                Card(shape = RoundedCornerShape(24.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xFF3A2D6B), Color(0xFF1B3B5A), Color(0xFF0E1A2B)))),
                            contentAlignment = Alignment.Center
                        ) {
                            HudPreview(settings, previewLevel, Modifier.fillMaxWidth(0.9f).height(92.dp))
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(settings.skin.label, style = MaterialTheme.typography.titleMedium)
                                Text(settings.skin.description, style = MaterialTheme.typography.bodySmall)
                            }
                            FilledTonalButton(onClick = { testing = true }, enabled = !testing) {
                                Icon(Icons.Default.PlayArrow, null)
                                Spacer(Modifier.width(6.dp))
                                Text("Test")
                            }
                        }
                    }
                }
            }

            fullWidth { SectionTitle("Skin") }
            items(SkinStyle.entries, key = { it.name }) { style ->
                val selected = settings.skin == style
                Card(
                    onClick = { update { it.copy(skin = style) } },
                    shape = RoundedCornerShape(18.dp),
                    border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                ) {
                    Column(Modifier.padding(10.dp)) {
                        HudPreview(settings.copy(skin = style, showPercent = false), 7, Modifier.fillMaxWidth().height(64.dp), max = 10)
                        Row(Modifier.padding(start = 4.dp, top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(style.label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                            if (selected) Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            fullWidth { SectionTitle("Color") }
            fullWidth {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    accentColors.forEach { argb ->
                        val selected = settings.accent == argb
                        Box(
                            Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(argb))
                                .border(3.dp, if (selected) MaterialTheme.colorScheme.primary else Color.Transparent, CircleShape)
                                .clickable { update { it.copy(accent = argb) } },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selected) Icon(Icons.Default.Check, null, tint = Color.Black, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            fullWidth { SectionTitle("Behavior") }
            fullWidth {
                Card(shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.padding(vertical = 8.dp)) {
                        OptionRow("Position on screen") {
                            SingleChoiceSegmentedButtonRow {
                                HudPosition.entries.forEachIndexed { i, p ->
                                    SegmentedButton(
                                        selected = settings.position == p,
                                        onClick = { update { it.copy(position = p) } },
                                        shape = SegmentedButtonDefaults.itemShape(i, HudPosition.entries.size)
                                    ) { Text(p.label) }
                                }
                            }
                        }
                        OptionRow("When nothing is playing, control") {
                            SingleChoiceSegmentedButtonRow {
                                IdleStream.entries.forEachIndexed { i, s ->
                                    SegmentedButton(
                                        selected = settings.idleStream == s,
                                        onClick = { update { it.copy(idleStream = s) } },
                                        shape = SegmentedButtonDefaults.itemShape(i, IdleStream.entries.size)
                                    ) { Text(s.label) }
                                }
                            }
                        }
                        OptionRow("Hide after ${"%.1f".format(settings.hideAfterMs / 1000f)} s") {
                            Slider(
                                value = settings.hideAfterMs.toFloat(),
                                valueRange = 800f..4000f,
                                steps = 15,
                                onValueChange = { v -> update { it.copy(hideAfterMs = v.toLong()) } }
                            )
                        }
                        SwitchRow("Show percentage", settings.showPercent) { on -> update { it.copy(showPercent = on) } }
                        SwitchRow("Haptic tick on each step", settings.hapticTick) { on -> update { it.copy(hapticTick = on) } }
                    }
                }
            }

            fullWidth {
                Text(
                    "Calls, media and the ringer are detected automatically — the rocker controls whatever you'd expect, just with a nicer panel.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun SetupCard(accessibilityEnabled: Boolean, hudEnabled: Boolean, onRequestAccessibility: () -> Unit, onToggle: (Boolean) -> Unit) {
    val ok = accessibilityEnabled
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (ok) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(if (ok) Icons.Default.CheckCircle else Icons.Default.Warning, null)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        when {
                            !ok -> "One step to go"
                            hudEnabled -> "SoundSkin is active"
                            else -> "Paused — using the system panel"
                        },
                        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        if (ok) "Press a volume key to see your HUD." else "Turn on SoundSkin in Accessibility settings so it can replace the volume panel.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (ok) Switch(checked = hudEnabled, onCheckedChange = onToggle)
            }
            if (!ok) {
                Spacer(Modifier.height(12.dp))
                Button(onClick = onRequestAccessibility, modifier = Modifier.fillMaxWidth()) { Text("Open Accessibility settings") }
                Spacer(Modifier.height(6.dp))
                Text(
                    "Find \"SoundSkin\" under Installed / Downloaded apps and switch it on. It only listens for the volume keys — it can't read your screen.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

/** A live VolumeHudView inside Compose. */
@Composable
private fun HudPreview(settings: HudSettings, level: Int, modifier: Modifier, max: Int = 15) {
    AndroidView(
        modifier = modifier,
        factory = { ctx -> VolumeHudView(ctx).apply { setLevel(level, max, animate = false) } },
        update = { v ->
            v.skin = settings.skin
            v.accent = settings.accent
            v.showPercent = settings.showPercent
            v.streamLabel = "Media"
            if ((v.level * max).toInt() != level) v.setLevel(level, max)
        }
    )
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 4.dp, top = 8.dp))
}

@Composable
private fun OptionRow(label: String, content: @Composable () -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(6.dp))
        content()
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

private fun androidx.compose.foundation.lazy.grid.LazyGridScope.fullWidth(content: @Composable () -> Unit) {
    item(span = { GridItemSpan(maxLineSpan) }) { content() }
}
