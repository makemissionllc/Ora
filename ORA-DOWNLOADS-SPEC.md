# Ora downloads: a solid download manager

Goal: downloads that behave like a dedicated download manager (FDM and
similar): fast, resumable, and hard to break, without leaking anything
from private tabs. Boards: the "Downloads" page of the mockup canvas.

Follow AGENTS.md for commits, device checks and docs. Do the phases in
order.

---

## 1. What "solid" means (acceptance list)

A download must survive all of these and finish with a correct file:

- Wi-Fi drops for 30 s, then returns.
- Switching from Wi-Fi to mobile data (and the reverse).
- Ora swiped away from recents. Ora force-stopped. The phone rebooted.
- Low storage at the start (refuse with a clear message) and mid-way
  (pause with "Not enough space").
- A server that doesn't support resuming (restart cleanly, say so).
- A link that expires (signed URLs): offer "Open the page again to
  refresh the link".
- Two files with the same name (save as "name (2).ext").
- 3 large downloads at once while browsing, without the browser
  stuttering.

## 2. How a download starts

1. GeckoView reports a download (`onExternalResponse`). Ora reads the
   name (Content-Disposition, then URL, then MIME type), size, type and
   whether the server accepts ranges (`Accept-Ranges: bytes`).
2. Ora cancels GeckoView's own stream and shows the **Download sheet**:
   editable file name, size and source site, save location, "Split into
   N connections" (only shown if ranges are supported), "Only on
   Wi-Fi", Cancel / Download.
3. Long-press a link → "Download link" opens the same sheet.

## 3. The engine

```
download/
  DownloadEngine      queue, limits, scheduling
  DownloadTask        one file: segments, state machine
  SegmentWorker       one HTTP range request
  DownloadStore       Room database (tasks + segments)
  DownloadService     user-initiated job / foreground service
  DownloadNotifier    notifications and actions
```

- **Network**: make requests with `GeckoWebExecutor` so cookies, logins,
  proxy, HTTPS-only and Secure DNS match the tab that started the
  download. Send the original Referer and User-Agent. For private tabs,
  use the private flag (check `FETCH_FLAGS_PRIVATE` in GeckoView 160).
  If something can't be done through GeckoWebExecutor, say so before
  adding another HTTP client.
- **Segments**: if the server supports ranges and the size is known
  and over 8 MB, split into N segments (default 4, setting 1 to 8).
  Pre-allocate the file and write each segment at its offset with a
  `FileChannel`, so there is no merge step. When a segment finishes
  early, split the largest remaining one (dynamic splitting).
- **State machine** per task: Queued → Connecting → Downloading →
  (Paused | Waiting for network | Waiting for Wi-Fi | Failed) →
  Verifying → Done. Every transition is saved to the store first, then
  shown.
- **Resume**: every segment's progress is saved at least every 2 s and
  on pause. On resume, check the server's ETag / Last-Modified. If they
  changed, restart from zero and tell the user why.
- **Retries**: network errors retry with backoff (2 s, 4 s, 8 s, up to
  60 s, 10 tries), then Failed with the reason. HTTP 403/410 on a
  previously working link means "Link expired".
- **Queue**: 3 downloads at once by default (setting 1 to 6). Others
  wait in order. The queue can be reordered by dragging.
- **Limits**: optional speed limit (setting), "Only on Wi-Fi", pause when
  Battery Saver is on (setting, off by default).
- **Background**: on Android 14+ run as a user-initiated data transfer
  job; on older versions use a `dataSync` foreground service. Check the
  current platform limits for each before building.
- **Integrity**: after the last byte, check the size matches. The user
  can paste a SHA-256 or MD5 in details; Ora shows "Matches" (Lichen) or
  "Doesn't match" (Ember).

## 4. Files and safety

- Default folder: Downloads/Ora through MediaStore. A different folder
  can be picked with the system picker (Storage Access Framework) and is
  remembered.
- Write to "name.ext.part" and rename when Done, so half files never
  look finished.
- Never open APKs or other executable types automatically. Show "This
  type of file can harm your device" before opening them.
- Private tab downloads: the file is saved (the user asked for it), but
  the entry is removed from the list when private tabs close, and it's
  never in history or the learning store. Say this on the sheet.

## 5. Screens

- **Phone · Downloads**: summary row ("2 downloading, 6.4 MB/s", Pause
  all), filter (All, Active, Done, Failed), Active section with a split
  progress bar (one part per connection), Today and earlier sections.
  Each row: type tile, name, status line, one action (pause, resume,
  retry or open).
- **Phone · Download sheet**: as in section 2.
- **Tablet · Downloads**: list on the left, details on the right: split
  progress, speed, time left, connections, size, source page, folder,
  checksum field, actions (Pause, Open folder, Copy link, Cancel).
- **Notification**: one per active download, grouped, with Pause /
  Resume / Cancel. Done shows Open.
- Status lines are plain: "312 MB of 1.2 GB, 4.1 MB/s, 3 min left",
  "Paused", "Waiting for Wi-Fi", "Link expired. Open the page again to
  refresh it.", "Server can't resume. Starting again."

## 6. Phases

1. Store, state machine, single connection, pause/resume, survives
   force-stop and reboot. Notification with actions.
2. Download sheet, file naming, .part files, folder choice, safety
   warnings.
3. Segments with dynamic splitting, queue and limits, Wi-Fi only.
4. Retries, link expiry, ETag checks, checksum, low storage.
5. Screens: phone list, tablet two-pane, filters, drag to reorder.
6. Private tab rules, logging (domain only), PROGRESS checks for the
   whole acceptance list in section 1.

## 7. First prompt for Claude Code

```
Read Designs/ORA-DOWNLOADS-SPEC.md. Plan phase 1 only: DownloadStore
(Room), DownloadTask state machine, one connection through
GeckoWebExecutor with Range resume, pause/resume, recovery after
force-stop and reboot, and a notification with Pause/Resume/Cancel.
Tell me which background mechanism you'll use on this phone's Android
version and why. Show the plan before writing code.
```
