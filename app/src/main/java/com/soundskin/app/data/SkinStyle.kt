package com.soundskin.app.data

enum class SkinStyle(val label: String, val description: String) {
    MINIMAL("Minimal", "Thin rounded bar"),
    NEON("Neon", "Glowing gradient bar"),
    RETRO_VU("Retro VU", "LED meter with peak hold"),
    WAVE("Wave", "Ripple that grows with volume"),
    DOTS("Dots", "Row of pulsing dots"),
    GRADIENT_RING("Ring", "Circular sweep"),
    PULSE("Pulse", "Rings pulsing outward"),
    SPECTRUM_BARS("Spectrum", "Equalizer-style bars")
}

enum class HudPosition(val label: String) { TOP("Top"), CENTER("Middle"), BOTTOM("Bottom") }

/** Which stream the rocker controls when nothing is playing and no call is active. */
enum class IdleStream(val label: String) { RINGER("Ringer"), MEDIA("Media") }

/** Accent colors offered in the app (ARGB). */
val accentColors: List<Int> = listOf(
    0xFFFFFFFF.toInt(),
    0xFF18FFFF.toInt(),
    0xFF7C4DFF.toInt(),
    0xFFFF4081.toInt(),
    0xFFFFAB40.toInt(),
    0xFF69F0AE.toInt(),
    0xFF448AFF.toInt(),
)

/** Snapshot of every HUD setting — read synchronously by the key handler. */
data class HudSettings(
    val enabled: Boolean = true,
    val skin: SkinStyle = SkinStyle.MINIMAL,
    val accent: Int = accentColors[0],
    val position: HudPosition = HudPosition.TOP,
    val showPercent: Boolean = true,
    val hapticTick: Boolean = false,
    val idleStream: IdleStream = IdleStream.MEDIA,
    val hideAfterMs: Long = 1500L,
)
