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
- Tablet tab search uses Chromium’s existing tab-search surface and preserves named, colored tab groups.
- Reader mode keeps Chromium’s text-size and theme controls, with Ora light/dark colors and a readable tablet text column.

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

The build rewrites Vanadium patch text in the submodule checkout to apply Ora branding, then applies those patches to Chromium. Building therefore modifies files inside the local submodule checkout. Review the resulting Git state before committing or updating submodule content.

## License and attribution

Ora Browser is distributed under GPL-2.0; see [LICENSE](LICENSE). The project builds on Chromium and the GrapheneOS Vanadium patch series. See the upstream projects for their respective licenses and attribution.
