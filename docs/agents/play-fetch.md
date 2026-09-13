# Play fetch (agent spec)

Working spec for mcpelauncher-style Google Play downloads.

**Current client:** `GooglePlayCatalog` + EmbeddedSetup WebView. Tokens live in EncryptedSharedPreferences on the headset. No AAS values in git, logs, or the UI.

Sign-in is Google's real login page in the 2D panel. The user types credentials there. RedRock never reads the password — only the one-time `oauth_token` cookie, exchanged for an AAS token.

## Legal path

Google Play has no consumer “save this APK to disk” API. The allowed pattern is the one Minecraft Bedrock Launcher uses:

1. Sign in with a **Google account that already owns** the app (purchased or free-install entitlement).
2. Ask Play to deliver the package for a device profile.
3. Keep the files for **personal, non-commercial** use on **this user’s** devices.
4. Do not sell, host, or otherwise redistribute the APK.

Play Console / the Developer API is only for **apps SaltMarsh publishes**. Wrong path for third-party titles.

An unofficial Play client (same class as mcpelauncher / `apkeep`) can violate Google’s Terms of Service. Google may terminate the account. Prefer a disposable Google account once real login exists. Never use someone else’s account.

If Play refuses (not owned, paid/DRM, region), fail with `FetchError.OWNERSHIP` or `FetchError.TOS`. Do not scrape mirrors.

## Where it runs

On-headset, from the 2D launcher. Not from `ImmersiveActivity`. Not from the Mac CLI in this pass.

```
Quest Home
  └─ LauncherActivity     2D panel (account, package, fetch, install)
        Play ───────────► com.mojang.minecraftpe.MainActivity  (hosted in RedRock)
        fetch ──────────► Play delivery ──► extract into filesDir/game (not PackageInstaller)
```

Desktop `./redrock fetch` is a later optional sibling. Same ownership rules if it appears.

## Secrets

| Location | What | Git |
|---|---|---|
| Headset (later) | email + AAS / auth token in EncryptedSharedPreferences | never |
| Mac (later) | `.env` or `~/.config/apkeep/apkeep.ini` | already gitignored |
| `apk/*.apk`, `*.obb` | Play artifacts | gitignored |

Log `packageName` and `FetchState` only. Never log tokens, cookie headers, or passwords.

## ABI and splits

Quest is `arm64-v8a`. Request:

- split APKs when Play offers them
- additional OBB / expansion files when present

An x86 mcpelauncher extract is the **wrong** artifact to sideload. If Play returns an incompatible set, `FetchError.ABI`.

## UI contract (`LauncherActivity`)

Keep the existing primary **Play** control: it hosts Minecraft's `MainActivity` inside RedRock. Fetch/unpack is **secondary** on the library panel. Do not put this chrome in `ImmersiveActivity`.

Stay on SpatialTheme. Default window remains `880dp` × `560dp` in the manifest unless the layout clearly needs a resize.

| Element | Behavior |
|---|---|
| Account row | `SignedOut` / `SigningIn` / email of `SignedIn`. Never show a token. Settings → Google is the place to inspect, sign in, sign out, or switch. |
| Version | Chip shows the full version. Tap to pick **Latest** (Play’s current build) or a known Bedrock release. |
| Play | **Download** until Minecraft is on disk; then starts hosted `MainActivity`. |

Fetch states the UI must render:

- `Idle` — no in-flight work
- `Fetching` — Play delivery in progress (`receivedBytes` / `totalBytes` for the bar)
- `Installing` — unpacking APKs into RedRock private storage
- `Ready` — extracted `libminecraftpe.so` is on disk; Play can host the game
- `Error` — `OWNERSHIP`, `TOS`, `ABI`, `NETWORK`, `UNKNOWN`

Bind Compose to `PlayCatalog.state`, not to Play protocol types. Production uses `GooglePlayCatalog`. `FakePlayCatalog` is only for UI dry-runs.

## Kotlin contract

Package: `com.saltmarshdigital.redrock.play`

- `PlayCatalog` — `signIn()`, `signInWithOAuth(email, oauthToken)`, `signOut()`, `account()`, `fetch(packageName)`, `install(file)`, `state`
- `GooglePlayCatalog` — EmbeddedSetup → AAS → Play delivery → extract into `filesDir/game`
- `FakePlayCatalog` — in-memory delays, no network

Do not call Play, HTTP, or PackageInstaller from composables. Do not install Minecraft as a second package.

## Out of scope

- `apkeep` / `./redrock fetch` on the Mac
- Paid/DRM circumvention
- Changing the sidecar CLI
- Putting passwords or tokens in git
