# SWRLZ-CORE Pzhp-LDS Build-Ready Patch Report

## Patch purpose

Prepare the source package for the user's phone-first workflow:

ChatGPT / Swurlz produces a full source ZIP -> user uploads ZIP to GitHub Codespaces -> Codespaces extracts and builds APK -> Codespaces zips APK -> user downloads APK ZIP to Android and installs/tests.

## Added files

- `scripts/build_android_debug_bundle.sh`
- `BUILD_APK_FROM_SOURCE_ZIP.md`
- `SWRLZ_CORE_BUILD_READY_PATCH_REPORT.md`

## Build helper behavior

The build helper:

1. Locates the project root and `android/gradlew`.
2. Validates `ANDROID_HOME` or `ANDROID_SDK_ROOT`.
3. Writes `android/local.properties` with `sdk.dir`.
4. Runs `./gradlew --no-daemon --stacktrace clean assembleDebug`.
5. Copies the debug APK into `APK_DOWNLOAD/`.
6. Creates APK and build-log SHA-256 checksums.
7. Creates one downloadable ZIP containing APK, checksums, and build log.
8. Does not edit source files if the build fails.

## Validation performed here

- Bash syntax check passed for `scripts/build_android_debug_bundle.sh`.
- `--help` mode executed successfully.
- Source ZIP integrity check passed.

## Android build status

Android APK compilation was not run in this environment because the Android SDK is not mounted here. The script is intended for Codespaces or another Linux builder with Android SDK available.
