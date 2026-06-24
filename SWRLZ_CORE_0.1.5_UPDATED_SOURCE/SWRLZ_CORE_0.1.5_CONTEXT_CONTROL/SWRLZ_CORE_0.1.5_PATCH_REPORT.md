# SWRLZ-CORE 0.1.5 Patch Report

## Patch name

**Context and Control Foundation**

## Coded in this source

- `CTRL-001` through `CTRL-009`: filtered bulk allow/block/reset controls and visible selection state.
- `CTX-001` through `CTX-003`: Device Context Capsule and stable hashed node identity.
- `APP-001` and `APP-002`: foreground app metadata, permissions, and framework hints.
- `DELTA-001`: accessibility-tree delta generation.
- Partial `CTX-004`, `DELTA-002`, and `DELTA-003`: context is refreshed each preview/mission and screen changes feed the watchdog; richer event detectors and action-specific expected-state verification remain planned.
- Partial `PRIV-001` through `PRIV-003`: context levels, local preview, and several sensitive-value redactors are present; secure-window detection, private-app policy, and persistent provider transmission auditing remain planned.
- `MENTOR-001` through `MENTOR-004`: editable backend, Keystore-backed encrypted token, health test, readable diagnostics, and optional preview approval for the next mission.
- Local-first Skill Library and independent SQLite backend retained.
- Version bumped to `0.1.5-context-control`, version code `5`.

## Validation performed here

- Python backend syntax: passed.
- FastAPI local health/storage tests: **2 passed**.
- Dead Emergent runtime URL scan: removed from active source/configuration.
- Android compilation: not available in this runtime because the Android SDK is absent.

## Important implementation note

Roadmap items coded in this source remain unchecked until the APK builds and the user verifies them on the phone. The detailed roadmap distinguishes **Verified**, **Coded**, **Partial**, and **Planned** status.
