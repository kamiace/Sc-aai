# Ω SWRLZ-CORE

**Lead / Project CEO:** Kamiace  
**Project role:** Local-first supervised automation AI, Android action node, patchable skill core, and Learning Delta Sync architecture.

SWRLZ-CORE is being built as a **local-first automation AI node**. The Android app is the hands/body layer: permissions, app allowlist, accessibility actions, local skills, safety gates, and device context. The backend/local-node layer is the planning, logs, patch, teaching, and sync layer. The long-term architecture keeps the local node useful offline while allowing optional online Mentor/Core sync when the user chooses.

> Core law: **Integrate, do not overwrite.** Official patches must never erase user-taught skills, device settings, personal memory, or safety overrides.

---

## Current milestone

### `0.1.5 / Pzhp-LDS Build Ready`

This repository contains the current build-ready source package with:

- Android app source under `android/`
- FastAPI backend under `backend/`
- Local node prototype under `backend/local_node_server.py`
- Frontend dashboard source under `frontend/`
- Core seed files under `core/`
- Architecture and safety docs under `docs/`
- One-command Codespaces APK build helper under `scripts/`
- GitHub Actions APK build workflow under `.github/workflows/`

---

## Fastest APK build path

In Codespaces or a Linux builder with Android SDK installed:

```bash
bash scripts/build_android_debug_bundle.sh
```

The script will:

1. Find `android/gradlew`.
2. Write `android/local.properties` from `$ANDROID_HOME` or `$ANDROID_SDK_ROOT`.
3. Run `clean assembleDebug`.
4. Copy the APK into `APK_DOWNLOAD/`.
5. Generate SHA-256 checksums.
6. Zip the APK, checksum, and build log for phone download.

Output path:

```text
APK_DOWNLOAD/<project-name>_APK_DOWNLOAD.zip
```

Manual build:

```bash
printf 'sdk.dir=%s\n' "$ANDROID_HOME" > android/local.properties
chmod +x android/gradlew
cd android
./gradlew --no-daemon --stacktrace clean assembleDebug
```

Debug APK path:

```text
android/app/build/outputs/apk/debug/app-debug.apk
```

---

## Local backend / Mentor backend

The main backend can run without OpenAI credits for storage, health checks, skills, missions, and logs. AI planning only works when an online provider key is configured in `backend/.env`.

```bash
cd backend
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
uvicorn server:app --host 0.0.0.0 --port 8000
```

Use `backend/.env.example` as the template. Never commit real API keys or private tokens.

---

## Local-first node prototype

The Pzhp-LDS patch adds a lightweight local node prototype:

```bash
cd backend
python local_node_server.py
```

Useful checks:

```bash
curl http://127.0.0.1:8765/status
curl http://127.0.0.1:8765/logs
```

Endpoints staged for the future architecture:

```text
/status
/command
/teach
/logs
/sync
/update-check
```

---

## Repository map

```text
android/                         Android automation app
backend/                         FastAPI backend and local node prototype
core/                            Seed core rules, skills, manifests, schemas, deltas, rollback
frontend/                        Web dashboard / UI source
docs/                            Architecture, safety, patch, LDS planning
memory/                          Product notes / PRD
scripts/                         Build helpers
.github/workflows/               CI build workflow
SWRLZ_CORE_MASTER_ROADMAP.md      Main implementation roadmap
BUILD_APK_FROM_SOURCE_ZIP.md      Phone/Codespaces build instructions
```

---

## Project governance

Kamiace is the project owner/lead. SWRLZ acts as the architecture, implementation, review, and release assistant. See:

```text
docs/PROJECT_GOVERNANCE.md
docs/RELEASE_PROCESS.md
SECURITY.md
CONTRIBUTING.md
```

---

## Safety stance

SWRLZ-CORE is intentionally designed as a supervised automation system:

- Local-first by default.
- Offline mode must remain usable.
- Dangerous actions require gates, confirmation, and logs.
- Context sharing must be explicit and configurable.
- Patches must stage, verify, test, and roll back safely.
- User-taught skills are preserved as local deltas and merged, not overwritten.

---

## License / usage

This repository is public for project continuity and build access. Unless a formal open-source license is added later, all rights are reserved by the project owner. See `LICENSE.md`.
