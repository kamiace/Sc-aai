# SWRLZ-CORE Project Governance

## Leadership

- **Project CEO / Lead owner:** Kamiace
- **AI architecture and implementation assistant:** SWRLZ

The repository should be managed like a serious product workspace while still supporting a phone-first build/test workflow.

## Operating law

**Integrate, do not overwrite.**

Official patches update the base core, but local learning, device settings, personal memory, safety overrides, and rollback history are layered into the active runtime instead of being erased.

## Repository principles

1. Public repo contains buildable source and docs, not secrets.
2. Every release-worthy patch gets a patch report and test checklist.
3. Android APK artifacts are build outputs, not source of truth.
4. Codespaces can act as the cloud build machine.
5. Phone testing determines whether checklist items are truly complete.
6. Safety and privacy gates are product features, not obstacles.

## Decision hierarchy

1. User safety and consent.
2. Project owner direction.
3. Local-first/offline-first design.
4. Build stability.
5. Feature expansion.
6. Visual polish.

## Current strategic path

1. Lock down 0.1.5 context/control foundation.
2. Add protected-app skipped explanation and test report clarity.
3. Build 0.1.6 Mission Ledger + Local Skill Learning.
4. Expand Pzhp-LDS local node and patch manifest flow.
5. Add provenance, identity, continuity, and multi-node sync.
