# SWRLZ-CORE 0.1.5 Phone Test Checklist

Record pass/fail notes under each item. Screenshots are especially useful for failures.

## Installation and version

- [ ] Build completes with `assembleDebug`.
- [ ] APK installs over 0.1.4 without a signing conflict.
- [ ] Home shows `0.1.5-context-control` and version code `5`.
- [ ] Existing local app data is preserved when installed as an update.

## Graceful offline mode

- [ ] With the Mentor URL blank, Home shows **GRACEFUL OFFLINE MODE**.
- [ ] Skills opens without a raw Ktor/404 exception.
- [ ] Cockpit mission attempt gives a short “Mentor not configured” explanation.
- [ ] Allowlist, Setup, Permissions, and local Skills remain usable offline.

## Allowlist bulk controls

- [ ] With no search and SYS off, **ALLOW SHOWN** enables all displayed non-system apps.
- [ ] Already-enabled apps stay enabled.
- [ ] **BLOCK SHOWN** disables all displayed apps without changing hidden apps.
- [ ] Search for one app, then bulk allow/block; only matching apps change.
- [ ] Turn SYS on; system apps appear and bulk allow asks for confirmation.
- [ ] Protected system packages are skipped by bulk allow.
- [ ] Count shows `allowed / shown` correctly.
- [ ] NONE, PARTIAL, and ALL states appear correctly.
- [ ] RESET returns the conservative default allowlist.

## Mentor Setup

- [ ] Backend URL saves and remains after closing/reopening the app.
- [ ] API token saves and remains after reopening.
- [ ] Token is visually hidden in the field.
- [ ] Blank URL reports offline mode rather than an exception.
- [ ] Invalid URL gives a readable connection error.
- [ ] Valid backend URL returns Connected.

## Context Viewer

- [ ] Device model, Android version, screen metrics, battery, and network appear.
- [ ] Current foreground package/app information appears when Accessibility is enabled.
- [ ] Minimal level removes detailed identifiers and coordinates.
- [ ] Standard level omits bounds but retains useful control descriptions.
- [ ] Detailed level includes node bounds and fuller app context.
- [ ] Local Only displays no outgoing device/app/screen data.
- [ ] A password field's value is absent/redacted.
- [ ] Six-digit OTP-like text is redacted.
- [ ] Token-like or long account-number text is redacted.
- [ ] APPROVE NEXT allows exactly one mission when preview approval is required.

## Mission runner and watchdog

- [ ] With a valid backend, a mission sends Device/App/Delta context successfully.
- [ ] A target app outside the allowlist is refused.
- [ ] A planned node tap still works; package checking does not invalidate the node index.
- [ ] Repeated unchanged screens trigger a warning.
- [ ] Continued no-progress behavior pauses safely.
- [ ] Three repeated identical actions are blocked when no progress is observed.
- [ ] Pause, Resume, Approve, and Take Over still work.

## Skill Library

- [ ] Local cache loads immediately.
- [ ] Backend failure does not erase local skills.
- [ ] Remote skills merge into local storage when synchronization works.

## Report format

Reply with item IDs or section names plus **PASS**, **FAIL**, or **PARTIAL**, and include the exact displayed error for failures. The roadmap will be updated only after observed results.
