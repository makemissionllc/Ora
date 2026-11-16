# Progress

The status board for Ora. Update it with every change: move items between sections, and add a line to the verification log.

Last updated: 2026-11-17, at commit `1f865d2`.

## Done

- GeckoView browser with phone and tablet layouts. Tabs, tab grid, sidebar, start page, link menu, downloads, extensions, desktop site, Back handling, bookmarks.
- Appearance: Auto, Light, Dark and Pure dark, plus website theme matching.
- Tab bar styles: phone (Top, Bottom, Compact) and tablet (Top, Bottom, Compact).
- Search: several engines, choice per search, address suggestions.
- Privacy: pop-up blocking, ad and tracker blocking (strict tracking protection), clear browsing data.
- Load feedback: load bar in the address field, failed-load message, page fade-in at 50% load.
- Tablet Settings: category rail with one centered column.
- GeckoView 160, with Kotlin 2.4.21 and compileSdk 37.2.
- Secure DNS (DNS over HTTPS, with fallback).

## In progress

- Documentation set: README, CHANGELOG, PROGRESS, and the AGENTS.md rule that keeps them current.
- In-app "What's new" page, built from the same entries as CHANGELOG.md.

## Pending

- Tablet check for the Secure DNS row in Settings > Privacy (see verification log).
- Reader mode. The approach is undecided. GeckoView `about:reader` has not been tested.
- Shrink-on-scroll on tablet: bars currently hide completely. The design calls for shrinking to smaller controls.
- Tablet Tabs section in Settings, not re-checked since the rail change.
- Preloading (for example, prefetching a link on long press). Needs a design, and it must not load pages the user did not ask for.
- Network: GeckoView 160 has no public DNS prefetch or preconnect control. Those options are internal and would need a decision before any work.
- Typing in the tablet address bar with the on-screen keyboard can insert spaces and capitals, so the address becomes a search. Not yet investigated. Possibly a keyboard issue, possibly the app.

## Known issues

- Typing "mozilla.org" in the address bar can produce a search instead of a page, if the keyboard inserts spaces or capitals.
- Tablet: when a page scrolls inside its own element, Ora's bars may not return with a scroll up. Seen on a Fandom wiki page.

## Verification log

Each entry lists the change, the devices, and what was checked. Devices: phone (wireless ADB) and tablet (serial `7ba8e294`).

| Date | Commit | Change | Phone | Tablet |
| --- | --- | --- | --- | --- |
| 2026-11-17 | `589f8ee` | Tablet Settings rail | Unchanged | Rail and centered column checked |
| 2026-11-17 | `c7b6cb6` | Page fade-in | Loaded page visible | Loaded page visible after relaunch |
| 2026-11-17 | `403206e` | GeckoView 160 | Start page and mozilla.org loaded | Start page loaded; a search page loaded after a retype |
| 2026-11-17 | `1f865d2` | Secure DNS row | Network card with the switch on | Not checked: toolbar hidden on a wiki page, Settings unreachable |
| 2026-11-17 | fade row | "Fade pages in" switch | Shown in Appearance | Not checked on the 160 build |
