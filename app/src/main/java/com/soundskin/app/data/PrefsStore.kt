package com.soundskin.app.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "soundskin_prefs")

class PrefsStore(private val context: Context) {

    private object Keys {
        val SKIN = stringPreferencesKey("skin_style")
        val HUD_ENABLED = stringPreferencesKey("hud_enabled") // string for compatibility with v0.1 saves
        val ACCENT = intPreferencesKey("accent")
        val POSITION = stringPreferencesKey("position")
        val SHOW_PERCENT = booleanPreferencesKey("show_percent")
        val HAPTIC = booleanPreferencesKey("haptic_tick")
        val IDLE_STREAM = stringPreferencesKey("idle_stream")
        val HIDE_AFTER = longPreferencesKey("hide_after_ms")
    }

    val settings: Flow<HudSettings> = context.dataStore.data.map { p -> p.toSettings() }

    private fun Preferences.toSettings(): HudSettings {
        val d = HudSettings()
        return HudSettings(
            enabled = this[Keys.HUD_ENABLED] != "false",
            skin = enumOr(this[Keys.SKIN], d.skin),
            accent = this[Keys.ACCENT] ?: d.accent,
            position = enumOr(this[Keys.POSITION], d.position),
            showPercent = this[Keys.SHOW_PERCENT] ?: d.showPercent,
            hapticTick = this[Keys.HAPTIC] ?: d.hapticTick,
            idleStream = enumOr(this[Keys.IDLE_STREAM], d.idleStream),
            hideAfterMs = this[Keys.HIDE_AFTER] ?: d.hideAfterMs,
        )
    }

    private inline fun <reified T : Enum<T>> enumOr(name: String?, default: T): T =
        name?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: default

    suspend fun update(transform: (HudSettings) -> HudSettings) {
        context.dataStore.edit { p ->
            val s = transform(p.toSettings())
            p[Keys.HUD_ENABLED] = if (s.enabled) "true" else "false"
            p[Keys.SKIN] = s.skin.name
            p[Keys.ACCENT] = s.accent
            p[Keys.POSITION] = s.position.name
            p[Keys.SHOW_PERCENT] = s.showPercent
            p[Keys.HAPTIC] = s.hapticTick
            p[Keys.IDLE_STREAM] = s.idleStream.name
            p[Keys.HIDE_AFTER] = s.hideAfterMs
        }
    }
}
