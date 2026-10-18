#!/bin/bash
# SPDX-License-Identifier: GPL-2.0-only
set -euo pipefail

ORA_ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"

python3 - "$ORA_ROOT" <<'PY'
from pathlib import Path
import shutil
import sys

ora_root = Path(sys.argv[1])

def replace_once(path, old, new):
    path = Path(path)
    text = path.read_text()
    old_count = text.count(old)
    new_count = text.count(new)
    if old_count == 1:
        path.write_text(text.replace(old, new, 1))
    elif old_count == 0 and new_count:
        return
    else:
        raise SystemExit(f"Ora toolbar patch expected one source match in {path}, found {old_count}")

resource_source = ora_root / "res/values-sw600dp/ora_toolbar_dimens.xml"
resource_destination = Path("chrome/android/java/res/values-sw600dp/ora_toolbar_dimens.xml")
resource_destination.parent.mkdir(parents=True, exist_ok=True)
shutil.copyfile(resource_source, resource_destination)

resource_gni = Path("ora/chromium_src/chrome/android/chrome_app_java_resources_ext_sources.gni")
gni = resource_gni.read_text()
resource_path = '  "java/res/values-sw600dp/ora_toolbar_dimens.xml",\n'
if '"java/res/values-sw600dp/ora_toolbar_dimens.xml"' not in gni:
    marker = "chrome_app_java_resources_ext_rel_path_sources = [\n"
    if gni.count(marker) != 1:
        raise SystemExit(f"Ora toolbar patch could not find resource list in {resource_gni}")
    resource_gni.write_text(gni.replace(marker, marker + resource_path, 1))

replace_once(
    "chrome/browser/flags/android/chrome_feature_list.cc",
    "BASE_FEATURE(kAndroidBottomBar, base::FEATURE_DISABLED_BY_DEFAULT);",
    "BASE_FEATURE(kAndroidBottomBar, base::FEATURE_ENABLED_BY_DEFAULT);",
)
replace_once(
    "chrome/browser/flags/android/java/src/org/chromium/chrome/browser/flags/ChromeFeatureList.java",
    "newCachedFlag(ANDROID_BOTTOM_BAR, false, /* defaultValueInTests= */ true);",
    "newCachedFlag(ANDROID_BOTTOM_BAR, true, /* defaultValueInTests= */ true);",
)
replace_once(
    "chrome/browser/flags/android/java/src/org/chromium/chrome/browser/flags/ChromeFeatureList.java",
    'newBooleanCachedFeatureParam(ANDROID_BOTTOM_BAR, "disable_on_ntp", true);',
    'newBooleanCachedFeatureParam(ANDROID_BOTTOM_BAR, "disable_on_ntp", false);',
)
replace_once(
    "chrome/browser/flags/android/java/src/org/chromium/chrome/browser/flags/ChromeFeatureList.java",
    'newBooleanCachedFeatureParam(ANDROID_BOTTOM_BAR, "keep_app_menu_in_toolbar", false);',
    'newBooleanCachedFeatureParam(ANDROID_BOTTOM_BAR, "keep_app_menu_in_toolbar", true);',
)
replace_once(
    "chrome/browser/ui/android/toolbar/java/src/org/chromium/chrome/browser/toolbar/settings/AddressBarPreference.java",
    "private static final @ToolbarPositionAndSource int DEFAULT_POSITION_AND_SOURCE =\n            ToolbarPositionAndSource.TOP_SETTINGS;",
    "private static final @ToolbarPositionAndSource int DEFAULT_POSITION_AND_SOURCE =\n            ToolbarPositionAndSource.BOTTOM_SETTINGS;",
)

control_container = "chrome/browser/ui/android/toolbar/java/src/org/chromium/chrome/browser/toolbar/top/ToolbarControlContainer.java"
replace_once(
    control_container,
    "mutateToolbarLayoutParams().topMargin =\n                ChromeFeatureList.sToolbarSnapshotRefactor.isEnabled() ? 0 : tabStripHeight;",
    "mutateToolbarLayoutParams().topMargin =\n                DeviceFormFactor.isNonMultiDisplayContextOnTablet(getContext())\n                        || ChromeFeatureList.sToolbarSnapshotRefactor.isEnabled()\n                        ? 0\n                        : tabStripHeight;",
)
replace_once(
    control_container,
    "        // When ToolbarSnapshotRefactor is enabled, the tab strip height offset is applied to\n"
    "        // mToolbarContainer's topMargin (in updateToolbarContainerTopMargin()) rather than\n"
    "        // mToolbarView's topMargin. Keeping mToolbarView's topMargin at 0 avoids applying the\n"
    "        // offset twice on native pages (e.g. NTP) where onBrowserControlsOffsetUpdate() is not\n"
    "        // dispatched to reset mToolbarView's topMargin after a tab strip height transition.\n"
    "        // TODO(crbug.com/557679152): Revisit once TopControlsStacker / BrowserControlsManager\n"
    "        // guarantees a resting-position offset update for non-animated transitions on native pages\n"
    "        // (NTP).",
    "        // Ora tablets keep the native toolbar at the top of the control container and place the\n"
    "        // composited tab strip beneath it. Other layouts retain Chromium's existing offset logic.",
)
replace_once(
    control_container,
    "backgroundDrawable.setLayerHeight(backgroundColorIndex, mToolbar.getTabStripHeight());",
    "backgroundDrawable.setLayerHeight(backgroundColorIndex, mToolbar.getTabStripHeight());\n"
    "        backgroundDrawable.setLayerInset(\n"
    "                backgroundColorIndex, 0, getToolbarHeight(), 0, 0);",
)
replace_once(
    control_container,
    "        // When app header state available, set the state accordingly.\n"
    "        if (appHeaderState != null && appHeaderState.isInDesktopWindow()) {\n"
    "            backgroundDrawable.setLayerInset(\n"
    "                    backgroundTabImageIndex,\n"
    "                    appHeaderState.getLeftPadding(),\n"
    "                    mTabStripTopPadding,\n"
    "                    appHeaderState.getRightPadding(),\n"
    "                    0);\n"
    "        }",
    "        int leftInset = 0;\n"
    "        int rightInset = 0;\n"
    "        if (appHeaderState != null && appHeaderState.isInDesktopWindow()) {\n"
    "            leftInset = appHeaderState.getLeftPadding();\n"
    "            rightInset = appHeaderState.getRightPadding();\n"
    "        }\n"
    "        backgroundDrawable.setLayerInset(\n"
    "                backgroundTabImageIndex,\n"
    "                leftInset,\n"
    "                getToolbarHeight() + mTabStripTopPadding,\n"
    "                rightInset,\n"
    "                0);",
)
replace_once(
    control_container,
    "        return tabStripHeight != 0 && e.getY() <= tabStripHeight;",
    "        int tabStripTop = mToolbar instanceof ToolbarTablet ? getToolbarHeight() : 0;\n"
    "        return tabStripHeight != 0\n"
    "                && e.getY() >= tabStripTop\n"
    "                && e.getY() <= tabStripTop + tabStripHeight;",
)
replace_once(
    control_container,
    "                ChromeFeatureList.sToolbarSnapshotRefactor.isEnabled() && mToolbar != null\n"
    "                        ? mToolbar.getTabStripHeight()\n"
    "                        : 0;",
    "                ChromeFeatureList.sToolbarSnapshotRefactor.isEnabled()\n"
    "                                && mToolbar != null\n"
    "                                && !(mToolbar instanceof ToolbarTablet)\n"
    "                        ? mToolbar.getTabStripHeight()\n"
    "                        : 0;",
)

replace_once(
    control_container,
    "     * Updates the top margin of {@code mToolbarContainer} to account for the horizontal tab strip\n"
    "     * height (when {@code ToolbarSnapshotRefactor} is enabled) and any top offset needed when\n"
    "     * Vertical Tabs is active.",
    "     * Updates the top margin for Android's existing tablet toolbar layout and any top offset\n"
    "     * needed when Vertical Tabs is active. Ora tablet toolbars stay above the tab strip.",
)
replace_once(
    "chrome/browser/ui/android/toolbar/java/res/layout/toolbar_tablet.xml",
    'android:layout_marginTop="@dimen/tab_strip_height"',
    'android:layout_marginTop="0dp"',
)

top_toolbar = "chrome/browser/ui/android/toolbar/java/src/org/chromium/chrome/browser/toolbar/top/TopToolbarCoordinator.java"
replace_once(
    top_toolbar,
    "if (ChromeFeatureList.sToolbarSnapshotRefactor.isEnabled()) {\n"
    "                mControlContainer.mutateToolbarLayoutParams().topMargin = 0;\n"
    "            } else {",
    "if (ChromeFeatureList.sToolbarSnapshotRefactor.isEnabled()\n"
    "                    || mToolbarLayout instanceof ToolbarTablet) {\n"
    "                mControlContainer.mutateToolbarLayoutParams().topMargin = 0;\n"
    "            } else {",
)
replace_once(
    top_toolbar,
    "            // TODO(peilinwang): This is a temporary fix for https://crbug.com/504438014. The\n"
    "            // toolbar for tablets is the only UI element where the height of its capture is\n"
    "            // different from its actual height, and sometimes the offset and capture don't get\n"
    "            // updated at the same time. When that happens, the composited layer will appear to be\n"
    "            // in the wrong place. Remove this, and the math for capture diff for the progress bar\n"
    "            // and toolbar after ToolbarSnapshotRefactor launches. This assumes that the tabstrip is\n"
    "            // always positioned right above the toolbar.\n"
    "            diff = tabStripHeight;",
    "            // TODO(peilinwang): This is a temporary fix for https://crbug.com/504438014. The\n"
    "            // toolbar capture and offset can be updated at different times on tablets.\n"
    "            // Ora's tablet capture begins at the toolbar row; the tab strip is composited below.\n"
    "            diff = mToolbarLayout instanceof ToolbarTablet ? 0 : tabStripHeight;",
)
replace_once(
    top_toolbar,
    "        int finalYOffset = mLayerYOffset - diff + hairlineAdjustment;",
    "        int tabletTabStripOffset =\n"
    "                mToolbarLayout instanceof ToolbarTablet ? tabStripHeight : 0;\n"
    "        int finalYOffset = mLayerYOffset - tabletTabStripOffset - diff + hairlineAdjustment;",
)

replace_once(
    "chrome/browser/ui/android/tabstrip/java/src/org/chromium/chrome/browser/tabstrip/TabStripTopControlLayer.java",
    "        mTabStrip.onLayerYOffsetChanged(yOffset, effectiveHeight);",
    "        int toolbarTopOffset = mControlContainer.getToolbarHeight();\n"
    "        mTabStrip.onLayerYOffsetChanged(yOffset + toolbarTopOffset, effectiveHeight);",
)
PY
