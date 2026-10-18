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
        raise SystemExit(f"Ora reader patch expected one source match in {path}, found {old_count}")

css_path = "components/dom_distiller/core/css/distilledpage_common.css"
replace_once(css_path, "--google-blue-300: 138, 180, 248;", "--google-blue-300: 108, 147, 255;")
replace_once(css_path, "--google-blue-600: 26, 115, 232;", "--google-blue-600: 59, 109, 240;")
replace_once(css_path, "--google-grey-900: 32, 33, 36;", "--google-grey-900: 35, 38, 47;")
replace_once(
    css_path,
    ".dark {\n  background-color: rgb(var(--google-grey-900));\n  color: #FFF;\n}",
    ".dark {\n  background-color: #141518;\n  color: #ecedf0;\n}",
)
replace_once(
    css_path,
    ".dark code,\n.dark pre {\n  background-color: #000;\n  border-color: rgba(255, 255, 255, 0.5);\n}",
    ".dark code,\n.dark pre {\n  background-color: #2e3138;\n  border-color: rgba(255, 255, 255, 0.5);\n}",
)
replace_once(
    css_path,
    ".light tbody tr:nth-child(odd) {\n  background-color: #EEE;\n}",
    ".light tbody tr:nth-child(odd) {\n  background-color: #e6e8ed;\n}",
)
replace_once(
    css_path,
    ".dark tbody tr:nth-child(odd) {\n  background-color: #333;\n}",
    ".dark tbody tr:nth-child(odd) {\n  background-color: #2e3138;\n}",
)
replace_once(
    css_path,
    ".light th,\n.light td {\n  border-inline-start: 2px solid #AAA;\n}",
    ".light th,\n.light td {\n  border-inline-start: 1px solid #d7dae1;\n}",
)
replace_once(
    css_path,
    ".dark th,\n.dark td {\n  border-inline-start: 1px solid #555;\n}",
    ".dark th,\n.dark td {\n  border-inline-start: 1px solid #424650;\n}",
)
path = Path(css_path)
text = path.read_text()
responsive = "\n\n/* Ora responsive reader layout. */\n@media (min-width: 720px) {\n  #main-content {\n    max-width: 720px;\n  }\n\n  #content {\n    margin: 32px 24px;\n  }\n}\n"
if "/* Ora responsive reader layout. */" not in text:
    path.write_text(text.rstrip() + responsive)
PY
