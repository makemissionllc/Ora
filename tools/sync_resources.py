#!/usr/bin/env python3
"""Stage Ora XML resources at the paths consumed by Chromium's GN extension."""
from pathlib import Path
import re
import shutil
import sys

ROOT = Path(__file__).resolve().parent.parent

def sync_resources(chromium: Path) -> int:
    extension = chromium / 'ora/chromium_src/chrome/android'
    manifest = extension / 'chrome_app_java_resources_ext_sources.gni'
    text = manifest.read_text()
    marker = 'chrome_app_java_resources_ext_rel_path_sources = [\n'
    if text.count(marker) != 1:
        raise ValueError(f'Expected one Ora resource list in {manifest}')
    resources = sorted((ROOT / 'res').glob('values*/ora_*.xml'))
    if not resources:
        raise ValueError('No Ora XML resources found')
    changed = 0
    for source in resources:
        resource_path = source.relative_to(ROOT / 'res')
        relative = 'java/res/' + resource_path.as_posix()
        # Older staging scripts put these XML files in Chromium's main tree.
        # Its resource checker rejects unlisted files there. Remove only exact
        # repository copies; preserve any separately edited source file.
        legacy = chromium / 'chrome/android/java/res' / resource_path
        if legacy.is_file():
            if legacy.read_bytes() != source.read_bytes():
                raise ValueError(
                    f'Legacy Ora resource has local edits: {legacy}. '
                    'Merge those edits into the repository res/ file before syncing.'
                )
            legacy.unlink()
        destination = extension / relative
        destination.parent.mkdir(parents=True, exist_ok=True)
        if not destination.exists() or destination.read_bytes() != source.read_bytes():
            shutil.copyfile(source, destination)
            changed += 1
        if f'"{relative}"' not in text:
            text = text.replace(marker, marker + f'  "{relative}",\n', 1)
    if manifest.read_text() != text:
        manifest.write_text(text)
    block = text.split(marker, 1)[1].split(']', 1)[0]
    missing = [name for name in re.findall(r'"([^"\n]+)"', block)
               if not (extension / name).is_file()]
    if missing:
        raise ValueError(f'Ora GN resource files are missing: {missing}')
    return changed

if __name__ == '__main__':
    try:
        changed = sync_resources(Path(sys.argv[1]).resolve())
    except (OSError, ValueError, IndexError) as error:
        raise SystemExit(f'Ora resource sync failed: {error}')
    print(f'Ora XML resources verified ({changed} updated).')
