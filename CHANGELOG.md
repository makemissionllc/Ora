# Changelog

All notable changes to Ora. Newest first. The in-app "What's new" page shows the same entries, written for people using the browser rather than building it.

Format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/). Dates are commit dates.

## Unreleased

### 2026-11-17

**Added**
- Default browser: Settings > About has a row that asks Android to make Ora your default browser. Web links opened from other apps open in a new Ora tab. (`462778d`)
- Export the log from Settings > Diagnostics as a text file, to send for debugging. Export saves it wherever you choose. (`1c48b60`)
- Profiles: a profile switcher at the top of the tab overview (phone) and the tab sidebar (tablet). Each profile keeps its own open tabs and bookmarks. Long-press a profile to remove it, which also deletes its bookmarks. (`52a68f5`)
- Feature use is counted on this device: new tabs, menus, find, settings and bar styles. Diagnostics shows the counts and Clear usage removes them. A "Count feature use" switch in Settings > Diagnostics turns it off. No pages or searches are recorded. (`fd47d98`)
- Pages fade in once they are half loaded, rather than showing their pieces as they arrive. A "Fade pages in" switch in Settings > Appearance turns it off. (`c7b6cb6`)
- Secure DNS: site addresses look up over encrypted DNS, and fall back to the network's own DNS if that fails. A Network card in Settings > Privacy turns it on or off. (`1f865d2`)
- Tablet Settings: a category rail sits beside one centered column. (`589f8ee`)
- A load bar runs along the address field while a page loads. Failed loads show a message. (`9f9b292`)
- Tablet tabs open in desktop mode. Long-press reload. (`a248e8e`)
- A floating phone Compact bar, adaptive fold on scroll, and Back to the start page from a page opened from it. (`a4d6906`)
- Tablet Compact: a long-press on Back opens the start page, and forward and tabs buttons sit beside the address bar. (`ecff87e`)
- Phone tab bar layouts modeled on Safari, and a tablet tab overview. (`538cd0b`, `6734c89`, `01ac130`)

**Changed**
- GeckoView upgraded from 153 to 160. (`403206e`)
- The app icon uses the new charcoal design. (`cf85810`)
- Find in page: open it from the menu, type to search the page, and step through matches. (`1ab7996`)
- On phones, the Compact address pill narrows toward the centre as you scroll, with its URL centred. (`6e0ddeb`)
- Tablet Compact: the address bar is a floating row at the top with back, forward, tabs and menu buttons, and it no longer runs across the full width. Its description in Settings says so. (`ecff87e`)
- Menu actions run after the menu has closed, so Settings, Library and the rest open cleanly. On tablets the panel slides out first. (`2d2e8a1`)
- On tablets, the ⋯ menu slides in from the right as a panel, not up from the bottom. Tap the dimmed page or press Back to close it. (`5b89721`)
- On tablets, Settings is full screen again, as before. The side panel was reverted. (`5b89721`)
- On tablets, the tab overview covers the whole window, with no toolbar or sidebar. (`98ba017`)
- Load bar no longer flashes, and phone bars float above the page. (`205a2d6`)

**Fixed**
- Profile names in the tab overview and sidebar are centred in their chips. (`98ba017`)
- The "Fade pages in" description no longer runs into its switch in Settings > Appearance.
- The Library label in the phone menu stays on one line and is no longer cut off. Profile chips have more room below them.
- Settings text no longer runs into the switches on Appearance, Tabs and Search, and the Count feature use row has space above it. (`7d758af`)
- Address field clears correctly, and search no longer asks which engine to use every time. (`67b6619`)

### 2026-11-16

**Added**
- Address bar suggestions and autocomplete. Tablet sidebar is remembered. (`6408adc`)
- Start page, tab bar styles, link long-press menu, and downloads. (`c03e168`)
- Several search engines can be turned on, with a choice per search. (`5a289d4`)
- Website theme matches the app palette. A log of browser events is available on the Diagnostics page. (`26bdf05`)
- System Back closes overlays first, then steps back through page history, then exits. (`007cb98`)
- Privacy section with a confirmed "Clear browsing data" action. (`a3c4a11`)
- Pages slide in from the right, with tab page previews. (`bc1890e`)
- Extensions page: install from Firefox Add-ons, enable, and remove. (`84c93a2`, `7ee736e`)
- Overlays morph out of the tabs button. (`093899e`)
- Dark and Pure dark appearance modes, and a Settings page. (`41ece75`)
- Desktop site toggle in the phone menu. (`cf8fbc8`)
- First GeckoView browser with phone and tablet layouts. (`55d20b4`)

**Changed**
- Code structure refactored for readability. (`88d2e55`)
- Tablet layout matches the mockup, with a top toolbar and a searchable sidebar. (`f2f4c27`)
- Phone Compact address field is a floating capsule. (`6734c89`)
