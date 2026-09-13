# RedRock

Quest hybrid app: a **2D Horizon window**. Play hosts Minecraft inside RedRock (extracted APK, not a second Quest app).

```
Home / library
    └─ LauncherActivity     2D panel (com.oculus.intent.category.2D)
            Play
            └─ MonoGameScreen      full-view GLES 3
                    Leave back to library
```

## Prereqs

- Android platform-tools (`adb`). Homebrew: `brew install android-platform-tools`
- Headset in Developer Mode
- JDK 17+ (OpenJDK 21 is fine)

## Commands

```bash
./redrock status              # adb server + attached devices
./redrock build               # assembleDebug → apk/redrock-debug.apk
./redrock load                # sideload newest apk/
./redrock run                 # build + load + launch
./redrock logs                # follow logcat, write logs/
./redrock mods push file.so   # install an arm64 .so onto the headset
./redrock mods list           # list mods on the headset
```

Wireless:

```bash
./redrock pair 192.168.x.x:PAIRPORT 123456
./redrock connect 192.168.x.x:5555
```

Copy `.env.example` to `.env` for `QUEST_HOST` and package name.

## Layout

```
app/          Android / Spatial SDK hybrid
apk/          sideload APKs (gitignored)
mods/         staged arm64 .so mods (gitignored)
logs/         captured logcat (gitignored)
redrock       CLI
```

## License

[BSD 3-Clause](LICENSE). Use the code however you want. It is provided **as is**, with no warranty. The authors are not responsible for how you use it.

The **RedRock** name is not included. Forks must not impersonate the official project. See [TRADEMARKS.md](TRADEMARKS.md).

Remote: [Bit-Byte5/RedRock](https://github.com/Bit-Byte5/RedRock).
