package com.soundskin.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "soundskin_prefs")

class PrefsStore(private val context: Context) {

    private object Keys {
        val SKIN = stringPreferencesKey("skin_style")
        val HUD_ENABLED = stringPreferencesKey("hud_enabled")
    }

    val skinStyle: Flow<SkinStyle> = context.dataStore.data.map { prefs ->
        prefs[Keys.SKIN]?.let { runCatching { SkinStyle.valueOf(it) }.getOrNull() } ?: SkinStyle.MINIMAL
    }

    val hudEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.HUD_ENABLED] != "false" // default true
    }

    suspend fun setSkinStyle(style: SkinStyle) {
        context.dataStore.edit { it[Keys.SKIN] = style.name }
    }

    suspend fun setHudEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.HUD_ENABLED] = if (enabled) "true" else "false" }
    }
}
