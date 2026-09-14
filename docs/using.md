# Using RedRock

The left rail is the chrome. Selected tabs lift toward you.

| Icon | Tab |
|---|---|
| Grass block | Game — hero cube only. Play/Download is the bottom bar. |
| Toolbox | Native mods |
| Books | These instructions (headset copy of the docs) |
| Computer | Live game log |
| Sliders | Settings: Google account + Look/Volume |
| Unofficial | Disclaimer (not affiliated with Mojang, Google, Meta, …) |

Bottom bar: **version chip** + **Play / Download**.

## Google account (Settings → Google)

Play fetch uses **one** Google account. Open Settings and confirm the email before you download.

- Signed out: tap the card or **Sign in**. Google’s real login page opens in this window. RedRock does not collect the password — only the one-time `oauth_token` cookie, exchanged on-device for an AAS token stored in EncryptedSharedPreferences.
- Signed in: email + letter avatar. **Sign out** or **Switch account**.
- Sign-in from Settings does **not** start a download. Download still uses the bottom-bar button.

Use an account that **already owns** Minecraft on Play. Prefer an account you can afford to lose: an unofficial Play client can violate Google’s Terms of Service. Never use someone else’s account.

Tokens never belong in git, logs, or the UI. The public repo must stay clean — see [contributing](contributing.md).

## Download

If Minecraft is not extracted yet, the bottom button says **Download**.

1. Pick **Latest** (whatever Play currently serves) or a listed Bedrock release on the version chip.
2. Press Download.
3. If you are not signed in, the Google page opens first.
4. Progress shows on the bar. Failures: ownership, Play refused (TOS), no arm64 build, network.

Quest needs **arm64-v8a**. An x86 mcpelauncher extract from a Mac is the wrong artifact.

RedRock unpacks into **private app storage** (`filesDir/game/…`), not PackageInstaller. You will not get a second “Minecraft” icon in Unknown Sources from this path.

## Play

When the payload is ready (`libminecraftpe.so` extracted and preflight passed), the button says **Play**. That starts hosted `com.mojang.minecraftpe.MainActivity` in the **same 2D window**.

If Play still says Download, the game is not on disk. If it says the game is not ready, preflight failed — check the Log tab and [troubleshooting](troubleshooting.md).

Stereo OpenXR / VRed is a scaffold. Toggling VRed does not give working stereo yet.

## Version picker

Tap the chip (disabled while a fetch is running). **Latest** asks Play for the current version. Other rows are known Android `versionCode`s for Bedrock 1.21.x listed in `MinecraftReleases.kt`. Play can still refuse a version the account cannot have.
