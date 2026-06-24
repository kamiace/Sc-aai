# SWRLZ-CORE Pzhp-LDS Patch Report

## Patch type

Planning + seed-file + local-node prototype patch.

## Based on

`SWRLZ_Core_Update_TODO.docx`, which defined the local-first AI node, online source-of-truth core, Learning Delta Sync engine, patch manifest flow, merge rules, safety gates, and minimum file set.

## Added

- Master blueprint for local-first architecture.
- Local-first operating rules.
- Learning Delta Sync spec and packet schema.
- Patch manifest spec with backup, compare, verify, stage, merge, test, activate, log, and rollback flow.
- Safety gate risk levels and logging rules.
- Pzhp-LDS next patch checklist.
- Initial `core/` folder with version, manifest, rules, skills, example device profile, example deltas, example action log, merge report, rollback folder, and user-taught skill schema.
- Minimal FastAPI local node prototype with `/status`, `/command`, `/teach`, `/logs`, `/sync`, and `/update-check`.
- Local node tests for status, teach, logs, and update-check.

## Not yet done

- Android UI integration for local node controls.
- True patch downloading and hash verification.
- Real merge engine.
- Real rollback activation.
- Network sync to a hosted core inbox.
- Native Kotlin service implementation.

## Recommended next engineering scope

Implement the Pzhp-LDS core as a staged feature:

1. Android-visible local node status.
2. Append-only local deltas from Teach Mode.
3. Merge report generation.
4. Patch manifest check with compare-only behavior.
5. Staging and rollback before any live runtime replacement.

## Validation performed

- `python -m py_compile backend/server.py backend/local_node_server.py` passed.
- Backend/local-node tests passed: **4 passed**.
- Archive integrity passed after packaging.

## Build impact

This patch adds docs, core seed files, schemas, and a local node prototype. It does not change Android Gradle dependencies or the installed Android UI. APK rebuild is optional unless you want the new docs bundled in the source package for GitHub/Codespaces tracking.

## Hotfix: Local node core path

- Fixed `backend/local_node_server.py` default `CORE_DIR` from `ROOT.parent / "core"` to `ROOT / "core"`.
- Reason: `core/` lives inside the project root, not beside it. Without this fix, `/status` could report `core_version: unknown` unless `SWRLZ_CORE_DIR` was manually set.
- Added/verified local-node status coverage so the default packaged layout resolves `core/version.json` correctly.
