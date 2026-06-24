# Safety Gate Specification

## Risk levels

| Risk | Description | Examples | Default policy |
|---|---|---|---|
| low | Reversible or informational actions | Open app, read status, change brightness | Allowed if app/package is allowed. |
| medium | Actions that modify settings or send prepared local workflows | Modify Wi-Fi setting, run multi-step workflow | Confirm until trusted. |
| high | Actions that can delete, spend, message, share, or alter accounts | Delete files/email, send external message, share data | Explicit approval every mission. |
| locked | Actions too sensitive for normal automation | Permanent deletion, financial transfer, physical movement | Explicit unlock + review + approval. |

## Required gate checks

- App/package is allowed.
- Action type is permitted for the current trust level.
- Required permissions are present.
- Current context matches the expected app/screen.
- User approval exists for medium/high/locked actions.
- Emergency stop is available.
- Result can be verified or safely paused.

## Logging

Every attempted action records timestamp, trigger, mission ID, risk, decision, result, and rollback data if available.
