# Progress

The status board for Ora. Update it with every change: move items between sections, and add a line to the verification log.

Last updated: 2026-11-17, at commit `5ddf098`.

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
- Selection card for selected page text: Search, Copy, Select all, Share with highlight, and Call, Message or Add to calendar for phone numbers and dates (`5ddf098`).
- App icon uses the charcoal ring-and-dot design (`cf85810`).
- Phone Compact pill narrows toward the centre as you scroll, with the URL centred (`6e0ddeb`).
- Keeps tabs, the page and the search when the screen rotates (`configChanges` in the manifest, so the activity is not recreated).
- Clears the link menu when a new page starts.

## Being finished

- Pull to refresh on pages: gesture and indicator are built; the indicator still needs a held-pull check on the device. Debug logging removed.
- Safari-style pull feedback (indicator follows the finger, springs back, then reloads).
- Settings page reorganized into sections.
- Haptic feedback across the app, with a Settings switch to turn it off.

## In progress

- Documentation set: README, CHANGELOG, PROGRESS, and the AGENTS.md rule that keeps them current.
- In-app "What's new" page, built from the same entries as CHANGELOG.md.

## Pending

### Tab management (from the Chrome and Safari comparison)

- Drag one tab onto another to make a group, and drag tabs into or out of groups.
- Name and colour-code tab groups.
- Mini tab switcher at the bottom for quick switching within a group.
- Inactive tabs: move unused tabs to a separate collection after a chosen time.
- Recent tabs: list tabs open on other devices signed into the same account. Needs a sync design first.
- Search open tabs, bookmarks and history from one search field.

### Better app usage

- Keyboard autocorrect and capitals in the address bar (see Known issues).
- Find in page, from the Search and selection menus.
- Profiles that keep tabs and history apart (a decision is needed on storage).
- Incognito tabs: private tabs that keep no history, cookies or thumbnails once closed.
- Background tabs: open a link in a tab without switching to it. The page need not load right away; it can wait until the tab is selected.
- Open in a new tab: opens the link in a new tab and switches to it immediately.

### Carried over

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
| 2026-11-17 | `5ddf098` | Selection card | Card shown for a selected word; Close cleared the card and handles. Phone number and date actions not checked | Card shown for a selected word; Close cleared the card and handles. Phone number and date actions not checked |
| 2026-11-17 | rotation fix (uncommitted) | Configuration changes handled in place | Loaded page, search and tab kept after switch to landscape; auto-rotate restored | Not checked |
| 2026-11-17 | `6e0ddeb` | Compact pill shrinks toward centre, URL centred | Shrunk pill centred, URL centred (screenshot) | Not checked |
| 2026-11-17 | `cf85810` | App icon | Charcoal ring-and-dot icon on the home screen (screenshot) | Charcoal ring-and-dot icon on the home screen (screenshot) |
| 2026-11-17 | fade row | "Fade pages in" switch | Shown in Appearance | Not checked on the 160 build |
