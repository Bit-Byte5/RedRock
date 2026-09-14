# Architecture

Two processes of thought: the **2D library** you live in, and a **hosted** Minecraft `MainActivity` that must stay in that same Horizon task.

```
Quest Home
  └─ LauncherActivity     2D panel (library + fetch + settings)
        Download ──► Play APKs ──► filesDir/game/com.mojang.minecraftpe/
        Play ──► HostPreflight ──► com.mojang.minecraftpe.MainActivity
                      stub libminecraftpe.so ──► dlopen extracted libminecraftpe.so

ImmersiveActivity         VR category, later stereo only — do not merge with launcher
```

## Why two activities

Horizon removes a task when its last activity finishes. Launcher stays `singleTask` + affinity `…redrock.launcher`. Immersive stays `singleTask` + affinity `…redrock.immersive`. Do not use `singleInstance` on the launcher (Minecraft cannot join that task). Do not run the hosted game in a `:minecraft` process.

Full must-not-break list: [game host agent spec](agents/game-host.md).

## Packages (Java/Kotlin)

| Area | Where |
|---|---|
| Library UI | `LauncherActivity.kt`, `ui/` |
| Play catalog | `play/GooglePlayCatalog.kt`, `PlayCatalog.kt` |
| Sign-in WebView | `play/GoogleLoginPane.kt` |
| Extract / payload | `game/GameStore.kt` |
| Host glue | `game/GameRuntime.kt`, `MinecraftComponentFactory.kt`, `HostPreflight.kt` |
| Mods | `mods/ModStore.kt`, `cpp/mod_loader.c` |
| Sidecar | `./redrock`, `scripts/common.sh` |

UI binds to `PlayCatalog.state` only. Composables do not talk HTTP or Play protocol.

## Play fetch (short)

EmbeddedSetup WebView → `oauth_token` cookie → AAS on device → gplayapi delivery → extract. Ownership-only. Spec: [play-fetch](agents/play-fetch.md).

## Native

`app/src/main/cpp/` — stub `libminecraftpe.so` forwarder, mcpelauncher hook shim, PairIP / PlayFab / MAE stubs so the real Play Integrity libs are not loaded inside the host package. Load order in the game-host spec is strict.

## Layout on disk (repo)

```
app/                 Android / Spatial SDK hybrid
docs/                This documentation
apk/                 Sideload outputs (gitignored binaries)
mods/                Staging folder; *.so gitignored
logs/                logcat captures, gitignored
redrock              Computer CLI
third_party/         Font license (OFL)
.cursor/rules/       Cursor rules (public); rest of .cursor/ ignored
```

Application id: `com.saltmarshdigital.redrock`.
