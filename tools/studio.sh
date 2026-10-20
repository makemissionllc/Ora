#!/usr/bin/env bash
set -euo pipefail
ORA_ROOT=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)
cd "$ORA_ROOT"
export PATH="$ORA_ROOT/depot_tools:$PATH"
ORA_SRC="$ORA_ROOT/chromium/src"
ORA_OUT="$ORA_SRC/out/Ora-arm64"
ORA_APK="$ORA_ROOT/build/outputs/apk/ora/ora-arm64.apk"

fingerprint() {
    sha256sum build.sh common.sh patch.sh args.gn .gclient tools/studio.sh tools/sync_resources.py ora/*.sh
    git -C vanadium rev-parse HEAD
    printf 'include_webview_cts=%s include_emulator=%s\n' "${ORA_INCLUDE_WEBVIEW_CTS_ARCHIVE:-0}" "${ORA_INCLUDE_ANDROID_EMULATOR:-0}"
}

adb_command() {
    local sdk="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-$HOME/Android/Sdk}}"
    if [[ -x "$sdk/platform-tools/adb" ]]; then
        ORA_ADB="$sdk/platform-tools/adb"
    elif [[ -x "$ORA_SRC/third_party/android_sdk/public/platform-tools/adb" ]]; then
        ORA_ADB="$ORA_SRC/third_party/android_sdk/public/platform-tools/adb"
    else
        ORA_ADB=$(command -v adb) || { echo 'Set ANDROID_HOME or add adb to PATH.' >&2; exit 1; }
    fi
    ORA_DEVICE=()
    if [[ -n ${ORA_ADB_SERIAL:-} ]]; then ORA_DEVICE=(-s "$ORA_ADB_SERIAL"); fi
}

case "${1:-doctor}" in
    fingerprint) fingerprint ;;
    doctor)
        for tool in bash git python3 curl flock; do command -v "$tool"; done
        if [[ -f chromium/.ora-prepared ]] && [[ $(cat chromium/.ora-prepared) == "$(fingerprint)" ]]; then
            echo 'Ora sources: prepared'
        else
            echo 'Ora sources: preparation required (Gradle task prepareOra)'
        fi
        adb_command
        "$ORA_ADB" devices -l
        ;;
    prepare)
        options=(--prepare-only --skip-system-deps)
        if [[ ${ORA_RESUME_PREPARATION:-false} == true || ${ORA_RESUME_PREPARATION:-0} == 1 ]]; then
            options+=(--resume)
        fi
        # System package installation needs a terminal. IDE preparation skips sudo.
        ORA_WAIT_FOR_BUILD_LOCK=1 ORA_PREPARE_IF_NEEDED=1 bash "$ORA_ROOT/build.sh" "${options[@]}"
        ;;
    assemble)
        exec 9>"$ORA_ROOT/chromium/.ora-build.lock"
        if ! flock -n 9; then
            echo 'Waiting for the active Ora Browser build before compiling...'
            flock 9
        fi
        if [[ ! -f chromium/.ora-prepared ]] || [[ $(cat chromium/.ora-prepared) != "$(fingerprint)" ]]; then
            echo 'Run the prepareOra Gradle task before compiling.' >&2; exit 1
        fi
        python3 "$ORA_ROOT/tools/sync_resources.py" "$ORA_SRC"
        mkdir -p "$ORA_OUT" "$(dirname "$ORA_APK")"
        python3 - "$ORA_ROOT/args.gn" "$ORA_OUT/args.gn" <<'PY'
from pathlib import Path
import re
import sys
args, count = re.subn(r'^target_cpu\s*=\s*"arm"\s*$', 'target_cpu = "arm64"', Path(sys.argv[1]).read_text(), flags=re.M)
if count != 1:
    raise SystemExit('Expected one target_cpu = "arm" in args.gn')
Path(sys.argv[2]).write_text(args)
PY
        cd "$ORA_SRC"
        gn gen "$ORA_OUT"
        autoninja -C "$ORA_OUT" -j "${ORA_BUILD_JOBS:-4}" chrome_public_apk
        mapfile -t artifacts < <(find "$ORA_OUT/apks" -maxdepth 1 -type f -name 'Chrome*.apk')
        if [[ ${#artifacts[@]} != 1 ]]; then
            echo 'Expected exactly one Chrome APK in the output directory.' >&2; exit 1
        fi
        cp -- "${artifacts[0]}" "$ORA_APK"
        echo "APK: $ORA_APK"
        ;;
    install)
        [[ -f "$ORA_APK" ]] || { echo 'Run assembleOra first.' >&2; exit 1; }
        adb_command
        "$ORA_ADB" "${ORA_DEVICE[@]}" install -r "$ORA_APK"
        ;;
    run)
        adb_command
        "$ORA_ADB" "${ORA_DEVICE[@]}" shell am start -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -p com.makemission.ora
        ;;
    project)
        cd "$ORA_SRC"
        python3 build/android/gradle/generate_gradle.py --output-directory "$ORA_OUT" --target //chrome/android:chrome_public_apk --project-dir "$ORA_SRC/out/OraStudio"
        ;;
    *) echo "Unknown action: $1" >&2; exit 2 ;;
esac
