"""SWRLZ-CORE independent Mentor backend.

Key properties
- No Emergent runtime or credit dependency.
- SQLite persistence by default; no MongoDB service required.
- Optional OpenAI Responses API for Mentor planning/chat/skill distillation.
- Skills, missions, events, and chat history remain available even when no AI key is set.
- Optional shared API token for a private single-user deployment.
"""

from __future__ import annotations

import asyncio
import json
import logging
import os
import re
import sqlite3
import threading
import uuid
from datetime import datetime, timezone
from pathlib import Path
from typing import Annotated, Any, AsyncIterator

from dotenv import load_dotenv
from fastapi import APIRouter, FastAPI, HTTPException, Request, WebSocket, WebSocketDisconnect
from fastapi.responses import JSONResponse, StreamingResponse
from pydantic import BaseModel, BeforeValidator, ConfigDict, Field
from starlette.middleware.cors import CORSMiddleware

ROOT_DIR = Path(__file__).resolve().parent
load_dotenv(ROOT_DIR / ".env")

DATA_DIR = Path(os.getenv("SWRLZ_DATA_DIR", str(ROOT_DIR / "data"))).expanduser()
DATA_DIR.mkdir(parents=True, exist_ok=True)
SQLITE_PATH = Path(os.getenv("SWRLZ_SQLITE_PATH", str(DATA_DIR / "swurlz_core.sqlite3"))).expanduser()
OPENAI_API_KEY = os.getenv("OPENAI_API_KEY", "").strip()
OPENAI_MODEL = os.getenv("OPENAI_MODEL", "gpt-5.5").strip() or "gpt-5.5"
API_TOKEN = os.getenv("SWRLZ_API_TOKEN", "").strip()
CORS_ORIGINS = [x.strip() for x in os.getenv("CORS_ORIGINS", "*").split(",") if x.strip()]

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(name)s: %(message)s")
log = logging.getLogger("swurlz")

app = FastAPI(title="SWRLZ-CORE Mentor", version="0.1.4")
api = APIRouter(prefix="/api")


def utc_now_iso() -> str:
    return datetime.now(timezone.utc).isoformat()


# ---------------------------------------------------------------------------
# Tiny local document store
# ---------------------------------------------------------------------------
class LocalStore:
    """A small SQLite JSON-document store suited to a private single-user node."""

    def __init__(self, path: Path):
        self.path = path
        self._lock = threading.RLock()
        self._conn = sqlite3.connect(path, check_same_thread=False)
        self._conn.row_factory = sqlite3.Row
        self._conn.execute("PRAGMA journal_mode=WAL")
        self._conn.execute("PRAGMA synchronous=NORMAL")
        self._conn.execute(
            """
            CREATE TABLE IF NOT EXISTS documents (
                collection TEXT NOT NULL,
                id TEXT NOT NULL,
                data TEXT NOT NULL,
                created_at TEXT NOT NULL,
                updated_at TEXT NOT NULL,
                PRIMARY KEY (collection, id)
            )
            """
        )
        self._conn.execute(
            "CREATE INDEX IF NOT EXISTS idx_documents_collection_updated "
            "ON documents(collection, updated_at DESC)"
        )
        self._conn.commit()

    def close(self) -> None:
        with self._lock:
            self._conn.close()

    async def put(self, collection: str, doc: dict[str, Any]) -> dict[str, Any]:
        return await asyncio.to_thread(self._put, collection, doc)

    def _put(self, collection: str, doc: dict[str, Any]) -> dict[str, Any]:
        item = dict(doc)
        item.setdefault("id", str(uuid.uuid4()))
        item.setdefault("created_at", utc_now_iso())
        item["updated_at"] = item.get("updated_at") or utc_now_iso()
        with self._lock:
            self._conn.execute(
                """
                INSERT INTO documents(collection,id,data,created_at,updated_at)
                VALUES(?,?,?,?,?)
                ON CONFLICT(collection,id) DO UPDATE SET
                    data=excluded.data,
                    updated_at=excluded.updated_at
                """,
                (
                    collection,
                    str(item["id"]),
                    json.dumps(item, ensure_ascii=False, separators=(",", ":")),
                    str(item["created_at"]),
                    str(item["updated_at"]),
                ),
            )
            self._conn.commit()
        return item

    async def get(self, collection: str, item_id: str) -> dict[str, Any] | None:
        return await asyncio.to_thread(self._get, collection, item_id)

    def _get(self, collection: str, item_id: str) -> dict[str, Any] | None:
        with self._lock:
            row = self._conn.execute(
                "SELECT data FROM documents WHERE collection=? AND id=?", (collection, item_id)
            ).fetchone()
        return json.loads(row["data"]) if row else None

    async def list(
        self,
        collection: str,
        *,
        where: dict[str, Any] | None = None,
        sort_key: str = "updated_at",
        reverse: bool = True,
        limit: int = 500,
    ) -> list[dict[str, Any]]:
        rows = await asyncio.to_thread(self._list_all, collection)
        if where:
            rows = [r for r in rows if all(r.get(k) == v for k, v in where.items())]
        rows.sort(key=lambda r: r.get(sort_key, ""), reverse=reverse)
        return rows[:limit]

    def _list_all(self, collection: str) -> list[dict[str, Any]]:
        with self._lock:
            rows = self._conn.execute(
                "SELECT data FROM documents WHERE collection=?", (collection,)
            ).fetchall()
        return [json.loads(r["data"]) for r in rows]

    async def update(self, collection: str, item_id: str, patch: dict[str, Any]) -> dict[str, Any] | None:
        current = await self.get(collection, item_id)
        if current is None:
            return None
        current.update(patch)
        current["updated_at"] = utc_now_iso()
        return await self.put(collection, current)

    async def delete(self, collection: str, item_id: str) -> int:
        return await asyncio.to_thread(self._delete, collection, item_id)

    def _delete(self, collection: str, item_id: str) -> int:
        with self._lock:
            cur = self._conn.execute(
                "DELETE FROM documents WHERE collection=? AND id=?", (collection, item_id)
            )
            self._conn.commit()
            return int(cur.rowcount)

    async def count(self, collection: str, where: dict[str, Any] | None = None) -> int:
        return len(await self.list(collection, where=where, limit=100_000))


store = LocalStore(SQLITE_PATH)


# ---------------------------------------------------------------------------
# Models
# ---------------------------------------------------------------------------
def _to_str(v: Any) -> str:
    return str(v) if v is not None else v


PyObjectId = Annotated[str, BeforeValidator(_to_str)]


class BaseDoc(BaseModel):
    model_config = ConfigDict(populate_by_name=True, extra="ignore")
    id: PyObjectId = Field(default_factory=lambda: str(uuid.uuid4()), alias="id")


class UiNode(BaseModel):
    id: str
    cls: str | None = None
    text: str | None = None
    desc: str | None = None
    pkg: str | None = None
    clickable: bool = False
    editable: bool = False
    bounds: list[int] | None = None


class PlanRequest(BaseModel):
    mission_id: str
    goal: str
    package: str | None = None
    screen_summary: str | None = None
    nodes: list[UiNode] = Field(default_factory=list)
    history: list[dict] = Field(default_factory=list)
    autonomy: str = "guarded"


class Action(BaseModel):
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


class PlanResponse(BaseModel):
    mission_id: str
    actions: list[Action]
    rationale: str
    needs_approval: bool
    next_observation_hint: str | None = None


class SkillCapsule(BaseDoc):
    name: str
    intent: str
    triggers: list[str] = Field(default_factory=list)
    plan: list[dict] = Field(default_factory=list)
    risk: str = "green"
    confidence: float = 0.7
    package_hints: list[str] = Field(default_factory=list)
    source: str = "demonstration"
    version: int = 1
    created_at: str = Field(default_factory=utc_now_iso)
    updated_at: str = Field(default_factory=utc_now_iso)


class Mission(BaseDoc):
    goal: str
    state: str = "drafted"
    autonomy: str = "guarded"
    package: str | None = None
    confidence: float = 0.8
    risk: str = "green"
    step: int = 0
    total: int = 0
    created_at: str = Field(default_factory=utc_now_iso)
    updated_at: str = Field(default_factory=utc_now_iso)


class MissionCreate(BaseModel):
    goal: str
    autonomy: str = "guarded"
    package: str | None = None


class ActionEvent(BaseDoc):
    mission_id: str
    seq: int
    phase: str
    action: dict
    observed: str | None = None
    timestamp: str = Field(default_factory=utc_now_iso)
    created_at: str = Field(default_factory=utc_now_iso)
    updated_at: str = Field(default_factory=utc_now_iso)


class ChatRequest(BaseModel):
    session_id: str
    text: str
    persona: str = "council"
    context: dict | None = None


# ---------------------------------------------------------------------------
# Prompts and optional OpenAI Mentor
# ---------------------------------------------------------------------------
COUNCIL_SYSTEM = """You are participating in the SWRLZ-CORE Council Cockpit.
There are three voices: USER SOVEREIGN, LOCAL OPERATOR, and SWURLZ MENTOR.
The user's word is final. The Operator is terse, technical, and action-oriented.
The Mentor is careful, strategic, and surfaces uncertainty and risk.
Start with [OPERATOR], [MENTOR], or [COUNCIL] as requested. Keep normal replies
under 180 words. Never recommend irreversible device actions without explicit
approval, rollback guidance, and a warning."""

PLANNER_SYSTEM = """You are the SWRLZ-CORE supervised Android action planner.
Return only STRICT JSON for the next 1-5 atomic actions. Allowed types:
tap, type_text, scroll, wait, back, home, open_app, ask_user, done.
Each action requires reason, risk (green|yellow|red|black), confidence, expected.
Red actions require approval. Black actions must become ask_user.

Progress watchdog rules:
- Inspect ACTION_HISTORY for repeated actions, watchdog events, failures, and unchanged screens.
- Never repeat the same tap three times when no meaningful state change was observed.
- When UI lag/animation is plausible, use one bounded wait (500-1500 ms), then re-observe.
- When a node may be stale, choose a newly observed semantic node rather than old coordinates.
- When blocked by an overlay, disabled control, permission barrier, or wrong route, ask the user
  or choose a safe alternate route. Do not perform random taps.

Schema:
{
  "rationale": "2-3 concise sentences",
  "needs_approval": false,
  "next_observation_hint": "what state change proves success",
  "actions": [{"type":"tap","node_id":"n1","reason":"...","risk":"green","confidence":0.9,"expected":"..."}]
}"""


def _extract_json(text: str) -> dict:
    text = text.strip()
    text = re.sub(r"^```(?:json)?\s*|\s*```$", "", text, flags=re.MULTILINE).strip()
    match = re.search(r"\{.*\}", text, re.DOTALL)
    if not match:
        raise ValueError(f"No JSON object in model output: {text[:200]}")
    return json.loads(match.group(0))


async def call_mentor(system: str, input_data: Any) -> str:
    if not OPENAI_API_KEY:
        raise HTTPException(
            status_code=503,
            detail="Online Mentor is not configured. Set OPENAI_API_KEY; local storage remains available.",
        )

    def _call() -> str:
        from openai import OpenAI

        client = OpenAI(api_key=OPENAI_API_KEY)
        response = client.responses.create(
            model=OPENAI_MODEL,
            instructions=system,
            input=input_data,
            store=False,
        )
        return response.output_text

    try:
        return await asyncio.to_thread(_call)
    except HTTPException:
        raise
    except Exception as exc:
        log.exception("Mentor request failed")
        raise HTTPException(status_code=502, detail=f"Mentor request failed: {type(exc).__name__}") from exc


# ---------------------------------------------------------------------------
# Authentication and WebSocket hub
# ---------------------------------------------------------------------------
@app.middleware("http")
async def private_api_token(request: Request, call_next):
    path = request.url.path.rstrip("/")
    public = {"/api", "/api/health", "/docs", "/openapi.json", "/redoc"}
    if API_TOKEN and request.url.path.startswith("/api") and path not in public:
        supplied = request.headers.get("X-Swurlz-Token", "")
        if supplied != API_TOKEN:
            return JSONResponse(status_code=401, content={"detail": "invalid SWRLZ API token"})
    return await call_next(request)


class Hub:
    def __init__(self) -> None:
        self.clients: dict[str, set[WebSocket]] = {}
        self.lock = asyncio.Lock()

    async def join(self, mission_id: str, ws: WebSocket) -> None:
        async with self.lock:
            self.clients.setdefault(mission_id, set()).add(ws)

    async def leave(self, mission_id: str, ws: WebSocket) -> None:
        async with self.lock:
            self.clients.get(mission_id, set()).discard(ws)

    async def broadcast(self, mission_id: str, payload: dict) -> None:
        dead: list[WebSocket] = []
        for ws in list(self.clients.get(mission_id, set())):
            try:
                await ws.send_json(payload)
            except Exception:
                dead.append(ws)
        for ws in dead:
            await self.leave(mission_id, ws)


hub = Hub()


# ---------------------------------------------------------------------------
# Health
# ---------------------------------------------------------------------------
@api.get("/")
async def root() -> dict:
    return await health()


@api.get("/health")
async def health() -> dict:
    return {
        "service": "swurlz-mentor",
        "version": "0.1.4",
        "ok": True,
        "time": utc_now_iso(),
        "storage": "sqlite",
        "sqlite_path": str(SQLITE_PATH),
        "mentor_provider": "openai" if OPENAI_API_KEY else "not-configured",
        "mentor_model": OPENAI_MODEL if OPENAI_API_KEY else None,
        "token_auth": bool(API_TOKEN),
    }


# ---------------------------------------------------------------------------
# Missions and events
# ---------------------------------------------------------------------------
@api.post("/missions", response_model=Mission)
async def create_mission(body: MissionCreate) -> Mission:
    mission = Mission(goal=body.goal, autonomy=body.autonomy, package=body.package)
    await store.put("missions", mission.model_dump())
    return mission


@api.get("/missions", response_model=list[Mission])
async def list_missions() -> list[Mission]:
    rows = await store.list("missions", sort_key="created_at", reverse=True, limit=200)
    return [Mission(**row) for row in rows]


@api.get("/missions/{mission_id}", response_model=Mission)
async def get_mission(mission_id: str) -> Mission:
    row = await store.get("missions", mission_id)
    if not row:
        raise HTTPException(404, "mission not found")
    return Mission(**row)


@api.post("/missions/{mission_id}/state")
async def set_mission_state(mission_id: str, body: dict) -> dict:
    patch = {k: v for k, v in body.items() if k in {"state", "step", "total", "confidence", "risk"}}
    row = await store.update("missions", mission_id, patch)
    if not row:
        raise HTTPException(404, "mission not found")
    await hub.broadcast(mission_id, {"kind": "mission_state", "mission_id": mission_id, **patch})
    return {"ok": True}


@api.post("/missions/{mission_id}/events", response_model=ActionEvent)
async def log_event(mission_id: str, body: dict) -> ActionEvent:
    seq = body.get("seq")
    if seq is None:
        seq = await store.count("events", where={"mission_id": mission_id})
    event = ActionEvent(
        mission_id=mission_id,
        seq=int(seq),
        phase=body.get("phase", "executing"),
        action=body.get("action", {}),
        observed=body.get("observed"),
    )
    await store.put("events", event.model_dump())
    await hub.broadcast(mission_id, {"kind": "event", **event.model_dump()})
    return event


@api.get("/missions/{mission_id}/events", response_model=list[ActionEvent])
async def list_events(mission_id: str) -> list[ActionEvent]:
    rows = await store.list(
        "events", where={"mission_id": mission_id}, sort_key="seq", reverse=False, limit=2000
    )
    return [ActionEvent(**row) for row in rows]


# ---------------------------------------------------------------------------
# Planner
# ---------------------------------------------------------------------------
@api.post("/plan", response_model=PlanResponse)
async def plan(body: PlanRequest) -> PlanResponse:
    node_lines = [
        f"- id={n.id} cls={n.cls or '-'} click={int(n.clickable)} edit={int(n.editable)} "
        f"text={(n.text or '')[:60]!r} desc={(n.desc or '')[:40]!r} bounds={n.bounds}"
        for n in body.nodes[:80]
    ]
    hist_lines = []
    for h in body.history[-30:]:
        action = h.get("action", {}) if isinstance(h, dict) else {}
        hist_lines.append(
            f"- seq={h.get('seq')} phase={h.get('phase')} action={action.get('type')} "
            f"node={action.get('node_id')} observed={h.get('observed', '')}"
        )

    user_text = (
        f"GOAL: {body.goal}\n"
        f"FOCUSED_PACKAGE: {body.package or 'unknown'}\n"
        f"AUTONOMY: {body.autonomy}\n"
        f"SCREEN_SUMMARY: {body.screen_summary or '(none)'}\n"
        f"NODES ({len(body.nodes)} total):\n" + "\n".join(node_lines)
        + "\n\nACTION_HISTORY:\n" + ("\n".join(hist_lines) or "(none)")
        + "\n\nReturn strict JSON only."
    )
    raw = await call_mentor(PLANNER_SYSTEM, user_text)
    try:
        data = _extract_json(raw)
        actions = [Action(**action) for action in data.get("actions", [])]
    except Exception as exc:
        log.error("Planner parse failed: %s | raw=%s", exc, raw[:500])
        raise HTTPException(502, f"planner returned invalid JSON: {exc}") from exc

    response = PlanResponse(
        mission_id=body.mission_id,
        actions=actions,
        rationale=data.get("rationale", ""),
        needs_approval=bool(data.get("needs_approval", False)),
        next_observation_hint=data.get("next_observation_hint"),
    )
    await hub.broadcast(body.mission_id, {"kind": "plan", **response.model_dump()})
    return response


# ---------------------------------------------------------------------------
# Council chat with local durable memory
# ---------------------------------------------------------------------------
@api.post("/council/chat")
async def council_chat(body: ChatRequest) -> StreamingResponse:
    persona = body.persona.lower()
    prefix = {
        "operator": "Respond as [OPERATOR] only.",
        "mentor": "Respond as [MENTOR] only.",
        "council": "Respond as [COUNCIL] with one Operator line and one Mentor line.",
    }.get(persona, "Respond as [COUNCIL].")

    history = await store.list(
        "chat_messages", where={"session_id": body.session_id}, sort_key="ts", reverse=False, limit=40
    )
    input_items: list[dict[str, str]] = []
    for msg in history[-24:]:
        role = "assistant" if msg.get("role") == "assistant" else "user"
        input_items.append({"role": role, "content": str(msg.get("text", ""))})

    user_text = f"{prefix}\n\nUSER: {body.text}"
    if body.context:
        user_text += f"\n\nDEVICE/MISSION CONTEXT: {json.dumps(body.context)[:3000]}"
    input_items.append({"role": "user", "content": user_text})

    user_doc = {
        "id": str(uuid.uuid4()),
        "session_id": body.session_id,
        "role": "user",
        "text": body.text,
        "ts": utc_now_iso(),
    }
    await store.put("chat_messages", user_doc)

    async def generate() -> AsyncIterator[str]:
        try:
            full = await call_mentor(COUNCIL_SYSTEM, input_items)
            for index in range(0, len(full), 96):
                yield f"data: {json.dumps({'delta': full[index:index + 96]})}\n\n"
                await asyncio.sleep(0)
            await store.put(
                "chat_messages",
                {
                    "id": str(uuid.uuid4()),
                    "session_id": body.session_id,
                    "role": "assistant",
                    "persona": persona,
                    "text": full,
                    "ts": utc_now_iso(),
                },
            )
            yield f"data: {json.dumps({'done': True})}\n\n"
        except HTTPException as exc:
            yield f"data: {json.dumps({'error': exc.detail})}\n\n"
        except Exception as exc:
            log.exception("Council chat failed")
            yield f"data: {json.dumps({'error': str(exc)})}\n\n"

    return StreamingResponse(
        generate(),
        media_type="text/event-stream",
        headers={"Cache-Control": "no-cache", "X-Accel-Buffering": "no"},
    )


@api.get("/council/history/{session_id}")
async def chat_history(session_id: str) -> list[dict]:
    return await store.list(
        "chat_messages", where={"session_id": session_id}, sort_key="ts", reverse=False, limit=500
    )


# ---------------------------------------------------------------------------
# Skills
# ---------------------------------------------------------------------------
@api.post("/skills", response_model=SkillCapsule)
async def create_skill(body: SkillCapsule) -> SkillCapsule:
    await store.put("skills", body.model_dump())
    return body


@api.get("/skills", response_model=list[SkillCapsule])
async def list_skills() -> list[SkillCapsule]:
    rows = await store.list("skills", sort_key="updated_at", reverse=True, limit=500)
    return [SkillCapsule(**row) for row in rows]


@api.get("/skills/{skill_id}", response_model=SkillCapsule)
async def get_skill(skill_id: str) -> SkillCapsule:
    row = await store.get("skills", skill_id)
    if not row:
        raise HTTPException(404, "skill not found")
    return SkillCapsule(**row)


@api.delete("/skills/{skill_id}")
async def delete_skill(skill_id: str) -> dict:
    return {"deleted": await store.delete("skills", skill_id)}


@api.patch("/skills/{skill_id}", response_model=SkillCapsule)
async def update_skill(skill_id: str, body: dict) -> SkillCapsule:
    row = await store.update("skills", skill_id, body)
    if not row:
        raise HTTPException(404, "skill not found")
    return SkillCapsule(**row)


@api.post("/skills/suggest")
async def suggest_skill(body: dict) -> dict:
    mission_id = body.get("mission_id")
    if not mission_id:
        raise HTTPException(400, "mission_id required")
    mission = await store.get("missions", mission_id)
    if not mission:
        raise HTTPException(404, "mission not found")
    events = await store.list(
        "events", where={"mission_id": mission_id}, sort_key="seq", reverse=False, limit=500
    )
    successful = [event for event in events if event.get("phase") == "success"]
    if not successful:
        return {"ok": False, "reason": "no successful actions"}

    prompt = (
        "Return strict JSON for a reusable Skill Capsule with keys name, intent, triggers, risk, "
        "confidence, package_hints, and plan. Preserve only successful, necessary, safe actions.\n\n"
        f"GOAL: {mission.get('goal')}\n"
        f"PACKAGE: {mission.get('package')}\n"
        f"SUCCESSFUL_ACTIONS: {json.dumps([e['action'] for e in successful])[:6000]}"
    )
    raw = await call_mentor(
        "You distill Android automation runs into safe, reusable SWRLZ Skill Capsules. Return JSON only.",
        prompt,
    )
    try:
        suggestion = _extract_json(raw)
    except Exception as exc:
        raise HTTPException(502, f"skill suggestion invalid JSON: {exc}") from exc
    return {"ok": True, "suggestion": suggestion}


# ---------------------------------------------------------------------------
# WebSocket telemetry
# ---------------------------------------------------------------------------
@app.websocket("/api/missions/{mission_id}/stream")
async def stream(ws: WebSocket, mission_id: str) -> None:
    if API_TOKEN:
        supplied = ws.headers.get("X-Swurlz-Token") or ws.query_params.get("token", "")
        if supplied != API_TOKEN:
            await ws.close(code=4401)
            return
    await ws.accept()
    await hub.join(mission_id, ws)
    try:
        rows = await store.list(
            "events", where={"mission_id": mission_id}, sort_key="seq", reverse=False, limit=200
        )
        await ws.send_json({"kind": "history", "events": rows})
        while True:
            message = await ws.receive_text()
            if message == "ping":
                await ws.send_json({"kind": "pong", "ts": utc_now_iso()})
    except WebSocketDisconnect:
        pass
    finally:
        await hub.leave(mission_id, ws)


app.include_router(api)
app.add_middleware(
    CORSMiddleware,
    allow_credentials=False if CORS_ORIGINS == ["*"] else True,
    allow_origins=CORS_ORIGINS,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.on_event("shutdown")
async def shutdown() -> None:
    store.close()
