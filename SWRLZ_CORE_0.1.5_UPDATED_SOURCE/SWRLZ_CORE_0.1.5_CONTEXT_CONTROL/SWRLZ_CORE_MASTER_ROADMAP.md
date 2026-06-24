# SWRLZ-CORE Master Roadmap — Detailed Checklist
## Android Prototype → Cross-Device Learning, Teaching, Creative Production, and Physical Automation Mesh

**Roadmap revision:** 1.1  
**Source patch:** 0.1.5 Context and Control Foundation  
**Current verified APK baseline:** 0.1.4 Offline-First

## How completion works

- `[x]` means the feature was coded, built successfully, tested on the target device/platform, and matched its acceptance result.
- `[ ]` with **Coded** means the source implementation exists but must still be compiled and tested by the user before it is checked off.
- `[ ]` with **Partial** means only part of the described feature exists.
- `[ ]` with **Planned** means it belongs to a future patch.

This prevents the roadmap from calling an idea complete merely because code was written.

# 0.1.4 — Recovery Baseline

- [x] **BASE-001 — Remove the dead Emergent preview fallback.** ✅ **Verified complete.** The app no longer silently contacts the expired preview URL when no custom server is configured.
- [x] **BASE-002 — Compile and install a working Android debug APK.** ✅ **Verified complete.** A real APK was produced and installed on the phone, proving the project can build and launch.
- [x] **BASE-003 — Graceful offline mode.** ✅ **Verified complete.** When no Mentor server is configured or reachable, local screens and stored skills remain usable and the app shows a readable status instead of crashing or exposing a raw Ktor/404 exception.
- [x] **BASE-004 — Local Skill Library foundation.** ✅ **Verified complete.** Skill Capsules are stored on-device and can still be viewed when the internet or Mentor backend is unavailable.
- [x] **BASE-005 — Accessibility and overlay permission workflows.** ✅ **Verified complete.** The app can guide the user to enable its Accessibility service and floating overlay, which provide the operator's eyes, hands, and visible telemetry.
- [x] **BASE-006 — Initial no-progress watchdog.** ✅ **Verified complete.** The mission runner notices repeated unchanged screens and can wait, re-observe, or pause instead of tapping the same target forever.
- [x] **BASE-007 — Independent FastAPI and SQLite backend source.** ✅ **Verified complete.** The project includes a backend that stores missions and skills locally without requiring Emergent or MongoDB.
- [x] **BASE-008 — Phone-controlled cloud build workflow.** ✅ **Verified complete.** GitHub/Codespaces can compile the Android APK so development does not require a separate PC.
- [ ] **BASE-009 — Verify every existing screen and control on-device.** ⏳ **Planned.** Open and test Home, Cockpit, Skills, Allowlist, Setup, Permissions, overlay controls, and mission errors on the actual phone.
- [ ] **BASE-010 — Automated Android regression tests.** ⏳ **Planned.** Add repeatable unit and UI tests so later patches cannot silently break previously working behavior.
- [ ] **BASE-011 — In-app version identifier.** 🧩 **Coded in the 0.1.5 source; build and phone testing still required before checking it off.** The Home screen now shows the version name and version code so screenshots and bug reports can identify the exact build being tested.
- [ ] **BASE-012 — Stable release signing.** ⏳ **Planned.** Use a preserved signing keystore so later APKs install as updates without uninstalling, losing local data, or resetting special permissions.

# 0.1.5 — Context and Control Foundation

- [ ] **CTRL-001 — ALLOW SHOWN.** 🧩 **Coded in the 0.1.5 source; build and phone testing still required before checking it off.** Adds every app currently visible after filters are applied; apps already allowed remain allowed and are not toggled off.
- [ ] **CTRL-002 — BLOCK SHOWN.** 🧩 **Coded in the 0.1.5 source; build and phone testing still required before checking it off.** Removes every currently visible app from the allowlist while leaving apps outside the current filter unchanged.
- [ ] **CTRL-003 — Filter-aware bulk actions.** 🧩 **Coded in the 0.1.5 source; build and phone testing still required before checking it off.** Bulk allow/block operations use only the displayed result set, not the entire installed-app list.
- [ ] **CTRL-004 — System-app filter support.** 🧩 **Coded in the 0.1.5 source; build and phone testing still required before checking it off.** The SYS filter determines whether system apps are included in both the list and bulk operations.
- [ ] **CTRL-005 — Search-filter support.** 🧩 **Coded in the 0.1.5 source; build and phone testing still required before checking it off.** Typing a package or app name narrows the bulk operation to only matching apps.
- [ ] **CTRL-006 — Non-inverting selection logic.** 🧩 **Coded in the 0.1.5 source; build and phone testing still required before checking it off.** Bulk operations use set addition/subtraction, so already-correct app states stay correct rather than being blindly inverted.
- [ ] **CTRL-007 — Visible selection status.** 🧩 **Coded in the 0.1.5 source; build and phone testing still required before checking it off.** Shows the number allowed out of the number displayed and labels the visible set as none, partial, or all allowed.
- [ ] **CTRL-008 — RESET DEFAULTS.** 🧩 **Coded in the 0.1.5 source; build and phone testing still required before checking it off.** Restores the original conservative allowlist without requiring the user to switch every app individually.
- [ ] **CTRL-009 — System bulk confirmation and protected packages.** 🧩 **Coded in the 0.1.5 source; build and phone testing still required before checking it off.** Bulk-enabling system apps requires confirmation and skips especially sensitive packages; individual manual selection remains possible.
- [ ] **CTX-001 — Device identity and hardware context.** 🧩 **Coded in the 0.1.5 source; build and phone testing still required before checking it off.** Collects manufacturer, model, Android version, SDK, build fingerprint, CPU ABIs, memory class, display dimensions, density, orientation, locale, timezone, and navigation mode.
- [ ] **CTX-002 — Runtime and capability context.** 🧩 **Coded in the 0.1.5 source; build and phone testing still required before checking it off.** Collects battery percentage, charging state, network type, internet capability, power saver, Accessibility status, overlay status, and notification availability.
- [ ] **CTX-003 — Stable Device Context Capsule.** 🧩 **Coded in the 0.1.5 source; build and phone testing still required before checking it off.** Creates a versioned context object with a locally derived, hashed device-node ID so mentors can distinguish devices without receiving the raw Android ID.
- [ ] **CTX-004 — Automatic context refresh.** 🟨 **Partially implemented; the description states what remains.** The capsule is regenerated for previews and every mission. Event-driven refresh for every material device change remains to be added.
- [ ] **APP-001 — Foreground app identification.** 🧩 **Coded in the 0.1.5 source; build and phone testing still required before checking it off.** Collects the current package, app label, version, system/user classification, window class, and installer source when Android exposes them.
- [ ] **APP-002 — App permissions and UI-framework hints.** 🧩 **Coded in the 0.1.5 source; build and phone testing still required before checking it off.** Collects requested permission states and uses accessibility classes to estimate whether the screen is native Android/Compose, WebView, Flutter, React Native, or unknown.
- [ ] **APP-003 — Persistent app/version knowledge index.** ⏳ **Planned.** Store learned behavior by package + app version + Android version + device family so old routes are not assumed valid after an update.
- [ ] **DELTA-001 — Accessibility-tree screen delta.** 🧩 **Coded in the 0.1.5 source; build and phone testing still required before checking it off.** Compares consecutive screen snapshots and records meaningful added/removed node fingerprints instead of repeatedly sending the same full state.
- [ ] **DELTA-002 — Rich screen-state change detection.** 🟨 **Partially implemented; the description states what remains.** Package and node changes are detected now. Dedicated keyboard, dialog, title, orientation, and overlay detectors still need expansion.
- [ ] **DELTA-003 — Expected post-action verification.** 🟨 **Partially implemented; the description states what remains.** The watchdog verifies whether the screen changed. Future work must compare each action's specific expected result against the observed result.
- [ ] **PRIV-001 — Sensitive-value redaction.** 🟨 **Partially implemented; the description states what remains.** Password nodes, likely one-time codes, long account/payment-number patterns, and token-like strings are redacted. Secure-window and broader sensitive-field classification still need expansion.
- [ ] **PRIV-002 — Context-sharing levels and private-app policy.** 🟨 **Partially implemented; the description states what remains.** Minimal, Standard, Detailed, and Local-Only levels are implemented. A separate user-managed private-app exclusion list remains planned.
- [ ] **PRIV-003 — Outgoing context preview and audit.** 🟨 **Partially implemented; the description states what remains.** Setup can display the exact outgoing context packet and redaction categories. Long-term per-provider transmission logs are still planned.
- [ ] **MENTOR-001 — Editable Mentor URL and encrypted token.** 🧩 **Coded in the 0.1.5 source; build and phone testing still required before checking it off.** The Setup screen saves the backend URL and stores the private token through Android Keystore-backed encrypted preferences.
- [ ] **MENTOR-002 — Change Mentor without rebuilding.** 🧩 **Coded in the 0.1.5 source; build and phone testing still required before checking it off.** The backend address and token can be changed inside the installed app instead of recompiling the APK.
- [ ] **MENTOR-003 — Connection test and readable diagnostics.** 🧩 **Coded in the 0.1.5 source; build and phone testing still required before checking it off.** The app can test the health endpoint and convert common 401, 404, rate-limit, timeout, and network failures into plain-language status messages.
- [ ] **MENTOR-004 — Context approval gate.** 🧩 **Coded in the 0.1.5 source; build and phone testing still required before checking it off.** When enabled, the user must preview and approve context for the next online mission before the runner will send device/app/screen information.

# 0.1.6 — Mission Ledger, Recovery, and Local Skills

- [ ] **LEDGER-001 — Persistent mission record.** ⏳ **Planned.** Save the goal, constraints, plan, actions, observations, results, mission ID, and session ID across app restarts.
- [ ] **LEDGER-002 — Timing, approval, correction, and retry history.** ⏳ **Planned.** Record how long steps took, what the user approved or corrected, and how failures were retried or recovered.
- [ ] **LEDGER-003 — Searchable timeline and export.** ⏳ **Planned.** Let the user filter past missions and export reports as readable Markdown plus structured JSON.
- [ ] **PME-001 — Permanent End Goal Spec.** ⏳ **Planned.** Store the complete intended final outcome so a resumed mission still knows its destination.
- [ ] **PME-002 — Decision Timeline.** ⏳ **Planned.** Record every important choice, who made it, why it was made, and which artifact or action it affected.
- [ ] **PME-003 — Checkpoint artifacts and dependency graph.** ⏳ **Planned.** Save valid intermediate files/states and map which later tasks depend on each checkpoint.
- [ ] **PME-004 — Reason-aware recovery.** ⏳ **Planned.** After interruption, restore the last valid checkpoint and reconstruct how and why the mission reached that state before continuing.
- [ ] **PME-005 — Resume, review, branch, and restart controls.** ⏳ **Planned.** Give the user explicit choices to continue, inspect history, create an alternative branch, or restart cleanly.
- [ ] **PAR-001 — Question dependency classification.** ⏳ **Planned.** Mark unanswered questions as blocking, non-blocking, or late-binding so only truly dependent work stops.
- [ ] **PAR-002 — Productive waiting.** ⏳ **Planned.** Continue independent or reversible work while one branch waits for user approval.
- [ ] **PAR-003 — Answer-impact analysis.** ⏳ **Planned.** When the user answers, calculate which completed work remains valid and rerun only affected downstream tasks.
- [ ] **PAR-004 — Conservative, Balanced, and Maximum Throughput modes.** ⏳ **Planned.** Let the user choose how aggressively the system performs provisional work while details are unresolved.
- [ ] **SKILL-001 — Verified local Skill Capsules.** ⏳ **Planned.** Convert successful missions into reusable local workflows with completion criteria and safety constraints.
- [ ] **SKILL-002 — Compatibility metadata.** ⏳ **Planned.** Attach device, OS, app version, framework, and required-permission information to every learned skill.
- [ ] **SKILL-003 — Offline skill replay.** ⏳ **Planned.** Execute trusted learned skills without contacting an online Mentor.
- [ ] **SKILL-004 — Skill editor, versions, rollback, import/export, and conflicts.** ⏳ **Planned.** Let users inspect and revise skills safely while preserving prior versions and resolving local/remote differences.

# 0.1.7 — Provenance and Identity

- [ ] **PROV-001 — Mandatory sender tags.** ⏳ **Planned.** Prefix automation-originated messages with a clear name such as [SWRLZ-CORE · LOCAL OPERATOR] so they cannot be confused with the human user.
- [ ] **PROV-002 — Module-specific identities.** ⏳ **Planned.** Provide distinct default identities for Operator, Watchdog, Device Observer, Skill Distiller, Continuity Sync, and Council Relay.
- [ ] **PROV-003 — Human-identity protection.** ⏳ **Planned.** Prevent any automation or provider from silently presenting its own words as something the user personally said.
- [ ] **PROV-004 — Source preservation across sync.** ⏳ **Planned.** Keep original sender labels and metadata when messages move between devices or AI providers.
- [ ] **PROV-005 — Immutable message metadata.** ⏳ **Planned.** Store sender ID, role, device, mission, session, and timestamp with every report or statement.
- [ ] **PROV-006 — Claim type classification.** ⏳ **Planned.** Mark records as observed, inferred, recommended, user-confirmed, or unverified.
- [ ] **PROV-007 — Search, confidence, evidence, and signatures.** ⏳ **Planned.** Filter history by source and trust level, count supporting evidence, and cryptographically sign cross-device messages.

# 0.1.8 — Multi-Mentor and Shared Swurlz Profile

- [ ] **AI-001 — Offline/local provider.** ⏳ **Planned.** Use on-device rules or local models when no network provider is selected.
- [ ] **AI-002 — OpenAI adapter.** ⏳ **Planned.** Route normalized mission/context packets through a private backend to an OpenAI model.
- [ ] **AI-003 — Gemini adapter.** ⏳ **Planned.** Support Google Gemini using its approved API/OAuth path without embedding secrets in the APK.
- [ ] **AI-004 — Claude adapter.** ⏳ **Planned.** Support Anthropic Claude through a private backend adapter.
- [ ] **AI-005 — Model choice, tests, health, and fallback.** ⏳ **Planned.** Let the user choose models, test each provider, see status, and define fallback order.
- [ ] **AI-006 — Cost and context controls.** ⏳ **Planned.** Limit token use, compress context, track provider cost, and keep credentials outside distributable clients.
- [ ] **PROFILE-001 — Provider-independent Swurlz profile.** ⏳ **Planned.** Create one portable schema for communication style, user preferences, project knowledge, and safety rules.
- [ ] **PROFILE-002 — Approved personal and project knowledge.** ⏳ **Planned.** Store only user-approved goals, preferences, projects, constraints, and memories with source attribution.
- [ ] **PROFILE-003 — Stable facts versus temporary circumstances.** ⏳ **Planned.** Prevent short-lived situations or one-off guesses from becoming permanent identity facts.
- [ ] **PROFILE-004 — Encrypted profile backup.** ⏳ **Planned.** Export, import, migrate, and restore the shared profile securely.
- [ ] **BRIDGE-001 — Visible experimental conversation bridge.** ⏳ **Planned.** Allow the user to connect a visible authenticated browser conversation as a supervisory council channel.
- [ ] **BRIDGE-002 — Designated conversation and tagged checkpoint briefs.** ⏳ **Planned.** Select the intended conversation and send compact sender-tagged mission updates at meaningful checkpoints.
- [ ] **BRIDGE-003 — Ask/Status/Recent Actions controls.** ⏳ **Planned.** Provide explicit controls to consult Swurlz, report current state, or share recent actions.
- [ ] **BRIDGE-004 — Approved reply import and takeover controls.** ⏳ **Planned.** Import only approved responses and support pause, disconnect, and immediate user takeover.
- [ ] **BRIDGE-005 — Bridge-failure independence.** ⏳ **Planned.** Local missions must continue safely or pause cleanly when a browser/provider layout changes or the session expires.

# 0.1.9 — Continuity Reconciliation

- [ ] **SYNC-001 — Offline change queue.** ⏳ **Planned.** Queue offline missions, learned skills, corrections, and memory candidates until connectivity returns.
- [ ] **SYNC-002 — Offline Interval Report.** ⏳ **Planned.** Condense the disconnected period into completed missions, failures, new skills, corrections, and unresolved questions.
- [ ] **SYNC-003 — Knowledge-diff synchronization.** ⏳ **Planned.** Track the last shared version and transmit only added, changed, or removed records.
- [ ] **SYNC-004 — Conflict comparison.** ⏳ **Planned.** Show where local knowledge and online understanding disagree.
- [ ] **SYNC-005 — User-controlled merge choices.** ⏳ **Planned.** Let the user accept, reject, merge, defer, or keep information local-only.
- [ ] **SYNC-006 — Provenance-preserving rollback.** ⏳ **Planned.** Retain original sources and allow a synchronization merge to be reversed.

# 0.2.0 — Cross-Device Mesh

- [ ] **MESH-001 — Secure node identity and pairing.** ⏳ **Planned.** Give every phone, PC, Pi, headset, or future node a cryptographic identity with explicit pairing, approval, and revocation.
- [ ] **MESH-002 — Per-node permissions and emergency stop.** ⏳ **Planned.** Restrict what each node may observe or do and provide mission-scoped authorization plus immediate remote stop.
- [ ] **MESH-003 — Capability manifests.** ⏳ **Planned.** Each node advertises available apps, tools, hardware, sensors, compute, storage, battery, trust state, and learned skills.
- [ ] **MESH-004 — Goal decomposition.** ⏳ **Planned.** Split one human goal into platform-specific sub-missions.
- [ ] **MESH-005 — Routing, parallelism, and handoff.** ⏳ **Planned.** Choose the best node for each subtask, run independent work in parallel, and continue missions on another device when needed.
- [ ] **MESH-006 — Checkpoint transfer and result merge.** ⏳ **Planned.** Move recoverable state between nodes and combine outcomes into one verified report.
- [ ] **MESH-007 — Secure resumable artifact transfer.** ⏳ **Planned.** Transfer files with encryption, hashes, permission checks, resume support, and version/conflict handling.

# 0.2.1 — PC Node

- [ ] **PC-001 — Windows/Linux/macOS pairing.** ⏳ **Planned.** Install a desktop node and pair it securely with the user's Android commander.
- [ ] **PC-002 — Desktop observation.** ⏳ **Planned.** Read approved window, application, process, and filesystem state.
- [ ] **PC-003 — Verified UI and terminal execution.** ⏳ **Planned.** Control keyboard/mouse and run narrowly scoped shell or PowerShell commands while checking results.
- [ ] **PC-004 — Desktop safety and remote approval.** ⏳ **Planned.** Provide manual takeover, emergency stop, activity timeline, and approval requests sent to Android.
- [ ] **PC-005 — Automated build-machine mission.** ⏳ **Planned.** Receive source, compile software, diagnose failures, return artifacts/checksums, and teach the user what was repaired.

# 0.2.2 — Server and VM Automation

- [ ] **VM-001 — Host and hypervisor discovery.** ⏳ **Planned.** Inspect host resources and choose an approved adapter for VirtualBox, Hyper-V, VMware, Proxmox, or another supported platform.
- [ ] **VM-002 — VM lifecycle automation.** ⏳ **Planned.** Create the VM, install an OS, verify health, snapshot before major changes, and restore when necessary.
- [ ] **SRV-001 — Container/runtime deployment.** ⏳ **Planned.** Install Docker or Podman and deploy the SWRLZ Mentor or another approved service.
- [ ] **SRV-002 — Secure server configuration.** ⏳ **Planned.** Configure TLS, minimum firewall ports, database, backups, restart tests, and health checks.
- [ ] **SRV-003 — Reusable deployment blueprint.** ⏳ **Planned.** Save the successful infrastructure recipe, recovery steps, and teaching guide.
- [ ] **SRV-004 — Raspberry Pi relay.** ⏳ **Planned.** Use a Pi or similar always-on device as a secure local coordinator and synchronization relay.

# 0.3.0 — Teaching and Explainability

- [ ] **TEACH-001 — Explain plans and outcomes.** ⏳ **Planned.** Describe the plan, important decisions, failures, recoveries, alternatives, and final result.
- [ ] **TEACH-002 — Quick, Study, and Full-DWAMN depth.** ⏳ **Planned.** Offer a short answer, a structured lesson, or an exhaustive simulation/debrief report.
- [ ] **TEACH-003 — Personalized lessons and exercises.** ⏳ **Planned.** Turn mission experience into practice tasks while distinguishing evidence from interpretation.
- [ ] **CHESS-001 — Ten-win chess benchmark.** ⏳ **Planned.** Detect a single-player board, use a local engine, execute verified legal moves, and win ten hard-level AI games consecutively.
- [ ] **CHESS-002 — Chess teaching output.** ⏳ **Planned.** Explain strategy creation, tactical motifs, alternatives, mistakes, annotated games, and personalized puzzles.
- [ ] **EMAIL-001 — Reversible email-cleaning benchmark.** ⏳ **Planned.** Identify the account, preview scope, protect chosen categories, and move approved messages to Trash first.
- [ ] **EMAIL-002 — Permanent deletion approval and verification.** ⏳ **Planned.** Require a second explicit approval before emptying Trash, then verify counts and report exactly what was removed or preserved.

# 0.3.1 — Creative Automation

- [ ] **ART-001 — Visible desktop art-tool control.** ⏳ **Planned.** Launch Paint or another approved application and operate it through observable mouse/tool actions.
- [ ] **ART-002 — Style-to-process translation.** ⏳ **Planned.** Convert a creative brief into palette, brush/stroke, composition, and refinement rules.
- [ ] **ART-003 — Live creation with checkpoints.** ⏳ **Planned.** Let the user watch, hear explanations, pause, request changes, and preserve intermediate versions.
- [ ] **ART-004 — Creative intent and quality modes.** ⏳ **Planned.** Protect must-preserve design decisions and support Fast, Standard, and Masterwork execution.

# 0.4.0 — Character Forge

- [ ] **FORGE-001 — Quick through AAA/Studio intake depth.** ⏳ **Planned.** Ask a small or extensive question set depending on required quality and production seriousness.
- [ ] **FORGE-002 — Question importance and consequences.** ⏳ **Planned.** Label questions Required, Recommended, or Optional and explain the risk of skipping them.
- [ ] **FORGE-003 — Project Profiles and Production Specification.** ⏳ **Planned.** Reuse studio standards and produce an approved technical/creative specification before irreversible work.
- [ ] **FORGE-004 — Existing-asset intake and audit.** ⏳ **Planned.** Accept concept art, models, rigs, textures, animations, and engine projects, then inspect compatibility and quality.
- [ ] **FORGE-005 — Asset-use classification.** ⏳ **Planned.** Mark each supplied file as Reference Only, Preserve, Editable Source, Replaceable, or Mandatory Dependency.
- [ ] **FORGE-006 — Internet-assisted concept generation.** ⏳ **Planned.** Use an approved image provider for concepts, mood boards, and reference sheets while preserving prompts, settings, and provenance.
- [ ] **FORGE-007 — Complete 3D production pipeline.** ⏳ **Planned.** Automate modeling, topology, UVs, textures/materials, rigging, animation, and LOD creation.
- [ ] **FORGE-008 — Engine validation and repair.** ⏳ **Planned.** Export, import into the target engine, verify scale/materials/rig/animation/collision/sockets, and repair detected failures.
- [ ] **FORGE-009 — Overnight checkpoint recovery.** ⏳ **Planned.** Resume after crash, power loss, or restart without forgetting the goal or why prior choices were made.
- [ ] **FORGE-010 — Parallel work while awaiting direction.** ⏳ **Planned.** Continue known independent tasks and reversible alternatives while blocking only branches that truly require an answer.
- [ ] **FORGE-011 — Complete deliverable package.** ⏳ **Planned.** Return editable sources, runtime assets, checkpoints, timeline, provenance, recovery manifest, and a user study guide.

# 0.5.0 — Physical Device Nodes

- [ ] **PHY-001 — Deterministic Physical Safety Governor.** ⏳ **Planned.** Keep AI planning separate from the controller that decides whether motor movement is physically authorized.
- [ ] **PHY-002 — Hard stop and movement boundaries.** ⏳ **Planned.** Require hardware emergency stop, local takeover, heartbeat timeout, geofence, speed cap, and safe-stop behavior.
- [ ] **PHY-003 — Sensor and command integrity.** ⏳ **Planned.** Verify obstacle/sensor health, sign commands, expire stale instructions, and preserve immutable movement logs.
- [ ] **PHY-004 — RC car and camera/sensor adapters.** ⏳ **Planned.** Begin with low-energy bounded devices before higher-risk mobility systems.
- [ ] **PHY-005 — Drone adapter.** ⏳ **Planned.** Check airspace, weather, battery, return-to-home, geofence, and required human supervision.
- [ ] **PHY-006 — Wheelchair modes.** ⏳ **Planned.** Support empty-chair positioning first and use much stricter stop/takeover rules when occupied.
- [ ] **PHY-007 — Vehicle preconditioning and supported movement.** ⏳ **Planned.** Use official manufacturer interfaces for climate/locks and only permit supervised remote movement where the vehicle officially supports it.

# 0.9.0 — Security, Reliability, and Product Hardening

- [ ] **SEC-001 — Threat model, encryption, secrets, and revocation.** ⏳ **Planned.** Analyze attack surfaces across Android, PC, backend, mesh, browser bridge, and physical nodes; protect data and revoke compromised nodes.
- [ ] **SEC-002 — Fine permissions, audit integrity, and external review.** ⏳ **Planned.** Enforce app/device/skill/mission scopes, detect tampering, and obtain independent security review.
- [ ] **REL-001 — Regression and benchmark suite.** ⏳ **Planned.** Measure mission success, first-attempt success, intervention rate, compatibility, and recovery performance.
- [ ] **REL-002 — Long-duration and failure testing.** ⏳ **Planned.** Test overnight runs, network loss, crashes, power loss, stale accessibility trees, and safe rollback.
- [ ] **PROD-001 — Onboarding, backup, updates, documentation, accessibility, and privacy.** ⏳ **Planned.** Make the system understandable, recoverable, updatable, and transparent for real users.
- [ ] **PROD-002 — Stable signed private beta.** ⏳ **Planned.** Create a repeatable signed release pipeline and test with a controlled user group.

# 1.0.0 — Full SWRLZ Personal Automation Operating Layer

- [ ] **V1-001 — Reliable Android, PC, and relay nodes.** ⏳ **Planned.** The core node types operate consistently and can remain available across local and remote conditions.
- [ ] **V1-002 — Cross-device missions, offline skills, and multi-provider mentoring.** ⏳ **Planned.** Goals can move between nodes, reuse local knowledge, and consult selected mentors.
- [ ] **V1-003 — Shared profile, provenance, and continuity reconciliation.** ⏳ **Planned.** Personal/project context remains synchronized without confusing user statements, automation reports, or provider inferences.
- [ ] **V1-004 — Persistent recovery and teaching.** ⏳ **Planned.** Long missions survive interruption and explain their methods and results to the user.
- [ ] **V1-005 — Secure control and artifact movement.** ⏳ **Planned.** Pairing, permissions, emergency stop, and verified transfers protect the user's devices and data.
- [ ] **V1-006 — Creative, infrastructure, and bounded physical demonstrations.** ⏳ **Planned.** Prove the system across digital production, professional PC/server work, and one carefully limited physical node.
- [ ] **V1-007 — Auditability, benchmarks, signed release, backup, and migration.** ⏳ **Planned.** The platform can be inspected, measured, updated, restored, and moved without losing continuity.

# Patch Tracking Template

## Patch `0.x.x — Name`

**Status:** Planned / Coding / Built / Phone-tested / Released  
**Source baseline:**  
**APK filename:**  
**Date started:**  
**Date completed:**  

### Checklist IDs included

- [ ] `ID` — description
- [ ] `ID` — description

### Acceptance tests

- [ ] Project compiles without errors.
- [ ] APK installs over the previous signed build or the signing-key difference is documented.
- [ ] Existing local data remains available when an upgrade installation is used.
- [ ] Accessibility and overlay behavior is verified.
- [ ] Every new control is tested with normal, empty, partial, and failure states.
- [ ] Network-unavailable and backend-unavailable paths remain graceful.
- [ ] Privacy preview shows the expected redactions.
- [ ] Rollback source/APK is preserved.
- [ ] Roadmap statuses are updated only after observed testing.

# Immediate Test Target — 0.1.5

After compiling this source, test the Allowlist bulk controls, Setup/Mentor configuration, Context Viewer, context levels, encrypted token saving, graceful offline behavior, local Skill Library, and mission failure/watchdog behavior. Once the results are reported, the corresponding **Coded** items can be changed to `[x]` only where the observed result passes.
