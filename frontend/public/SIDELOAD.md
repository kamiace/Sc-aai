# SWRLZ-CORE sideload guide

This source package does not bundle a stale APK. Build the Android project, then install the newly generated `app-debug.apk`.

```bash
cd android
./gradlew assembleDebug
```

The APK appears at `android/app/build/outputs/apk/debug/app-debug.apk`.

The app now supports graceful offline mode: local features remain available when no Mentor backend is configured, and readable status messages replace raw network exceptions.
