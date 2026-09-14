# Getting started

You need a Meta Quest (2 / Pro / 3 / 3S), Developer Mode, and a Google account that **already owns** Minecraft Bedrock on Play.

This repo does **not** ship Minecraft. RedRock asks Play for a build the signed-in account is entitled to, then hosts it inside RedRock. It does not install Minecraft as a second Quest app.

## On a computer

1. Git clone [Bit-Byte5/RedRock](https://github.com/Bit-Byte5/RedRock).
2. JDK 17+ (`java -version`). OpenJDK 21 is fine.
3. Android `adb` — Homebrew: `brew install android-platform-tools`.
4. Optional: copy `.env.example` to `.env` and set `QUEST_HOST` after wireless debugging works. **Do not commit `.env`.**

```bash
./redrock status              # adb + attached devices
./redrock run                 # build, sideload, launch
```

`./redrock build` writes `apk/redrock-debug.apk` (gitignored). `./redrock load` sideloads the newest file in `apk/`. Full command list: [CLI](cli.md).

USB: plug in, accept the RSA prompt in the headset, keep USB debugging on.

Wireless: Quest **Settings → System → Developer → Wireless debugging**. Pairing uses a **pairing** IP:port + six-digit code. Connecting uses a **different** connect port (often 5555).

```bash
./redrock pair 192.168.x.x:PAIRPORT 123456
./redrock connect 192.168.x.x:5555
```

Put the **connect** host in `.env` as `QUEST_HOST` so later `./redrock connect` needs no arguments.

## First launch on the headset

Sideloaded apps are not in the Store dock. Open **Unknown Sources** — see [Headset](headset.md).

Then:

1. Settings → **Google** — sign in with the account that owns Minecraft.
2. Bottom bar: pick **Latest** or a listed Bedrock version.
3. **Download**, wait until the button says **Play**.
4. **Play** starts Minecraft in **this same 2D window**. Stereo VR is not done yet.

## What you are not doing

- Not installing Minecraft from APKPure, mirrors, or a friend’s dump.
- Not putting APKs, OBB files, tokens, or headset IPs in git. The repo is public.
- Not expecting a Store listing or a bottom-bar icon.
