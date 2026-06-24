# SWRLZ-CORE Phone-Friendly APK Build Flow

This source package includes a one-command builder for the Android debug APK.

## Intended workflow

1. Upload the full SWRLZ-CORE source ZIP to GitHub Codespaces.
2. Extract it into a fresh folder.
3. Run the build helper from the project root:

```bash
bash scripts/build_android_debug_bundle.sh
```

4. Download the ZIP created in:

```text
APK_DOWNLOAD/
```

The output ZIP contains:

- debug APK
- APK SHA-256 checksum
- build log
- build log SHA-256 checksum

## Codespaces AI prompt

Paste this into Codespaces AI after uploading and extracting the source ZIP:

```text
Run this from the extracted SWRLZ-CORE project root:

bash scripts/build_android_debug_bundle.sh

If the build succeeds, show me the path to the ZIP inside APK_DOWNLOAD so I can download it to my phone.
If the build fails, do not edit source files. Show the exact Gradle error, failing task, and path to APK_DOWNLOAD/build_android_debug.log.
```

## Notes

- The script writes `android/local.properties` from `ANDROID_HOME` or `ANDROID_SDK_ROOT`.
- The script does not modify app source code.
- If Gradle fails, the script stops and leaves the build log in `APK_DOWNLOAD/`.
- Do not put API keys or private tokens into this source package.
