#!/bin/bash
# SPDX-License-Identifier: GPL-2.0-only
set -euo pipefail

ORA_ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
RESOURCE_SOURCE="$ORA_ROOT/res/values/ora_shape_dimens.xml"
RESOURCE_DESTINATION="chrome/android/java/res/values/ora_shape_dimens.xml"
RESOURCE_GNI="ora/chromium_src/chrome/android/chrome_app_java_resources_ext_sources.gni"
RESOURCE_PATH='  "java/res/values/ora_shape_dimens.xml",'

if [[ ! -f "$RESOURCE_GNI" ]]; then
    echo "Ora surface shapes: missing Chromium resource extension list: $RESOURCE_GNI" >&2
    exit 1
fi

mkdir -p "$(dirname -- "$RESOURCE_DESTINATION")"
cp -f "$RESOURCE_SOURCE" "$RESOURCE_DESTINATION"
if ! grep -Fq '"java/res/values/ora_shape_dimens.xml"' "$RESOURCE_GNI"; then
    sed -i "/^chrome_app_java_resources_ext_rel_path_sources = \[$/a\\$RESOURCE_PATH" "$RESOURCE_GNI"
fi
