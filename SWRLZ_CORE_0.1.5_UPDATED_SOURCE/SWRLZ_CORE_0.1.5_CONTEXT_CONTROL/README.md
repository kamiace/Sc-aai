# SWRLZ-CORE 0.1.5 — Context and Control Foundation

This patch advances the verified 0.1.4 offline-first APK toward a device-aware supervised automation agent.

## Main additions

- Filter-aware **ALLOW SHOWN**, **BLOCK SHOWN**, and **RESET** controls.
- All/partial/none visible-selection status.
- Device Context Capsule: model, Android/SDK, display, locale, battery, network, capabilities, and hashed node identity.
- Foreground App Context: package, label, version, system status, window class, installer, permissions, and framework hints.
- Accessibility-tree Screen Delta for meaningful change detection.
- Privacy redaction for password nodes, likely OTPs, account/payment-number patterns, and token-like text.
- Minimal, Standard, Detailed, and Local-Only context levels.
- Setup screen with editable Mentor URL, encrypted API token, connection test, Context Viewer, and one-mission context approval gate.
- Local-first Skill Library and graceful offline behavior.
- Stronger no-progress/repeated-action watchdog.
- Independent FastAPI + SQLite backend updated to accept 0.1.5 context fields.
- Detailed master roadmap with a plain-language description for every checkbox.

## Build from Codespaces or Android Studio

1. Copy `android/local.properties.example` to `android/local.properties`.
2. Set `sdk.dir` to the available Android SDK path. In GitHub Codespaces this is normally `$ANDROID_HOME`.
3. From the `android` directory run:

```bash
printf 'sdk.dir=%s\n' "$ANDROID_HOME" > local.properties
chmod +x gradlew
./gradlew --no-daemon --stacktrace clean assembleDebug
```

The APK should appear at:

```text
android/app/build/outputs/apk/debug/app-debug.apk
```

## Important test status

The backend syntax and local storage tests passed in this patch environment. The Android source could not be compiled here because no Android SDK is mounted. The user should compile it in the existing Codespaces environment and use `SWRLZ_CORE_0.1.5_PHONE_TEST_CHECKLIST.md` to report results.

## Graceful offline mode

Graceful offline mode means the app remains usable when no online Mentor is configured or reachable. Local skills, allowlist controls, context inspection, permissions, and safety UI still work. Online mission planning reports a short explanation instead of crashing or displaying raw Ktor/HTTP decoding errors.
