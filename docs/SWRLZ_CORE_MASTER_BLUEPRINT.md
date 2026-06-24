# SWRLZ-CORE Master Blueprint — Local-First Automation Node + Learning Delta Sync

## Purpose

SWRLZ-CORE is a local-first automation, learning, and teaching system. The local node handles immediate device work, offline memory, user teaching, logs, safety gates, and resumable execution. The online core acts as a source-of-truth blueprint, version oracle, patch manifest host, merge review layer, and optional AI Mentor.

## Core law

**Integrate, do not overwrite.**

Official patches may update the base core, official skills, schemas, rules, and action definitions, but they must not erase user-taught local skills, device settings, personal memory, active mission history, or safety overrides.

```text
Official Core
+ User-Taught Local Skills
+ Device Settings
+ Personal Memory
+ Safety Rules
+ Patch Updates
= Active Local Runtime
```

## Node roles

| Layer | Role |
|---|---|
| Local device node | Body, hands, immediate executor, offline worker, logger, safety gate. |
| Online core | Source-of-truth blueprint, official patch manifest, version history, reviewed skills. |
| Learning Delta Sync | Packages locally taught skills and corrections for later merge/review. |
| Merge engine | Combines official updates with local learning without destructive overwrites. |
| Rollback engine | Protects the last known-good runtime if staging, merge, or smoke tests fail. |

## Operating principle

The finished user experience should feel simple: give a mission, watch SWRLZ execute safely, receive a report. The implementation underneath is modular: command bird listens, safety bird checks, action bird executes, memory bird logs, teach bird learns, sync bird carries deltas, patch bird updates, merge bird integrates, rollback bird protects the timeline.
