# CLI (`./redrock`)

Run from the repo root. Needs `adb` and, for `build`/`run`, JDK 17+ and the Gradle wrapper.

```bash
./redrock help
```

| Command | What it does |
|---|---|
| `status` | adb version + `adb devices -l` |
| `pair IP:PORT [CODE]` | Wireless pairing (Quest pairing port) |
| `connect [IP[:PORT]]` | Wireless connect (`QUEST_HOST` / `QUEST_PORT` from `.env`) |
| `disconnect [target]` | Drop wireless/USB adb |
| `build` | `gradlew :app:assembleDebug` → `apk/redrock-debug.apk` |
| `load [apk]` | `adb install -r --user 0` newest `apk/*.apk` or a path |
| `run` | build + load debug APK + launch |
| `launch [package]` | Start the app (default `com.saltmarshdigital.redrock`) |
| `stop [package]` | Force-stop |
| `uninstall [package]` | Uninstall RedRock from the headset |
| `logs [package]` | Follow logcat, tee into `logs/` |
| `pull-logs` | Dump current logcat buffer |
| `mods list` / `push file.so` / `rm file.so` | Headset `files/mods` |
| `shell` | `adb shell` |

There is **no** `./redrock fetch`. Downloads happen **on the headset** from the 2D panel. Same ownership rules if a desktop fetch is added later.

`.env` (gitignored):

```
QUEST_HOST=
QUEST_PORT=5555
PACKAGE=com.saltmarshdigital.redrock
```

Copy from `.env.example`. Never put pairing codes, filled IPs, or tokens in a commit.
