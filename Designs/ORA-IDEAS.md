# Ora ideas

Ideas for Ora's custom features, grouped by priority. Nothing here is built yet. Each idea moves to PROGRESS.md *In progress* only when someone starts it.

Sizes: S is a small change to one screen; M touches a few screens or a store; L needs a new subsystem or a platform check first.
"Unknown, needs a check" means the API was not confirmed in GeckoView 160 or the Android SDK. Do not build on a guess.

## Rules every idea follows

1. **On-device only.** Nothing is sent to a server, and nothing leaves the phone or tablet unless the user shares it.
2. **Nothing from private tabs.** No idea reads, stores, logs or shows private-tab data unless the user allows it for that feature.
3. **Per profile.** Saved data, rules and settings are keyed by profile id, the same way tab groups and bookmarks are.
4. **Follows AGENTS.md.** Flat surfaces (rule 27), morph motion from the trigger (rule 10), `BrowserLog` for every state change and failure (rules 17 and 18), a Settings row for any preference (rule 9), and phone and tablet together (rule 2).
5. **No control without a working API.** If the platform cannot do it, the control is left out and the gap is recorded under *Not done* in PROGRESS.md.

## Priority 1 (do first)

| Idea | What it does | Phone / tablet | API | Size |
|---|---|---|---|---|
| Site tweaks | Per-site switches to hide elements, force dark, change font and widen text. | Same settings; the tablet can show them in the sidebar. | Force dark and font: unknown, needs a check (GeckoView 160 has a runtime-wide colour scheme, `COLOR_SCHEME_DARK`, and font size factor, but no confirmed per-site setting). Hide elements: needs a content script through `WebExtensionController`. | L |
| Element zapper | Long-press an element on a page and choose "Hide on this site". | Phone: long-press. Tablet: long-press, or right-click with a mouse. | Long-press: `GeckoSession.ContentDelegate.onContextMenu` with `ContextElement`, confirmed. Hiding the element: needs a content script, unknown until checked. | M |
| Command bar | Typing ">" in the address bar runs actions such as "close tab", "new private tab" or "open settings". | Same; the tablet can also open it with a keyboard shortcut. | None beyond Ora's own actions. Uses the existing address bar. | M |
| Profile rules | "Always open this site in profile X" for each site. | Same. | None beyond the profiles Ora already has. | S |

## Priority 2

| Idea | What it does | Phone / tablet | API | Size |
|---|---|---|---|---|
| Link peek | A floating preview card of a long-pressed link, without opening the tab. | Phone: a card above the page. Tablet: a card beside the pointer. | Long-press: `onContextMenu`, confirmed. A rendered preview of the link: unknown, needs a check (a second `GeckoSession` or a thumbnail). | L |
| Duplicate tab finder | Lists tabs that open the same address, with a button to close the extras. | Same. | None; Ora's own tab list. | S |
| Saved sessions | Save a named set of tabs and reopen it later. | Same; the tablet can show sessions in the sidebar. | Saving open pages: `GeckoSession$State` exists in GeckoView 160; the save and restore method names need a check. | M |
| Site keywords | Typing "w moon" in the address bar searches Wikipedia for "moon". | Same. | None; a stored list in Ora. | S |
| Media hub | Lists every tab that is playing sound, with play, pause and mute for each. | Phone: a sheet. Tablet: a sidebar section. | Playing and pausing: `MediaSession.Delegate` (`onPlay`, `onPause`, `onStop`, `onMetadata`), confirmed. Mute per tab: unknown, needs a check. | M |
| Privacy report per site | Shows what was blocked on the current site, opened from the address bar ring. | Same. | `ContentBlockingController.LogEntry` exists in GeckoView 160; the exact fields need a check. | M |
| Shelf | A per-profile scrapbook of quotes, images and links, kept on the device. | Same. | None; a local store with Android file access for images. | M |
| Data saver per site | Turns off autoplay and large images for chosen sites. | Same. | Autoplay: unknown, needs a check. Images: unknown, needs a check. | M |

## Priority 3

| Idea | What it does | Phone / tablet | API | Size |
|---|---|---|---|---|
| Page watch | Notifies when a page changes. Wi-Fi only, opt-in. | Same. | Background checks: Android WorkManager and a network constraint, not GeckoView. The page-change check itself is unknown, needs a check. | L |
| Routines | "Weekdays at 9, open School with these tabs." | Same. | Scheduled start: Android `AlarmManager` or WorkManager, not GeckoView. | M |
| Highlights that reappear on revisit | Saves selected text and shows it again the next time the page is opened. | Same. | Selecting text in a page: unknown, needs a content script. Storage: local. | L |
| Focus mode | Blocks chosen sites during set hours, per profile. | Same. | Blocking a load: `GeckoSession.NavigationDelegate` (`LoadRequest` exists in GeckoView 160); the blocking call needs a check. | M |
| Time on sites | Shows local screen time per site, on the learned page. | Same. | None; Ora's own page events. | M |
| Paste a list of links | Turns a pasted list of addresses into a new tab group. | Same. | None; uses the existing tab group code. | S |
| Copied link chip | Shows a chip on the start page when a link is on the clipboard. | Same. | Android `ClipboardManager`. | S |
| Tab saver | Freezes idle tabs and shows how much memory they use. | Same. | Freezing a tab: unknown, needs a check. Memory use: Android `ActivityManager`. | L |
| Full-page screenshot with markup | Captures the whole page and lets the user draw on it. | Same. | `GeckoDisplay.ScreenshotBuilder` exists; a full-page capture is unknown, needs a check. Markup is Ora's own. | L |
| Send tab to nearby device | Sends the current page to a nearby device with Quick Share. | Same. | Android share and Quick Share, not GeckoView. Quick Share availability on each device: unknown, needs a check. | M |
| Gesture editor | Lets the user change what each swipe or long press does. | Same. | None; Ora's own gesture code. | L |
| Start page widgets | Shelf, routines, a note and recent downloads on the start page. | Same. | None; Ora's own views. | M |
| Alternate app icons | One launcher icon per profile tint. | Same. | Android activity aliases through `PackageManager`. | S |

## Not decided

Nothing has been chosen to build. The order is for the user to set.
