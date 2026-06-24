# Pzhp-LDS Next Patch Plan

**Patch label:** Pzhp-LDS  
**Meaning:** Pzhp + Learning Delta Sync  
**Goal:** Convert SWRLZ from only an app/backend prototype into a local-first core with update manifests, learning deltas, merge reports, and rollback-ready patch rules.

## Planned work checklist

- [ ] Create `core/version.json`.
- [ ] Create `core/patch_manifest.json`.
- [ ] Create `core/rules.json`.
- [ ] Create `core/skills.json`.
- [ ] Create `core/device_profile.example.json`.
- [ ] Create `core/local_deltas.example.jsonl`.
- [ ] Create `core/action_log.example.jsonl`.
- [ ] Create `core/merge_report.example.md`.
- [ ] Create `core/schemas/user_taught_skill.schema.json`.
- [ ] Add docs for local-first rules, patch flow, Learning Delta Sync, safety gate, and master blueprint.
- [ ] Prototype local node endpoints: `/status`, `/command`, `/teach`, `/logs`, `/sync`, `/update-check`.
- [ ] Prototype teaching one local skill and saving it as an append-only delta.
- [ ] Prototype update-check behavior against a local/fake patch manifest.
- [ ] Prototype merge report generation without replacing live files.
- [ ] Add tests for local status, teach, logs, and update-check.

## Acceptance criteria

- Source package includes the minimum file set.
- Local node prototype starts without paid AI providers.
- `/status` reports core version and local storage paths.
- `/teach` appends a valid local delta without overwriting earlier records.
- `/logs` returns append-only action/log history.
- `/update-check` compares local and remote/fake versions without applying dangerous changes.
- Merge report can be generated as a human-readable file.
