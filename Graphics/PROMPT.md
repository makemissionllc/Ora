# Task: Redesign the Ora browser UI from the wireframes

## Goal
Redesign the Android UI of this browser (name: "Ora") to match the wireframes in
/home/rem/StudioProjects/Ora/Graphics/Mockups.
Target devices: Xiaomi Pad 8 Pro (11.2", 3:2 screen, 144Hz, Android 16 / HyperOS 3, stylus)
and OnePlus 15 (tall phone, Android).
This is a personal build. It will not be published, so no store rules apply.
The code is GPL-2.0, so keep the license and any headers intact.

## Step 0: Read first, change nothing yet
1. Read AGENTS.md, README.md, build.sh, patch.sh, common.sh and args.gn.
2. Work out how this project applies changes to Chromium (patches, the `res`
   folder, the `vanadium` folder, the `extensions` folder). Follow that method.
   Do NOT edit files inside `chromium/` or `depot_tools/` directly unless the
   project's own workflow says to.
3. Open every file in Graphics/Mockups. If they are HTML, read the markup and
   styles. If they are images, view them. Then reply with a short plan:
   which files you'll touch, in what order, and any risks. Wait for my OK.

## Wireframe map (light and dark; dark files start with "D-")
Tablet: Main (browsing), Tabs (tab overview), Sidebar (tab groups, bookmarks,
reading list), Start (start page), Split (split view), Settings, Reader.
Phone: M-Browse, M-Tabs, M-Menu (bottom sheet), M-Start, M-Settings,
M-Bookmarks (Library), M-Reader, M-Find (find on page), M-Downloads.

## Design rules (from the wireframes)
- Tablet toolbar: one slim 44dp row. Left: sidebar, back, forward. Center: small
  address pill (32dp tall, radius 10). Right: extensions, share, new tab, tabs.
  A 32dp tab strip sits directly under it.
- Phone: toolbar at the BOTTOM (56dp row, 40dp address pill with reader "Aa"
  and refresh inside). Add a setting to move it to the top.
- Colors (wireframe values; map them to Material 3 color roles):
  - Light: background #f4f5f7, surface #ffffff, placeholder #e6e8ed,
    text #23262f, muted #6b7080, accent #3b6df0.
  - Dark: background #141518, surface #23252b, placeholder #2e3138,
    text #ecedf0, muted #9097a6, accent #6c93ff.
- Corner radius: cards 12dp, bottom sheets 20dp, toggles 40x24dp.
- Support light, dark and follow-system themes. Dark must match the "D-" wireframes.
- The wireframes are iOS-like. Adapt them to Android: Material 3 components,
  system back gesture, edge-to-edge with correct insets, 48dp minimum touch targets.
- Tablet layouts must work in landscape and portrait on a 3:2 screen, and in
  split-screen and floating windows.

## Quality-of-life features to include in the UI
Tab groups (named, colored), search open tabs, swipe the address bar to switch tabs,
per-site settings (ad blocker, desktop site, zoom), reader mode with text size
and theme, find on page with match count, split view, downloads screen,
toolbar position setting, "send tab to my other device" button.
Build the UI first. Where a feature needs deep engine work, add the screen
and a clear TODO, and tell me what's missing.
Keep Titanium extension support working, including the extensions menu.

## Resources (use these; check each license before using)
Icons:
- Material Symbols: https://fonts.google.com/icons  (SVG and Android vector drawables)
- Material icons repo: https://github.com/google/material-design-icons
- Lucide (alternative, consistent line style): https://lucide.dev/icons
- Phosphor (alternative): https://phosphoricons.com
Use ONE icon family across the whole app, with a consistent stroke and size.
Prefer Android VectorDrawable XML so icons tint with the theme.

Components and theming:
- Material Components for Android: https://github.com/material-components/material-components-android
- Material 3 guidance: https://m3.material.io

Animations and transitions:
- Material motion: https://m3.material.io/styles/motion/overview
- Transition patterns (container transform, shared axis, fade through):
  https://m3.material.io/styles/motion/transitions/transition-patterns
- Android animation docs: https://developer.android.com/develop/ui/views/animations/overview
- Lottie for Android: https://github.com/airbnb/lottie-android
- Free Lottie files: https://lottiefiles.com/free-animations  (check each file's license)
Use animations sparingly: tab open/close, tab overview, bottom sheet, and
toolbar hide/show on scroll. Keep them short (150-300ms), smooth at 144Hz, and
respect the system "remove animations" setting.

Chromium reference for building:
- https://chromium.googlesource.com/chromium/src/+/main/docs/android_build_instructions.md

## How to work
- Make small, separate changes: theme and colors first, then the toolbar and tab
  strip, then each screen. Commit after each step with a clear message.
- Reuse existing Chromium and project resources where possible. Keep the changes
  small and easy to re-apply when Chromium updates.
- Put new icons and animations in the project's resource folder, named clearly
  (for example ic_ora_tab_group.xml). Keep a CREDITS.md listing each asset's
  source link and license.
- Rename user-visible "Titanium" or old branding to "Ora" only where it is
  just text, and ask before changing package names or app IDs.
- Do not start a full Chromium build unless I ask. Tell me which command to run
  and what to check.

## Done when
- The tablet and phone screens match the wireframes in light and dark mode.
- Nothing is hard-coded: colors, sizes and icons come from resources and themes.
- I get a short list of what changed, what to test on each device, and what is left.
