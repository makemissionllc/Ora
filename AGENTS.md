# Agent rules for the Ora browser

Ora is an Android browser (`com.makemission.ora`) built on GeckoView with Kotlin, Jetpack Compose and Material3. These rules apply to every change, whoever or whatever makes it.

## Working rules

1. **Commit after every change.** Each change set gets its own commit on the working branch. Push it after committing. Don't batch unrelated changes into one commit.
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

22. **The status bar is frozen.** Do not change the status bar, its implementation, or its fade. That covers `StatusBar.kt`, the top-strip tint and theme-colour sampling (`ThemeColor.kt`, `assets/theme-color/`), the status-icon light/dark switching, the 84 dp scroll fade, and the page inset under the bar. Keep all of it exactly as it is in the current code.
23. **Ask before touching the status bar.** If a task appears to need a status-bar change, stop and check with the user first, every time, even when the change looks small. Do not edit status-bar code without that yes.

## Layout rules (protected)

24. Overlays and panels start below the floating Compact row and never clip their own content. Too tall means scroll, never cut.
25. The tablet sidebar holds its own sidebar, back and forward buttons. The floating row never draws over the sidebar.
26. Find in page replaces the address capsule in place.
27. Surfaces are flat. No grain, noise, texture or ripple. The press response is the 94% scale only.
28. Dashed lines mean private or temporary and are used for nothing else.
29. Tints change Ora's surfaces only, never web content.

## Attribution

Commits made by an agent end with:

```
Co-Authored-By: Claude Haiku 5.5 <noreply@anthropic.com>
```
