# Contributing

The GitHub repo is **public**: [Bit-Byte5/RedRock](https://github.com/Bit-Byte5/RedRock). Every commit, PR, and issue is world-readable.

## License and name

Code: [BSD 3-Clause](../LICENSE). As-is, no warranty, authors not responsible for how you use it.

The **RedRock** name is not licensed. Forks may use the code. They may not impersonate the official project. [TRADEMARKS.md](../TRADEMARKS.md).

## Do not send

- Tokens, passwords, AAS, oauth cookies, Google emails in patches or issue text
- `.env`, `local.properties`, keystores
- `*.apk`, `*.obb`, `*.aab`, captured `logs/`
- Cracked dumps, APKPure mirrors, DRM/paid bypass
- Headset IPs or pairing codes

If you pasted a secret into an issue, rotate it. Do not assume GitHub deletion is enough.

Cursor always-on rule: `.cursor/rules/public-repo.mdc`.

## What to keep working

- Two tasks: `LauncherActivity` (2D) and `ImmersiveActivity` (VR later). Do not merge them.
- Host Minecraft in the 2D task. Do not PackageInstaller it as a second Quest app.
- Play fetch: account that **owns** the app, personal use, no redistribution.
- Bind UI to `PlayCatalog.state`.

Details: [architecture](architecture.md), [AGENTS.md](../AGENTS.md), [game-host](agents/game-host.md), [play-fetch](agents/play-fetch.md).

## Practical

- JDK 17+, `./redrock build` or Android Studio.
- Quest in Developer Mode for a real device check.
- In-headset copy of the user docs lives in `app/src/main/res/values/strings.xml` (`docs_ch*`). If you change behavior users see, update both GitHub `docs/` and those strings.
