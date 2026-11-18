# Changelog

All notable changes to Ora. Newest first. The in-app "What's new" page shows the same entries, written for people using the browser rather than building it.

Format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/). Dates are commit dates.

## Unreleased

### 2026-11-18

**Added**
- Reader view: the ⋯ menu has a Reader view row that shows the article without the clutter and says how long it takes to read. A bar at the bottom changes the text size, Sans or Serif, and the page colour (Stone, Paper or Basalt), and the choices are kept in Settings > Appearance. Back or the close button returns to the page. (`6e24dae`)
- App lock: Settings > Privacy can ask for a PIN when Ora opens and whenever you come back to it. The PIN is kept only as a salted hash, never as the number itself. After five wrong tries you wait 30 seconds, then longer each time, and the lock screen counts it down. (`dff3c60`)
- HTTPS-only mode: a Privacy switch that loads pages over a secure connection only. (`dff3c60`)
- Close all: the tab overview has a Close all button. It asks first, then opens one start page. The last 10 pages can be brought back from Reopen closed tab. (`32294e2`)
- Add to home screen: a row in the ⋯ menu pins a shortcut to the current page, named after its title. Private tabs and the start page say why they can't be added. (`d25373a`)
- Text size on web pages: Settings > Appearance has − and + buttons that make page text smaller or larger, from 50% to 200% in steps of 10. A Reset button returns to 100% in one tap. It applies at once, with no reload. (`45c36b6`)
- Copy page address: a row in the ⋯ menu copies the current page's address in one tap. On the start page it says there is nothing to copy. (`09a342a`)
- Gesture log: with "Count feature use" on, each swipe on the address bar, edge swipe for the sidebar and pull to refresh is counted and logged as registered or ignored, with the reason (too short, diagonal, downward, not at the top of the page) and its distance and time, never any page content. A swipe with nowhere to go is logged as a bounce. See Settings > Diagnostics. (`09a342a`)

**Changed**
- Mineral look: Light, Dark and Pure dark now use the Mineral palette. Dark is Basalt, Slate and Shale with Chalk text; light is Stone, Paper and Ink. The selected state is a Chalk fill with dark text. (`e875a4c`)
- Ora now uses the Instrument Sans font throughout. (`c8a18fd`)
- On phones, the ⋯ menu has a new look: tiles for the main actions, plain rows below, and a Desktop site switch. A bookmarked page lights its Bookmark tile. (`c511758`)
- The tab overview has the Mineral look on phone and tablet: the open tab has a Chalk ring, each tab shows an initial and a round close button, New tab is Chalk and Close all is red. The search box says how many tabs you have open, such as "Search 9 open tabs". (`470a20c`)
- The Library has the Mineral look: round Bookmarks and History tabs, a round search box and a coloured letter beside each page. The History list on the tablet sidebar matches. Under your history it now says "Private tabs are never saved." (`37594fb`)
- The card a site shows when it asks for your camera, microphone, location or notifications has the Mineral look: an icon, a clear question, a red Block, a grey Not now and a light Allow. It now says that Not now asks again next time. (`3040ffc`)
- The lock screen has the Mineral look: the Ora mark, "Enter your PIN", a row of dots that fill as you type and a round keypad. Hold the delete key to clear the whole PIN at once. It now follows Light, Dark and Pure dark instead of always showing Light. (`d466b61`)
- The tablet sidebar has the Mineral look: the open profile is a Chalk pill, Tabs, Bookmarks and History sit in a round control, and each row has a coloured letter. The open tab has a soft fill and a small dot. On phones the profile chips in the tab overview match. (`9205d78`)
- Settings has the Mineral look: cards are rounded Slate, switches have a Chalk track and a dark knob, Done is a Chalk pill, the open category on the tablet is a Chalk pill and Clear browsing data is red. Every switch now gives a short tap of vibration when it flips. (`3ad3c58`)
- Switching tabs now fades the page in instead of cutting over. With Remove animations on, it is instant. (`638b37c`)
- With a keyboard attached, Ctrl+Tab moves to the next open tab. (`638b37c`)

**Changed**
- On tablet, the sidebar now grows out of the sidebar button and shrinks back into it when it closes. (`6e5bc92`)
- On a tablet with a keyboard, Escape closes the sidebar. (`6e5bc92`)

**Fixed**
- Auto-fill on a login page now reaches your password app. It no longer stops with "Contents can't be auto-filled". (`55c62be`)

**Changed**
- When Auto-fill cannot help, the message says why: turn on a password app in your phone's settings, or tap a login field first, then reload the page if it still fails. (`55c62be`)

### 2026-11-17

**Added**
- Touch feedback: toolbar buttons and link-menu rows sink slightly when pressed and spring back on release, so a tap feels answered. With Remove animations on, the change is instant. (`8d3c3fe`)
- Opening a link in a background tab, or an image, now says so with a message, and the tab count swells. A link sent from another app says it opened in a new tab.
- Site permissions: when a site asks for your location, camera, microphone or notifications, a card asks Allow, Not now or Block. Allow and Block are remembered for that site; change or clear them in Settings > Privacy > Site permissions, where clearing asks first. In a private tab nothing is remembered. If Android has not yet allowed the app, it asks first. (`d47a696`)
- Browsing history: the tablet sidebar and the phone Library list the pages you opened, newest first and grouped by day. Search by title or address, remove one page, or clear all after a confirmation. Each profile keeps its own. Turn saving off in Settings > Privacy; private tabs are never saved. (`b5e1949`)
- Reopen closed tab: the ⋯ menu brings back the last 10 tabs you closed, with their back and forward pages. Private tabs are not kept. With nothing closed, it says so. (`9ef0c1d`)
- Quick resume: when you leave Ora, your open tabs are kept, and they reopen the next time you start it. Turn it off in Settings > Home page. Private tabs are never kept, and turning the switch off clears what was kept. (`24f1261`)
- Two privacy switches in Settings > Privacy: Global Privacy Control, which asks sites not to sell or share your data (on by default), and Fingerprinting protection, which hides details sites use to recognise your device (off by default, as some pages may look different). (`d12870d`)
- Lab: a "Device font on web pages" switch. On, web pages use your device's font instead of the fonts each site asks for. Some icons may show as boxes. It applies to pages loaded after you change it. (`a287715`)
- The sidebar search has a × button that clears the text in one tap. (`9ca2ced`)
- Overlays appear and disappear at once when Android's Remove animations setting is on. (`40a19c2`)
- A Material colours switch in Settings > Appearance. When it is on, buttons, the accent and icons take colours from your theme. Turn it off to keep the original blue design. (`b2ef99d`)
- Tapping ⋯ again closes the tablet menu, with the same slide-out as Back and a tap on the dimmed page. (`1deea80`)
- Auto-fill for text fields: long-press an empty text field and the selection card offers "Auto-fill", which asks your Android password app for suggestions. The card rides above the keyboard. (`4141fbd`)
- On tablet, swipe right from the left edge of a page to open the sidebar. A Settings switch turns this off if a site's own swipes get in the way. (`fbaf6de`)
- On tablet, Back closes an open sidebar before it goes back through page history. (`fbaf6de`)
- Address bar swipe: swipe sideways on the address bar to move through open tabs, the pages of the current tab, or profiles. Choose the target and the motion in Settings > Address bar swipe. (`01de1e1`)
- Full-screen video: swipe up or down on the left for brightness and on the right for volume. Turn this off in Settings > Video. (`01de1e1`)
- Full-screen video: a seek bar along the bottom shows where the video is and lets you drag to another point. (`11f3e74`)
- Full-screen video: double-tap the left half to go back 10 seconds, or the right half to go forward 10. Repeated taps add up in one "+N s" hint. (`ed85a8a`)
- On phones, tapping the shrunk address pill brings the full bar back, instead of opening the address for editing. (`01de1e1`)

**Fixed**
- Back leaves a full-screen video instead of going back a page. A Settings switch, Back exits full screen (Settings > Video), turns this off. (`11f3e74`)
- On phone, a site's permission card appears below the status bar, so its title is no longer cut off. (`982a384`)
- On tablet, the sidebar no longer shows a white strip behind the profile chips or beside the tab list. The profile chips scroll sideways when they do not fit.
- On tablet, the ⋯ menu's Share, Bookmark and New tab icons show again. The floating Compact bar no longer covers them. (`1deea80`)
- Closing the selection card on an empty text field no longer crashes the browser. (`4141fbd`)
- A page that cannot load shows a plain reason and a Try again button, in place of a blank error page. (`9597b05`)
- On tablet, the profile chips in the sidebar scroll to the edge instead of being cut off partway. (`db79a5a`)
- Pages no longer flash white while they load in dark modes. The page area takes the browser's own dark background until the page appears. (`d8d0615`)
- Quick resume keeps each tab's back and forward pages, not only its address. After you leave Ora and start it again, a tab comes back where you left it. (`23346d7`)

**Changed**
- Opening a page from the start page fades the start page out and the page in, instead of cutting over. (`9db59f6`)
- Messages appear as a snackbar inside the app instead of system toasts. A new message replaces the one on screen. (`a14acf0`)
- The tablet sidebar has rounded corners on its open edge. (`a14acf0`)
- Cards, menus, the address field, tiles and side panels share one set of softer rounded corners. (`9ca2ced`)
- Password autofill now follows its switch fully: with it on, your Android password app offers suggestions above the keyboard on login pages; with it off, the page is hidden from Android autofill. The Settings text says where suggestions appear.

**Fixed**
- On tablet, the edge-swipe area starts below the toolbar, so the sidebar button opens the sidebar when tapped. (`20954f4`)
- The ⋯ menu's actions sit in two rows of three, so "New private tab" no longer runs into "Tabs" and "Library" on tablets. (`e88c778`)

**Added**
- Saved passwords: Settings > Passwords > Saved passwords lists logins the browser has saved. Search by site or username, show or hide a password, copy it, or delete it after a confirmation. Logins are stored encrypted on the device.
- New private tab: the ⋯ menu has "New private tab". A private tab keeps no history, cookies or thumbnails, and it cannot be reopened after it closes. Its start page says so. (`8abb39f`)
- Motion style: Settings > Appearance has a Motion section with Smooth, Elegant, Fast and Premium. It sets how quickly pages, menus and panels move. (`67c0218`)
- Tab overview: search open tabs by title or address, on phone and tablet. The count shows matches, and a message appears when none match. (`ce7dbd1`)
- Link menu: "Open in background tab" opens a link without switching to it. (`9104c7f`)
- Cookies: Settings > Privacy has a Cookies card. Allow all cookies, block third-party cookies, or block all cookies. (`c3d1857`)
- Password autofill: a Passwords switch in Settings > Privacy lets Android password apps, such as Bitwarden or Google Password Manager, fill login fields. (`b1fa65a`)
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
- Tablet Compact has a Show or Hide sidebar button at its start. (`762bf4e`)
- Phone tab bar layouts modeled on Safari, and a tablet tab overview. (`538cd0b`, `6734c89`, `01ac130`)

**Changed**
- On phones, the floating bottom bar has no fade behind it. Only the top fades under the status bar. (`2a47c6a`)
- On phones, the page starts below the status bar in the page's own top colour. As you scroll, it slides up under a soft fade. The page ends above the floating bar. (`01de1e1`)
- On tablet, the Clear history confirmation grows out of its button and shrinks back into it when it closes. (`01de1e1`)
- Menus, the tab overview and cards fade and grow on one shared timing, with a quicker fade-out. (`40a19c2`)
- On a tablet, switching between Tabs and Bookmarks in the sidebar clears the search, so a filter for tabs never hides bookmarks. (`d3f1e6e`)
- On tablets, the top fade under the status bar is taller, so the clock and battery stay readable over a page. (`ce7dbd1`)
- Link menu: "Open in new tab" now switches to the new tab. (`9104c7f`)
- GeckoView upgraded from 153 to 160. (`403206e`)
- The app icon uses the new charcoal design. (`cf85810`)
- Find in page: open it from the menu, type to search the page, and step through matches. (`1ab7996`)
- On phones, the Compact address pill narrows toward the centre as you scroll, with its URL centred. (`6e0ddeb`)
- Tablet Compact: the address bar is a floating row at the top with back, forward, tabs and menu buttons, and it no longer runs across the full width. Its description in Settings says so. (`ecff87e`)
- Tablet Compact floats over the top of the page with no strip behind it, and shrinks as you scroll, like the phone bar. (`b635b95`)
- Tablet Compact sits above the page, not over it, so the page never runs under the address bar. No dark strip shows behind the floating controls. (`762bf4e`)
- Phones: the page fades into the status bar, so the clock and battery stay readable. (`6acf4de`)
- Phones: pages and the start page begin below the status bar, so buttons at the top, such as sign-in, can be tapped. (`b1fa65a`)
- Phones: the status bar sits on a solid band, and the page fades into it just below the status bar. (`c3d1857`)
- Tablet Compact address pill is narrower, and it has no background band. (`6acf4de`)
- Menu actions run after the menu has closed, so Settings, Library and the rest open cleanly. On tablets the panel slides out first. (`2d2e8a1`)
- On tablets, the ⋯ menu slides in from the right as a panel, not up from the bottom. Tap the dimmed page or press Back to close it. (`5b89721`)
- On tablets, Settings is full screen again, as before. The side panel was reverted. (`5b89721`)
- On tablets, the tab overview covers the whole window, with no toolbar or sidebar. (`98ba017`)
- Load bar no longer flashes, and phone bars float above the page. (`205a2d6`)

**Fixed**
- On tablets, the floating Compact row no longer sits on a white strip over the start page. The row floats over the page, and the start page content sits clear of it. (`0c35a28`)
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
