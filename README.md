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

## Branches and releases

`main` is the stable line and `develop` is the working branch. Each feature or fix gets its own branch off `develop`, named `feature/<name>`, `fix/<name>`, `docs/<name>` or `chore/<name>`. Changes to a feature go back on that feature's branch, then it is merged into `develop` with `git merge --no-ff`. `develop` is merged into `main` when it builds and is checked. [AGENTS.md](AGENTS.md) has the full Branch rules (30 to 36).

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

- `browser/`: the browser screen, tabs (`BrowserTabs.kt`), the GeckoView engine (`GeckoEngine.kt`), the tab grid and sidebar, the start page, menus, downloads and extensions. Extension installs (from addons.mozilla.org, an address or a file) go through `extensions/ExtensionsRepository.kt` and the sheet in `extensions/ExtensionInstallSheet.kt`. `browser/MainBrowserHandoff.kt` backs the ⋯ menu row that opens an https page in another browser when a passkey sign-in fails in Ora.
- `settings/`: `SettingsRepository.kt` holds every persisted setting. `SettingsScreen.kt` is the Settings page, with a category rail on tablet. `DefaultBrowser.kt` checks and requests the system browser role.
- `browser/ExternalLinks.kt` holds a web link sent to Ora by another app, until the browser opens it in a new tab. `MainActivity` fills it.
- `browser/AutofillBridge.kt`: a hidden 1 px view beside Compose that passes the page's login fields to Android's autofill framework. The framework walk stops at Compose's root view, so the GeckoView inside it is otherwise never seen.
- `ui/theme/`: `Theme.kt` holds `BrowserTokens` (dimensions) and `browserColors()` (palettes for Light, Dark and Pure dark). `Motion.kt` holds the shared `OraMotion` values. `ui/PressFeedback.kt` has the `pressFeedback()` modifier that gives touchable elements their press response.
- `profiles/`: `ProfilesRepository` keeps the profile list and the active one; `ProfileSwitcher` is the chip row shown in the tab overview and sidebar. Each profile's bookmarks are stored under its own key.
- `log/`: `BrowserLog`, which every error and browser event goes through. It shows on Settings > Diagnostics. `UsageStats` counts feature use (action names only) on the same page.
- `bookmarks/`: the bookmark store.
- `browser/KeyShortcuts.kt`, `browser/ShortcutsCard.kt`: physical keyboard shortcuts (`MainActivity.dispatchKeyEvent` feeds `KeyShortcuts`; BrowserScreen acts on them) and the card that lists them.
- `settings/SettingsRepository.kt` `BarButton`: the action behind the round button in the Compact bar (picked in Settings > Tabs).
- `browser/QuickSwitcher.kt`: the Ctrl+K quick switcher and its pure `quickMatches` ranking.
- `settings/SavedSessions.kt` and `browser/SessionsDialog.kt`: named sets of open tabs (per profile, never private tabs) and the dialog that saves, opens and deletes them.
- `settings/TutorialScreen.kt`: the interactive tutorial (lessons, practice areas, saved progress).
- `settings/SearchShortcuts.kt`: address bar search keywords (`resolve`, built-in list, your own saved in `SettingsRepository`).
- `settings/SiteTextSizes.kt`: per-site text sizes (`siteKeyFor`, saved by host).
- `browser/PagePdf.kt`: Save as PDF (GeckoView `saveAsPdf`, written to Downloads/Ora through MediaStore, named from the page title).
- `readinglist/`: `ReadingListRepository`, the pages saved to read later, one list per profile (`ora_reading_list`, never from a private tab). `browser/ReadingListViews.kt` draws it in the phone Library and the tablet sidebar (a pinned row above Private tabs).
- `labs/`: `LabFeature` and the `Labs` flags (Settings > Labs, opt-in security features, off by default), `LookalikeCheck`, `SiteStats` and `SiteStatsCard` (the padlock's stats card: connection, certificate, blocked trackers), `AppFingerprint` (SHA-256 of the installed package and signer, shown in Diagnostics), `ScamPatterns` (scam and site-builder heuristics, fed by `extensions/SiteScanBridge` and `assets/sitescan/`) and `SiteFindingsCard` (the card that lists what Ora noticed about a site: lookalike address, unencrypted page, mixed content, certificate problems, redirect chains). `log/LogPrivacy.siteLabel` swaps site names for salted hash codes when the Hash sites lab is on. `BrowserScreen` applies the Gecko settings; `BrowserTab` reads the flags for the user agent, lookalike and scenario log; `GeckoEngine.labPrefs` writes the start-up referrer prefs.
- `history/`: `HistoryRepository`, the browsing history store. Each profile has its own list, and a page is recorded when it finishes loading (never in a private tab). `browser/HistoryViews.kt` draws the list in the tablet sidebar and the phone Library.
- `permissions/`: site permission prompts. `SitePermissionDelegate` answers GeckoView's permission requests (location, camera, microphone, notifications) from saved choices, or asks. `SitePrompts` queues the questions and `SitePromptCard.kt` draws the Allow / Not now / Block card. `SitePermissionStore` keeps the choices per profile in `ora_site_permissions` (private tabs save nothing). `AndroidManifest.xml` declares the matching Android permissions.
- `onboarding/` holds the first-launch sequence. `FirstRun.kt` decides what shows before the browser, `NoticeText.kt` has the notice words and `NOTICE_VERSION`, `BeforeYouStartScreen.kt` is the first screen and `NoticeScreen.kt` the full text. After it, `SetupSteps.kt` has one composable per setup step (Welcome, Look, Blocking, Profile, Ready) on the `SetupChrome.kt` scaffold; `browser/FirstUseTips.kt` holds the one-time tips.
- `browser/AppLinks.kt` finds the app for a link and keeps each profile's "Always" sites in `ora_app_links_<profileId>`; `browser/AppLinkCard.kt` is the card that asks, and `BrowserTabs.appLinkDecision` decides whether to open, ask or stay.
- `browser/PopupCard.kt` draws the card that asks before a page opens a window without a tap. `browser/PopupStore.kt` keeps each profile's "Always allow" sites in `ora_popups_<profileId>`, and `popupDecision` in the same file decides open or ask. `GeckoEngine.kt` starts GeckoView with Gecko's own pop-up blocker off, through a config file it writes to app storage, so those pop-ups reach Ora.
- `browser/ErrorPage.kt` builds the card for a failed load: one card per cause, after a connection check (ConnectivityManager). It is a data: URI page, so it needs no network, and its colours are CSS copies of the Mineral values.
- `downloads/`: the download list and engine. `DownloadsRepository.kt` owns the list and the saved queue, and `DownloadEngine.kt` fetches a file in parallel parts with retries. `DownloadStore.kt` and `DownloadDatabase.kt` keep the queue in Room. `DownloadService.kt` and `DownloadJobService.kt` keep downloads running in the background; a restarted job builds the repository itself (`DownloadsRepository.ensure`), unfinished downloads continue after a restart unless the user paused them, and a download that ran out of quick retries is tried again every 2 minutes (up to 30 tries without new bytes). `DownloadSafety.kt` holds the file-type, plain-http and checksum checks, and `DownloadsScreen.kt` draws the Downloads page on phone and tablet.

## Design sources

Mockups are in the `Designs/` folder and in the Browser Wireframes artifacts named in [AGENTS.md](AGENTS.md).

- `browser/OraSheet.kt`: the shared phone bottom sheet (spring rise, finger-following drag, half to full height). New sheets use it, not `ModalBottomSheet`. Spring values are `OraMotion.sheetSpring/sheetSnap/sheetExit`.

- Hints: `OraSheet` takes a `hintId`; `SettingsRepository.shouldShowHint/markHintSeen` track which cues played, and the Hints switch in Settings turns them off or replays them. `LocalHints` carries the repository to composables.

## Fonts

Ora's own screens can use Instrument Sans, Inter, Manrope, DM Sans, Newsreader, Fraunces or JetBrains Mono (Settings > Appearance > Font). The fonts are variable TTFs in `app/src/main/res/font`, taken from the Google Fonts repository under the SIL Open Font License 1.1. The catalog is `ui/theme/AppFonts.kt`; add a font by dropping a TTF in `res/font` and adding an `AppFont` entry.

The same font can be applied to websites with Settings > Appearance > Font > Use font on websites. That is the built-in extension in `app/src/main/assets/webfont` (it holds its own copy of the font files, so keep them in step with `res/font`) and `extensions/WebFontBridge.kt`.

## Preloading

`browser/Preloader.kt` gets a head start on the site you are likely to open next. While you type in the address bar (`AddressBar` in `BrowserScreen.kt`), once typing pauses for 250 ms the autocompleted domain, or else the first suggestion that is a site, has its address looked up in the background. Android keeps DNS answers system-wide, so GeckoView finds it ready. After the lookup, `Preloader.connect` sends a HEAD request to the site root through `GeckoWebExecutor`, so Gecko opens (and keeps) the TCP and TLS connection the tab then reuses; nothing is stored. With the "Load the top page ahead" switch on (`SettingsRepository.loadAhead`, default off) `Preloader.prefetch` also reads up to 512 KB of the top suggestion's page into Gecko's cache, only for https pages without a query, fragment or action word in the path (`prefetchUrl`). Tapping the address bar with nothing typed calls `Preloader.warmTop` for the three best sites of `HistoryEntry.frecency` (`history/HistoryRepository.kt`: visit count `n` in the saved JSON, older saves read as 1, weighted by how recent the last visit was; `topSites` keeps one per site). Frequent pages also join the address suggestions, never in a private tab. It is skipped for private tabs, search words, IP addresses and local names, and each site is looked up once every five minutes. The choice (Off, Wi-Fi only, the default, or Always) is `SettingsRepository.preloadMode`, in Settings > Tabs > Preloading. Wi-Fi only skips metered networks and Data Saver. At start-up `GeckoEngine.configText` also turns on Gecko's own speculative connections (`network.predictor.enabled`, `network.http.speculative-parallel-limit`, DNS prefetch) in `gecko-config.yaml` when preloading is allowed then, so a change of the setting takes full effect on the next start. Lookups are logged by domain only (tag `preload`).

## File uploads

GeckoView only shows a file chooser if the session has a `PromptDelegate`. `browser/FilePicker.kt` holds `FilePromptDelegate` (set on every tab in `BrowserTab`) and `FilePicker`, which launches Android's `ACTION_GET_CONTENT` through a launcher registered in `MainActivity.onCreate`, with the page's accepted types and multiple choice when asked. Gecko cannot read the picker's content addresses (it answers "Unrecognized path"), so each chosen file is first copied to `cache/uploads/<n>/<name>` on a background thread and the copy is handed over; copies older than an hour are deleted at the next pick. The page's accept list is turned into MIME types by `FilePicker.acceptedTypes` (extensions like `.jpg` are looked up with `MimeTypeMap`, `*/*` means no filter), and short messages report Preparing and Attached. No choice answers the page with a dismiss. Logged under tag `upload`. 

## Page prompts

`PagePromptDelegate` (browser/PagePrompts.kt) extends `FilePromptDelegate` and queues alerts, confirms, text boxes, select lists, date and time pickers, leave-page and resend warnings in `PagePrompts`. `PagePromptHost` (browser/PagePromptHost.kt) shows the first one in an `OraDialog`, so it grows out of the last press. Texts are never logged, only the kind (tag `prompt`).

## Full-screen video player

`browser/VideoPlayerControls.kt` draws Ora's own controls over a page's full-screen video (top bar, centre play with 10 s skips, progress, lock, speed, fit/fill, mute, rotate, picture-in-picture, double-tap skip, hold for 2×, brightness and volume swipes). The video itself is driven by the bundled `video-control` WebExtension (`assets/videoctl`, content script in every frame) through `extensions/VideoControlBridge.kt`: the page reports time, length, pause, speed, mute and size; Ora sends seek, play, pause, rate, mute, fit and exit. While a video is full screen the page's top inset is zero so it fills the display. Events are logged under tag `video`.

## Slow page tips

`browser/SlowLoadTip.kt`: `SlowLoadTipHost` waits 8 s of one page loading, then `slowLoadAdvice` picks the tip from the network state, the load progress and Data Saver (offline, no answer yet, Data Saver on, or mostly loaded). It grows from the bottom edge with the shared overlay motion, offers Keep waiting and Try again or Stop loading, and is logged by domain (tag `tips`). The switch is `SettingsRepository.slowLoadTips` (Settings > Search).

## Search suggestions

`browser/SearchSuggest.kt` asks the default search engine for phrases as you type, once typing pauses for 120 ms (`SearchEngine.suggestTemplate` holds each engine's endpoint; all answer in the OpenSearch shape the parser reads). `withQuerySuggestions` in `AddressSuggestions.kt` puts those phrases above up to two matching bookmarks or open tabs; with no phrases the old "Search for" row stays. Each row shows a search icon and the part after what was typed in bold, and the arrow puts the phrase in the field. The text typed is sent to the engine, so it is off in private tabs and has its own switch (`SettingsRepository.searchSuggestions`, Settings > Search). Answers are kept in memory only (the last 40), and no search text is logged.

The suggestions list is drawn by the address field in `BrowserScreen.kt` as a non-focusable `Popup` holding an `AnimatedVisibility` (overlay fade plus `expandVertically` from the top, `OraMotion`), in the capsule's colour and width, so it unfolds out of the capsule and folds back into it. It keeps the last rows while folding.
