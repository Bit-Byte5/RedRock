# Agent notes

RedRock is a Quest hybrid app plus an ADB sidecar. Home opens a **2D Horizon window**. Play enters a **mono full-view GLES host** in that window. Sideload artifacts live in `apk/` (gitignored).

## Must not break

- Keep `LauncherActivity` and `ImmersiveActivity` as two tasks. Do not merge them.
- Minecraft first loads **non-stereoscopic** into the 2D window via hosted `com.mojang.minecraftpe.MainActivity`. Do not PackageInstaller the game as a second Quest app.
- Play fetch is **mcpelauncher-style**: Google account that **owns** the app, personal use, no redistribution.
- Never commit Google tokens, passwords, `.env`, or APK/OBB files.
- Log package names and `FetchState`, never credentials.

## Where to look

| Topic | File |
|---|---|
| Hybrid 2D / VR split | `.cursor/rules/redrock-hybrid.mdc` |
| Mono game host | [docs/agents/game-host.md](docs/agents/game-host.md) |
| Play fetch legality and secrets | `.cursor/rules/play-fetch.mdc` |
| Play fetch spec and 2D UI contract | [docs/agents/play-fetch.md](docs/agents/play-fetch.md) |
| Catalog API | `app/src/main/java/com/saltmarshdigital/redrock/play/` (`GooglePlayCatalog`) |
| Native mods | `app/src/main/java/com/saltmarshdigital/redrock/mods/ModStore.kt`, `app/src/main/cpp/mod_loader.c` |
| Game surface (placeholder GLES) | `app/src/main/java/com/saltmarshdigital/redrock/game/` |
| Sidecar CLI | `redrock` |

Fetch UI belongs on the 2D library panel only. Download opens Google's EmbeddedSetup page in a WebView; credentials stay on that page. `GooglePlayCatalog` talks to Play. The game surface is that same window, full view. Do not add store chrome to immersive.
