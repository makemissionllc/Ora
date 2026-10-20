# Changelog

Changes are recorded here for each repository update.

## 2026-10-20

### Changed

- Set Android Studio’s Gradle task to one Ninja worker by default and documented how to increase it, after the four-worker build was terminated by the host OOM killer. Resumed the cached compile with `-Pora.jobs=1` and updated the persistent service example.

## 2026-10-19

### Changed

- Documented how to run long arm64 Gradle builds in a user systemd service with a dedicated Gradle process tree, inspect their logs, and stop or restart them. Resumed the cached build through this service after chat interruption stopped the earlier detached job.

## 2026-10-18

### Added

- Added a pinned Gradle wrapper and Android Studio Gradle project with prepare, arm64 assemble, ADB install/launch, diagnostics, and Chromium Java project generation tasks.
- Added shared Android Studio build-only and install/launch Run configurations and documented first-time setup, incremental builds, output location, device selection, and signing behavior.

### Changed

- Removed obsolete identical Ora XML copies from Chromium’s main resource tree during sync to fix its unlisted-resource packaging failure; separately edited copies are preserved with a clear error.
- Fixed Ora theme, toolbar, and shape XML staging paths to match Chromium’s GN extension-resource list. Added resource validation and incremental syncing before Android Studio builds.

- Added Ora light and dark Material 3 color roles and connected them through the existing Chromium extension-resource hook. The Ora palette disables Android dynamic colors so the interface uses the wireframe colors.
- Enabled Chromium’s phone bottom toolbar by default, kept its menu button available on the browser bar and start page, and defaulted the existing Address bar preference to bottom. Added tablet-specific toolbar, address pill, and tab strip sizing and placed the tab strip below the toolbar.
- Matched shared Chromium card, settings-card, and bottom-sheet corner radii to the Ora wireframes.
- Matched tab grid cards, thumbnails, highlights, and tab overview dialogs to the Ora corner scale.
- Enabled Chromium’s desktop Android tab-search surface for searching open tabs on tablets.
- Applied the Ora reader palette and constrained the article column on tablet-sized viewports; existing reader text-size and theme controls remain available.
- Aligned bright, dim, and elevated surface roles with Ora colors so settings, menus, and dark sheets use the intended surface palette. Documented the split-view engine TODO and the existing Chromium screens reused by Ora.
- Fixed Android Studio preparation failing when the bootstrap already holds the checkout lock. IDE preparation and compilation now wait, and preparation rechecks readiness under the lock before resetting sources.
- Added preparation-only, resume, and skip-system-dependency modes to `build.sh`; preparation no longer needs release signing inputs. Added a configuration fingerprint to reuse prepared sources and restored the interrupted dependency backup before resuming.
- Kept native build output separate from Gradle’s APK output, ignored local Gradle/build files, and anchored shell helper paths to their source files for IDE and terminal invocation.

## 2026-10-17

### Changed

- Rebranded the generated Android app as Ora Browser and set its application ID to `com.makemission.ora`.
- Updated build-time patch rewriting and Chromium resource paths to use the Ora namespace.
- Updated the bundled extension output name and filter-list destination to use the Ora namespace.
- Replaced inherited Titanium project documentation and listing text with Ora Browser information. Documented the separately signed companion extension’s upstream publisher.
- Updated the issue template to link to the Ora repository.
- Added repository guidance requiring a review of cumulative changes and a changelog update for each future work prompt.
- Made the build script fail fast, block concurrent builds, reuse existing checkouts, recover interrupted patch application, and apply Ora-rewritten patches from a temporary copy. Seeded gclient’s local remote-head reference to avoid a stalled network lookup. Limited default Ninja concurrency to four jobs, configurable with `ORA_BUILD_JOBS`; updated the repository instructions and build documentation to match.
- Skipped the unused multi-gigabyte Android WebView CTS test archive during browser dependency sync by default, with `ORA_INCLUDE_WEBVIEW_CTS_ARCHIVE=1` to opt in. The build restores Chromium’s original `DEPS` file on both success and failure.
- Also skipped Chromium’s Android emulator package for physical-device browser builds by default; `ORA_INCLUDE_ANDROID_EMULATOR=1` opts in. The README documents both build options.
