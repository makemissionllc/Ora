# Ora v2: profiles, tab groups, private tabs, extensions and layout fixes

Design source: the "UI v2" page of the mockup canvas (Mineral design).
Boards are named below in **bold**. Work through the phases in order and
check each one on a device before moving on. Follow AGENTS.md for
commits, device checks and keeping README, CHANGELOG and PROGRESS current.

---

## 1. How Ora is organised

```
Profile (Personal, Anime, ...)      ← owns a tint, search engine, home page
├── Tab groups (Watching, Reading)  ← named, coloured, can collapse
│   └── Tabs
├── Tabs not in a group
└── Private tabs                    ← separate space, never saved
```

Rules:

- Everything belongs to one profile. Switching profiles swaps tabs,
  groups, bookmarks, history and the tint.
- A tab is in at most one group. Groups belong to one profile and never
  move between profiles.
- Private tabs belong to the profile that opened them, but live in their
  own space: no groups, no thumbnails, nothing written to disk. Closing
  the last private tab clears their cookies, history and site choices.
- Inactive tabs (not opened in 14 days) move into a collapsed "Inactive
  tabs" row. They stay in their group (later phase).

## 2. What makes normal and private look different

| | Normal tabs | Private tabs |
| --- | --- | --- |
| Background | Profile tint (Basalt by default) | Obsidian `#111214`, surfaces `#1B1C20` |
| Address bar | Solid Slate capsule | Same capsule with a 1.5 dp **dashed** Fog border, and a "Private" chip at the left |
| Tab overview | Thumbnails and group stacks | List rows, no thumbnails, dashed favicon outline |
| New tab button | Chalk fill | Dashed Chalk outline |
| Extensions | On as set | Off unless "Allow in private tabs" is on for that extension |
| Saved | Yes (quick resume) | Never |

Dashed means temporary everywhere in Ora. Don't use dashed lines for
anything else.

Phone overview: a segmented control at the top, "6 tabs | 2 private",
switches between the spaces (**Phone · Tab groups in a tinted profile**,
**Phone · Private tabs**). The private space has an explanation card,
"Lock private tabs" (uses App lock's PIN), and "Close private" in Ember.

Tablet: the sidebar's bottom row "Private tabs (2)" opens the same space
inside the sidebar, using the Obsidian colours.

## 3. Profiles govern a tint

Each profile picks one of five tints (**Tablet · Profile: tint and tab
groups**). A tint changes only Ora's own surfaces, never web pages.

| Tint | Base | Surface | Raised | Marker |
| --- | --- | --- | --- | --- |
| Basalt | `#1A1B1E` | `#232528` | `#2D3034` | `#9A968F` |
| Moss | `#1A1E19` | `#232922` | `#2D342B` | `#8FA77A` |
| Clay | `#201B19` | `#2A2321` | `#352C29` | `#C98E72` |
| Tide | `#191C21` | `#21262D` | `#2B3139` | `#7E9BB8` |
| Plum | `#1E1A21` | `#28222C` | `#332B38` | `#A88BB8` |

- Chalk `#ECE6DC`, Fog `#A29D94`, Lichen and Ember stay the same in
  every tint. The selected state is always a Chalk fill.
- The marker colour is the small dot next to the profile name in chips.
- Light theme: keep Stone/Paper/Ink. Tints apply in dark themes only for
  now. Pure dark ignores tints.
- Implementation: `browserColors(theme, tint)` in `ui/theme/Theme.kt`.
  Add `tint` to the profile in `ProfilesRepository`, default Basalt.

## 4. Tab groups

Data (per profile, stored under `ora_tab_groups_<profileId>`):

```kotlin
enum class GroupColor { Lichen, Ember, Ochre, Tide, Plum, Fog }
data class TabGroup(val id: String, val name: String,
                    val color: GroupColor, val collapsed: Boolean, val order: Int)
// Tab gets: val groupId: String?   (saved with quick resume)
```

Group colours: Lichen `#A9C08C`, Ember `#E58A6B`, Ochre `#D8B46A`,
Tide `#7E9BB8`, Plum `#A88BB8`, Fog `#A29D94`. Always show the group's
name next to its colour. Colour alone never identifies a group.

Interactions:

- Long-press a tab (overview or sidebar) → "Add to group" sheet: list of
  groups, plus "New group".
- "New group" (phone overview bottom bar, tablet sidebar, profile page):
  name field and the six colours. A new group with no tabs is allowed.
- Phone overview: each group is a stacked card (**Phone · Tab groups**).
  Tap opens the group: its tabs in a grid, with Rename, Colour, Ungroup
  and Close group in a ⋯ menu.
- Tablet sidebar: group headers collapse and expand with a chevron. Tabs
  not in a group sit under "Not in a group".
- Profile page: reorder groups by dragging the grip, Edit to rename or
  recolour.
- Later: drag a tab onto a group header in the tablet sidebar.

## 5. Extensions

Boards: **Tablet · Extensions**, **Phone · Extensions**.

- List with a switch per extension, search, and "Get extensions"
  (opens addons.mozilla.org in a new tab).
- Detail: version and blocked count, site access (On all sites / Ask
  on each site / Only when I tap it), "Allow in private tabs" (off by
  default), what it can do (from its permissions), Extension settings,
  Remove (Ember, with a confirmation).
- Tablet shows list and detail side by side. Phone opens the detail as
  its own screen.

## 6. Layout fixes from the tablet screenshots

### 6a. ⋯ panel clips its first row (**Fix · ⋯ panel below the bar**)

The Share / Bookmark / New tab tiles are cut off at the top of the panel.

- Panel top = status bar + 8 dp + 52 dp (Compact row) + 12 dp. Right 12 dp,
  bottom 12 dp, width 380 dp, corner 22 dp.
- 16 dp padding on all sides inside the panel. Nothing inside may have a
  negative offset or be translated upward.
- Clip only the panel's own shape. If the content is taller than the
  space, the panel scrolls (`verticalScroll`). It never cuts content off.
- Order: site row (domain, "Secure connection", "12 blocked" chip),
  tiles 3×2, then Find in page, Reader view, Copy page address, Add to
  home screen, Reopen closed tab, Desktop site, Downloads, Extensions,
  Settings. Disabled rows (no article, nothing closed) use Fog text.

### 6b. Sidebar layering (**Fix · Sidebar holds its own buttons**)

Today the sidebar slides under the floating row, so the sidebar, back
and forward buttons sit dimmed on top of it. There's also a big empty gap
above the profile chips, and the page isn't inset.

- When the sidebar opens, the sidebar, back and forward buttons move
  INTO the sidebar's first row (8 dp from its top, same height as the
  Compact row). They are not drawn on top of the sidebar.
- The rest of the Compact row (address capsule, tabs, ⋯) re-centres
  over the page area: left edge = sidebar width + 12 dp.
- Sidebar: 300 dp wide, starts below the status bar, right corners
  22 dp, Slate. Order: button row, profile chips, Tabs | Bookmarks |
  History, search, groups, "New tab group", then "Private tabs" pinned
  at the bottom.
- The page sits in a rounded card (22 dp) inset 12 dp from the sidebar,
  right and bottom. No dim layer over the page while the sidebar is open.

### 6c. Find in page overlaps the address bar (**Fix · Find in page**)

The find bar draws behind the address capsule at the top.

- Find in page replaces the address capsule in the same slot (560 dp
  wide): search icon, text field, "1 of 4", previous, next, Done.
- Morph from the capsule on the shared overlay timings. Done or Back
  morphs back. Matches use a highlight with a Basalt outline.

### 6d. Grain on tiles

A speckled texture shows behind some tiles and rows (the "C" tile on
the start page, Lab and Diagnostics in Settings). Find what draws noise
or grain (a texture, shader or broken press effect) and remove it.
Surfaces are flat. The press response is the 94% scale from
`pressFeedback()` only.

## 7. Phases

1. **Fixes 6a to 6d.** Tablet only. Check each on the tablet with
   wikipedia.org and the start page.
2. **Mineral tokens and profile tints**: `browserColors(theme, tint)`,
   Instrument Sans in `res/font`, tint picker on a new profile page.
3. **Private space**: Obsidian colours, dashed capsule, segmented
   overview on phone, sidebar space on tablet, "Lock private tabs".
4. **Tab groups**: data and storage, long-press "Add to group",
   phone stacks, tablet collapsible headers, profile page list.
5. **Extensions screens**: list, detail, site access, private-tab switch.
6. Later: inactive tabs, drag into groups, Reader view.

For every phase: build, install, check on the device the board names,
record what was and wasn't checked in PROGRESS.md.

## 8. Add to AGENTS.md

```
## Layout rules (protected)
- Overlays and panels start below the floating Compact row and never
  clip their own content. Too tall = scroll, never cut.
- The tablet sidebar holds its own sidebar/back/forward buttons. The
  floating row never draws over the sidebar.
- Find in page replaces the address capsule in place.
- Surfaces are flat. No grain, noise or texture.
- Dashed lines mean private/temporary and are used for nothing else.
- Tints change Ora's surfaces only, never web content.
```
