# Security Policy

SWRLZ-CORE is an automation project. Security, privacy, and user control are treated as first-class architecture requirements.

## Never commit

- OpenAI or other provider API keys
- `SWRLZ_API_TOKEN`
- `.env`
- `local.properties`
- signing keys / keystores
- personal credentials
- raw private logs containing sensitive data

## Reporting security issues

Open a private report to the project owner when possible. If only public issues are available, avoid posting secrets or exploit-ready details. Describe the affected area and ask for a private follow-up.

## High-risk areas

Changes require extra review when they touch:

- Android Accessibility automation
- app allowlist / blocklist logic
- Permission Controller / Package Installer / System UI handling
- external messages, payments, account changes, file deletion
- context sharing to backend or online Mentor
- patch download, manifest verification, staging, rollback
- local memory, taught skills, logs, or deltas

## Security design rule

The stricter safety rule wins by default until reviewed. Local user safety overrides must not be silently erased by official patches.
