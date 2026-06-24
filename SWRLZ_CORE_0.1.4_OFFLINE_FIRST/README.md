# SWRLZ-CORE 0.1.4 — Offline-First Recovery Build

This project has been detached from the expired Emergent preview backend.
The Android and web Skill Libraries now load local data first. Online planning,
Council chat, synchronization, and skill distillation are optional features that
activate only when an independent Mentor backend is configured.

## What changed

- Removed the hard-coded `instant-design-13.preview.emergentagent.com` fallback.
- Added on-device Android Skill Capsule persistence with DataStore.
- Added browser-local Skill Capsule persistence with `localStorage`.
- Added defensive HTTP status/content-type handling; a 404 is no longer parsed as JSON.
- Replaced MongoDB/Emergent integrations with a self-contained FastAPI + SQLite backend.
- Added optional OpenAI Mentor support, local Council history, mission telemetry, and API-token protection.
- Added a repetition/no-progress watchdog to the Android mission runner.
- Removed committed secrets and the stale APK placeholder.

## Android

1. Copy `android/local.properties.example` to `android/local.properties`.
2. Set `sdk.dir` to your Android SDK path.
3. Leave `SWURLZ_BACKEND_URL` blank for offline-only use, or set it to your independent HTTPS backend.
4. Optionally set the same `SWURLZ_API_TOKEN` used by the backend.
5. Run:

```bash
cd android
./gradlew assembleDebug
```

The debug APK will be written under `android/app/build/outputs/apk/debug/`.

## Backend

See `backend/README.md`. Core storage routes work with no AI key. Set `OPENAI_API_KEY`
only when online Mentor assistance is desired.

## Web cockpit

```bash
cd frontend
cp .env.example .env
npm install
npm run build
```

Leave `REACT_APP_BACKEND_URL` blank for an offline/local Skill Library. Set it to the
independent backend origin to enable missions, Council chat, and synchronization.

## Security

This is a private research build. Review permissions, keep approval gates enabled,
and never expose the backend publicly without TLS and a strong `SWRLZ_API_TOKEN`.
