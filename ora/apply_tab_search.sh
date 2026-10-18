#!/bin/bash
# SPDX-License-Identifier: GPL-2.0-only
set -euo pipefail

python3 <<'PY'
from pathlib import Path

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
        raise SystemExit(f"Ora tab search patch expected one source match in {path}, found {old_count}")

replace_once(
    "chrome/browser/flags/android/chrome_feature_list.cc",
    "BASE_FEATURE(kTabSearchForDesktop, base::FEATURE_DISABLED_BY_DEFAULT);",
    "BASE_FEATURE(kTabSearchForDesktop, base::FEATURE_ENABLED_BY_DEFAULT);",
)
replace_once(
    "chrome/browser/flags/android/java/src/org/chromium/chrome/browser/flags/ChromeFeatureList.java",
    "newMutableFlagWithSafeDefault(TAB_SEARCH_FOR_DESKTOP, false);",
    "newMutableFlagWithSafeDefault(TAB_SEARCH_FOR_DESKTOP, true);",
)
PY
