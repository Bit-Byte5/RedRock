# RedRock

Oculus Quest workspace for sideloading builds, reading headset logs, and keeping the project on git. The app itself is not in this repo yet — this tree is the load / log / device sidecar.

## Prereqs

- [Android platform-tools](https://developer.android.com/tools/releases/platform-tools) (`adb`). Homebrew: `brew install android-platform-tools`
- Headset in **Developer Mode** (Meta Quest Developer Hub is already a good way to turn that on)
- USB debugging accepted on the headset, or **Wireless debugging** from Quest Settings → System → Developer

`adb` on this machine: `/opt/homebrew/bin/adb` (also under `$ANDROID_HOME/platform-tools`).

## Quick start

```bash
cp .env.example .env          # optional: QUEST_HOST + PACKAGE
./redrock status              # adb server + attached devices
./redrock load                # installs the newest file in apk/
./redrock logs                # follows logcat, writes logs/
```

Wireless (from the Wireless debugging screen: pairing IP/port + code, then the separate connect port):

```bash
./redrock pair 192.168.1.20:37123 123456
./redrock connect 192.168.1.20:5555
```

Put `QUEST_HOST` in `.env` and `./redrock connect` will reuse it.

## Layout

```
apk/          drop Quest APKs here (gitignored)
logs/         captured logcat (gitignored)
scripts/      shared adb helpers
redrock       CLI
```

## Git

Remote: [SaltMarshDigital/RedRock](https://github.com/SaltMarshDigital/RedRock) (private).
