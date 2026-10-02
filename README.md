# SoundSkin

Replaces Android's volume panel with an animated HUD in one of eight skins: Minimal, Neon, Retro VU, Wave, Dots, Ring, Pulse and Spectrum. Kotlin + Jetpack Compose.

## Build
Open the folder in Android Studio and run, or from a terminal:
```
gradlew assembleDebug
```

## Set up on the phone (once)
Open SoundSkin and tap **Open Accessibility settings**. Find SoundSkin (under *Installed* / *Downloaded apps*) and switch it on. That's the only permission needed.

## Using it
- Press a volume key and your HUD appears instead of the system panel.
- **Test** animates the preview so you can try skins without pressing keys.
- Pick a **skin** (each card is a live preview) and an accent **color**.
- **Behavior**: HUD position (top / middle / bottom), which stream the keys control when nothing is playing (media or ringer), how long the HUD stays up, percentage on/off, and a haptic tick per step.
- The switch on the status card pauses SoundSkin, and the stock panel comes back instantly without touching Accessibility settings.

Calls, media and the ringer are detected automatically: during a call the keys change call volume, while music plays they change media volume, and otherwise they change whatever you chose under *Behavior*.

## How it works
Android has no API for replacing the volume panel, so SoundSkin uses an accessibility service that can see hardware key events first (the same approach every custom volume-panel app uses). It changes the volume itself without asking the system to show its UI, then draws the HUD as an accessibility overlay. That needs no "draw over other apps" permission and no persistent notification. The service only listens for the two volume keys and can't read screen content.
