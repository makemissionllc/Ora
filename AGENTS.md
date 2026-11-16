# Agent rules for the Ora browser

Ora is an Android browser (`com.makemission.ora`) built on GeckoView with Kotlin, Jetpack Compose and Material3. These rules apply to every change, whoever or whatever makes it.

## Working rules

1. **Commit after every change.** Each change set gets its own commit on the working branch. Push it after committing. Don't batch unrelated changes into one commit.
2. **Phone first.** Build and verify on the phone before the tablet. Tablet work follows once the phone version works.
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

## Attribution

Commits made by an agent end with:

```
Co-Authored-By: Claude Haiku 5.5 <noreply@anthropic.com>
```
