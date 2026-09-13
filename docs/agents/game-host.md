# Minecraft host (agent spec)

Play loads **extracted** Minecraft into the **same 2D Quest window**. Not PackageInstaller. Not a second Quest app. Not stereo.

```
LauncherActivity  (com.oculus.intent.category.2D, singleTask, …redrock.launcher)
  library UI
  Download ──► Play APKs ──► filesDir/game/com.mojang.minecraftpe/{apks,lib/arm64-v8a}
  Play ──► com.mojang.minecraftpe.MainActivity   same task, default process
              stub libminecraftpe.so ──► dlopen extracted libminecraftpe.so

ImmersiveActivity (com.oculus.intent.category.VR)
  later stereo only
```

## Load order (must stay in this order)

1. `GooglePlayCatalog.fetch` writes splits under `cacheDir/play/<pkg>/`.
2. `install()` calls `GameStore.extract` (not PackageInstaller). Native libs come from `lib/arm64-v8a/` inside the APKs. Dex APKs stay as files. If `version.txt` already matches, extract is skipped; a new version replaces the payload.
3. `LauncherActivity.playMinecraft` requires `GameStore.payload()` (or adopts the Play cache). Then it starts `GameStore.MAIN_ACTIVITY` **without** `NEW_TASK`.
4. `MinecraftComponentFactory.instantiateActivity` builds Minecraft’s real `MainActivity` from a `PathClassLoader` over the extracted dex + `libDir`. That activity is GameActivity / AppCompat — theme is `RedRockGame`, not `Theme.Black`.
5. `GameRuntime` replaces Play Integrity–protected `.so` files (`libpairipcore`, `libmaesdk`, `libPlayFabMultiplayer`) with RedRock no-op stubs. The real copies SIGSEGV inside a host package.
6. **Before** `MainActivity` `<clinit>`, `GameRuntime.startPairIp` runs `com.pairip.application.Application` against those stubs so `VMRunner.executeVM` does not jump into the real PairIP VM.
7. NativeActivity/GameActivity loads **RedRock’s** stub `libminecraftpe.so`, which `dlopen`s the extracted real library using `REDROCK_MINECRAFTPE` / `filesDir/game/…/libminecraftpe.so`.

## Must not break

- Do not PackageInstaller `com.mojang.minecraftpe` — that is the extra Quest app.
- Do not run the hosted activity in `:minecraft`. Horizon will not keep it in the library window.
- Do not make the launcher `singleInstance` — Minecraft cannot join that task.
- Do not copy the Mac mcpelauncher extract onto Quest.

## Out of scope

- OpenXR / stereo / `ImmersiveActivity` Minecraft
