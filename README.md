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
- `settings/`: `SettingsRepository.kt` holds every persisted setting. `SettingsScreen.kt` is the Settings page, with a category rail on tablet. `DefaultBrowser.kt` checks and requests the system browser role.
- `browser/ExternalLinks.kt` holds a web link sent to Ora by another app, until the browser opens it in a new tab. `MainActivity` fills it.
- `browser/AutofillBridge.kt`: a hidden 1 px view beside Compose that passes the page's login fields to Android's autofill framework. The framework walk stops at Compose's root view, so the GeckoView inside it is otherwise never seen.
- `ui/theme/`: `Theme.kt` holds `BrowserTokens` (dimensions) and `browserColors()` (palettes for Light, Dark and Pure dark). `Motion.kt` holds the shared `OraMotion` values. `ui/PressFeedback.kt` has the `pressFeedback()` modifier that gives touchable elements their press response.
- `profiles/`: `ProfilesRepository` keeps the profile list and the active one; `ProfileSwitcher` is the chip row shown in the tab overview and sidebar. Each profile's bookmarks are stored under its own key.
- `log/`: `BrowserLog`, which every error and browser event goes through. It shows on Settings > Diagnostics. `UsageStats` counts feature use (action names only) on the same page.
- `bookmarks/`: the bookmark store.
- `history/`: `HistoryRepository`, the browsing history store. Each profile has its own list, and a page is recorded when it finishes loading (never in a private tab). `browser/HistoryViews.kt` draws the list in the tablet sidebar and the phone Library.
- `permissions/`: site permission prompts. `SitePermissionDelegate` answers GeckoView's permission requests (location, camera, microphone, notifications) from saved choices, or asks. `SitePrompts` queues the questions and `SitePromptCard.kt` draws the Allow / Not now / Block card. `SitePermissionStore` keeps the choices per profile in `ora_site_permissions` (private tabs save nothing). `AndroidManifest.xml` declares the matching Android permissions.

## Design sources

Mockups are in the `Designs/` folder and in the Browser Wireframes artifacts named in [AGENTS.md](AGENTS.md).
