#!/bin/bash
set -euo pipefail

SCRIPT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
cd "$SCRIPT_DIR"
source "$SCRIPT_DIR/common.sh"
PREPARE_ONLY=0
RESUME=0
SKIP_SYSTEM_DEPS=0
for option in "$@"; do
    case "$option" in
        --prepare-only) PREPARE_ONLY=1 ;;
        --resume) RESUME=1 ;;
        --skip-system-deps) SKIP_SYSTEM_DEPS=1 ;;
        *) echo "Unknown option: $option" >&2; exit 2 ;;
    esac
done
if [[ "$PREPARE_ONLY" == 0 ]]; then
    : "${LOCAL_TEST_JKS:?Set LOCAL_TEST_JKS for a release build, or use --prepare-only for Android Studio}"
    : "${STORE_TEST_JKS:?Set STORE_TEST_JKS for a release build, or use --prepare-only for Android Studio}"
fi

# Prevent two builds from modifying the same Chromium checkout at once.
mkdir -p "$SCRIPT_DIR/chromium"
exec 9>"$SCRIPT_DIR/chromium/.ora-build.lock"
if [[ ${ORA_WAIT_FOR_BUILD_LOCK:-0} == 1 ]]; then
    if ! flock -n 9; then
        echo "Waiting for the active Ora Browser build to release the checkout..."
        flock 9
    fi
elif ! flock -n 9; then
    echo "Another Ora Browser build is already running." >&2
    exit 1
fi

# IDE preparation may have queued behind a successful bootstrap. Check only
# after acquiring its lock, before clearing markers or resetting any sources.
if [[ "$PREPARE_ONLY" == 1 && ${ORA_PREPARE_IF_NEEDED:-0} == 1 ]] &&
    [[ -f "$SCRIPT_DIR/chromium/.ora-prepared" ]] &&
    [[ $(cat "$SCRIPT_DIR/chromium/.ora-prepared") == "$("$SCRIPT_DIR/tools/studio.sh" fingerprint)" ]]; then
    echo "Ora sources are already prepared; keeping incremental build state."
    exit 0
fi

export VERSION=$(grep -m1 -o '[0-9]\+\(\.[0-9]\+\)\{3\}' "$SCRIPT_DIR/vanadium/args.gn")
export CHROMIUM_SOURCE=https://chromium.googlesource.com/chromium/src.git
export DEBIAN_FRONTEND=noninteractive

if [[ "$SKIP_SYSTEM_DEPS" == 0 ]]; then
sudo apt-get update
sudo apt-get install -y sudo lsb-release file nano git curl python3 python3-pillow imagemagick librsvg2-bin
sudo dpkg --add-architecture i386
sudo apt-get update
sudo apt-get install -y libgcc-s1:i386
fi

if [[ ! -d "$SCRIPT_DIR/depot_tools/.git" ]]; then
    git clone --depth 1 https://chromium.googlesource.com/chromium/tools/depot_tools.git "$SCRIPT_DIR/depot_tools"
fi
if [[ ! -x "$SCRIPT_DIR/depot_tools/gclient" ]]; then
    echo "The depot_tools checkout is incomplete; repair or remove it, then retry." >&2
    exit 1
fi
export PATH="$SCRIPT_DIR/depot_tools:$PATH"

mkdir -p "$SCRIPT_DIR/chromium/src/out/Default"
cd "$SCRIPT_DIR/chromium/src"
if ! git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
    git init
fi
if git remote get-url origin >/dev/null 2>&1; then
    git remote set-url origin "$CHROMIUM_SOURCE"
else
    git remote add origin "$CHROMIUM_SOURCE"
fi

ORA_PATCH_DIR=""
ORA_CHROMIUM_DEPS_BACKUP=""
restore_build_temps() {
    if [[ -n "$ORA_CHROMIUM_DEPS_BACKUP" && -f "$ORA_CHROMIUM_DEPS_BACKUP" ]]; then
        mv -- "$ORA_CHROMIUM_DEPS_BACKUP" "$SCRIPT_DIR/chromium/src/DEPS"
    fi
    if [[ -n "$ORA_PATCH_DIR" ]]; then rm -rf -- "$ORA_PATCH_DIR"; fi
}
trap restore_build_temps EXIT
trap 'exit 130' INT
trap 'exit 143' TERM
# A failed refresh must not leave an older checkout marked ready.
rm -f "$SCRIPT_DIR/chromium/.ora-prepared"

if [[ "$RESUME" == 0 ]]; then
git fetch --depth 1 origin "+refs/tags/$VERSION:refs/tags/$VERSION"
if [[ -d .git/rebase-apply ]]; then
    git am --abort >/dev/null 2>&1 || rm -rf .git/rebase-apply
fi
git checkout --force --detach "$VERSION"
# gclient otherwise queries Chromium's remote HEAD (which can stall on the
# public server); the pinned release commit is sufficient for local branch detection.
CHROMIUM_TAG_COMMIT=$(git rev-parse "$VERSION^{commit}")
git update-ref refs/remotes/origin/main "$CHROMIUM_TAG_COMMIT"
git symbolic-ref refs/remotes/origin/HEAD refs/remotes/origin/main

# Rewrite a temporary copy so the upstream Vanadium submodule stays untouched.
ORA_PATCH_DIR=$(mktemp -d "$SCRIPT_DIR/chromium/ora-patches.XXXXXX")
cp -a "$SCRIPT_DIR/vanadium/patches/." "$ORA_PATCH_DIR/"

# https://grapheneos.org/build#browser-and-webview
rm -f "$ORA_PATCH_DIR"/*trichrome-{apk-build-targets,browser-apk-targets}.patch
rm -f "$ORA_PATCH_DIR"/*{detailed,supported}-language*.patch
rm -f "$ORA_PATCH_DIR"/*javascript-optimizer-{site-setting,settings-UI}.patch
rm -f "$ORA_PATCH_DIR"/*component-updates.patch
rm -f "$ORA_PATCH_DIR"/*{pdf,PDF,for-content-public,toolbar-button,configs-from-config-app,config-app-parsing,new-tab-card,predictive-back}*.patch
# rm -f "$ORA_PATCH_DIR"/*crashpad*.patch
replace "$ORA_PATCH_DIR" "VANADIUM" "ORA"
replace "$ORA_PATCH_DIR" "Vanadium" "Ora"
replace "$ORA_PATCH_DIR" "vanadium" "ora"
git am --whitespace=nowarn --keep-non-patch "$ORA_PATCH_DIR"/*.patch
else
    if [[ -d .git/rebase-apply ]] || ! git merge-base --is-ancestor "$VERSION" HEAD || [[ ! -d ora ]] || ! git diff --ignore-submodules=all --quiet -- . ':!DEPS'; then
        echo "Checkout cannot be resumed safely. Run preparation without --resume." >&2
        exit 1
    fi

fi
cp "$SCRIPT_DIR/.gclient" "$SCRIPT_DIR/chromium/.gclient"

if [[ "$SKIP_SYSTEM_DEPS" == 0 ]]; then
    ./build/install-build-deps.sh --no-prompt
fi

# Ora builds for a physical device, not Android WebView tests or an emulator.
# Omit those large, unused packages unless explicitly requested. Restore
# Chromium's DEPS file even on failure.
if [[ ${ORA_INCLUDE_WEBVIEW_CTS_ARCHIVE:-0} != 1 || ${ORA_INCLUDE_ANDROID_EMULATOR:-0} != 1 ]]; then
    CHROMIUM_DEPS="$SCRIPT_DIR/chromium/src/DEPS"
    ORA_CHROMIUM_DEPS_BACKUP=$(mktemp "$SCRIPT_DIR/chromium/ora-DEPS.XXXXXX")
    cp -p "$CHROMIUM_DEPS" "$ORA_CHROMIUM_DEPS_BACKUP"
    python3 - "$CHROMIUM_DEPS" "${ORA_INCLUDE_WEBVIEW_CTS_ARCHIVE:-0}" "${ORA_INCLUDE_ANDROID_EMULATOR:-0}" <<'PY'
from pathlib import Path
import re
import sys

path = Path(sys.argv[1])
contents = path.read_text()
if sys.argv[2] != "1":
    entry = "'src/android_webview/tools/cts_archive/cipd': {"
    start = contents.index(entry)
    end = contents.index("\n  },", start)
    block = contents[start:end]
    condition = "'condition': 'checkout_android',"
    if block.count(condition) != 1:
        raise SystemExit("Could not locate the WebView CTS archive condition exactly once")
    contents = contents[:start] + block.replace(condition, "'condition': 'False',", 1) + contents[end:]

if sys.argv[3] != "1":
    entry = "'src/third_party/android_sdk/public': {"
    start = contents.index(entry)
    end = contents.index("\n  },", start)
    block = contents[start:end]
    block, count = re.subn(
        r"\n          \{\s+'package': 'chromium/third_party/android_sdk/public/emulator',"
        r"\s+'version': Var\('android_sdk_emulator_version'\),\s+\},",
        "",
        block,
        flags=re.S,
    )
    if count != 1:
        raise SystemExit("Could not locate the Android emulator package exactly once")
    contents = contents[:start] + block + contents[end:]

path.write_text(contents)
PY
fi

gclient sync -D --no-history --nohooks
if [[ -n "$ORA_CHROMIUM_DEPS_BACKUP" ]]; then
    mv -- "$ORA_CHROMIUM_DEPS_BACKUP" "$SCRIPT_DIR/chromium/src/DEPS"
    ORA_CHROMIUM_DEPS_BACKUP=""
fi
gclient runhooks

source "$SCRIPT_DIR/patch.sh"
cp "$SCRIPT_DIR/args.gn" out/Default/args.gn
gn gen out/Default
"$SCRIPT_DIR/tools/studio.sh" fingerprint > "$SCRIPT_DIR/chromium/.ora-prepared.tmp"
mv "$SCRIPT_DIR/chromium/.ora-prepared.tmp" "$SCRIPT_DIR/chromium/.ora-prepared"
if [[ "$PREPARE_ONLY" == 1 ]]; then
    echo "Ora Browser sources are ready for Android Studio builds."
    exit 0
fi
set_keys
mkdir -p out/tmp out/release

BUILD_JOBS=${ORA_BUILD_JOBS:-4}
autoninja -C out/Default -j "$BUILD_JOBS" chrome_public_apk
mv $(find out/Default/apks -name 'Chrome*.apk') out/tmp/$VERSION-armeabi-v7a.apk
sed -i 's/target_cpu = "arm"/target_cpu = "arm64"/' out/Default/args.gn
autoninja -C out/Default -j "$BUILD_JOBS" chrome_public_apk chrome_public_bundle
mv $(find out/Default/apks -name 'Chrome*.apk') out/tmp/$VERSION-arm64-v8a.apk
mv $(find out/Default/apks -name 'Chrome*.aab') out/tmp/$VERSION-arm64-v8a.aab

export PATH="$SCRIPT_DIR/chromium/src/third_party/jdk/current/bin:$PATH"
export ANDROID_HOME="$SCRIPT_DIR/chromium/src/third_party/android_sdk/public"
sign_apk out/tmp/$VERSION-armeabi-v7a.apk out/release/$VERSION-armeabi-v7a.apk
sign_apk out/tmp/$VERSION-arm64-v8a.apk out/release/$VERSION-arm64-v8a.apk
sign_aab out/tmp/$VERSION-arm64-v8a.aab out/release/$VERSION-arm64-v8a.aab
rm -rf "$SCRIPT_DIR/keys"
