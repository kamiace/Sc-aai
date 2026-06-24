"""SWRLZ local-first node prototype for Pzhp-LDS.

This server is intentionally small and local-first. It stores status, commands,
teaching deltas, logs, and patch-check results on disk without requiring paid AI
providers. It is a prototype reference for the future Android/Kotlin service.
"""
from __future__ import annotations

import json
import os
import uuid
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field

ROOT = Path(__file__).resolve().parent.parent
DATA_DIR = Path(os.getenv("SWRLZ_LOCAL_NODE_DATA", str(ROOT / "local_node_data")))
CORE_DIR = Path(os.getenv("SWRLZ_CORE_DIR", str(ROOT / "core")))
DATA_DIR.mkdir(parents=True, exist_ok=True)

app = FastAPI(title="SWRLZ Local Node Prototype", version="0.1.6-pzhp-lds-planning")


def now() -> str:
    return datetime.now(timezone.utc).isoformat()


def read_json(path: Path, fallback: Any) -> Any:
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except FileNotFoundError:
        return fallback


def append_jsonl(path: Path, obj: dict[str, Any]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("a", encoding="utf-8") as handle:
        handle.write(json.dumps(obj, ensure_ascii=False, separators=(",", ":")) + "\n")


def read_jsonl(path: Path, limit: int = 100) -> list[dict[str, Any]]:
    if not path.exists():
        return []
    rows = []
    for line in path.read_text(encoding="utf-8").splitlines()[-limit:]:
        if not line.strip():
            continue
        try:
            rows.append(json.loads(line))
        except json.JSONDecodeError:
            rows.append({"type": "corrupt_log_line", "raw": line})
    return rows


class CommandRequest(BaseModel):
    text: str
    mission_id: str = Field(default_factory=lambda: str(uuid.uuid4()))
    dry_run: bool = True


class TeachRequest(BaseModel):
    skill_name: str
    trigger_phrases: list[str]
    steps: list[str]
    safety_level: str = "low"
    requires_confirmation: bool = True
    scope: str = "private_local"
    user_approved: bool = True
    device_id: str = "local_node"


@app.get("/status")
def status() -> dict[str, Any]:
    version = read_json(CORE_DIR / "version.json", {})
    return {
        "ok": True,
        "node": "SWRLZ Local Node Prototype",
        "time": now(),
        "core_version": version.get("core_version", "unknown"),
        "core_dir": str(CORE_DIR),
        "data_dir": str(DATA_DIR),
        "endpoints": ["/status", "/command", "/teach", "/logs", "/sync", "/update-check"],
        "paid_ai_required": False,
    }


@app.post("/command")
def command(body: CommandRequest) -> dict[str, Any]:
    event = {
        "timestamp": now(),
        "mission_id": body.mission_id,
        "sender": "SWRLZ-CORE · LOCAL NODE",
        "event": "command_received",
        "text": body.text,
        "dry_run": body.dry_run,
        "result": "accepted_for_planning_only" if body.dry_run else "queued_not_executed_in_prototype",
    }
    append_jsonl(DATA_DIR / "action_log.jsonl", event)
    return {"ok": True, "mission_id": body.mission_id, "dry_run": body.dry_run, "message": event["result"]}


@app.post("/teach")
def teach(body: TeachRequest) -> dict[str, Any]:
    if body.safety_level not in {"low", "medium", "high", "locked"}:
        raise HTTPException(400, "invalid safety_level")
    if body.scope not in {"private_local", "personal_synced", "official_candidate"}:
        raise HTTPException(400, "invalid scope")
    version = read_json(CORE_DIR / "version.json", {})
    delta = {
        "type": "user_taught_skill",
        "skill_name": body.skill_name,
        "created_on_device": body.device_id,
        "created_at": now(),
        "core_version_when_taught": version.get("core_version", "unknown"),
        "trigger_phrases": body.trigger_phrases,
        "steps": body.steps,
        "safety_level": body.safety_level,
        "requires_confirmation": body.requires_confirmation,
        "scope": body.scope,
        "status": "local_candidate",
        "user_approved": body.user_approved,
        "synced": False,
    }
    append_jsonl(DATA_DIR / "local_deltas.jsonl", delta)
    append_jsonl(DATA_DIR / "action_log.jsonl", {"timestamp": now(), "event": "teach_saved", "skill_name": body.skill_name})
    return {"ok": True, "delta": delta}


@app.get("/logs")
def logs(limit: int = 100) -> dict[str, Any]:
    return {
        "ok": True,
        "actions": read_jsonl(DATA_DIR / "action_log.jsonl", limit=limit),
        "local_deltas": read_jsonl(DATA_DIR / "local_deltas.jsonl", limit=limit),
    }


@app.get("/sync")
def sync_status() -> dict[str, Any]:
    deltas = read_jsonl(DATA_DIR / "local_deltas.jsonl", limit=10000)
    unsynced = [d for d in deltas if not d.get("synced")]
    return {"ok": True, "unsynced_count": len(unsynced), "mode": "report_only_prototype", "unsynced": unsynced[:20]}


@app.get("/update-check")
def update_check(remote_manifest_path: str | None = None) -> dict[str, Any]:
    local_version = read_json(CORE_DIR / "version.json", {})
    manifest_path = Path(remote_manifest_path) if remote_manifest_path else CORE_DIR / "patch_manifest.json"
    remote_manifest = read_json(manifest_path, {})
    local_core = local_version.get("core_version", "unknown")
    remote_core = remote_manifest.get("core_version", "unknown")
    update_available = remote_core != "unknown" and remote_core != local_core
    return {
        "ok": True,
        "local_core_version": local_core,
        "remote_core_version": remote_core,
        "update_available": update_available,
        "mode": "compare_only_no_apply",
        "manifest_path": str(manifest_path),
    }
