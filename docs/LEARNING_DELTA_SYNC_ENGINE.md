# Learning Delta Sync Engine

## Purpose

When the user teaches SWRLZ locally, the system saves that teaching as a learning delta. When internet returns, unsynced deltas can be uploaded or shown to the online core for review. The core may keep them private-local, personal-sync them across the user's devices, reject them, or promote them into official core skills.

## Delta lifecycle

```text
teach locally
→ save append-only delta
→ mark local_candidate
→ internet returns
→ upload or queue for review
→ duplicate/conflict/safety analysis
→ user/core decision
→ keep local | personal sync | official candidate | reject-with-reason
→ never erase original without explicit approval
```

## Required delta fields

```json
{
  "type": "user_taught_skill",
  "skill_name": "example_skill_name",
  "created_on_device": "phone_node_01",
  "created_at": "2026-06-23T18:22:00Z",
  "core_version_when_taught": "0.1.6-pzhp-lds",
  "trigger_phrases": ["example trigger", "alternate phrase"],
  "steps": ["step 1", "step 2", "step 3"],
  "safety_level": "low|medium|high|locked",
  "requires_confirmation": true,
  "scope": "private_local|personal_synced|official_candidate",
  "status": "local_candidate",
  "user_approved": true,
  "synced": false
}
```

## Scopes

| Scope | Meaning |
|---|---|
| private_local | Stays on this device unless user changes it. |
| personal_synced | May sync to the user's own approved devices. |
| official_candidate | May be reviewed for inclusion in official core skills. |

## Conflict rules

- Different names and no dangerous trigger overlap: keep both.
- Trigger overlap: ask the user which should respond first or require runtime clarification.
- Official update improves a locally modified skill: create a merged copy and preserve the original.
- Safety conflict: stricter rule wins by default until reviewed.
- Device capability mismatch: disable only the unsafe action path, not the whole skill.
- Rejection never deletes the original local delta.
