# Legal

Not legal advice. Read this as project policy for a **public** repository.

## This software

RedRock source is [BSD 3-Clause](../LICENSE). You may use, modify, and redistribute the code. It is provided **as is**, with no warranty. The authors are not responsible for how you use it, including bans, bricked headsets, or lost Google / Minecraft accounts.

The **RedRock** name, logo, and branding are **not** part of that grant. [TRADEMARKS.md](../TRADEMARKS.md).

## This is unofficial

RedRock is not affiliated with, endorsed by, sponsored by, or approved by Microsoft, Mojang Studios, Xbox, Google, Meta, or their subsidiaries.

Minecraft® is a trademark of Mojang Synergies AB / Microsoft. Google Play™ and Android are trademarks of Google LLC. Meta Quest® and Horizon OS are trademarks of Meta Platforms, Inc.

## Minecraft

You must already own Minecraft Bedrock on Google Play. RedRock is a host for **your** copy. It is not a store, not a cracked APK, and not a redistributor.

Do not commit, host, or share Play APKs or OBB files. `apk/` and `*.obb` are gitignored.

## Google Play fetch

Play has no consumer “save this APK to disk” API. The pattern here matches mcpelauncher / similar unofficial clients:

1. Sign in with an account that **owns** the title.
2. Ask Play to deliver for an `arm64-v8a` device profile.
3. Keep files for **personal, non-commercial** use on **that user’s** devices.

Play Console / Developer API is for apps **you** publish. Wrong path for third-party titles.

An unofficial Play client can violate Google’s Terms of Service. Google may terminate the account. Prefer a disposable account. Never use someone else’s.

If Play refuses (not owned, paid/DRM, region), RedRock fails with `OWNERSHIP` or `TOS`. Do not scrape mirrors. Do not implement a bypass.

Sign-in: Google EmbeddedSetup in a WebView. RedRock does not collect passwords in app code. AAS / oauth stay in EncryptedSharedPreferences on the headset.

## Meta / Quest

Sideloading requires Developer Mode. You are responsible for Meta’s developer and content policies on your devices.

## Third-party in tree

UI typeface: Minecraft-style font under SIL OFL in `third_party/minecraft-font/`. Play protocol: [gplayapi](https://github.com/aurora-pro/gplayapi) (see that project’s license). Meta Spatial SDK is a separate dependency with Meta’s terms.
