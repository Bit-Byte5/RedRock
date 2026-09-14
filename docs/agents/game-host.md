# Minecraft host (agent spec)

Play loads **extracted** Minecraft into the **same 2D Quest window**. Not PackageInstaller. Not a second Quest app. Not stereo. Human-facing docs: [../architecture.md](../architecture.md), [../using.md](../using.md).

```
LauncherActivity  (com.oculus.intent.category.2D, singleTask, …redrock.launcher)
  library UI
  Download ──► Play APKs ──► filesDir/game/com.mojang.minecraftpe/{apks,lib/arm64-v8a}
  Play ──► HostPreflight ──► com.mojang.minecraftpe.MainActivity   same task, default process
              stub libminecraftpe.so ──► dlopen extracted libminecraftpe.so

ImmersiveActivity (com.oculus.intent.category.VR)
  later stereo only
```

Native `SIGABRT` cannot be caught. **Do not start `MainActivity` unless `HostPreflight` passed.** One new abort later = add **one** preflight check. Do not overlay dex `resources.arsc` onto RedRock `Resources` to paper over a missing asset.

## Load order (must stay in this order)

1. Fetch + size check — `GooglePlayCatalog.fetch` writes splits under `cacheDir/play/<pkg>/`. Incomplete downloads fail.
2. Extract + zip repair / remap — `install()` calls `GameStore.extract` (not PackageInstaller). Native libs come from `lib/arm64-v8a/` inside the APKs. Dex APKs stay as files. Skip extract only when `version.txt` matches **and** the payload is host-ready (`libminecraftpe.so`, dex, `install_pack` zip with `assets/bootstrap.json`). Otherwise repair or re-extract. `install_pack.apk` is rebuilt from local ZIP headers if the central directory is missing, and `assets/assets/` is remapped to `assets/`.
3. Library `HostPreflight` — `LauncherActivity.playMinecraft` prepares assets, attaches stubs, then checks payload + zip + a temporary `AssetManager` over **asset packs only** can `open("bootstrap.json")` (cookie != 0) + stub `.so` files. Failure: toast, stay on the library, no `startActivity`.
4. `startActivity` same task, no `NEW_TASK` — `GameStore.MAIN_ACTIVITY`.
5. PairIP stubs + `startPairIp` — `GameRuntime` replaces `libpairipcore`, `libmaesdk`, `libPlayFabMultiplayer`. **Before** `MainActivity` `<clinit>`, `startPairIp` runs `com.pairip.application.Application` against those stubs. Do not call `SignatureCheck.verifyIntegrity`.
6. `bindActivity` asset packs only — `addAssetPath` `install_pack` (and other asset-only APKs) onto the **activity** `AssetManager` so native `AAssetManager_open` sees `bootstrap.json`. Do not `addAssetPath` dex APKs (`base.apk`, `config.*`) onto `Resources`. Do not replace the activity `Resources` — GameActivity inflates AppCompat / games-activity `0x7f` layouts from RedRock. Application `getResources()` stays RedRock.
7. Theme `RedRockGame` — not Minecraft `AppTheme`, not `Theme.Black`.
8. Then GameActivity / `libminecraftpe` — RedRock’s stub `libminecraftpe.so` `dlopen`s the extracted real library using `REDROCK_MINECRAFTPE` / `filesDir/game/…/libminecraftpe.so`.

`MinecraftComponentFactory.instantiateActivity` builds Minecraft’s real `MainActivity` from a `PathClassLoader` over the extracted dex + `libDir`.

## Must not break

- Do not PackageInstaller `com.mojang.minecraftpe` — that is the extra Quest app.
- Do not run the hosted activity in `:minecraft`. Horizon will not keep it in the library window.
- Do not make the launcher `singleInstance` — Minecraft cannot join that task.
- Do not copy the Mac mcpelauncher extract onto Quest.

## Out of scope

- OpenXR / stereo / `ImmersiveActivity` Minecraft
