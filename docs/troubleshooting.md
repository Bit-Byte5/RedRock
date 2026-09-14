# Troubleshooting

## Cannot find RedRock on the Quest

Unknown Sources — [headset](headset.md). Developer Mode, reboot, `./redrock load` again.

## `./redrock status` shows no device

- USB: cable, RSA prompt, USB debugging.
- Wireless: pair with the **pairing** port, connect with the **connect** port. They are not the same.
- `adb` missing: `brew install android-platform-tools` or set `ANDROID_HOME`.

## Download says this account does not own Minecraft

Wrong Google account, or Play has no entitlement. Settings → Google → switch. No mirrors, no paid bypass.

## Play refused this download

Play TOS / region / DRM. RedRock surfaces `TOS` and stops. That is not a bug to “fix” with another store.

## No arm64 build

Play did not give `arm64-v8a`. Quest cannot use an x86 extract.

## Network error

Headset needs internet for Play. Check Quest wifi. Tokens stay on device; this is not “paste an AAS into `.env`”.

## Button still says Download after a long wait

Fetch failed or unpack failed. Log tab (computer icon) or `./redrock logs`. Look for `RedRockPlay` / `FetchState`. Never paste log lines that contain cookies or tokens into a public issue — redact first. The repo and issue tracker are public.

## Play → “Game is not ready” / “Could not repair game assets”

Host preflight refused to start `MainActivity` (native abort cannot be caught). Stay on the library. Try Download again for that version. Do not copy a Mac mcpelauncher folder onto the headset.

## Game starts then immediately dies

Log tab + `./redrock logs`. Confirm you are on a version Play actually delivered. Disable extra mods, leave Core on.

## Google sign-in keyboard never appears

Tap a field on the EmbeddedSetup page. Quest IME is picky; the pane tries to focus the WebView.

## Signed in but the email looks wrong

Settings → Google is the source of truth. Sign out / switch. Do not share screenshots of tokens.

## I almost committed an APK or `.env`

Stop. Those are gitignored on purpose. If they got in, remove them from git history before you push. See [contributing](contributing.md).
