# Agent rules for the Ora browser

Ora is an Android browser (`com.makemission.ora`) built on GeckoView with Kotlin, Jetpack Compose and Material3. These rules apply to every change, whoever or whatever makes it.

## Working rules

1. **Commit after every change.** Each change set gets its own commit on its feature branch (see Branch rules). Push it after committing. Don't batch unrelated changes into one commit.
2. **Phone and tablet together.** Every change is implemented and verified on both the phone and the tablet in the same change set. Don't ship a feature to one form factor and leave the other for later.
3. **Look before you tap.** Take a screenshot of the device before every tap, and tap only at coordinates read from that screenshot. Never tap blind.
4. **Keep temp files out of the repo.** Screenshots and scratch files go in the scratchpad directory, or are deleted before committing.
5. **Every change adds one quality-of-life feature.** Each change must ship at least one small usability improvement alongside the main work, such as a clearer empty state, a shortcut, a confirmation for destructive actions, or better feedback. Name it in the commit message.

## Design rules

6. **Follow the mockups.** New UI matches the Browser Wireframes/Mockups design artifacts (`276c32f4-41c4-484c-aa26-057985be5e83` for the browser, `5927e31d-3f42-4276-bdd3-393ef09c2c98` earlier). When a mockup and this file disagree, the mockup wins for visuals, and this file wins for process.
7. **Use the palette, not literal colors.** Read colors only through `browserColors()` in `ui/theme/Theme.kt`. Every appearance mode (Light, Dark, Pure dark) must look right.
8. **Dimensions live in `BrowserTokens`.** Don't hard-code spacing or sizes in screens when a token fits.
9. **Settings go in `SettingsRepository`** and get a row on the Settings page. Don't add ad hoc preferences.

## Motion rules

10. **Every component morphs into another.** Opening, closing, or replacing a component animates from the element that triggered it, and back. A new surface should grow out of its origin (for example the tabs button) and shrink back into it. Don't pop components in or out.
10a. **A new component morphs out of the one that led into it.** When a component is designed, say which component opens it (the "previous component") and make it grow out of that one's shape and position, and fold back into it on close. The model is the compact address bar turning into the find bar: the same capsule changes its contents and width in place, and nothing pops in beside it. So a suggestions list grows out of the address capsule, a date picker out of the field that asked for it, a confirmation card out of the button that was tapped. Rules for doing it:
    - Name the previous component in the commit message and in `PROGRESS.md`. If there is none (the first screen, a system-driven prompt), say so.
    - The new surface starts at the previous component's bounds (width, height, corner radius, colour) and animates to its own; its content fades in after the shape has mostly arrived, and fades out before the shape folds back.
    - Use `AnimatedContent`, `AnimatedVisibility` or shared-bounds style animation with `OraMotion`. Closing reverses the opening toward the same origin, including when the user dismisses by tapping outside or pressing back.
    - Where Android itself draws the surface (the system file picker, a permission dialog) a morph is not possible: record it as an exception in `PROGRESS.md`.
10b. **Every new or changed surface animates in from where it came from, with no exceptions for small work.** Whenever you add or edit a component, page, sheet, dialog, list or feature file, its entry and exit must grow out of and fold back into the element that led to it, as in rule 10a. This applies to edits as well as new files: if you touch a surface that pops in or out, fix it in the same change. A page opened from a list row grows from that row; a category page grows from the category that was tapped and returns to it; a sheet grows from its button. The commit message and `PROGRESS.md` name the origin (or say there is none), and a change that adds or edits a surface without an origin animation is not finished. Only Android-drawn surfaces are exempt, and each is recorded in `PROGRESS.md`.
11. **Animations are consistent.** Use the shared motion values, not per-screen numbers:
    - Overlay enter: fade in over 220 ms, scale from 0.88 over 280 ms, origin at the trigger.
    - Overlay exit: fade out over 160 ms, scale to 0.88 over 200 ms, same origin.
    - Toolbar fold: fade plus expand or shrink toward the bottom edge over 160–260 ms.
    - Use one easing per direction. Don't mix tween shapes within one transition.
12. **Animate with Compose primitives** (`AnimatedContent`, `AnimatedVisibility`, `togetherWith`). Don't hand-roll timers or `Thread.sleep`.

## Code rules

13. Keep changes small and focused. Don't refactor unrelated files in the same change.
14. Run `./gradlew assembleDebug` before committing. Don't commit code that doesn't compile.
15. Write commit messages in the imperative mood, with a short summary and a body that lists the morph or animation change and the quality-of-life feature.
16. Don't commit build outputs, `.idea/`, or local device settings.

## Logging rules

17. **Log every error and browser process.** Route each error, GeckoView event (page load, load error, crash, permission request, extension install or enable failure, navigation) and app process (startup, settings change, tab open or close) through `BrowserLog`. Don't leave a failure silent or only in `Log.e` without a `BrowserLog` entry.
18. **Logs are readable from the app.** Entries live in `BrowserLog` with a level, a tag and a timestamp, and show on the Diagnostics page in Settings, where they can be copied or cleared. A new feature that can fail adds its log calls in the same change.

## Documentation rules

19. **Keep the four project documents current.** `README.md`, `CHANGELOG.md`, `PROGRESS.md` and this file are the record of the project. Every prompt that changes code, docs or rules updates them in the same change set, not later:
    - `CHANGELOG.md`: add one user-facing entry under *Unreleased* for each addition, improvement or fix, with its commit hash. Group by date, and use *Added*, *Changed*, *Fixed* or *Removed*.
    - `PROGRESS.md`: move items between *Done*, *In progress*, *Pending* and *Known issues*. Add a row to the verification log with the devices checked and what was seen on each. Mark a device *Not checked* if it was not.
    - `README.md`: update build, run, or code-layout notes when they change.
    - `AGENTS.md`: record any new rule the user gives, in the right section.
20. **Changelog stays in step with the app.** The in-app "What's new" page (Settings > About) reads the same entries as `CHANGELOG.md`. When an entry is added to `CHANGELOG.md`, add it to the in-app list too, in the same change set. Write entries in plain words for a user, not for a developer.
21. **Record only what was checked.** Do not write a device as verified in `PROGRESS.md` unless a screenshot or test was seen on that device. If a check was skipped or blocked, say so in the log.

## Status bar rules

22. **The status bar is frozen.** Do not change the status bar, its implementation, or its fade. That covers:
    - `browser/StatusBar.kt`: `StatusBarOverlay` (the solid strip and its scroll fade), `sampleTopRow` (the top-strip tint, read from the page's top pixels) and `isLightStatusStrip` (the icon light/dark decision).
    - In `browser/BrowserScreen.kt`: the strip colour block (`stripColor`, `barColor`, the `isAppearanceLightStatusBars` effect and the `capturePixels` sampling that sets `selected.topColor`), the two `StatusBarOverlay` calls, and the page inset under the bar (`phoneZoneHeight` and the status-bar insets around the page).
    - `BrowserTokens.StatusScrollFade` (the 84 dp fade) in `ui/theme/Theme.kt`.
    - `topColor` on the tab in `browser/BrowserTabs.kt`.
    There is no `ThemeColor.kt` or `assets/theme-color/` on this line; the page's own `theme-color` is not read. Keep all of it exactly as it is in the current code.
23. **Ask before touching the status bar.** If a task appears to need a status-bar change, stop and check with the user first, every time, even when the change looks small. Do not edit status-bar code without that yes.

## Layout rules (protected)

24. Overlays and panels start below the floating Compact row and never clip their own content. Too tall means scroll, never cut.
25. The tablet sidebar holds its own sidebar, back and forward buttons. The floating row never draws over the sidebar.
26. Find in page replaces the address capsule in place.
27. Surfaces are flat. No grain, noise, texture or ripple. The press response is a scale only: 94% on small controls, 97.5% on wide rows (over 220 dp), none on full-screen areas (`pressFeedback()`). Put `pressFeedback()` before `.clip/.background` so the whole tile shrinks, and never on a scrim or a sheet body.
28. Dashed lines mean private or temporary and are used for nothing else.
29. Tints change Ora's surfaces only, never web content.

## Branch rules

Every feature lives on its own branch, so one feature can be fixed or reverted without touching another.

30. **`main` is the stable line; `develop` is the working branch.** `develop` holds every finished feature and is what new branches start from. Nobody commits feature work straight to `develop` or `main`.
31. **One branch per feature, named `<type>/<short-kebab-name>`.** Types: `feature/` (new work, such as `feature/phone-overview-search`), `fix/` (a bug, `fix/download-resume-referer`), `docs/` (documents only, `docs/branch-workflow`), `chore/` (build or tooling). The name says what the branch is for; lower-case, no dates, no issue-less words like `stuff` or `wip`. A branch holds that one feature and nothing else.
32. **Start from an up-to-date `develop`.** `git checkout develop && git pull`, then `git checkout -b feature/<name>`. Before starting, check `git status`: if there are uncommitted changes that belong to another feature, commit them on their own branch first, never carry them into a new one.
33. **Changing an existing feature means going back to its branch.** Check out the feature's branch (`git checkout feature/<name>`, recreate it from the merge commit in `git log` if it was deleted), make the change there, and merge again. Don't patch a feature from a different feature's branch.
34. **Merge back with a merge commit, after the checks.** When the feature is done (rule 14 build passes, devices checked or marked Not checked, documents updated), merge it into `develop` with `git merge --no-ff feature/<name>`, push `develop`, and keep the feature branch on GitHub so it can be reopened. Merge `develop` into the feature branch first if `develop` moved, and fix conflicts on the feature branch.
35. **`develop` reaches `main` only when it builds and its features are checked.** Merge with `--no-ff` and push. Tag a release on `main`.
36. **Commit hashes in the documents are the feature branch's commits.** The CHANGELOG and in-app entries name the commit on the feature branch (the merge commit is not used), so they stay valid after the merge. Never force-push `develop` or `main`.

## Building new components, pages and features

Follow this recipe so new work looks and moves like the rest of Ora. The examples name real code to copy from.

### Structure
- **One file per surface** in the package that owns it (`browser/` for browser UI, `extensions/`, `settings/`, and so on). A surface is a screen, sheet, panel or dialog, for example `GroupSheets.kt` or `PrivateSpacePanel.kt`. Small helpers it alone uses stay `private` in that file; shared pieces (`PillButton`, `SidebarSearch`, `SegmentLabel`) stay in `TabViews.kt` or move to `ui/` only when a second screen needs them.
- **State lives in a repository or store**, never in a composable. Per-profile data is keyed by profile id in its own preferences file (`ora_tab_groups_<profileId>`, bookmarks and history the same way). A composable holds only view state (`remember`, `rememberSaveable`): which row is open, what was typed.
- **Preferences** go through `SettingsRepository` with a row on the Settings page (rule 9). Saved data a feature adds (such as a tab's group) is written next to the data it belongs to and read with a default, so old saves still load.
- **Phone and tablet from one component** where it can be. Take a `tablet: Boolean` and change the layout, not the logic: the tablet shows list and detail side by side (`ExtensionsScreen`), the phone opens the detail as its own screen. If they truly differ, share the state and write two small layouts.

### Look
- Colours: `browserColors()` only. A new surface reads `LocalBrowserColors`, so it follows Light, Dark, Pure dark, profile tints and the Private space (`privateBrowserColors`) without extra code. A decorative colour with no palette slot is added once to `Mineral` (as the group colours were), never inline. Destructive actions use `Mineral.Ember`.
- Sizes and radii: `BrowserTokens`. Cards and rows use `CardRadius` or `RadiusSmall`; add a token before adding a literal that appears twice.
- Text: Instrument Sans through the theme. Titles 22 sp bold, rows 15 sp medium, secondary lines 12 to 14 sp in `SecondaryText`, small section labels 12 to 13 sp semibold in `SecondaryText`.
- Controls: pills (`PillButton`, `SegmentLabel`) for choices, `Switch` for on/off, a text button in Ember for a destructive action. Primary action is the Chalk (`Accent`) fill; the secondary is a quiet `AddressBarBackground` row. Rows are at least 44 dp tall.
- Surfaces are flat (rule 27). Use `Modifier.clickable`, and the 94% press scale where a tile has one. No ripple, no shadow, no gradient.
- Dashed outline (`Modifier.dashedOutline`) only for private or temporary things (rule 28).

### Motion
- **Pick the right primitive.** A sheet or overlay that grows from a button: `AnimatedVisibility` or `AnimatedContent` with `OraMotion.overlayFadeIn/ScaleIn` and `overlayFadeOut/ScaleOut`, scale `OraMotion.MORPH_SCALE`, origin at the trigger (`TransformOrigin` from the trigger's position). A list that opens a detail on phone: `AnimatedContent` with `fadeIn + scaleIn` `togetherWith` `fadeOut + scaleOut` (`ExtensionsScreen`). A folding section: `AnimatedVisibility(fadeIn(OraMotion.enter()) + expandVertically(OraMotion.enter()))`, and the matching exit with `shrinkVertically`. A chevron or icon that turns: `animateFloatAsState(target, OraMotion.enter())` and `rotate`.
- **Origin and return.** Whatever opens something is also where it closes to. Dialogs that confirm a destructive action grow out of the button that asked (the Clear history card). A standard `AlertDialog` is fine only when there is no on-screen trigger to morph from, and says so in `PROGRESS.md`.
- **One easing per direction**, taken from `OraMotion`. Never write `tween(...)` or a new duration in a screen. If a feature needs a new motion, add it to `Motion.kt` with a name, then use it.
- **Drag and drop** stays inside Ora (`TabDrag.kt`): `tabDragSource` and `tabDropTarget`. A target highlights while something hovers over it. A long press held still keeps its old meaning, so a new gesture never removes an existing route.
- **Reduce surprises.** Nothing appears or disappears without a transition; a list that changes keeps item keys stable so rows move instead of jumping.

### Behaviour
- **Every action that cannot be undone asks first** and says what it will do and how many items it touches ("Close group" says how many tabs close). Prefer an undo for small losses (closed tab) and a confirmation for big ones.
- **Empty states say what to do next**, in one plain sentence ("No bookmarks yet. Use the bookmark button to save a page.").
- **Search fields filter in place** as you type, are case-insensitive, and match title and address. Say "Nothing matches" when empty.
- **Private tabs** never reach disk, groups, thumbnails, history or the extension list unless the user allowed it. Check `isPrivate` before storing or showing anything.
- **Don't add a control that does nothing.** If the platform has no API for a setting, leave it out and record it under *Not done* in `PROGRESS.md`.
- **Log it** (rules 17 and 18): `BrowserLog.info("tag", ...)` for each state change a user caused, `warn` or `error` with the throwable for failures, in the same change as the feature.

### Done checklist for a new surface
1. Reads colours and sizes from the theme and tokens; works in Light, Dark, Pure dark and the Private space.
2. Phone and tablet layouts both exist and were looked at (rule 2); a device not checked is written as *Not checked* (rule 21).
3. Opens and closes with the shared motion from the element that led to it (rules 10, 10a and 10b), whether the surface is new or only edited; has a stable-key list.
4. Empty state, confirmation for destructive actions, and the one named quality-of-life feature.
5. State in a repository, a Settings row if it has a preference, `BrowserLog` calls.
6. `./gradlew assembleDebug` passes (check the APK time), then CHANGELOG, `Changelog.kt`, PROGRESS and README updated, commit with the hash recorded, pushed on the feature branch, then merged into `develop` (rule 34).

## Attribution

Commits made by an agent end with:

```
Co-Authored-By: Claude Haiku 5.5 <noreply@anthropic.com>
```
