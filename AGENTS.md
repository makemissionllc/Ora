# Repository instructions

## Before making changes

- Read this file and any more specific AGENTS.md files that apply to the files being changed.
- Inspect the current Git status and both staged and unstaged diffs before editing. Preserve existing user changes, including the staged IntelliJ project files currently present in this checkout.
- Review the repository structure before deciding where a change belongs. This repository contains build scripts and patches; Chromium source is fetched into the ignored chromium directory during a build.
- Keep the Vanadium submodule’s upstream patch series, version data, and branding intact. Ora branding is applied by the root build scripts when preparing those patches for Chromium.
- The build script rewrites files in the Vanadium submodule checkout and removes selected patch files as part of its existing build preparation. Inspect submodule status before and after any build-related operation, and do not commit submodule changes unless the user explicitly requested them.
- Coordinate app identity changes across args.gn, build.sh, patch.sh, .gclient, README.md, fastlane metadata, and any affected issue templates. Keep the Android application ID valid and dot-separated.

## After each user work prompt

- Review the complete staged and unstaged diff for all work performed for the current prompt, including changes made in follow-up turns.
- Update Changelog.md with the current date and a concise record of each completed repository update. Do this for code-only, configuration-only, and documentation-only changes.
- Update README.md whenever a change alters the app identity, build process, features, dependencies, or other user-facing project information.
- Check for stale project-owned names, paths, identifiers, and links related to the change. Retain upstream project and third-party artifact names only where they identify actual upstream sources.
- In the final response, summarize the files changed, mention any build or test verification performed, and call out relevant external dependencies or limits.
