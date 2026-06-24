# Patch Manifest Specification

## Patch flow

```text
backup → compare → download → verify → stage → merge → test → activate → log
if failed: rollback → notify → keep old working version
```

## Manifest fields

```json
{
  "core_version": "0.1.6-pzhp-lds",
  "patch_name": "Pzhp-LDS Local Core + Learning Delta Sync",
  "updated_at": "2026-06-24T00:00:00Z",
  "compatibility": {
    "min_android_app_version": "0.1.5-context-control",
    "schema_version": 1
  },
  "files": [
    {
      "path": "core/rules.json",
      "sha256": "example",
      "safety_notes": "Safety rule changes must prefer stricter behavior during merge.",
      "restart_required": false
    }
  ],
  "rollback": {
    "required": true,
    "instructions": "Restore the previous runtime folder and keep staged patch for inspection."
  }
}
```

## Safety requirements

- Verify hashes before staging.
- Never hot-replace unverified executable action modules.
- Back up the last known-good runtime.
- Run smoke tests before activation.
- Write a human-readable update log.
- Keep failed patches for inspection rather than deleting evidence.
