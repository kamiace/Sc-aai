# SWRLZ-CORE 0.1.4 Offline-First Patch Report

## Failure repaired

The original client used an expired Emergent preview origin as its default backend.
`GET /api/skills` then returned `404 text/plain`, which the Android Ktor client attempted
to decode as `List<SkillCapsule>`. This produced the app-wide transformation exception.

## Android repairs

- Backend origin now defaults to blank instead of a preview service.
- Skill Capsules are stored locally with Android DataStore and shown before any sync.
- Network refresh is optional and preserves local data on failure.
- HTTP status and content type are validated before JSON decoding.
- User-facing errors are short and actionable rather than raw stack traces.
- Mission planning stops cleanly when no Mentor is configured.
- A progress watchdog blocks repeated no-change actions and escalates after bounded retries.

## Web repairs

- Browser Skill Library is local-first through `localStorage`.
- Backend URL is optional and no dead origin is embedded.
- Council/mission features expose a clear offline state.
- Remote responses are status/content-type validated.
- Stale APK download was removed.

## Backend repairs

- Replaced Emergent/MongoDB dependencies with FastAPI + SQLite.
- Core missions, events, skills, and Council history work without an AI key.
- Online Mentor calls are optional through OpenAI Responses API.
- Added health diagnostics, API-token protection, Docker packaging, and offline tests.

## Not performed automatically

- No production server was deployed because that requires a hosting account/domain controlled by the owner.
- No OpenAI key is embedded in the project.
- An Android APK is included only when the build environment can obtain the required Android SDK and Gradle dependencies.

## Validation performed

- Backend syntax check: passed.
- Backend regression suite: **8 tests passed** (health, local storage, missions, events, skills, stubbed planning, Council SSE/history, skill distillation, WebSocket).
- React production build: **compiled successfully**.
- Secret/dead-runtime scan: no embedded Emergent key, OpenAI key, Mongo URL, or active preview URL remains in runtime code.
- Android source was reviewed and repackaged, but an APK could not be compiled in this workspace because the Android SDK is not installed and external SDK downloads are unavailable here. The package includes exact Gradle build instructions and an SDK configuration template.
