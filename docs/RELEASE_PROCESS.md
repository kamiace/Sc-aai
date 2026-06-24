# SWRLZ-CORE Release Process

## Standard release flow

```text
plan -> patch source -> package full source ZIP -> build APK -> phone test -> update roadmap -> next patch
```

## Build-ready repository flow

```text
commit source -> run GitHub Action or Codespaces script -> download APK bundle -> install -> test -> report results
```

## Required release files

Each meaningful patch should update or add:

- `SWRLZ_CORE_MASTER_ROADMAP.md`
- patch report
- phone test checklist
- build instructions if changed
- relevant docs/specs

## APK build script

Primary command:

```bash
bash scripts/build_android_debug_bundle.sh
```

The output bundle should be downloaded from:

```text
APK_DOWNLOAD/
```

## Release gates

A patch should not be considered verified until:

1. Source ZIP or repo source integrity is confirmed.
2. Backend/local tests pass where applicable.
3. Android APK builds in Codespaces or Android Studio.
4. APK installs on phone.
5. User confirms phone checklist results.

## Failure rule

If a build fails, preserve the exact error and patch the source deliberately. Do not randomly rewrite files.
