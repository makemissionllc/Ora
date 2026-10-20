#!/bin/bash
# SPDX-License-Identifier: GPL-2.0-only
set -euo pipefail

ORA_ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
CHROME_ANDROID="chrome/android/java"
ORA_CHROME_ANDROID="ora/chromium_src/chrome/android"
APP_RESOURCES_GNI="$ORA_CHROME_ANDROID/chrome_app_java_resources_ext_sources.gni"
APP_STYLES="$CHROME_ANDROID/res/values/styles.xml"

for path in "$APP_RESOURCES_GNI" "$APP_STYLES"; do
    if [[ ! -f "$path" ]]; then
        echo "Ora UI foundation: expected Chromium file is missing: $path" >&2
        exit 1
    fi
done

copy_resource() {
    local source="$1"
    local destination="$2"
    mkdir -p "$(dirname -- "$destination")"
    cp -f "$source" "$destination"
}

copy_resource "$ORA_ROOT/res/values/ora_colors.xml" "$ORA_CHROME_ANDROID/java/res/values/ora_colors.xml"
copy_resource "$ORA_ROOT/res/values-night/ora_colors.xml" "$ORA_CHROME_ANDROID/java/res/values-night/ora_colors.xml"
copy_resource "$ORA_ROOT/res/values/ora_theme.xml" "$ORA_CHROME_ANDROID/java/res/values/ora_theme.xml"
copy_resource "$ORA_ROOT/res/values-night/ora_theme.xml" "$ORA_CHROME_ANDROID/java/res/values-night/ora_theme.xml"

add_resource_source() {
    local resource_path="$1"
    if ! grep -Fq "\"$resource_path\"" "$APP_RESOURCES_GNI"; then
        sed -i "/^chrome_app_java_resources_ext_rel_path_sources = \[$/a\\  \"$resource_path\"," "$APP_RESOURCES_GNI"
    fi
}

add_resource_source "java/res/values/ora_colors.xml"
add_resource_source "java/res/values-night/ora_colors.xml"
add_resource_source "java/res/values/ora_theme.xml"
add_resource_source "java/res/values-night/ora_theme.xml"

sed -i 's|<style name="Base.Theme.Chromium" parent="Theme.BrowserUI.DayNight">|<style name="Base.Theme.Chromium" parent="Theme.Ora">|' "$APP_STYLES"
sed -i 's|<style name="Base.Theme.Chromium.Activity.Fullscreen" parent="Theme.BrowserUI.DayNight">|<style name="Base.Theme.Chromium.Activity.Fullscreen" parent="Theme.Ora">|' "$APP_STYLES"

if grep -Fq '<style name="Base.Theme.Chromium" parent="Theme.BrowserUI.DayNight">' "$APP_STYLES" || \
        grep -Fq '<style name="Base.Theme.Chromium.Activity.Fullscreen" parent="Theme.BrowserUI.DayNight">' "$APP_STYLES"; then
    echo "Ora UI foundation: failed to connect the Ora app theme in $APP_STYLES" >&2
    exit 1
fi
