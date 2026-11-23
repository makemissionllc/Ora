# Ora: tabs, menus and motion

Mockups: canvas "Android Browser Mockups", page **Tabs, menus, motion** (boards `V9-*`).
Rules from AGENTS.md still apply. Record only what was checked on a device (rule 21).

## Order of work

1. Status bar overlap in the top layout (needs the owner's yes, rule 22. Given for this one change, see 1).
2. Long-press menus: link and image, plus Save image as.
3. Customisable bottom toolbar (top layout).
4. Group strip: "Open in new tab in group", then the strip on every layout.
5. Tab switch animation.
6. Motion tokens and the small-component audit.

One commit per numbered item, or smaller. Build with `assembleDebug` after each.

---

## 1. Status bar overlap (top layout)

**Problem:** in the top layout ("Compact top"), the address capsule sits at the very top, so the clock and status icons draw over it (screenshot 2026-11-20, example.com).

**Fix:** the capsule starts at `WindowInsets.statusBars` top + 8 dp. The page keeps drawing under the status bar, and the existing site-tinted strip and 84 dp fade stay exactly as they are.

**Allowed:** only the capsule's top offset and the page's top inset in the top layout.
**Not allowed:** any change to the status bar files, the tint, icon switching or the fade (rule 22). If the fix seems to need one of those, stop and ask.

Check: the phone in portrait and landscape, and the tablet. The capsule must never touch the clock. Boards: `V9-P-TopBefore`, `V9-P-TopAfter`.

## 2. Long-press menus

Source: `ContentDelegate.onContextMenu(session, x, y, element)`. Use `element.type` (IMAGE, VIDEO, AUDIO, NONE), `linkUri`, `srcUri`, `title`, `altText` and `textContent`.

The menu is a Slate card that grows from the finger. It has a header (favicon or thumbnail, title, address), a row of 3 quick buttons, and then a list. Boards: `V9-P-LinkMenu`, `V9-P-ImageMenu`.

**Link menu**
- Quick: Copy link, Share, Preview.
- List:
  - Open in new tab
  - Open in new tab in group › (see 4)
  - Open in private tab
  - Copy link text
  - Download link
  - Add to reading list
  - Open in app (only when an app handles it)
- "From extensions": menu items added by extensions (`menus` API), if GeckoView passes them. Otherwise hide the section.
- In a private tab, "Open in new tab" opens a private tab, and there's no group item.

**Image menu**
- Quick: Save image, Copy image, Share.
- List:
  - Save image as…
  - Open image in new tab
  - Copy image address
  - Search with this image (asks first, because it uploads the image to the search engine. Off if the engine has no image search.)
- If the image is also a link, add a section "This image is also a link" with Open link in new tab and Copy link address.

**Save image:** download through the download engine, using the page's cookies (GeckoWebExecutor), into `Pictures/Ora` via MediaStore. Show a snackbar: "Saved to Pictures" with an Open action.

**Save image as** (`V9-P-SaveAs`)
- File name field, with the extension shown as a chip.
- "Save to" defaults to Downloads › Pictures. **Change** opens the system folder picker (SAF) and remembers the choice.
- Format: Original, PNG or WebP. Converting re-encodes the image and drops metadata. Original keeps the bytes as they are.
- "Always ask where to save" switch.

**Copy image:** share a `content://` URI through FileProvider as ClipData. Delete the cache file after 10 minutes.

**Preview page:** a sheet with a temporary session.
- Nothing goes into history unless the user taps "Open in new tab".
- From a private tab, the preview session is private.

**Logs:** the action name and domain only. Never file names, image addresses or link text.

## 3. Customisable bottom toolbar (top layout)

Settings › Appearance › Toolbar (`V9-P-Toolbar`). Long-pressing an empty part of the bar also opens it.

- **Preview** at the top.
- **"In the bar":**
  - Up to 5 buttons, with Menu always last and not removable.
  - Drag to reorder, and minus to remove.
- **"Add a button":**
  - Share, Bookmark, Home, Reload, New tab, Private tab, Find in page, Reader view, Downloads, Translate.
  - Only show Translate if the TranslationsController works on the device.
- **Presets:**
  - Classic: Back, Forward, Share, Bookmark, Tabs.
  - Reading: Back, Reader view, Find in page, Bookmark, Tabs.
  - Minimal: Back, Tabs.
  - Choosing a preset, then editing it, switches the chip to Custom.
- **Switch:** "Swipe the bar to switch tabs" (default on).
- **Reset** returns to Classic.
- **Storage:** an ordered list in `SettingsRepository`. Unknown ids from an older version are dropped, never crash.
- **Accessibility:** each button keeps a content description. Buttons stay at least 48 dp wide.

## 4. Group strip

### 4a. "Open in new tab in group"
- In the link menu, the group item shows the current group's dot and name.
- Tapping it opens the picker sheet (`V9-P-GroupPicker`):
  - The current group comes first, marked "This group".
  - Then the other groups in the profile.
  - Then "New group…".
- The tab opens in the background, and the strip shows it straight away.

### 4b. The strip (all layouts)
It shows **only when the current tab is in a group**, and never for private tabs.

- **Phone, top layout** (`V9-P-GroupStrip`):
  - A 60 dp row just above the bottom toolbar.
  - On the left, the group's dot with a chevron. Tapping it opens the group sheet.
  - Then a favicon for each tab in the group. The active tab has a ring in the group colour and a small close badge.
  - "+" adds a tab to this group.
- **Phone, bottom capsule layout** (`V9-P-GroupStripCapsule`): the same row, floating above the capsule over the existing bottom fade, with no border.
- **Tablet** (`V9-T-GroupStrip`):
  - A 48 dp row under the address row.
  - On the left, a group chip (dot, name, chevron).
  - Then pills with favicon and title. The active pill is a Chalk fill.
  - "+" at the end.
  - When the sidebar is open, hide the row, because the sidebar already shows the group.
- **Group sheet** (`V9-P-GroupSheet`):
  - The group's name, colour, tab count and an options button.
  - The tab list, with the current tab filled in Chalk.
  - Buttons: "New tab in group" and "Ungroup".
- **Behaviour:**
  - Tapping a favicon switches tabs with the animation in 5 (frame 5).
  - Long-pressing a favicon lets you drag to reorder within the group.
  - If there are more than 6 favicons, the row scrolls sideways.
  - Scrolling the page hides the strip together with the toolbar.
- **Setting:** "Show group strip" (default on), in Settings › Tabs.

## 5. Tab switch animation

**Why it flashes now:** the view switches to the new GeckoSession before that session has drawn anything, so a blank frame shows.

**New behaviour** (`V9-M-TabSwitch`), driven by the finger on the bottom toolbar (or the capsule):

1. **Touch:** nothing moves yet.
2. **Lift (after 12 dp sideways):** the page shrinks to 87%, its corners round to 28 dp, and the bars fade out. The neighbour tabs peek in at both sides as capsules. The domain and favicon of the centre tab show under it.
3. **Travel:** the capsules follow the finger 1:1, and the label changes as each capsule passes the middle.
4. **Settle:** on release, a spring centres the nearest capsule. Fling velocity can skip one tab. The capsule then grows back to full screen and the bars return.
5. **Jump from the strip:** lift, slide straight to the chosen capsule, then zoom in.

**How**
- **Bitmaps, not live views:** during the gesture, show bitmaps. Use the current tab from `GeckoView.capturePixels()`, and neighbours from the thumbnail cache. A tab with no thumbnail shows its favicon on Slate.
- **Swap behind the overlay:** switch the GeckoSession behind the overlay. Keep the overlay until the new session reports its first paint (`onFirstComposite` / `onFirstContentfulPaint`), or for 300 ms at most, then zoom in.
- **Animation:** Compose `Animatable` with `graphicsLayer` (scale, clip shape, translationX). Use the Spatial spring (damping 0.9, stiffness 400).
- **Thumbnail cache:** an LRU of half-size bitmaps, about 24 MB. Private tab thumbnails stay in memory only and are cleared when private tabs close.
- **Reduced motion:** a 120 ms crossfade, no zoom.
- **Spaces:** never crosses between private and normal tabs, or between profiles. At the end of the list, rubber-band and stop.

## 6. Motion tokens and audit

Put these in `ui/theme/Motion.kt` and use only these values:

| Speed | Timing | Curve | Used for |
|---|---|---|---|
| Quick | 120 ms | standard ease out | press, switch thumb, check fill, icon swaps |
| Standard | 220 ms | emphasised ease out (0.2, 0, 0, 1) | menus, panel sections, chips, snackbars |
| Exit | 150 ms | ease in (0.3, 0, 1, 1) | anything leaving |
| Spatial | spring, damping 0.9, stiffness 400 | no visible bounce | capsule zoom, sheets, sidebar, tab travel |
| Reduced motion | 120 ms fade only | | when Android's "Remove animations" is on |

**Rules**
- One thing moves at a time.
- Nothing runs longer than 400 ms.
- A gesture always drives its own motion.

**Audit list** (`V9-M-Audit`, `V9-M-Sidebar`). Do these one commit each:

1. **Sidebar Tabs / Bookmarks / History:**
   - The Chalk pill slides under the labels.
   - The list moves 24 dp in the same direction and fades through.
   - Swiping the list sideways changes section, with the pill tracking the finger.
2. **⋯ menu and dropdowns:** grow from the button that opened them (92% → 100%), and close back into it.
3. **Long-press menus:** grow from the finger.
4. **Tab count badge:** the number rolls up or down.
5. **Bars on scroll:** follow the scroll, then settle shown or hidden.
6. **Address capsule focus:** the capsule widens to full width and the text moves left.
7. **Group strip:**
   - A new tab slides in from "+".
   - The active ring glides between icons.
8. **Sheets:** the scrim and sheet move together, and dragging down follows the finger.
9. **Snackbars:** rise 12 dp and fade in, then leave by fading.
10. **Clear history dialog:** morphs out of its button (already Pending).
11. **Reload ↔ Stop:** turns 90° while crossfading.
12. **Switches and checks:** the thumb slides, and the track colour crossfades.
13. **Closing a tab in the switcher:** the card shrinks, and its neighbours close the gap.

Check each one on the phone and the tablet, with "Remove animations" both on and off.
