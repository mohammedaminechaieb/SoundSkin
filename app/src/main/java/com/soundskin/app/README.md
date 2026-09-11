# SoundSkin — READY FOR FINAL USE

Animated custom volume HUD overlay, replacing the stock system volume dialog. Native Android, Kotlin.

## What changed since the last drop
- **8 skins now, not 3:** added Wave, Dots, Gradient Ring, Pulse, and Spectrum Bars
  alongside Minimal, Neon, and Retro VU. `data/SkinStyle.kt` and `hud/VolumeHudView.kt`
  both fully rewritten — copy both over, don't patch.
- **Multi-stream targeting:** previously the rocker always adjusted `STREAM_MUSIC`,
  even during a call or with nothing playing. `accessibility/VolumeKeyAccessibilityService.kt`
  now auto-detects which stream is contextually active (active call → voice call
  volume, music playing → media volume, otherwise → ringer/notification volume) —
  see the honesty note below on the one case this still doesn't cover.
- `ui/HomeScreen.kt` updated: skin list is now scrollable (8 options no longer fit
  one screen) and there's a one-line explanation of the auto-targeting behavior.

## Important — how this actually works, and its real limits (read before you start)
There's no public API for a third-party app to suppress the system volume dialog,
or to literally replace Android's system sound engine. The technique here — an
**AccessibilityService with `canRequestFilterKeyEvents="true"`** intercepting the
hardware volume keys before the system dialog can show, then calling
`AudioManager.adjustStreamVolume(..., flags = 0)` ourselves — is the same one every
non-root "custom volume panel" app on the Play Store uses. "Override the default
sound" in practice means: our HUD is the only thing you see when you press the
rocker, across whichever stream is actually relevant at the time. That's now true
for call / media / ringer.

**One real gap, honestly flagged:** there's no public API to detect "an alarm is
currently sounding," so `STREAM_ALARM` isn't specially targeted — while an alarm is
ringing, the rocker still adjusts media/ringer as usual on this build, rather than
alarm volume like stock Pixel does. If that matters to you, the fix is a manual
"always control this stream" override in Settings rather than more auto-detection
(there's no reliable way to auto-detect it), and I can build that toggle if you want
it — it's a small addition.

This also still means the user has to explicitly enable an Accessibility Service —
Android shows a real permission-style warning for that ("this app can observe your
actions..."), because the API is powerful and Android wants users to know it's on.
Expected and unavoidable for this feature.

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
        │   ├── data/SkinStyle.kt                        ← REWRITTEN (8 skins)
        │   ├── accessibility/VolumeKeyAccessibilityService.kt  ← REWRITTEN (multi-stream)
        │   ├── data/PrefsStore.kt
        │   ├── hud/
        │   │   ├── HudOverlayService.kt
        │   │   └── VolumeHudView.kt                     ← REWRITTEN (8 skins)
        │   └── ui/HomeScreen.kt                          ← MODIFIED (scroll + new labels)
        └── res/
            ├── values/ (strings.xml, themes.xml)
            └── xml/volume_accessibility_config.xml
```

## What YOU need to do
1. **Copy in the 4 changed files** — `SkinStyle.kt`, `VolumeKeyAccessibilityService.kt`,
   `VolumeHudView.kt`, `HomeScreen.kt` — overwriting your existing copies at the same paths.
2. **Add the launcher icon**: right-click `app/src/main/res` → **New → Image Asset** →
   "Launcher Icons (Adaptive and Legacy)" → pick any art → Finish.
3. On-device: grant "draw over other apps" AND enable the Accessibility Service —
   both have a button on the Home screen that deep-links to the right settings page.
4. Rebuild.

## How the pieces fit together
- **VolumeKeyAccessibilityService** — `onKeyEvent` fires first on `ACTION_DOWN`,
  calls `resolveActiveStream()` to pick call/media/ringer, adjusts that real stream,
  then starts `HudOverlayService` with the new level and returns `true` to swallow
  the event so the system dialog never appears.
- **HudOverlayService** — short-lived foreground service, 1.5s auto-hide timer reset
  on every key press so holding the rocker keeps the HUD visible continuously.
- **VolumeHudView** — one Canvas view, 8 `onDraw` paths keyed off `SkinStyle`. Three
  (Wave, Pulse, Spectrum Bars) animate continuously via a lightweight ~30fps Handler
  tick, not just on level change — that's `phase` incrementing in the background,
  only running while the view is attached (auto-hide removes the view, so it's not
  ticking in the background when the HUD isn't showing).
- **HomeScreen** — live `AndroidView`-embedded preview at a fixed 70% level so
  switching skins shows an instant preview without pressing a real key.

## Remaining known limitations
- `STREAM_ALARM` isn't auto-targeted (see honesty note above) — flag if you want the
  manual override toggle built.
- No haptic tick per segment on Retro VU — could reuse `VibrationEffect` the same way
  HaptiKit does, worth connecting the two projects eventually if you want it.
- Spectrum Bars is cosmetic/randomized motion, not real FFT audio analysis — there's
  no API access to what's actually playing through another app's audio stream, so
  this is animated "activity," not a literal frequency readout. Said plainly so it's
  not mistaken for something it isn't.

## Status: done (matches your ask — many skins, broad stream coverage)
