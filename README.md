# RedRock

Public open-source Quest app: a **2D Horizon window** that can host **Minecraft you already own**. Unofficial. Not a store. Not a cracked APK.

```
Home / library (2D window)
    └─ LauncherActivity
            Download  →  Play APKs into RedRock private storage
            Play      →  hosted Minecraft in the same window
```

Stereo VR is not implemented yet. Sideloaded builds live under **Unknown Sources**, not the Store dock.

**Repo:** [github.com/Bit-Byte5/RedRock](https://github.com/Bit-Byte5/RedRock) (public). Do not commit tokens, `.env`, APKs, or headset IPs.

## Docs

Full write-up: **[docs/](docs/README.md)** — getting started, headset, using the library, mods, CLI, architecture, contributing, legal.

On the headset: left rail **Books** tab (same topics, shorter).

## Quick start

Prereqs: JDK 17+, `adb` (`brew install android-platform-tools`), Quest in Developer Mode.

```bash
./redrock status    # adb + devices
./redrock run       # build, sideload, launch
```

Then Unknown Sources → RedRock → Settings → Google → sign in with the account that owns Minecraft → **Download** → **Play**.

Wireless:

```bash
./redrock pair 192.168.x.x:PAIRPORT 123456
./redrock connect 192.168.x.x:5555
```

Copy `.env.example` to `.env` for `QUEST_HOST`. Never commit `.env`.

## License

[BSD 3-Clause](LICENSE). Use the code however you want. **As is**, no warranty, authors not responsible for how you use it.

The **RedRock** name is not included. Do not impersonate the official project. [TRADEMARKS.md](TRADEMARKS.md). [Legal](docs/legal.md).
