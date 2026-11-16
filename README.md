# Ora

Ora is an Android browser (`com.makemission.ora`) built on GeckoView, with Kotlin, Jetpack Compose and Material3. It has a phone layout and a tablet layout, and it runs the same engine on both.

## Documents

| File | What it is for |
| --- | --- |
| [README.md](README.md) | What Ora is, how to build and run it, and where things live. |
| [CHANGELOG.md](CHANGELOG.md) | What changed, grouped by date. The in-app "What's new" page mirrors the same entries. |
| [PROGRESS.md](PROGRESS.md) | What is done, in progress, and pending, plus known issues and verification results. |
| [AGENTS.md](AGENTS.md) | Rules for every change: commits, devices, design, motion, logging. |

Keep these four up to date with every change. [AGENTS.md](AGENTS.md) has the rule.

## Build

```bash
JAVA_HOME=/opt/android-studio/jbr ./gradlew assembleDebug
```

The build needs compileSdk 37.2 (for GeckoView 160), Kotlin 2.4.21, and Android Gradle Plugin 9.4.1. The versions are pinned in `gradle/libs.versions.toml`.

## Run on a device

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell monkey -p com.makemission.ora -c android.intent.category.LAUNCHER 1
```

`install -r` returns to the home screen, so relaunch with `monkey` afterwards.

## Code layout

All paths are under `app/src/main/java/com/makemission/ora/`.

- `browser/`: the browser screen, tabs (`BrowserTabs.kt`), the GeckoView engine (`GeckoEngine.kt`), the tab grid and sidebar, the start page, menus, downloads and extensions.
- `settings/`: `SettingsRepository.kt` holds every persisted setting. `SettingsScreen.kt` is the Settings page, with a category rail on tablet.
- `ui/theme/`: `Theme.kt` holds `BrowserTokens` (dimensions) and `browserColors()` (palettes for Light, Dark and Pure dark). `Motion.kt` holds the shared `OraMotion` values.
- `log/`: `BrowserLog`, which every error and browser event goes through. It shows on Settings > Diagnostics.
- `bookmarks/`: the bookmark store.

## Design sources

Mockups are in the `Designs/` folder and in the Browser Wireframes artifacts named in [AGENTS.md](AGENTS.md).
