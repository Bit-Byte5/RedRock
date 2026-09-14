# Headset

RedRock is a **sideloaded** hybrid app (`com.saltmarshdigital.redrock`). Horizon does not put it next to Store titles.

Supported devices (manifest): Quest 2, Quest Pro, Quest 3, Quest 3S.

## Developer Mode

Use the Meta Horizon mobile app (or Meta Quest Developer Hub):

1. Enable Developer Mode for the headset.
2. Reboot the headset if **Unknown Sources** is missing.

USB debugging and wireless debugging live under Quest **Settings → System → Developer**.

## Finding RedRock after sideload

1. Press the Meta button.
2. Open **Apps** / **Library**.
3. Left side — folder with a question mark — **Unknown Sources**.
4. **RedRock**.

If that folder is empty: Developer Mode off and on in the phone app, reboot, sideload again (`./redrock load`).

## Two activities, one library window

Horizon Home launches `LauncherActivity` as a **2D** panel (`com.oculus.intent.category.2D`). That window is the library (Game, Mods, Docs, Log, Settings). **Play** hosts Minecraft **in that same task**. It does not drop you into a stereo VR scene.

`ImmersiveActivity` (`com.oculus.intent.category.VR`) exists for a later stereo pass. Do not start it from Play yet. Do not `finish()` the 2D window to “enter” the game — Horizon drops a task when its last activity finishes.

Default panel size is `1024dp` × `640dp` (min `720` × `480`) in `AndroidManifest.xml`.

## Controls in the library

Laser pointer + trigger to click. The Quest keyboard appears for Google sign-in fields.

## Controls once Minecraft is hosted

Point + trigger, or **A** to confirm. Stick up/down moves the highlight. **B** leaves back toward the library chrome.

Look and Volume sliders in Settings are local to RedRock’s window. They are not written into Minecraft yet.
