# Changelog

All notable changes to Ora. Newest first. The in-app "What's new" page shows the same entries, written for people using the browser rather than building it.

Format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/). Dates are commit dates.

## Unreleased

### 2026-11-17

**Added**
- Pages fade in once they are half loaded, rather than showing their pieces as they arrive. A "Fade pages in" switch in Settings > Appearance turns it off. (`c7b6cb6`)
- Secure DNS: site addresses look up over encrypted DNS, and fall back to the network's own DNS if that fails. A Network card in Settings > Privacy turns it on or off. (`1f865d2`)
- Tablet Settings: a category rail sits beside one centered column. (`589f8ee`)
- A load bar runs along the address field while a page loads. Failed loads show a message. (`9f9b292`)
- Tablet tabs open in desktop mode. Long-press reload. (`a248e8e`)
- A floating phone Compact bar, adaptive fold on scroll, and Back to the start page from a page opened from it. (`a4d6906`)
- Phone tab bar layouts modeled on Safari, and a tablet tab overview. (`538cd0b`, `6734c89`, `01ac130`)

**Changed**
- GeckoView upgraded from 153 to 160. (`403206e`)
- Load bar no longer flashes, and phone bars float above the page. (`205a2d6`)

**Fixed**
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
