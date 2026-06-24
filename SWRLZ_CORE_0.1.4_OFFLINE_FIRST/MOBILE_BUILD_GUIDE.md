# SWRLZ-CORE — Android-only cloud build guide

You do not need a PC or Android Studio. The included GitHub Actions workflow compiles the APK on GitHub's Linux build machine and gives you a downloadable bundle containing:

- `SWRLZ_CORE_0.1.4_OFFLINE_FIRST-debug.apk`
- APK SHA-256 checksum
- `SWRLZ_CORE_0.1.4_FULL_SOURCE.zip`

## One-time setup from an Android phone

1. Create a **private** GitHub repository.
2. Upload this project's contents so that `android`, `backend`, `frontend`, and `.github` are at the repository root.
3. Open the repository's **Actions** tab.
4. Select **Build SWRLZ-CORE APK**.
5. Tap **Run workflow**.
6. When the run turns green, open it and download the artifact named `SWRLZ_CORE_0.1.4_ANDROID_DOWNLOADS`.
7. Extract the artifact ZIP on your phone and install the APK. Android may ask you to allow installation from the browser or file manager you used.

## Optional online Mentor settings

The app builds and runs offline when no secrets are supplied. To connect it to an independent Mentor backend, add these repository Actions secrets:

- `SWURLZ_BACKEND_URL`
- `SWURLZ_API_TOKEN`

Never commit API keys, tokens, passwords, or a real `local.properties` file into the repository.

## Notes

- The produced APK is a debug-signed private testing build. It is installable directly but is not a Play Store release package.
- Each workflow run creates a fresh APK and full-source archive.
- The Actions page can be opened and operated in an Android browser.
