# Changelog

All notable changes to Ora. Newest first. The in-app "What's new" page shows the same entries, written for people using the browser rather than building it.

Each entry is written on the feature's own branch (`feature/<name>`, `fix/<name>`) and names that branch's commit hash. The entry reaches `develop` when the branch is merged, and `main` when `develop` is released. See the Branch rules in [AGENTS.md](AGENTS.md).

Format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/). Dates are commit dates.

## Unreleased

### 2026-11-20

**Added**
- A new full-screen video player, in the style of Samsung Internet: a clear top bar with the page's title, rotate and picture-in-picture, a play button with 10-second back and forward, a progress bar with times, and a bottom row with lock, speed (0.5× to 2×), fit or fill, and mute. Double-tap the left or right side to skip 10 seconds, hold anywhere to play at 2× while you hold, swipe the left side for brightness and the right side for volume, and lock the controls so a touch cannot change anything. Controls fade away after a few seconds while playing, stay up while paused, are announced by screen readers, and the screen stays on. Works for videos inside frames too. The page now fills the whole screen behind the video on tablets. (`b7894ba`)
- Website pop-ups now work: date, time, month and week fields open a calendar or time picker (with Today or Now, and Clear), drop-down lists open a list you can tap, and alerts, confirmations, questions and leave-page warnings show in Ora's own dialogs. Each grows out of the spot you tapped. Before, nothing happened. (`0e3ce2d`)

**Fixed**
- Uploading works: tapping an Upload or Choose file button on a website now opens Android's file picker, for one file or several, and only shows the kinds of file the site accepts. Before, nothing happened. (`efb97ed`)

**Added**
- Frequent sites: Ora now counts how often you open each page and ranks your history by visits and how recent they are. Pages you visit most also match what you type in the address bar, and tapping the bar before typing warms your top sites so they open sooner. Old history still works and counts as one visit each. Never in private tabs. (`cd48c83`)
- Faster connections: preloading now opens a real connection to the site, not just the address lookup, so the page can start straight away. (`cd48c83`)
- Load the top page ahead: a new switch under Settings > Search fetches the page behind the first address suggestion before you open it. It is off by default, uses extra data, follows the Preloading choice, skips addresses with a query or actions like logout, and is never used in private tabs. (`cd48c83`)
- Slow page tips: when a page takes more than 8 seconds to load, a tip says why (offline, the site is slow to answer, Data Saver, or almost done) and offers Try again, Stop loading or Keep waiting. Turn it off with Slow page tips under Settings > Search. (`cd48c83`)

**Fixed**
- Big downloads no longer stop by themselves and wait for you to tap Resume. They carry on after Ora is closed by the system or the tablet sleeps, and after a lost connection they are tried again every 2 minutes (up to 30 tries without new data, never for a missing or forbidden file). While it waits, the row says Connection lost. Trying again soon. A download you pause yourself still stays paused. (`1428738`)

**Added**
- Search suggestions: as you type in the address bar, Ora now shows phrases from your default search engine, like Google does, with the part you have not typed yet in bold. Tap one to search, or tap its arrow to put it in the field and keep typing. Your bookmarks and open tabs still appear below. What you type is sent to your search engine, so there is a Search suggestions switch under Settings > Search, and private tabs never use it. (`9176236`)
- Preloading: while you type in the address bar, Ora now looks up the address of the site you are most likely heading to, so the page starts sooner. Choose Off, Wi-Fi only or Always under Settings > Tabs. Only the address is looked up, nothing is downloaded, and private tabs never do it. Wi-Fi only skips mobile data and Data Saver. (`5fdd3d7`)

### 2026-11-19

**Added**
- The tick on the other Settings choices, appearance, motion style and the two swipe options, now grows in and shrinks out like the font list, so a choice is seen as you make it. (`d6faf5f`)
- The tick on the chosen row now grows in and shrinks out when you pick something else, in the font list, the search engine list and the group list, so you can see which row you just chose. (`ebe50dc`)
- Empty tab tiles now breathe softly until their preview is captured, so a blank tile looks like it is still loading instead of broken. It stops if animations are turned off. (`4f2abfb`)
- Counts now roll: when you open or close a tab, the number in the tab overview slides up for more and down for fewer instead of just changing. (`a00ecac`)
- Use font on websites: a new switch under Font shows web pages in your chosen font too. It changes open pages at once, leaves icons and code alone, and is off by default. (`c81ef1f`)
- Font: pick a typeface for Ora in Settings > Appearance. Choose from Instrument Sans, Inter (close to San Francisco), Manrope, DM Sans, Newsreader and Fraunces (serifs), or JetBrains Mono. Every font is shown in its own letters with a live preview, and it works offline. Web pages keep their own fonts. (`a563112`)

**Changed**
- Confirmation cards now grow out of the button you pressed and shrink back into it, instead of popping up. Back closes a card the same way and changes nothing. (`38c6bf8`)

**Fixed**
- Sharing the same link to Ora twice now switches to the tab that already has it, not a new copy. Links with no site address, or that are not web links, are ignored and noted in Diagnostics instead of opening a blank tab. (`bd5cc53`)

**Added**
- Bottom sheets can be dragged from anywhere, not only the handle: pull the list up to grow the sheet, and pull down at the top of the list to shrink it. A fast flick down from full height lands on half height first; a second flick closes it. (`fd16c3c`)
- Hints: the first time a bottom sheet is taller than its half height, it lifts a little and a caption says Pull up for more. It plays once per hint and never blocks anything. Settings has a Hints switch; turning it on again replays them. (`9c147f8`)
- One shared bottom sheet for the menu, Downloads, groups, profiles and the search chooser. It rises on a spring, follows your finger, pulls up from half to full height, and a flick down drops it away. Tapping outside or Back closes it the same way. (`8adfd4a`)
- Profile rules: in Settings > Tabs, choose a site and a profile, and typing that site in the address bar opens it in that profile. Private tabs never follow a rule. (`32c2e34`)
- The Downloads list ends with a totals line, such as 4 saved, 1.2 GB and 1 failed, on phone and tablet. (`c415203`)
- Each download now has its own notification, with Pause or Resume and Cancel while it runs, Retry when it fails, and Open when it is done. A private download never shows its file name. (`20e7f5f`)
- A slim row of chips for the other tabs in the open tab's group floats at the bottom of the page, so you can switch within the group in one tap. It only shows for a group of two or more tabs, never for private tabs. (`cf0f67c`)
- Downloads keep going when you leave Ora. A notification shows how many are running, with Pause all and Cancel all. (`0c93cda`)
- Paused downloads come back after a restart, and a download can be resumed from where it stopped. (`b5403e8`, `d4bd14c`)
- A download whose file changed on the server starts again, and the row says so. (`0a8d9c5`)
- A dropped connection is retried after 2, 5, 15 and 30 seconds, then every minute, up to ten times. When the network is gone, it waits and resumes when it comes back. (`665274c`)
- Search in the tab overview also finds bookmarks and history, on phone and tablet. Matches show under the tabs, and the field says when nothing matches. (`394bb9f`)
- Files that can run code, such as .apk, .exe, .sh or .jar, ask before they open. A card says the type can harm your device, with Cancel and Open anyway. (`4834f3f`)
- A finished download can be checked against the SHA-256 or MD5 its website publishes. Paste it under the file, and Ora says whether it matches. (`4834f3f`)
- A download that came over plain http is marked in its details, because anyone on the network can change what arrives. (`4834f3f`)
- On the phone, a finished download has an Info button that opens its details, with the same checksum check as the tablet. (`4834f3f`)

**Changed**
- Profile rules now also apply when you open a bookmark, a history entry or a search match from the tab overview, not only a typed address. (`3d177c8`)
- Delete file and Clear finished in Downloads now ask with a card that grows out of the button you pressed and shrinks back into it, like the open warning. (`7527665`)
- On Android 14 and later, downloads keep running in the background as a system-managed download job, with the same Pause all and Cancel notification buttons.  (`5bc0a47`)
- A download paused or failed before an app restart keeps the page it came from, so resuming still sends the right Referer.  (`69ea180`)
- Downloads use the same sign-in and cookies as the tab that started them. Private-tab downloads share nothing with normal tabs and are not saved to the list after a restart. (`94ff36b`, `0c93cda`)

### 2026-11-18

**Added**
- Pop-up card: when a page opens several pop-ups without a tap, they are no longer hidden behind the newest one. The card shows how many more wait, and you go through them one at a time.
- Messages such as "Page didn't finish loading" now sit above the bottom bar on the phone, not under it.
- Closing a group now says how many tabs closed ("2 tabs closed"), and Undo brings back all of them, not just the last one.
- Passkeys with Bitwarden: Ora now declares the two Android permissions that let sites' passkey register and sign-in go through your passkey app. Tested with Bitwarden on the phone. Google Password Manager may still refuse until Google approves Ora.  (`0840e06`)
- Passkey not working? Open in main browser: on a page that uses https, the ⋯ menu has a row that opens the same page in another browser you choose. Some sites' passkey sign-in does not work in Ora yet, so this gets you in without leaving the page behind. It is hidden on pages that are not https, and says so if no other browser is installed.  (`94fc3b0`)
- Setup on a new install: after the notice, Ora welcomes you and asks three quick things, how it should look (bar position and theme), what it should block (Standard or Strict fingerprinting protection, HTTPS-only, App lock with a PIN) and your first profile (name, colour, and quick Work, School or Other profiles). Every step has Back and Skip, every choice saves straight away, and closing Ora part-way resumes where you were. The last screen lists what you chose and where to change each one in Settings. "Use the defaults and start browsing" skips it all. Settings > About > Run setup again shows the steps with your current choices. After setup, short tips explain swiping the bar up for tabs (and, on tablet, swiping right for the sidebar), each shown once with Got it.  (`49e2ad4`)
- Before you start: the first time you open Ora, and again after any update that changes it, a notice explains in plain words that Ora is experimental, what stays on your device, what the diagnostic log keeps, and your rights. "I understand" unlocks after about a second so a stray tap can't skip it, and Back leaves the app instead of skipping it. "Read the full notice" opens the whole text, with a Copy notice button. You can read it again at Settings > About > Notice and privacy, which shows the date you accepted it.  (`2a2bd6c`)
- Open links in apps: when you tap a link that an app on your phone can open (YouTube, a phone number, an email address, a store page), a card above the bar asks "Open in the YouTube app?" with Stay in Ora, Always and Open. The page keeps loading in Ora behind the card, and a link that comes with a web fallback loads that page if you stay. Always is remembered for that site only. Settings > Privacy has Ask, Always and Never, and a list of remembered sites with Remove. Private tabs always ask and never remember. A page can't start an app on its own, only your tap can.  (`eb45ae6`)
- Add to Firefox now installs. On addons.mozilla.org, tapping Add to Firefox (or opening any .xpi extension file) shows an install sheet instead of downloading a file. The sheet names the extension, says it is signed by Mozilla and comes from addons.mozilla.org (or names the other site it came from), lists what it can do, and has Allow in private tabs, off, with Cancel and Add extension. Nothing is installed until you choose Add extension.  (`c48a3f8`)
- Install from a file: on the Extensions screen, pick an .xpi from your phone. If an .xpi is already in your Downloads, a dashed Found in Downloads card offers Install, and in the Downloads list an .xpi file's action is Install, not Open. Only extensions signed by Mozilla can be installed, and the screen says so.  (`c48a3f8`)
- Installing an extension says what went wrong in plain words: not signed by Mozilla, doesn't work with this version of Ora, the file is damaged, or the download failed. An empty Extensions list now says how to add one.  (`c48a3f8`)
- Failed pages now show a card that says what went wrong, in one plain title and one sentence, instead of a blank page. There are cards for offline, an address that can't be found, a site that isn't answering or refused the connection, a blocked port, a certificate problem and a few others. Try again reloads the address, and Go back returns to the page before it, greyed out when there is no earlier page. Details for support is folded under the card, with a Copy details button for a bug report.  (`a1f02a2`)
- Pop-up card: when a page opens a new window on its own, without a tap, Ora asks first. The card names the site, with Open (opens it this time), Always allow (opens it and lets that site open pop-ups from now on) and Block (stops it). Private tabs have no Always allow.  (`9b91073`)
- Pop-up sites: Settings > Privacy > Pop-ups and ads lists the sites you chose Always allow for, with Remove to stop allowing one. Block pop-up windows in the same section turns the check off, and then pop-ups open as tabs without asking.  (`9b91073`)
- Fast downloads over http: Settings > Privacy > Network has a new switch, off by default. On, plain http:// files download in parts and can be paused and resumed, the same as https files. Off, plain http files download on one connection and cannot be paused. Their data is not encrypted, and the switch says so.  (`352ab8d`)
- Downloads are split into parts when the site allows it, and a dropped connection is retried. Unfinished downloads come back paused after Ora restarts, so you choose when to continue.  (`352ab8d`)
- A "Download started" note appears at the bottom when a download begins and fades away by itself. Swipe it away if it covers something, or tap it to open Downloads.  (`352ab8d`)
- Swipe down on the tablet's Compact row to see all your tabs. A swipe up on the phone's address bar already did this; now the tablet's top bar works the same way in reverse.  (`84bf99b`)
- Search tabs, bookmarks and history in one field: on tablet, the sidebar's Tabs search now also lists matching bookmarks and history entries under your tabs, so you can find a page without choosing the right list first.  (`dae97f0`)
- Drag a tab into a group: touch and hold a tab, then move your finger to pick it up, and drop it on a group card in the overview or on a group's header in the tablet sidebar. The group lights up as you hover over it. In the sidebar, drop a tab on "Not in a group" to take it out of its group. Holding a tab still without moving still opens Add to group, as before.  (`e2e298a`)
- Inactive tabs: a tab you haven't opened for 14 days moves out of the way into a folded "Inactive tabs (N)" row, in the tab overview and in the tablet sidebar. Tap the row to open or fold it, and tap a tab inside to open it. Searching still finds them, tabs inside a group stay in their group, and private tabs are never moved.  (`f8d3eb6`)
- Extensions screens: the list has a search that filters your installed extensions, a switch on each one, and a Get extensions row that opens addons.mozilla.org in a new tab. Tap an extension to see its version, what it can do (read from its permissions), Allow in private tabs (off unless you turn it on), Extension settings when it has a page for them, and Remove in red, which asks first. On tablet the list and the details sit side by side; on phone the details open as their own screen. Choosing who can use an extension on each site is not there yet.  (`b295e8f`)
- Tab groups: long-press a tab (in the overview or the tablet sidebar) and choose Add to group to put it in a named, coloured group, or make a new one with a name and one of six colours. In the overview, groups are stacked cards; tap one to open its tabs, with Rename, Colour, Ungroup and Close group in its ⋯ menu. In the tablet sidebar, groups have headers that fold with a chevron, tabs outside any group sit under "Not in a group", and "New tab group" is at the bottom. Close group asks first. Groups are kept per profile and your tabs stay in their groups when Ora reopens. Private tabs can't be grouped.  (`3c04e7e`)
- Private space: a private tab turns the whole app Obsidian (Dark look only), its address bar gets a dashed Fog border and a "Private" chip, and the tab overview has a "tabs | private" switch. The private side lists tabs as rows with a dashed icon outline and no thumbnails, has a note on what is not saved, a dashed New private tab button, Lock private tabs (when App lock is set) and Close private, which closes every private tab in one tap. On tablet the sidebar has a pinned "Private tabs (N)" row that opens the same space inside the sidebar, with Back to tabs.  (`e6f4b4c`)
- Profile tints: each profile can take a tint (Basalt, Moss, Clay, Tide or Plum) that colours Ora's own bars, sidebar and sheets in the Dark look. Web pages are never tinted, and Light and Pure dark ignore it. Long-press a profile chip to open its page: rename it, pick a tint and see it applied as you pick, with Cancel putting the old tint back. The + chip opens the same page for a new profile.  (`6661f54`)
- Find in page on tablet: it opens in the address bar's place, with the search icon, the text field, "1 of 4", previous, next and Done, and the field is ready to type at once. Back or Done brings the address bar back.  (`2fdaa3d`)
- The ⋯ menu panel on tablet no longer cuts off its top row: it starts below the top bar, has even space inside, and scrolls if it is taller than the screen.  (`2fdaa3d`)
- With the sidebar open on tablet, its button, Back and Forward move into the sidebar. The address bar, Tabs and ⋯ sit over the page, and the page sits in a rounded card.  (`2fdaa3d`)

**Changed**
- The Open in main browser row is now hidden when no other browser is installed, so it never leads to a dead end.  (`c5dacf7`)
- A page that fails checks the phone's connection first. With no internet, the card says you're offline, whatever the browser reported.  (`a1f02a2`)
- A pop-up opened by tapping a link now opens as a new tab and switches to it. From a private tab it opens as a private tab.  (`9b91073`)
- Download entries in the log name the site a file came from and its size, never the file name. A file name can say what you downloaded.  (`fb8d03c`)
- On phone, the Downloads page opens as a sheet from the bottom that you can drag up to full screen. Pause is shown only on downloads that can pause.  (`352ab8d`)
- The Downloads page now matches the design: a summary of what is moving (count, speed and bytes left), an All, Active, Done and Failed filter, Active and Today sections, and rows with a file-type tile and a four-part progress bar. Finished files open on tap and share on a long press, and each filter has its own empty message. Pause, resume and retry are not in yet. Phone only so far.  (`c59ddf0`)
- The Downloads page opens from the toolbar button and shrinks back into it when closed, and it has a Copy link action with a short confirmation on the details page. The tablet layout is not checked on a device yet.  (`e182fda`)

**Fixed**
- Closing a group now says how many tabs closed ("2 tabs closed"), and Undo brings back all of them, not just the last one.
- A pop-up that a page opens without a tap is no longer dropped silently. Ora now sees it and asks first, so the card can show.  (`9b91073`)
- The log no longer fills with a line each time you scroll a page that was not at its top. Reader's "Page report" is written only when a page's answer changes, not every time it loads.  (`7d8fdd3`)
- A page that fails to load is logged with the same plain words the error page uses (for example "The address could not be found") instead of a category and code number. Pages that fail because of a proxy or a bad address now get their own words too.  (`1f2fb61`)
- Installing an extension: tapping Add again while it is installing does nothing, the button says "Adding…", and the extension is installed once. A failed install now says why in plain words, not a blank message.  (`729e558`)
- Pull to refresh reloads a page after a shorter pull, about 15% less than before, and a short haptic tick marks the moment the pull is far enough to reload.  (`4daf86e`)
- With the PIN lock on, asking for a private tab now shows the lock screen even when you are on a regular tab, so you can unlock from there. Before, the lock could stay on with no way to enter the PIN, and private tabs would not open.  (`e63eab9`)
- Swiping the address bar moves only between tabs in the current space. From a normal tab it no longer lands on a private tab, and from a private tab it no longer lands on a normal one.  (`4ba9ba3`)
- Links and images opened from a private tab (Open in new tab, Open in background tab, Open image in new tab) now open as private tabs instead of normal tabs. Nothing about them is logged.  (`56a579e`)
- Searches are no longer written to the log with the words you typed. The log now says only which search engine was used, for example "Searched with Google". Searches from a private tab leave no log line at all.  (`2c75efd`)
- The app icon is no longer cropped on the home screen. The Ora mark now sits in the middle of Android's icon shape on its own charcoal background, and it also works with themed icons.  (`3232d02`)
- A download that failed before it started is kept in the list after Ora restarts, so Retry is still there.  (`352ab8d`)
- Tabs that were waiting to load when Ora was closed no longer lose their pages: they could be saved as blank, and open as a blank page.  (`e57a556`)
- Find in page now waits for you to finish typing, so a word that is on the page no longer shows "No matches", and the count starts at 1 instead of moving on with every letter you type.  (`c59ddf0`)
- Tabs waiting to load after Ora reopens now show their website name in the tablet sidebar and the tab list, instead of "about:blank".  (`32b298f`)
- When Android rebuilds Ora's window, your open tabs now carry over instead of every tab being opened a second time.  (`bc21b78`)
- Reopened tabs no longer fail with "Saved history is not valid": tabs saved before their page had loaded were read back as broken history. They now reopen at their address, and a history that cannot be read is dropped instead of being saved again.  (`45fc871`)
- Private tabs now have their own list and numbering, separate from normal tabs, and the tab counts no longer include them. A private tab cannot be opened while Ora is locked (a notice says to unlock first), and a pop-up from a private tab opens as a private tab.  (`8048a8c`)
- Ora no longer starts twice when Android rebuilds its window right after launch: startup (crash handler, usage counts, app lock) runs once, and the window now survives font, density, language and similar changes. If a rebuild still happens, the log says so.  (`1a1c84d`)
- Quick resume no longer saves your tabs while they are still coming back, and an empty list never replaces a good save, so reopened tabs keep their own pages and history. Ora now says "Reopened N tabs" when it restores several.  (`cd65469`)
- The speckled grain behind tiles and rows (the start page, Lab and Diagnostics) is gone. It was Android's press ripple; the press now only shrinks the tile a little.  (`2fdaa3d`)

**Added**
- App lock: Settings > Privacy can ask for a PIN when Ora opens and whenever you come back to it. The PIN is kept only as a salted hash, never as the number itself. After five wrong tries you wait 30 seconds, then longer each time, and the lock screen counts it down. (`dff3c60`)
- HTTPS-only mode: a Privacy switch that loads pages over a secure connection only. (`dff3c60`)
- Close all: the tab overview has a Close all button. It asks first, then opens one start page. The last 10 pages can be brought back from Reopen closed tab. (`32294e2`)
- Add to home screen: a row in the ⋯ menu pins a shortcut to the current page, named after its title. Private tabs and the start page say why they can't be added. (`d25373a`)
- Text size on web pages: Settings > Appearance has − and + buttons that make page text smaller or larger, from 50% to 200% in steps of 10. A Reset button returns to 100% in one tap. It applies at once, with no reload. (`45c36b6`)
- Copy page address: a row in the ⋯ menu copies the current page's address in one tap. On the start page it says there is nothing to copy. (`09a342a`)
- Gesture log: with "Count feature use" on, each swipe on the address bar, edge swipe for the sidebar and pull to refresh is counted and logged as registered or ignored, with the reason (too short, diagonal, downward, not at the top of the page) and its distance and time, never any page content. A swipe with nowhere to go is logged as a bounce. See Settings > Diagnostics. (`09a342a`)

**Changed**
- The log now keeps only the domain of a page (for example google.com), never the full address or what you searched for, and says nothing about what you do in private tabs. It is also kept between launches, up to about 5,000 lines, so an export covers days instead of one session, with the date on every line. Clear still empties it.  (`1bbf346`)
- Tabs reopened at start, or when you switch profile, now load only the tab you were on. The others load the first time you open them, so Ora starts faster and uses less data and memory. The notice says so.  (`ecd913c`)
- Mineral look: Light, Dark and Pure dark now use the Mineral palette. Dark is Basalt, Slate and Shale with Chalk text; light is Stone, Paper and Ink. The selected state is a Chalk fill with dark text. (`e875a4c`)
- Ora now uses the Instrument Sans font throughout. (`c8a18fd`)
- On phones, the ⋯ menu has a new look: tiles for the main actions, plain rows below, and a Desktop site switch. A bookmarked page lights its Bookmark tile. (`c511758`)
- The tab overview has the Mineral look on phone and tablet: the open tab has a Chalk ring, each tab shows an initial and a round close button, New tab is Chalk and Close all is red. The search box says how many tabs you have open, such as "Search 9 open tabs". (`470a20c`)
- The Library has the Mineral look: round Bookmarks and History tabs, a round search box and a coloured letter beside each page. The History list on the tablet sidebar matches. Under your history it now says "Private tabs are never saved." (`37594fb`)
- The card a site shows when it asks for your camera, microphone, location or notifications has the Mineral look: an icon, a clear question, a red Block, a grey Not now and a light Allow. It now says that Not now asks again next time. (`3040ffc`)
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
