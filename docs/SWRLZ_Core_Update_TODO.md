# SWRLZ-Core Update TODO - Local-First AI / Learning Delta Sync

SWRLZ-Core Automation AI

Update TODO: Local AI Server, Patch Core, and Learning Delta Sync

Purpose: catch the main SWRLZ-Core conversation up to the latest architecture decisions and convert the idea into a clear implementation checklist. This document is written as an update note that can be pasted into the main project thread or used as the next patch planning reference.

1. Architecture Summary

The automation AI should not be one giant online-only brain. It should be a local-first system with an optional online patch and sync layer. The local node handles device actions, offline operation, user teaching, logs, and safety gates. The online core acts as the source-of-truth blueprint, version oracle, and merge review layer.

Local device = body, hands, immediate executor, offline working node.

Online core = source-of-truth blueprint, official patch manifest, version history, and approved skill library.

Learning delta sync = user-taught changes captured locally, then uploaded on internet connection for review and intelligent integration.

Main design law: integrate, do not overwrite. Local learning must survive core updates.

2. Core Design Law

SWRLZ-Core Law #1: Integrate, Do Not Overwrite.

Official patches may update the base core, skills, rules, and action definitions, but they must never erase user-taught local skills, device settings, personal memory, or active safety overrides. The local node should merge layers together into an active runtime state.

Official Core

+ User-Taught Local Skills

+ Device Settings

+ Personal Memory

+ Safety Rules

+ Patch Updates

= Active Local AI

3. Target System Modules

4. Local Custom AI Server TODO

Goal: create a lightweight server/node that can run locally on the device and keep basic automation functional without internet or paid runtime dependencies.

[ ] Prototype local node in Termux or a lightweight Python/Node server running on localhost.

[ ] Define a simple local API: /command, /teach, /status, /logs, /sync, /update-check.

[ ] Create local storage folders for config, skills, user deltas, logs, backups, and cache.

[ ] Make offline mode first-class: commands that do not need the internet should still work.

[ ] Add device profile fields: device_id, device_name, platform, permissions, capabilities, current_core_version.

[ ] Add foreground notification/service mode for Android so the system is less likely to be killed by battery management.

[ ] Design later upgrade path to a native Android/Kotlin app service after prototype proves the flow.

5. Online Core / Patch Server TODO

Goal: the online side should act like a patch oracle and source-of-truth blueprint. It should not directly control the user device in a reckless way.

[ ] Create SWRLZ_CORE repository/folder structure.

[ ] Add version.json with core_version, patch_name, updated_at, compatibility, and notes.

[ ] Add patch_manifest.json listing changed files, hashes, safety notes, restart requirements, and rollback instructions.

[ ] Separate official core rules, skill definitions, action modules, memory schema, and docs.

[ ] Require hash verification before applying downloaded patches.

[ ] Plan signed updates later, especially before distributing beyond personal use.

[ ] Never allow raw unverified executable code to replace local action modules automatically.

SWRLZ_CORE/

  identity.md

  rules.json

  skills.json

  actions/

  schemas/

  memory/

  docs/

  version.json

  patch_manifest.json

  changelog.md

6. Patch Flow: Backup -> Compare -> Merge -> Test -> Activate -> Log

[ ] Local node checks online patch_manifest.json when internet is available.

[ ] Compare local core_version against online core_version.

[ ] Download changed files only, not the whole brain every time.

[ ] Verify hashes/signatures before staging update.

[ ] Backup the existing working version before patching.

[ ] Apply update to staging, not directly to live runtime.

[ ] Run compatibility checks and simple smoke tests.

[ ] Merge official patch with local learned deltas and device-specific settings.

[ ] Activate the new merged runtime only if tests pass.

[ ] If patch fails, rollback to previous working version and notify the user.

[ ] Write an update log entry with timestamp, old version, new version, success/failure, and rollback status.

backup -> compare -> download -> verify -> stage -> merge -> test -> activate -> log

if failed: rollback -> notify -> keep old working version

7. Learning Delta Sync Engine TODO

Goal: when the user teaches the local AI, save the teaching as a local delta. When internet returns, notify the online core that local learning happened. The online side reviews/integrates; it does not blindly overwrite.

[ ] Create a user_taught_skill packet schema.

[ ] Save every taught skill with timestamp, device_id, core_version_when_taught, trigger phrases, steps, safety level, and approval status.

[ ] Mark new teachings as local_candidate until reviewed or promoted.

[ ] Support three scopes: private local skill, personal synced skill, official core skill.

[ ] On internet return, upload unsynced local deltas to the sync inbox.

[ ] Server/core reviews deltas for duplicates, conflicts, safety issues, and general usefulness.

[ ] If approved for personal sync, distribute it only to the user own devices.

[ ] If approved as official, promote it into the core skill library with versioned patch notes.

[ ] If rejected or conflicting, keep it local and notify user instead of deleting it.

{

  "type": "user_taught_skill",

  "skill_name": "example_skill_name",

  "created_on_device": "phone_node_01",

  "created_at": "2026-06-23T18:22:00",

  "core_version_when_taught": "0.3.7",

  "trigger_phrases": ["example trigger", "alternate phrase"],

  "steps": ["step 1", "step 2", "step 3"],

  "safety_level": "low|medium|high",

  "requires_confirmation": true,

  "scope": "private_local|personal_synced|official_candidate",

  "status": "local_candidate",

  "user_approved": true,

  "synced": false

}

8. Merge Rules and Conflict Handling

The merge engine needs to preserve both official evolution and local growth. A conflict should become a review item, not a destructive overwrite.

[ ] If official skill and local skill have different names, keep both unless triggers overlap dangerously.

[ ] If triggers overlap, ask the user which skill should respond first or require clarification at runtime.

[ ] If official update improves a skill that the user modified locally, create a merged copy and preserve the user version as backup.

[ ] If a safety rule changes, the stricter rule should win by default until reviewed.

[ ] If device capability changes, only disable actions that cannot safely run on that device.

[ ] Never erase local deltas without explicit user approval.

[ ] Always produce a human-readable merge report.

9. Safety and Permission Gates

The AI can be powerful without being reckless. Every action needs a scope and risk level. Anything dangerous must require confirmation and logs.

[ ] Define action risk levels: low, medium, high, locked.

[ ] Low risk: open app, change brightness, play local file, start timer, read local status.

[ ] Medium risk: send a prepared message, modify settings, run multi-step app workflow.

[ ] High risk: delete files, spend money, send external messages, alter accounts, share data, contact people.

[ ] Locked: actions that should not run without explicit unlock, review, and user confirmation.

[ ] Require confirmation for medium/high actions until the user intentionally changes trust settings.

[ ] Log all actions with timestamp, trigger, decision, result, and rollback data if applicable.

[ ] Add emergency stop/pause command that interrupts active automation immediately.

10. Minimum File Set to Build Next

[ ] version.json - current local core version and patch metadata.

[ ] patch_manifest.json - online update manifest.

[ ] rules.json - safety rules, permissions, confirmation requirements.

[ ] skills.json - available skills and trigger phrases.

[ ] device_profile.json - current device capabilities and permissions.

[ ] local_deltas.jsonl - user-taught skills and corrections saved as append-only events.

[ ] merge_report.md - human-readable report after every sync/merge.

[ ] action_log.jsonl - action history and server-log style audit trail.

[ ] rollback/ - backups of prior working versions.

11. Next Patch Plan: Pzhp-LDS

Suggested patch label: Pzhp-LDS, meaning Pzhp plus Learning Delta Sync. This is the next architecture update to carry into the main conversation.

[ ] Write SWRLZ_CORE_MASTER_BLUEPRINT.md with the local-first architecture.

[ ] Write LOCAL_FIRST_RULES.md with the no-paid-runtime dependency rule and offline-first requirement.

[ ] Write LEARNING_DELTA_SYNC_ENGINE.md with packet schema, sync flow, merge rules, and scopes.

[ ] Write PATCH_MANIFEST_SPEC.md with update, hash, backup, rollback, and staging rules.

[ ] Write SAFETY_GATE_SPEC.md with action risk levels and confirmation rules.

[ ] Prototype a minimal local command server with /status, /command, /teach, and /logs endpoints.

[ ] Prototype a fake patch_manifest.json and have the local server detect that an update exists.

[ ] Prototype teaching one local skill and saving it to local_deltas.jsonl.

[ ] Prototype merge report generation without actually replacing any files.

12. Copy/Paste Update for Main Conversation

Use this short version to catch the main thread up quickly:

Update for SWRLZ-Core:

We decided the automation AI should be a local-first custom AI server/node that can run on the device, or on another device elsewhere, while keeping the main SWRLZ-Core blueprint separate as an online source-of-truth core. The local node should keep working offline and should check an online patch_manifest.json when internet returns.



Core law: integrate, do not overwrite. Official patches can update the base core, but user-taught local skills, device settings, personal memory, and safety overrides must be layered on top and preserved.



New module: Learning Delta Sync Engine. When a user teaches the local AI something, the local node saves it as a learning delta packet. When internet returns, it notifies/uploads those changes for review. The server/main core can merge, reject, keep local, personal-sync, or promote to official core skill. It must not erase the local teaching just because an official update exists.



Patch flow: backup -> compare -> download -> verify -> stage -> merge -> test -> activate -> log. If anything fails, rollback and notify user.



Needed docs/files: SWRLZ_CORE_MASTER_BLUEPRINT.md, LOCAL_FIRST_RULES.md, LEARNING_DELTA_SYNC_ENGINE.md, PATCH_MANIFEST_SPEC.md, SAFETY_GATE_SPEC.md, version.json, patch_manifest.json, rules.json, skills.json, device_profile.json, local_deltas.jsonl, action_log.jsonl, merge_report.md, rollback backups.



Next patch label idea: Pzhp-LDS.

13. Guiding Metaphor

Autotron Bird Choir: many small modules doing clear jobs in sync. Voice bird listens, command bird parses, safety bird checks, action bird executes, memory bird logs, teach bird learns, sync bird carries updates, merge bird integrates, patch bird updates, and rollback bird protects the timeline.

End goal: the build phase is complex, but the finished automation AI conquers tasks straightforwardly, safely, and elegantly.