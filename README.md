# SoundSkin — Project 4 / 6

Animated custom volume HUD overlay, replacing the stock system volume dialog. Native Android, Kotlin.

## Important — how this actually works (read before you start)
There's no public API for a third-party app to suppress the system volume dialog.
The one non-root technique that actually works — and what every "custom volume
panel" app on the Play Store uses — is an **AccessibilityService with
`canRequestFilterKeyEvents="true"`**. That flag lets the service see hardware key
events (like the volume rocker) before the rest of the system does. SoundSkin's
service intercepts `KEYCODE_VOLUME_UP`/`KEYCODE_VOLUME_DOWN`, adjusts the stream
itself via `AudioManager.adjustStreamVolume(..., flags = 0)` (deliberately omitting
`FLAG_SHOW_UI`, which is what normally triggers the stock dialog), consumes the key
event so the system never sees it, and shows our own animated HUD instead.

This means the user has to explicitly enable an Accessibility Service — Android
shows a real permission-style warning dialog for that ("this app can observe your
actions..."), because the API is powerful and Android wants users to know it's
turned on. That's expected and unavoidable for this feature; the README below and
the in-app string explain to the user exactly why it's needed and that it's limited
to key events, not screen content.

## What's included here (the actual code)
```
SoundSkin/
├── build.gradle.kts, settings.gradle.kts, gradle.properties
└── app/
    ├── build.gradle.kts
    └── src/main/
        ├── AndroidManifest.xml
        ├── java/com/soundskin/app/
        │   ├── MainActivity.kt
        │   ├── data/ (SkinStyle enum, DataStore PrefsStore)
        │   ├── accessibility/VolumeKeyAccessibilityService.kt
        │   ├── hud/
        │   │   ├── HudOverlayService.kt     (owns the overlay window, auto-hide timer)
        │   │   └── VolumeHudView.kt         (Canvas + ValueAnimator — all 3 skins)
        │   └── ui/HomeScreen.kt             (permission gates + live skin preview)
        └── res/
            ├── values/ (strings.xml, themes.xml)
            └── xml/volume_accessibility_config.xml
```

## What YOU need to add locally
1. Open in Android Studio — generates `gradle/wrapper/`, `local.properties`, pulls
   Compose/DataStore dependencies, same as the previous two projects.
2. Launcher icon via Image Asset — cosmetic, skipped here.
3. On-device: grant "draw over other apps" AND enable the Accessibility Service —
   both have a button on the Home screen that deep-links to the right settings page.

## How the pieces fit together
- **VolumeKeyAccessibilityService** — the interception point. `onKeyEvent` fires
  first on `ACTION_DOWN`, adjusts the real stream, then starts `HudOverlayService`
  with the new level and returns `true` to swallow the event.
- **HudOverlayService** — a short-lived foreground service. Each key press resets a
  1.5s auto-hide timer, so holding the rocker down keeps the HUD visible
  continuously rather than flickering.
- **VolumeHudView** — one Canvas view, three `onDraw` paths keyed off `SkinStyle`:
  - **Minimal**: a single rounded bar, animated width via `ValueAnimator`.
  - **Neon**: same bar, drawn twice — a `BlurMaskFilter`-blurred glow pass underneath
    a crisp gradient pass on top, for the glow effect.
  - **Retro VU**: 16 discrete LED-style segments (green → yellow → red as they fill),
    plus a white "peak hold" marker that lags behind and decays slowly, like a real
    hardware VU meter.
- **HomeScreen** embeds a live `VolumeHudView` via `AndroidView` so switching skins
  shows an instant preview at a fixed 70% level, without needing to press a real key.

## Known v0.1 limitations (matches the roadmap's scope)
- Only `STREAM_MUSIC` is intercepted for now — ringtone/alarm/notification streams
  still show the stock dialog. Extending to those streams is straightforward
  (same `adjustStreamVolume` call with a different stream constant) whenever you
  want it, just say the word.
- If the "Custom HUD enabled" switch is off, volume keys fall through to default
  system behavior — useful as a quick kill switch without disabling the
  Accessibility Service entirely.
- No haptic tick per segment yet on the Retro VU skin — a nice, cheap addition later
  (could reuse a lot of what HaptiKit already does with `VibrationEffect`, worth
  connecting the two eventually).

## Next when you're ready
Tell me when this one's running and I'll move on to **LockForge**.
