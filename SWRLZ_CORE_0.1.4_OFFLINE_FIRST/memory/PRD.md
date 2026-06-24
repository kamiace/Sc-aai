# SWRLZ-CORE — Product Requirements (v0.1.4 offline-first recovery)

## Product intent

A private Android personal action AI with three cooperating roles:

1. **User Sovereign** — defines goals, permissions, corrections, and final approval.
2. **Local Operator** — observes semantic UI state, performs supervised actions, stores local skills, and remains useful offline.
3. **Optional Swurlz Mentor** — provides online research, planning, Council conversation, and skill distillation through an independently hosted backend.

## Current architecture

- `android/` — Kotlin + Jetpack Compose client, AccessibilityService, overlay telemetry, local Skill Capsule DataStore, guarded mission runner, repetition/no-progress watchdog.
- `backend/` — independent FastAPI + SQLite service. Storage works offline from AI providers; OpenAI Mentor capabilities are optional.
- `frontend/` — React Council Cockpit, Planner Lab, local-first Skill Library, mission telemetry.

## Non-negotiable behavior

- No hard dependency on an expiring preview host.
- No AI-provider key embedded in a mobile or browser client.
- No destructive or security-sensitive action without explicit user approval.
- Every action must have an expected result and a verification path.
- Repeated actions with unchanged state must be interrupted and diagnosed.
- Local data must remain available through network, provider, quota, or backend outages.
- The user can pause, take over, inspect the action ledger, revert configuration changes, and delete learned skills.

## Implemented in this recovery

- Local-first Android and browser Skill Libraries.
- Defensive HTTP status/content-type validation.
- Independent SQLite backend with optional token authentication.
- Optional online planning, Council chat, memory, and skill distillation.
- Mission event persistence and WebSocket telemetry.
- Progress watchdog for repeated actions and unchanged screens.
- Secret/configuration templates and Docker backend packaging.

## Next build milestones

- Execute trusted Skill Capsules locally without cloud replanning.
- Add explicit skill editor, version history, conflict resolution, export/import, and rollback.
- Add condition-based waits using accessibility events instead of fixed delays.
- Add Android local Council chat and durable conversation memory UI.
- Add optional on-device speech recognition/TTS and a small local model.
- Add screenshot/MediaProjection perception only with visible consent and automatic sensitive-field redaction.
- Add automated Android unit/UI tests and signed release build workflow.
