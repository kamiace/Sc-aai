# Contributing to SWRLZ-CORE

SWRLZ-CORE is currently a lead-owner directed project. Contributions should preserve the local-first architecture and the core law: **integrate, do not overwrite**.

## Project roles

- **Lead / Project CEO:** Kamiace
- **Architecture / implementation assistant:** SWRLZ
- **Repository target:** public build/source continuity for `kamiace/Sc-aai`

## Contribution rules

1. Do not commit secrets, API keys, tokens, `.env`, APK signing keys, or `local.properties`.
2. Do not remove user-taught local skill, delta, memory, rollback, or safety-layer concepts.
3. Prefer small, named patches with clear patch reports.
4. Each feature patch should include a matching test checklist update.
5. Android builds should use `scripts/build_android_debug_bundle.sh` or GitHub Actions.
6. Automation features must preserve user control, explicit confirmations, and audit logs.
7. Risky actions require safety gate review before implementation.

## Patch labels

Use compact labels such as:

```text
0.1.5a-protected-app-clarity
0.1.6-mission-ledger
0.1.7-provenance
Pzhp-LDS
```

## Pull request expectation

Every pull request should explain:

- What changed.
- Why it changed.
- What was tested.
- What still needs phone testing.
- Whether it affects permissions, automation actions, privacy, or safety gates.
