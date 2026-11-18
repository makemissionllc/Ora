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

- `browser/`: the browser screen, tabs (`BrowserTabs.kt`), the GeckoView engine (`GeckoEngine.kt`), the tab grid and sidebar, the start page, menus, downloads and extensions. Extension installs (from addons.mozilla.org, an address or a file) go through `extensions/ExtensionsRepository.kt` and the sheet in `extensions/ExtensionInstallSheet.kt`.
- `settings/`: `SettingsRepository.kt` holds every persisted setting. `SettingsScreen.kt` is the Settings page, with a category rail on tablet. `DefaultBrowser.kt` checks and requests the system browser role.
- `browser/ExternalLinks.kt` holds a web link sent to Ora by another app, until the browser opens it in a new tab. `MainActivity` fills it.
- `browser/AutofillBridge.kt`: a hidden 1 px view beside Compose that passes the page's login fields to Android's autofill framework. The framework walk stops at Compose's root view, so the GeckoView inside it is otherwise never seen.
- `ui/theme/`: `Theme.kt` holds `BrowserTokens` (dimensions) and `browserColors()` (palettes for Light, Dark and Pure dark). `Motion.kt` holds the shared `OraMotion` values. `ui/PressFeedback.kt` has the `pressFeedback()` modifier that gives touchable elements their press response.
- `profiles/`: `ProfilesRepository` keeps the profile list and the active one; `ProfileSwitcher` is the chip row shown in the tab overview and sidebar. Each profile's bookmarks are stored under its own key.
- `log/`: `BrowserLog`, which every error and browser event goes through. It shows on Settings > Diagnostics. `UsageStats` counts feature use (action names only) on the same page.
- `bookmarks/`: the bookmark store.
- `history/`: `HistoryRepository`, the browsing history store. Each profile has its own list, and a page is recorded when it finishes loading (never in a private tab). `browser/HistoryViews.kt` draws the list in the tablet sidebar and the phone Library.
- `permissions/`: site permission prompts. `SitePermissionDelegate` answers GeckoView's permission requests (location, camera, microphone, notifications) from saved choices, or asks. `SitePrompts` queues the questions and `SitePromptCard.kt` draws the Allow / Not now / Block card. `SitePermissionStore` keeps the choices per profile in `ora_site_permissions` (private tabs save nothing). `AndroidManifest.xml` declares the matching Android permissions.
- `onboarding/` holds the first-launch sequence. `FirstRun.kt` decides what shows before the browser, `NoticeText.kt` has the notice words and `NOTICE_VERSION`, `BeforeYouStartScreen.kt` is the first screen and `NoticeScreen.kt` the full text. After it, `SetupSteps.kt` has one composable per setup step (Welcome, Look, Blocking, Profile, Ready) on the `SetupChrome.kt` scaffold; `browser/FirstUseTips.kt` holds the one-time tips.
- `browser/AppLinks.kt` finds the app for a link and keeps each profile's "Always" sites in `ora_app_links_<profileId>`; `browser/AppLinkCard.kt` is the card that asks, and `BrowserTabs.appLinkDecision` decides whether to open, ask or stay.
- `browser/PopupCard.kt` draws the card that asks before a page opens a window without a tap. `browser/PopupStore.kt` keeps each profile's "Always allow" sites in `ora_popups_<profileId>`, and `popupDecision` in the same file decides open or ask. `GeckoEngine.kt` starts GeckoView with Gecko's own pop-up blocker off, through a config file it writes to app storage, so those pop-ups reach Ora.
- `browser/ErrorPage.kt` builds the card for a failed load: one card per cause, after a connection check (ConnectivityManager). It is a data: URI page, so it needs no network, and its colours are CSS copies of the Mineral values.

## Design sources

Mockups are in the `Designs/` folder and in the Browser Wireframes artifacts named in [AGENTS.md](AGENTS.md).
