# Ora Browser

Ora Browser is a Chromium-based browser for Android. This repository contains Ora’s downstream build configuration and patches; it checks out Chromium during the build and applies the upstream [Vanadium](https://github.com/GrapheneOS/Vanadium) patch set alongside Ora-specific changes.

The Android application ID is `com.makemission.ora`. The generated app is branded **Ora Browser**.

## What’s included

- Chromium-based Android browser build with desktop-style extension support.
- Ora downstream patches for extension installation and browser behavior.
- Manifest V2 compatibility and loading unpacked extensions for development.
- Build configuration for ARM and ARM64 APKs, plus an ARM64 Android App Bundle.
- Ora’s light and dark Material 3 color palette, with theme selection following Android’s system appearance.
- Phone browsers default to the bottom toolbar, with the existing Address bar setting to move it to the top. Tablet toolbar, address pill, and tab strip use the wireframe dimensions and order.
- Shared browser cards use 12dp corners and bottom sheets use 20dp corners.
- Tab overview cards use the same 12dp corners in grid and group views.
- Tablet tab search uses Chromium’s existing tab-search surface and preserves named, colored tab groups.
- Reader mode keeps Chromium’s text-size and theme controls, with Ora light/dark colors and a readable tablet text column.
- Chromium’s existing Android surfaces provide bookmarks, downloads, find-in-page, per-site settings, and send-tab actions.

Split view is not implemented yet. Chromium’s Android tab engine does not support showing two tabs in one browser window; TODO: add an Ora split-view screen and connect it to a tab-pair engine once that support exists.

Ora is a downstream Chromium project. Its code and patches do not provide the operating-system-level hardening available when using GrapheneOS with Vanadium.

## Installing and using extensions

Ora supports extensions from the Chrome Web Store, Opera Add-ons, and Microsoft Edge Add-ons. Local CRX or ZIP packages can be installed through the bundled companion extension. For development, open `chrome://extensions`, enable **Developer mode**, choose **Load unpacked**, and select an extension folder using the Android file picker.

The current build configuration fetches the separately published, signed companion extension package from the [Titanium Extension for Android project](https://github.com/jqssun/android-titanium-extension). Its publisher and in-extension branding are independent of Ora Browser.

For Chromium debug pages, visit `chrome://chrome-urls`; experiments are available at `chrome://flags`. WebRTC IP handling is under **Settings → Privacy and security**.

## Repository layout

- `args.gn`: Chromium GN build arguments, including the Android application ID.
- `build.sh`: prepares the Linux build environment, checks out Chromium, applies patches, builds, and signs APK/AAB outputs.
- `patch.sh`: applies Ora-specific source and resource changes after the upstream patch set.
- `vanadium/`: upstream Vanadium submodule with version information and its Chromium patch series.
- `extensions/`: extension bundling helpers and the code that stages the bundled CRX.
- `res/`: Ora app icon and light/dark theme resources.
- `ora/`: Ora-owned Chromium resource and source integration helpers.
- `fastlane/metadata/`: Android store listing text and screenshots.

## Building

The build script is designed for Linux and installs system dependencies with apt. A full Chromium build requires substantial disk space, memory, and time, plus network access to fetch Chromium and its dependencies.

Clone the repository and initialize its Vanadium submodule:

~~~sh
git clone --recurse-submodules https://github.com/makemissionllc/Ora.git
cd Ora
~~~

Provide signing inputs as environment variables: `LOCAL_TEST_JKS` is base64-encoded `local.properties` containing `keyAlias`, `keyPassword`, and `storePassword`; `STORE_TEST_JKS` is the base64-encoded signing keystore.

Then run:

~~~sh
./build.sh
~~~

The script writes signed outputs under `chromium/src/out/release`. The GitHub Actions workflow expects the same two values as repository secrets. For a CI build, run the **Build** workflow in Actions.

The build script prevents overlapping builds, reuses the existing Chromium and `depot_tools` checkouts, and resets Chromium to the pinned version before applying patches. It seeds the local remote-head reference for `gclient`, avoiding a separate network lookup. It rewrites a temporary copy of the Vanadium patch series, leaving the upstream submodule unchanged. Browser builds skip Chromium’s large Android WebView CTS test archive and Android emulator package; set `ORA_INCLUDE_WEBVIEW_CTS_ARCHIVE=1` or `ORA_INCLUDE_ANDROID_EMULATOR=1` to include either one. Chromium’s `DEPS` file is restored immediately after dependency sync. Ninja uses four jobs by default; set `ORA_BUILD_JOBS` to change the concurrency for your machine.

## Android Studio

Open this repository root in Android Studio, trust the project, and import it as a Gradle project. Use the Gradle wrapper and Android Studio’s bundled JDK (JDK 17 or newer). Gradle sync only loads the build tasks; it does not download Chromium or start compilation.

Ora uses Chromium’s GN/Ninja build system. The root Gradle project connects Android Studio to that compiler. In the **Gradle** tool window, open **Ora Browser → Tasks → ora**:

- `doctorOra`: show required tools, preparation status, and ADB devices.
- `prepareOra`: fetch dependencies, run hooks, and apply Ora source changes. Run once before the first build; subsequent calls reuse a matching preparation marker.
- `assembleOra`: build the arm64 APK for devices such as the Pad 8 Pro.
- `installOra`: build and install over ADB.
- `runOra`: build, install, and launch `com.makemission.ora`.
- `generateChromiumProject`: after a successful build, generate the Chromium Java source project under `chromium/src/out/OraStudio` for source navigation. Open that generated project separately when you need Chromium Java indexing.

Select **Ora Browser (build APK)** in the Run configuration menu and click Run to compile. Choose **Ora Browser (build, install, launch)** to also install and open the app on a connected device. You can also run the same tasks in Android Studio’s terminal:

~~~sh
./gradlew doctorOra
./gradlew assembleOra
./gradlew runOra -Pora.serial=YOUR_ADB_SERIAL
~~~

The APK is copied to `build/outputs/apk/ora/ora-arm64.apk`. Chromium’s local APK build uses its development signing configuration; use the existing `build.sh` release workflow with your signing inputs for distribution builds. Ora XML files under `res/values*` are synced into Chromium’s Ora extension resource directory before compilation, so incremental builds pick up palette and dimension edits. Missing registered resources produce a clear sync error. Sync also removes identical XML copies left in Chromium’s main resource tree by older staging scripts; it preserves separately edited copies and asks you to merge them into the repository’s `res/` files. Native intermediate files stay in `chromium/src/out/Ora-arm64` for incremental builds. `./gradlew clean` removes the root Gradle outputs and keeps the expensive Chromium checkout and native cache.

System dependencies require a terminal with sudo. Before your first IDE build, initialize the Vanadium submodule and prepare the Linux environment and sources:

~~~sh
git submodule update --init --recursive
bash build.sh --prepare-only
~~~

This preparation mode needs no release signing secrets. If a previous preparation stopped during dependency sync, resume it with `bash build.sh --resume --prepare-only`, or `./gradlew prepareOra -Pora.resume=true` when system dependencies are already installed. IDE tasks pass `--skip-system-deps` to avoid interactive sudo prompts. Preparation without `--resume` resets the fetched Chromium checkout before applying patches; preserve edits there before refreshing preparation. A failed preparation does not create a ready marker.

If another Ora build is active, IDE tasks wait for its checkout lock and then recheck readiness. They reuse successful preparation instead of resetting the checkout again. The Build output shows a waiting message while the active build finishes; current bootstrap progress is in `build/ora-build.log`.

Android Studio tasks use one compiler worker by default to limit memory use. Set `-Pora.jobs=4` to increase parallelism on machines with enough memory. If several devices are connected, select one using `-Pora.serial=SERIAL`. ADB is found in `ANDROID_SDK_ROOT`, `ANDROID_HOME`, `~/Android/Sdk`, the fetched Chromium SDK, or `PATH`. Linux, substantial memory/disk space, and network access for the first Chromium sync are still required. The root project exposes Gradle build/run tasks; Android app variants and the native compiler remain managed by Chromium.

### Long builds outside Android Studio

On Linux with a user systemd session, a transient service can own the entire Gradle/compiler process tree. This lets a long build continue after a terminal or chat process exits. Set `JAVA_HOME` to your installed JDK before starting:

~~~sh
mkdir -p build
systemd-run --user --unit=ora-browser-build \
  --working-directory="$PWD" --setenv=JAVA_HOME="$JAVA_HOME" \
  /bin/bash -c 'exec ./gradlew --no-daemon --console=plain -Pora.jobs=1 assembleOra > build/ora-persistent-build.log 2>&1'
systemctl --user status ora-browser-build.service
tail -f build/ora-persistent-build.log
~~~

`--no-daemon` gives the service its own Gradle process tree instead of reusing a daemon owned by another session. To stop this build, run `systemctl --user stop ora-browser-build.service`. Before relaunching a stopped or failed unit with the same name, run `systemctl --user reset-failed ora-browser-build.service`. This transient service does not resume automatically after a reboot or the user service manager exits. Android Studio builds use the same checkout lock and wait if this service is compiling.

## License and attribution

Ora Browser is distributed under GPL-2.0; see [LICENSE](LICENSE). The project builds on Chromium and the GrapheneOS Vanadium patch series. See the upstream projects for their respective licenses and attribution.
