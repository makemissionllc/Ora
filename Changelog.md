# Changelog

Changes are recorded here for each repository update.

## 2026-10-18

### Added

- Added Ora light and dark Material 3 color roles and connected them through the existing Chromium extension-resource hook. The Ora palette disables Android dynamic colors so the interface uses the wireframe colors.
- Enabled Chromium’s phone bottom toolbar by default, kept its menu button available on the browser bar and start page, and defaulted the existing Address bar preference to bottom. Added tablet-specific toolbar, address pill, and tab strip sizing and placed the tab strip below the toolbar.
- Matched shared Chromium card, settings-card, and bottom-sheet corner radii to the Ora wireframes.

## 2026-10-17

### Changed

- Rebranded the generated Android app as Ora Browser and set its application ID to `com.makemission.ora`.
- Updated build-time patch rewriting and Chromium resource paths to use the Ora namespace.
- Updated the bundled extension output name and filter-list destination to use the Ora namespace.
- Replaced inherited Titanium project documentation and listing text with Ora Browser information. Documented the separately signed companion extension’s upstream publisher.
- Updated the issue template to link to the Ora repository.
- Added repository guidance requiring a review of cumulative changes and a changelog update for each future work prompt.
