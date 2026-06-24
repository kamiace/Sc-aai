# SWRLZ-CORE · Sideload guide

This recovery package intentionally does **not** point at an Emergent preview server and does not ship a stale APK.
Build the Android project locally, then install the generated debug APK.

## Build

1. Install Android Studio or the Android command-line SDK.
2. Copy `android/local.properties.example` to `android/local.properties`.
3. Set `sdk.dir` to the Android SDK location.
4. Leave `SWURLZ_BACKEND_URL` blank for offline/local-library mode, or set it to your independent HTTPS Mentor backend.
5. From `android/`, run:

```bash
./gradlew assembleDebug
```

The APK will appear at:

```text
android/app/build/outputs/apk/debug/app-debug.apk
```

## Install on Android 8.0+

1. Transfer `app-debug.apk` to the phone.
2. Allow the file manager/browser to **Install unknown apps** when Android asks.
3. Open the APK and tap **Install**.
4. Launch **SWRLZ-CORE**.

## First-run permissions

The app asks you to grant these manually:

- **Accessibility Service** — lets the supervised Local Operator read semantic UI nodes and perform approved taps, typing, scrolling, Back, and Home.
- **Display over other apps** — lets the floating telemetry strip remain visible.

## Offline behavior

- The Skill Library opens from on-device storage.
- A missing backend does not generate a raw Ktor exception.
- New cloud-planned missions, Council chat, remote synchronization, and skill distillation stay disabled until a Mentor backend is configured.

## Online Mentor setup

Run the independent FastAPI backend in `backend/` and set:

```properties
SWURLZ_BACKEND_URL=https://your-private-backend.example
SWURLZ_API_TOKEN=the-same-private-token-used-by-the-backend
```

Keep `OPENAI_API_KEY` on the backend only. Never put it in `local.properties`, the APK, or React environment files.

## Sovereignty controls

- **Pause/Resume** stops action dispatch without discarding mission state.
- **Take Over** aborts the mission and returns full control to you.
- **Approve** is required for elevated-risk actions.
- The progress watchdog stops repeated no-change actions and requests a fresh observation instead of random tapping.
