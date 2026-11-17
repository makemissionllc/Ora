package com.makemission.ora.changelog

/** Kind of change, shown as a small label next to each line on the What's new page. */
enum class ChangeKind(val label: String) { ADDED("Added"), CHANGED("Changed"), FIXED("Fixed") }

/** One user-facing line about a change, written in plain words. */
data class ChangeItem(val kind: ChangeKind, val text: String)

/** The changes made on one date. Mirrors the entries in CHANGELOG.md; update both together. */
data class ChangeDay(val date: String, val items: List<ChangeItem>)

/** Newest first. Keep in step with CHANGELOG.md, per AGENTS.md rule 20. */
val Changelog: List<ChangeDay> = listOf(
    ChangeDay(
        date = "17 Nov 2026",
        items = listOf(
            ChangeItem(ChangeKind.ADDED, "Set Ora as your default browser from Settings > About. Web links opened from other apps open in a new Ora tab."),
            ChangeItem(ChangeKind.ADDED, "Export the log from Diagnostics as a text file, to send when something goes wrong. Choose where to save it."),
            ChangeItem(ChangeKind.ADDED, "Profiles keep their own open tabs and bookmarks. Switch them at the top of the tab overview or the tab sidebar. Long-press a profile to remove it, which also deletes its bookmarks."),
            ChangeItem(ChangeKind.ADDED, "Feature use is counted on this device, so Diagnostics can show which controls you use most. Clear it there any time. Turn counting off in Settings > Diagnostics. No pages or searches are recorded."),
            ChangeItem(ChangeKind.ADDED, "Pages fade in once they are half loaded, instead of showing their pieces as they arrive. Turn it off in Settings > Appearance."),
            ChangeItem(ChangeKind.ADDED, "Secure DNS looks up site addresses over encrypted DNS, and falls back to your network's DNS if that fails. Turn it on or off in Settings > Privacy."),
            ChangeItem(ChangeKind.ADDED, "Tablet Settings has a category list beside one centered column."),
            ChangeItem(ChangeKind.ADDED, "A load bar runs along the address field while a page loads. Failed loads show a message."),
            ChangeItem(ChangeKind.ADDED, "Tablet tabs open pages in desktop mode. Long-press reload."),
            ChangeItem(ChangeKind.ADDED, "A floating Compact bar on phones, which folds away as you scroll. Back returns to the start page from pages opened there."),
            ChangeItem(ChangeKind.ADDED, "Phone tab bar layouts modeled on Safari, and a tab overview on tablets."),
            ChangeItem(ChangeKind.CHANGED, "The browser engine is upgraded to GeckoView 160."),
            ChangeItem(ChangeKind.CHANGED, "The load bar no longer flashes, and phone bars float above the page."),
            ChangeItem(ChangeKind.ADDED, "Find in page, from the menu. Type to search the page, then step through the matches."),
            ChangeItem(ChangeKind.CHANGED, "The app icon uses the new charcoal design."),
            ChangeItem(ChangeKind.CHANGED, "On phones, the Compact address bar narrows toward the centre as you scroll, and its address is centred."),
            ChangeItem(ChangeKind.CHANGED, "On tablets, the ⋯ menu slides in from the right as a panel, not up from the bottom. Tap the dimmed page or press Back to close it."),
            ChangeItem(ChangeKind.CHANGED, "On tablets, Settings is full screen again, as before."),
            ChangeItem(ChangeKind.CHANGED, "On tablets, the tab overview covers the whole window, with no toolbar or sidebar."),
            ChangeItem(ChangeKind.FIXED, "Profile names in the tab overview and sidebar are centred in their chips."),
            ChangeItem(ChangeKind.FIXED, "The \"Fade pages in\" description no longer runs into its switch in Settings > Appearance."),
            ChangeItem(ChangeKind.FIXED, "The Library label in the phone menu stays on one line and is no longer cut off. Profile chips have more room below them."),
            ChangeItem(ChangeKind.FIXED, "Settings text no longer runs into the switches on Appearance, Tabs and Search."),
            ChangeItem(ChangeKind.FIXED, "The address field clears correctly, and search stops asking which engine to use every time."),
        ),
    ),
    ChangeDay(
        date = "16 Nov 2026",
        items = listOf(
            ChangeItem(ChangeKind.ADDED, "Address suggestions and autocomplete. The tablet sidebar is remembered."),
            ChangeItem(ChangeKind.ADDED, "A start page, three tab bar styles, a long-press menu for links and images, and downloads."),
            ChangeItem(ChangeKind.ADDED, "Several search engines can be on, and you choose one for each search."),
            ChangeItem(ChangeKind.ADDED, "Pages match the app's light or dark colors. Diagnostics shows the browser's log."),
            ChangeItem(ChangeKind.ADDED, "System Back closes menus first, then steps back through the page, then exits."),
            ChangeItem(ChangeKind.ADDED, "A Privacy section with a \"Clear browsing data\" action that asks before it clears."),
            ChangeItem(ChangeKind.ADDED, "Pages slide in from the right, with previews in the tab grid."),
            ChangeItem(ChangeKind.ADDED, "An Extensions page: install from Firefox Add-ons, turn on, or remove."),
            ChangeItem(ChangeKind.ADDED, "Menus grow out of the tabs button and shrink back into it."),
            ChangeItem(ChangeKind.ADDED, "Dark and Pure dark appearance modes, and a Settings page."),
            ChangeItem(ChangeKind.ADDED, "A desktop site switch in the phone menu."),
            ChangeItem(ChangeKind.ADDED, "The first browser with phone and tablet layouts."),
        ),
    ),
)
