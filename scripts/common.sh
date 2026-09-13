#!/usr/bin/env bash
# Shared helpers for the RedRock Quest/ADB workspace.

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

REDROCK_ENV="${ROOT}/.env"
ADB_BIN="${ADB_BIN:-}"

info()  { printf 'redrock: %s\n' "$*"; }
warn()  { printf 'redrock: warning: %s\n' "$*" >&2; }
die()   { printf 'redrock: error: %s\n' "$*" >&2; exit 1; }

load_env() {
  if [[ -f "$REDROCK_ENV" ]]; then
    # shellcheck disable=SC1090
    set -a
    source "$REDROCK_ENV"
    set +a
  fi
  PACKAGE="${PACKAGE:-com.saltmarshdigital.redrock}"
  QUEST_PORT="${QUEST_PORT:-5555}"
}

require_java() {
  if ! command -v java >/dev/null 2>&1; then
    die "java not found. Install a JDK 17+ (OpenJDK 21 on this machine is fine)."
  fi
}

find_adb() {
  if [[ -n "$ADB_BIN" && -x "$ADB_BIN" ]]; then
    return 0
  fi
  local candidate
  for candidate in \
    "$(command -v adb 2>/dev/null || true)" \
    "${ANDROID_HOME:-}/platform-tools/adb" \
    "${ANDROID_SDK_ROOT:-}/platform-tools/adb" \
    "$HOME/Library/Android/sdk/platform-tools/adb" \
    /opt/homebrew/bin/adb \
    /usr/local/bin/adb
  do
    if [[ -n "$candidate" && -x "$candidate" ]]; then
      ADB_BIN="$candidate"
      return 0
    fi
  done
  return 1
}

require_adb() {
  if ! find_adb; then
    die "adb not found. Install Android platform-tools (brew install android-platform-tools) or set ANDROID_HOME."
  fi
  if ! "$ADB_BIN" start-server >/dev/null 2>&1; then
    die "adb is installed but the server failed to start ($ADB_BIN)."
  fi
}

adb() {
  require_adb
  command "$ADB_BIN" "$@"
}

connected_serials() {
  adb devices | awk 'NR>1 && $2=="device" {print $1}'
}

device_count() {
  connected_serials | grep -c . || true
}

require_device() {
  require_adb
  local count
  count="$(device_count)"
  if [[ "$count" -lt 1 ]]; then
    die "no Quest/Android device in 'device' state.
  USB: plug in, accept the RSA prompt in the headset, enable USB debugging.
  Wireless: Developer → Wireless debugging, then:  ./redrock pair <ip:port> <code>
            ./redrock connect <ip:port>"
  fi
}

pick_apk() {
  local given="${1:-}"
  if [[ -n "$given" ]]; then
    [[ -f "$given" ]] || die "APK not found: $given"
    printf '%s\n' "$given"
    return
  fi
  local latest
  latest="$(ls -t "$ROOT"/apk/*.apk 2>/dev/null | head -n1 || true)"
  [[ -n "$latest" ]] || die "no APK given and none in apk/. Drop a build in apk/ or pass a path."
  printf '%s\n' "$latest"
}

quest_target() {
  local arg="${1:-}"
  if [[ -n "$arg" ]]; then
    if [[ "$arg" == *:* ]]; then
      printf '%s\n' "$arg"
    else
      printf '%s:%s\n' "$arg" "$QUEST_PORT"
    fi
    return
  fi
  [[ -n "${QUEST_HOST:-}" ]] || die "no host given. Pass ip[:port] or set QUEST_HOST in .env"
  printf '%s:%s\n' "$QUEST_HOST" "$QUEST_PORT"
}
