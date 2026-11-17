# Progress

The status board for Ora. Update it with every change: move items between sections, and add a line to the verification log.

Last updated: 2026-11-17, at commit `0b92ba3`.

## Done
- Status bar tints from the page's top colour, pages run under it, and a soft fade replaces the solid band once the page scrolls; icons switch light and dark (`2cadb19`). Diagnostics logs the sampled colour per page (`bd2c82e`). Verified on phone and tablet with the start page; a scrolled phone page was seen (see verification log).
- Status-bar fades merged into one StatusBarFade composable; Extensions search results use the shared motion values (`0b92ba3`). Start page checked on both devices after the change.
- Motion style setting: Settings > Appearance > Motion with Smooth, Elegant, Fast and Premium (`67c0218`). Verified on phone and tablet; Fast applied on both, Smooth restored.

- GeckoView browser with phone and tablet layouts. Tabs, tab grid, sidebar, start page, link menu, downloads, extensions, desktop site, Back handling, bookmarks.
- Appearance: Auto, Light, Dark and Pure dark, plus website theme matching.
- Tab bar styles: phone (Top, Bottom, Compact) and tablet (Top, Bottom, Compact).
- Search: several engines, choice per search, address suggestions.
- Privacy: pop-up blocking, ad and tracker blocking (strict tracking protection), clear browsing data.
- Load feedback: load bar in the address field, failed-load message, page fade-in at 50% load.
- Tablet Compact tab bar: a floating row at the top (back, forward, address bar, tabs, menu), modeled on the phone Compact bar.
- Tablet Settings: category rail with one centered column, full screen. Tablet ⋯ menu opens as a side panel from the right, closed by tapping the dimmed page or Back.
- GeckoView 160, with Kotlin 2.4.21 and compileSdk 37.2.
- Secure DNS (DNS over HTTPS, with fallback).
- Selection card for selected page text: Search, Copy, Select all, Share with highlight, and Call, Message or Add to calendar for phone numbers and dates (`5ddf098`).
- App icon uses the charcoal ring-and-dot design (`cf85810`).
- Phone Compact pill narrows toward the centre as you scroll, with the URL centred (`6e0ddeb`).
- Find in page from the browser menu, with match count and previous/next (`1ab7996`).
- Tab overview search: open tabs are filtered by title or address on phone and tablet (`ce7dbd1`).
- Link menu: "Open in new tab" switches to the new tab; "Open in background tab" opens one without switching (`9104c7f`).
- Feature-use counts on this device, shown on Diagnostics with a switch and Clear usage (`fd47d98`).
- Profiles with their own tabs and bookmarks, switched at the top of the tab overview (phone) and the tab sidebar (tablet) (`52a68f5`).
- Log export from Diagnostics, as a text file chosen in the save dialog (`1c48b60`).
- Cookie policy card in Settings > Privacy: allow all, block third-party, or block all (`c3d1857`). Phone check: the card and its radio choices shown (screenshot). Tablet not checked. Cookie blocking on a live site not yet checked.
- Password autofill switch in Settings > Privacy, which turns on Android password apps for login fields (`b1fa65a`). Filling from Bitwarden or Google Password Manager on a login page is not yet checked.
- Default browser row in Settings > About, and web links from other apps open in a new tab (`462778d`). The system request dialog is not yet checked on a device.
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
- Motion: Smooth still uses 260 ms enter and 200 ms exit. AGENTS.md specifies overlay enter 220 ms fade and 280 ms scale, and exit 160 ms fade and 200 ms scale. Scale and fade do not yet use separate values.

### Status bar
- theme-color: read the page's `<meta name="theme-color">` when present. GeckoView 160 has no callback for it, so this needs a built-in WebExtension content script. Until then the strip uses the sampled top row.
- Check the light-page icon switch and a scrolled tablet page.

### Tab management (from the Chrome and Safari comparison)

- Drag one tab onto another to make a group, and drag tabs into or out of groups.
- Name and colour-code tab groups.
- Mini tab switcher at the bottom for quick switching within a group.
- Inactive tabs: move unused tabs to a separate collection after a chosen time.
- Recent tabs: list tabs open on other devices signed into the same account. Needs a sync design first.
- Search bookmarks and history from one search field. Open-tab search is done (see Done).

### Better app usage

- Keyboard autocorrect and capitals in the address bar (see Known issues).
- Profiles: history, cookies and logins are still shared between profiles. Keeping them apart needs a storage decision.
- Incognito tabs: private tabs that keep no history, cookies or thumbnails once closed.
- Open in a new tab: opens the link in a new tab and switches to it immediately.
- Built-in ad blocking beyond tracking protection: Privacy already has "Block ads and trackers" (GeckoView strict tracking protection). A fuller blocker would need a filter-list design, a choice of lists, and per-site exceptions before any work.

### Carried over

- Tablet check for the Secure DNS row in Settings > Privacy (see verification log).
- Reader mode. The approach is undecided. GeckoView `about:reader` has not been tested.
- Shrink-on-scroll on tablet: bars currently hide completely. The design calls for shrinking to smaller controls.
- Tablet Tabs section in Settings, not re-checked since the rail change.
- Preloading (for example, prefetching a link on long press). Needs a design, and it must not load pages the user did not ask for.
- Network: GeckoView 160 has no public DNS prefetch or preconnect control. Those options are internal and would need a decision before any work.
- Typing in the tablet address bar with the on-screen keyboard can insert spaces and capitals, so the address becomes a search. Not yet investigated. Possibly a keyboard issue, possibly the app.

## Known issues

- Password autofill: Bitwarden is installed on both devices but reports "Did you add an account?" (no logged-in vault), so no login fill has been offered yet. Sign in to Bitwarden, then retest on a login page.
- Phone: tapping a password field on a local login page turned the whole page black (captures measured fully black), reproduced after a clean relaunch. Not seen on the tablet. Cause not found.
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
| 2026-11-17 | rotation fix (hash not recorded) | Configuration changes handled in place | Loaded page, search and tab kept after switch to landscape; auto-rotate restored | Not checked |
| 2026-11-17 | `6e0ddeb` | Compact pill shrinks toward centre, URL centred | Shrunk pill centred, URL centred (screenshot) | Not checked |
| 2026-11-17 | `cf85810` | App icon | Charcoal ring-and-dot icon on the home screen (screenshot) | Charcoal ring-and-dot icon on the home screen (screenshot) |
| 2026-11-17 | `1ab7996` | Find in page bar from the menu; match count, arrows and close | "the" gave 2/69 matches; arrows and close work (screenshot) | "the" gave 1/43 matches; close works (screenshot) |
| 2026-11-17 | `fd47d98` | Feature-use counts, Diagnostics section, Count switch | Feature use showed menu.open 1 after a menu tap; switch in Settings > Diagnostics (screenshots) | Same list and switch shown in Diagnostics (screenshots); the Log page shows the counts too |
| 2026-11-17 | `52a68f5` | Profiles: switcher, separate tabs and bookmarks | Created "Fork", selected it, switched back to Personal; Personal showed 1 saved bookmark, Fork showed 0 saved (screenshots). Long-press removal asked for confirmation; after Remove, Fork was gone and Personal was selected (screenshots) | Switcher in the sidebar with Personal selected (screenshot). Separate bookmarks, switching and removal not checked |
| 2026-11-17 | `1c48b60` | Export log as a text file | Export opened the save dialog; saved ora-log-20261117-094904.txt to Download with the four entries (checked on the device) | Export button in Diagnostics; saved ora-log-20261117-095032.txt to Download (screenshot and file listing) |
| 2026-11-17 | `462778d` | Default browser row; incoming http/https links open in a new tab | Not checked: phone not connected over ADB | Row shows "Ora ✓" (already default). VIEW intent for mozilla.org opened it as a new tab (2 of 2 tabs, screenshot). "Set" dialog checked: with Ora removed as default via adb, the row showed "Set"; the dialog listed Ora, Set as default made Ora the default again and the row showed "Ora ✓" (screenshots) |
| 2026-11-17 | `97e7682` | Menu Library label on one line; profile chips get room | Library label whole in the menu; profile chips fully drawn in the tab overview (screenshots) | Profile chips fully shown in the sidebar (screenshot). Library label in the tablet menu not re-checked |
| 2026-11-17 | fade row | "Fade pages in" switch | Shown in Appearance | Not checked on the 160 build |
| 2026-11-17 | `e3ebbf3` | Tablet Settings as a side panel; tap the dimmed page to close | Settings still full screen (screenshot); the "Fade pages in" description touches its switch (fixed in the next row) |
| 2026-11-17 | tablet overview full screen; profile chip centring (`98ba017`); phone overview backdrop (`f35eaa5`) | Overview shows no page behind its profile row; profile name centred; only the Personal profile exists on this phone (screenshots) | Overview covers the whole window, no toolbar or sidebar; profile names centred in chips (screenshots) |
| 2026-11-17 | Tablet ⋯ menu as a right side panel; tablet Settings reverted to full screen (`5b89721`) | ⋯ menu still a bottom sheet (screenshot) | ⋯ menu slides in from the right; Back closed it (screenshots); Settings opens full screen with its category list (screenshot) |
| 2026-11-17 | Tablet Compact floating top row (`ecff87e`) | Phone Compact bottom bar unchanged (screenshot) | Top row shows back, forward, pill, tabs and menu; Settings description updated; Tabs button opens the overview; Done closes it (screenshots) |
| 2026-11-17 | Tablet Compact floats over the page and shrinks on scroll (`b635b95`) | Bottom Compact bar unchanged (screenshot). A card from another source appeared over the start page in later captures; not traced to Ora yet | Compact row over theverge.com shrank with the buttons folded (screenshot). Clean start-page capture with the row unshrunk not taken |
| 2026-11-17 | Menu actions run after the menu closes (`2d2e8a1`) | "Find in page" from the sheet: the find bar opened after the sheet closed (screenshots) | Settings from the panel opened after the panel slid out (screenshots) |
| 2026-11-17 | fade text fix (`8c762ca`) | "Fade pages in" text keeps a gap from its switch | Text wraps clear of the switch (screenshot). Tablet not checked | Settings slides in from the right with the Appearance page; tap on the dimmed page closed it (screenshots) |
| 2026-11-17 | Status-bar tint and scroll fade (`2cadb19`) | Phone (`3C15C5000LM00000`): Google page scrolled, dark top. Tablet (`7ba8e294`): start page | Phone: page runs under the bar and the fade shows on scroll (screenshot). Tablet: start-page strip matches (screenshot). Not checked: light-page icons, tablet scrolled page, solid strip on a fresh load |
| 2026-11-17 | Status colour log (`bd2c82e`) | Phone and tablet: start page only (screenshots) | Log entry not seen on either device: it fires only after a web page loads. Not checked |
| 2026-11-17 | Tablet Compact in the top bar layout, sidebar toggle (`762bf4e`) | Bottom Compact bar unchanged (screenshot) | Compact row sits above the page with no dark strip; sidebar toggle shows the sidebar (screenshots). Shrink on scroll and the side panel below the bar not checked |
| 2026-11-17 | Password autofill with Bitwarden, local login page on localhost (no submit) | Tapping the password field starts an Android autofill session and Bitwarden is bound, but it reports no account; the page then turned fully black (repeated) (screenshots, logcat) | Page visible after the tap; the same Bitwarden "no account" message; no fill offered (screenshot, logcat) |
| 2026-11-17 | Cookie card; solid status band with page fade (`c3d1857`) | Status band blends into the start page, icons clear (screenshot). Cookies card and Passwords switch shown in Settings > Privacy (screenshot) | Start page captured after the change (screenshot). Settings card not checked |
| 2026-11-17 | Password autofill switch; phone page below the status bar (`b1fa65a`) | Start page and the Google sign-in page sit below the status bar, with the fade kept (screenshots). Autofill fill not checked on a login page | Start page captured with the Passwords card not seen after the text change; autofill fill not checked |
| 2026-11-17 | Tablet Compact row transparent over the start page (`0c35a28`) | Bottom Compact bar unchanged (not re-checked this change) | Start page shows behind the floating row; no white strip; search field clear below the row (screenshot). theverge.com shows behind the row, with the pill over the page (screenshot). Shrink on scroll and side panel not re-checked |
| 2026-11-17 | Refactor: shared disclosure chevron; bookmark state computed once (`451f542`) | Start page unchanged after install (screenshot). Menu chevrons show on Find in page, Downloads and Extensions (screenshot) | Start page unchanged after install (screenshot). Tablet menu chevron not checked |
| 2026-11-17 | Tab overview search; tablet top fade taller (`ce7dbd1`) | Search field shown; typing "start" left only the Start page tile (screenshots) | Search field shown in the overview (screenshot); filter not typed on the tablet |
| 2026-11-17 | Link menu: new-tab and background-tab rows (`9104c7f`) | Both rows shown in the long-press menu on a link (screenshot). Opening the background tab not checked | Both rows shown in the long-press menu on a link (screenshot). Opening the background tab not checked |
| 2026-11-17 | Phone status-bar fade; tablet Compact pill narrower, no band (`6acf4de`) | Status fade: clock and battery readable over the page (screenshot). Bottom Compact bar unchanged | Pill narrower and centred (screenshot). Gap above the page is the window's white background, not yet matched to the page; shrink and side panel not checked |
| 2026-11-17 | Motion style setting (`67c0218`) | Motion section shows four styles; Fast selected and its description updated; Smooth restored (screenshots). Elegant showed as selected before any tap; cause not traced | Motion section shows four styles; Fast selected and description updated; Smooth restored (screenshots) |
| 2026-11-17 | Status-bar fades merged; Extensions motion via OraMotion (`0b92ba3`) | Start page unchanged after install (screenshot). Fade over a loaded page not checked | Start page unchanged after install (screenshot). Fade over a loaded page not checked |
