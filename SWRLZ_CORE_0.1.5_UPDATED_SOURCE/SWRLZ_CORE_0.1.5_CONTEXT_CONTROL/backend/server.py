"""SWRLZ-CORE independent Mentor backend (0.1.5).

- No Emergent or MongoDB dependency.
- SQLite storage works with no AI key.
- Optional OpenAI Responses API powers planning and Council features.
- Accepts the Android Device/App/Screen context fields introduced in 0.1.5.
"""
from __future__ import annotations

import asyncio
import json
import os
import re
import sqlite3
import threading
import uuid
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

from dotenv import load_dotenv
from fastapi import APIRouter, FastAPI, HTTPException, Request
from fastapi.responses import JSONResponse
from pydantic import BaseModel, ConfigDict, Field
from starlette.middleware.cors import CORSMiddleware

ROOT = Path(__file__).resolve().parent
load_dotenv(ROOT / ".env")
DATA_DIR = Path(os.getenv("SWRLZ_DATA_DIR", str(ROOT / "data"))).expanduser()
DATA_DIR.mkdir(parents=True, exist_ok=True)
DB_PATH = Path(os.getenv("SWRLZ_SQLITE_PATH", str(DATA_DIR / "swurlz.sqlite3"))).expanduser()
OPENAI_API_KEY = os.getenv("OPENAI_API_KEY", "").strip()
OPENAI_MODEL = os.getenv("OPENAI_MODEL", "gpt-5.4-mini").strip()
API_TOKEN = os.getenv("SWRLZ_API_TOKEN", "").strip()
CORS = [x.strip() for x in os.getenv("CORS_ORIGINS", "*").split(",") if x.strip()]

app = FastAPI(title="SWRLZ-CORE Mentor", version="0.1.5")
api = APIRouter(prefix="/api")


def now() -> str:
    return datetime.now(timezone.utc).isoformat()


class Store:
    def __init__(self, path: Path):
        self.lock = threading.RLock()
        self.conn = sqlite3.connect(path, check_same_thread=False)
        self.conn.row_factory = sqlite3.Row
        self.conn.execute("PRAGMA journal_mode=WAL")
        self.conn.execute(
            "CREATE TABLE IF NOT EXISTS docs(collection TEXT,id TEXT,data TEXT,updated TEXT,PRIMARY KEY(collection,id))"
        )
        self.conn.commit()

    async def put(self, collection: str, doc: dict[str, Any]) -> dict[str, Any]:
        return await asyncio.to_thread(self._put, collection, doc)

    def _put(self, collection: str, doc: dict[str, Any]) -> dict[str, Any]:
        item = dict(doc)
        item.setdefault("id", str(uuid.uuid4()))
        item.setdefault("created_at", now())
        item["updated_at"] = now()
        with self.lock:
            self.conn.execute(
                "INSERT INTO docs(collection,id,data,updated) VALUES(?,?,?,?) "
                "ON CONFLICT(collection,id) DO UPDATE SET data=excluded.data,updated=excluded.updated",
                (collection, item["id"], json.dumps(item, ensure_ascii=False), item["updated_at"]),
            )
            self.conn.commit()
        return item

    async def get(self, collection: str, item_id: str) -> dict[str, Any] | None:
        return await asyncio.to_thread(self._get, collection, item_id)

    def _get(self, collection: str, item_id: str):
        with self.lock:
            row = self.conn.execute("SELECT data FROM docs WHERE collection=? AND id=?", (collection, item_id)).fetchone()
        return json.loads(row["data"]) if row else None

    async def list(self, collection: str) -> list[dict[str, Any]]:
        return await asyncio.to_thread(self._list, collection)

    def _list(self, collection: str):
        with self.lock:
            rows = self.conn.execute("SELECT data FROM docs WHERE collection=? ORDER BY updated DESC", (collection,)).fetchall()
        return [json.loads(row["data"]) for row in rows]

    async def update(self, collection: str, item_id: str, patch: dict[str, Any]):
        item = await self.get(collection, item_id)
        if item is None:
            return None
        item.update(patch)
        return await self.put(collection, item)

    async def delete(self, collection: str, item_id: str) -> int:
        def work():
            with self.lock:
                cur = self.conn.execute("DELETE FROM docs WHERE collection=? AND id=?", (collection, item_id))
                self.conn.commit()
                return int(cur.rowcount)
        return await asyncio.to_thread(work)


store = Store(DB_PATH)


class Model(BaseModel):
    model_config = ConfigDict(extra="ignore")


class UiNode(Model):
    id: str
    cls: str | None = None
    text: str | None = None
    desc: str | None = None
    pkg: str | None = None
    clickable: bool = False
    editable: bool = False
    password: bool = False
    enabled: bool = True
    checked: bool | None = None
    selected: bool = False
    scrollable: bool = False
    bounds: list[int] | None = None


class PlanRequest(Model):
    mission_id: str
    goal: str
    package: str | None = None
    screen_summary: str | None = None
    nodes: list[UiNode] = Field(default_factory=list)
    history: list[dict] = Field(default_factory=list)
    autonomy: str = "guarded"
    device_context: dict | None = None
    app_context: dict | None = None
    screen_delta: dict | None = None
    context_level: str = "STANDARD"


class Action(Model):
    type: str
    node_id: str | None = None
    text: str | None = None
    direction: str | None = None
    ms: int | None = None
    package: str | None = None
    reason: str = ""
    risk: str = "green"
    confidence: float = 0.8
    expected: str | None = None


class PlanResponse(Model):
    mission_id: str
    actions: list[Action]
    rationale: str
    needs_approval: bool
    next_observation_hint: str | None = None


class MissionCreate(Model):
    goal: str
    autonomy: str = "guarded"
    package: str | None = None


class Mission(Model):
    id: str = Field(default_factory=lambda: str(uuid.uuid4()))
    goal: str
    state: str = "drafted"
    autonomy: str = "guarded"
    package: str | None = None
    confidence: float = 0.8
    risk: str = "green"
    step: int = 0
    total: int = 0
    created_at: str = Field(default_factory=now)
    updated_at: str = Field(default_factory=now)


class Skill(Model):
    id: str = Field(default_factory=lambda: str(uuid.uuid4()))
    name: str
    intent: str
    triggers: list[str] = Field(default_factory=list)
    plan: list[dict] = Field(default_factory=list)
    risk: str = "green"
    confidence: float = 0.7
    package_hints: list[str] = Field(default_factory=list)
    source: str = "demonstration"
    version: int = 1
    created_at: str = Field(default_factory=now)
    updated_at: str = Field(default_factory=now)


@app.middleware("http")
async def auth(request: Request, call_next):
    if API_TOKEN and request.url.path.startswith("/api") and request.url.path not in {"/api", "/api/health"}:
        if request.headers.get("X-Swurlz-Token", "") != API_TOKEN:
            return JSONResponse(status_code=401, content={"detail": "invalid SWRLZ API token"})
    return await call_next(request)


@api.get("/")
@api.get("/health")
async def health():
    return {
        "ok": True,
        "service": "swurlz-mentor",
        "version": "0.1.5",
        "storage": "sqlite",
        "mentor_provider": "openai" if OPENAI_API_KEY else "not-configured",
        "context_schema": 1,
    }


@api.post("/missions", response_model=Mission)
async def create_mission(body: MissionCreate):
    return await store.put("missions", Mission(**body.model_dump()).model_dump())


@api.get("/missions", response_model=list[Mission])
async def list_missions():
    return await store.list("missions")


@api.post("/missions/{mission_id}/state")
async def mission_state(mission_id: str, body: dict):
    item = await store.update("missions", mission_id, {k: v for k, v in body.items() if k in {"state", "step", "total"}})
    if not item:
        raise HTTPException(404, "mission not found")
    return {"ok": True}


@api.post("/missions/{mission_id}/events")
async def event(mission_id: str, body: dict):
    item = {"id": str(uuid.uuid4()), "mission_id": mission_id, "timestamp": now(), **body}
    return await store.put("events", item)


@api.get("/skills", response_model=list[Skill])
async def skills():
    return await store.list("skills")


@api.post("/skills", response_model=Skill)
async def create_skill(body: Skill):
    return await store.put("skills", body.model_dump())


@api.delete("/skills/{skill_id}")
async def delete_skill(skill_id: str):
    return {"deleted": await store.delete("skills", skill_id)}


PLANNER = """You are SWRLZ-CORE's supervised Android action planner.
Return only strict JSON containing rationale, needs_approval, next_observation_hint, and 1-5 actions.
Allowed actions: tap, type_text, scroll, wait, back, home, open_app, ask_user, done.
Every action needs reason, risk (green/yellow/red/black), confidence, and expected.
Never repeat the same failed tap three times. Respect the supplied device/app/screen context and progress history.
Black-risk actions must become ask_user. Red actions require approval."""


async def call_openai(prompt: str) -> str:
    if not OPENAI_API_KEY:
        raise HTTPException(503, "Online Mentor is not configured. Set OPENAI_API_KEY; local storage remains available.")

    def work():
        from openai import OpenAI
        client = OpenAI(api_key=OPENAI_API_KEY)
        response = client.responses.create(model=OPENAI_MODEL, instructions=PLANNER, input=prompt, store=False)
        return response.output_text

    return await asyncio.to_thread(work)


def extract_json(text: str) -> dict:
    text = re.sub(r"^```(?:json)?\s*|\s*```$", "", text.strip(), flags=re.MULTILINE)
    match = re.search(r"\{.*\}", text, re.DOTALL)
    if not match:
        raise ValueError("model response did not contain a JSON object")
    return json.loads(match.group(0))


@api.post("/plan", response_model=PlanResponse)
async def plan(body: PlanRequest):
    prompt = json.dumps(body.model_dump(), ensure_ascii=False)[:24000]
    data = extract_json(await call_openai(prompt))
    return PlanResponse(
        mission_id=body.mission_id,
        actions=[Action(**a) for a in data.get("actions", [])],
        rationale=data.get("rationale", ""),
        needs_approval=bool(data.get("needs_approval", False)),
        next_observation_hint=data.get("next_observation_hint"),
    )


@api.post("/skills/suggest")
async def suggest_skill(body: dict):
    return {"ok": False, "reason": "skill distillation is planned for a later patch"}


app.include_router(api)
app.add_middleware(
    CORSMiddleware,
    allow_origins=CORS,
    allow_credentials=False if CORS == ["*"] else True,
    allow_methods=["*"],
    allow_headers=["*"],
)
