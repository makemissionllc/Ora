# Ora: open in apps, extensions from AMO, passkeys, and the notice

Mockups: canvas pages "Apps, extensions, passkeys" and "Onboarding"
(board "Onboarding · Before you start"). Follow AGENTS.md. One commit
per section, phone and tablet together, device checks recorded in
PROGRESS.md. Check every API named here in GeckoView 160 / the Android
SDK first; if one doesn't exist, stop and say so.

---

## 1. Open links in apps

Setting: Settings > Open links in apps: **Ask** (default) / **Always** /
**Never**. Per-site choices are kept per profile in
`ora_app_links_<profileId>`, listed in the same Settings page with
Remove.

When it applies:
- A top-level navigation the user caused (tap, typed address, link
  from another app), to a different site than the current page.
  Never for redirects inside the same site, subframes or page scripts.
- Not in private tabs by default: always Ask, and never remember.

How to detect an app:
- Build `Intent(ACTION_VIEW, uri)` with `CATEGORY_BROWSABLE`.
- Find non-browser handlers with `queryIntentActivities` (add a
  `<queries>` entry for VIEW http/https), and drop any app that also
  handles a generic `https://example.invalid` (that's a browser).
- To open, use `Intent.FLAG_ACTIVITY_REQUIRE_NON_BROWSER` and catch
  `ActivityNotFoundException`. On failure, stay in Ora and log it.

What the user sees (board **Open in app: ask once, remember**):
- Ask: a card above the bottom bar: "Open in the YouTube app?" with
  Stay in Ora / Always / Open. The page keeps loading in Ora behind it.
- An "App" chip inside the address capsule while the page has an app,
  so the choice is always one tap away.
- `intent:`, `market:`, `tel:`, `mailto:` and other non-web schemes:
  the same card ("Open in Phone?"). For `intent:` with a fallback URL,
  "Stay in Ora" loads the fallback.
- Log "Opened in app (<package>) for <domain>" / "Stayed in Ora",
  domain only, nothing from private tabs.

## 2. Extensions from addons.mozilla.org and from files

Bug: tapping "Add to Firefox" on AMO downloads the `.xpi` instead of
installing it.

Fix:
- Any response with type `application/x-xpinstall`, or a URL ending in
  `.xpi`, is not a download. Hand it to
  `WebExtensionController.install(url)` and show Ora's install sheet
  (board **AMO link installs, not downloads**): name, "Signed by Mozilla,
  from addons.mozilla.org", what it can do (permissions), "Allow in
  private tabs" (off), Cancel / Add extension. Use GeckoView's install
  prompt delegate for the permissions; check its exact name in 160.
- Installing from addons.mozilla.org needs one confirmation (the sheet).
  From any other site, the sheet also says where it came from.
- Errors in plain words: not signed, not compatible with this version,
  corrupt file, network failed.

Install from a file (board **Install from a file**):
- Extensions screen gets "Install from a file": the system file picker
  (`ACTION_OPEN_DOCUMENT`, `.xpi`), copy to app cache, install from that
  file URI, then delete the copy.
- If a `.xpi` is already in Downloads/Ora, show a dashed "Found in
  Downloads" card with Install. In the Downloads list, an `.xpi` row's
  action is Install, not Open.
- Signed extensions only (GeckoView release enforces signing). Say so
  under the list.

## 3. Passkeys

Facts to work with:
- GeckoView already implements WebAuthn on Android through Google
  Play's FIDO2 library and Android's Credential Manager.
- Signing in on behalf of websites needs Google's approval of the
  browser's package as a privileged app. Until Ora
  (`com.makemission.ora`) is approved, sites will likely fail with
  "Browser not authorized".

Work:
1. Make sure WebAuthn is turned on in Ora's GeckoView setup, and test
   on webauthn.io (register and sign in) on the phone (Android 14+).
   Record exactly what happens.
2. If it fails as "not authorized", show the card (board **Passkey not
   available yet**): "Passkeys aren't available in Ora yet", with
   "Use a password" and "Open in main browser" (hands the URL to the
   default browser with a chooser). Log the failure with the domain.
3. Security keys (USB/NFC) may still work through the FIDO2 path:
   test and record.
4. Write the approval steps in PROGRESS.md under Pending for the owner:
   Google's privileged-app request form, the signing certificate
   SHA-256, and the package name. Don't fake approval or ship any
   workaround that bypasses it.

## 4. "Before you start" notice

Board **Onboarding · Before you start**. It's the first onboarding
screen, before Welcome, shown once. One button: "I understand".
"Read the full notice" opens the full text. The full text is also at
Settings > About > Notice and privacy.

Short version (on the screen):
- Ora is a personal project that's still being built. It hasn't had a
  security audit.
- Use your main browser for banking, payments, health and work
  accounts.
- Your browsing stays on this device. Ora has no servers and collects
  nothing.
- A diagnostic log on this device helps find bugs. It keeps site names
  only, never full addresses, searches or private tabs, and leaves only
  if you export it.
- Small print: Under Kenya's Data Protection Act, 2019 you can ask
  what's kept about you and have it deleted. In Ora you can see and
  delete everything yourself in Settings. Ora is provided as-is,
  without warranty.

Full notice (Settings > About > Notice and privacy):

> **Ora is experimental.** Ora is a personal, educational project in
> active development. It has not been independently audited for
> security. Don't use it for banking, payments, health records, work
> accounts or anything else you can't afford to have exposed.
>
> **What stays on your device.** Tabs, history, bookmarks, saved
> passwords, downloads, settings and anything Ora learns about how you
> browse are stored only on this device, separately for each profile.
> Ora has no servers and does not send this data anywhere. Websites you
> visit and services you choose (for example your search engine) receive
> what any browser sends them.
>
> **The diagnostic log.** Ora keeps a log on this device to find and fix
> bugs. It records events such as "page loaded on example.com" and
> errors. It never records full web addresses, what you search for, or
> anything from private tabs. The log is kept up to about 5,000 lines.
> It leaves your device only if you export and share it yourself. You
> can view and clear it in Settings > Diagnostics.
>
> **Your rights.** If you are in Kenya, the Data Protection Act, 2019
> gives you rights over your personal data, including to be told how it
> is used, to access it, to correct it and to have it deleted. The
> Office of the Data Protection Commissioner (odpc.go.ke) handles
> complaints. Because Ora keeps your data only on your device, you can
> exercise these yourself: Settings > Privacy (history, cookies, site
> permissions, saved passwords), Settings > Diagnostics (log) and
> "What Ora has learned". Uninstalling Ora deletes everything.
>
> **No warranty.** Ora is provided "as is", without warranties of any
> kind. To the extent the law allows, the developer is not responsible
> for loss or exposure of data while using it.

Notes for building:
- Store "notice accepted" with the date and the notice version. If the
  notice text changes, show it again.
- No network calls. Plain words. Mineral look.
- Owner: have the "No warranty" and rights wording checked by someone
  qualified before a public release; consumer law can limit what a
  disclaimer does.

## 5. Order

1. Extensions from AMO and files (fixes a bug you hit today).
2. Open links in apps.
3. Notice screen and the full notice.
4. Passkeys test and fallback card; approval steps written down.
