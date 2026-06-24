"""Local regression tests for the independent SWRLZ-CORE backend.

These tests never contact Emergent or OpenAI. Mentor calls are stubbed so storage,
streaming, planning, and WebSocket behavior can be verified offline.
"""
from __future__ import annotations

import json
import os
import sys
import tempfile
import uuid
from pathlib import Path

import pytest
from fastapi.testclient import TestClient

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

_TEST_DIR = tempfile.TemporaryDirectory(prefix="swurlz-backend-test-")
os.environ["SWRLZ_SQLITE_PATH"] = str(Path(_TEST_DIR.name) / "test.sqlite3")
os.environ.pop("OPENAI_API_KEY", None)
os.environ.pop("SWRLZ_API_TOKEN", None)

import server  # noqa: E402  (environment must be configured before import)


@pytest.fixture(scope="session")
def client():
    with TestClient(server.app) as test_client:
        yield test_client


@pytest.fixture()
def fake_mentor(monkeypatch):
    async def _fake(system, input_data):
        if "strict JSON" in system or "action planner" in system:
            return json.dumps(
                {
                    "rationale": "The Settings node is visible and is the safest next action.",
                    "needs_approval": False,
                    "next_observation_hint": "The Settings screen title appears.",
                    "actions": [
                        {
                            "type": "tap",
                            "node_id": "n1",
                            "reason": "Open Settings",
                            "risk": "green",
                            "confidence": 0.94,
                            "expected": "Settings opens",
                        }
                    ],
                }
            )
        if "distill" in system.lower():
            return json.dumps(
                {
                    "name": "open_settings",
                    "intent": "Open Android Settings",
                    "triggers": ["open settings"],
                    "risk": "green",
                    "confidence": 0.91,
                    "package_hints": ["com.android.settings"],
                    "plan": [{"type": "tap", "node_id": "n1"}],
                }
            )
        return "[COUNCIL] [OPERATOR] Local state ready. [MENTOR] The supervised route is safe."

    monkeypatch.setattr(server, "call_mentor", _fake)


def create_mission(client: TestClient, goal: str = "Open Settings") -> dict:
    response = client.post(
        "/api/missions",
        json={"goal": goal, "autonomy": "guarded", "package": "com.android.settings"},
    )
    assert response.status_code == 200, response.text
    return response.json()


def test_health_is_local_and_independent(client):
    response = client.get("/api/health")
    assert response.status_code == 200
    data = response.json()
    assert data["ok"] is True
    assert data["storage"] == "sqlite"
    assert data["service"] == "swurlz-mentor"
    assert data["mentor_provider"] == "not-configured"


def test_missions_events_and_state(client):
    mission = create_mission(client)
    mission_id = mission["id"]

    listed = client.get("/api/missions").json()
    assert any(item["id"] == mission_id for item in listed)
    assert client.get(f"/api/missions/{mission_id}").json()["state"] == "drafted"

    changed = client.post(f"/api/missions/{mission_id}/state", json={"state": "paused", "step": 2})
    assert changed.status_code == 200
    assert client.get(f"/api/missions/{mission_id}").json()["state"] == "paused"

    for index in range(3):
        logged = client.post(
            f"/api/missions/{mission_id}/events",
            json={
                "phase": "success",
                "action": {"type": "tap", "node_id": f"n{index}", "risk": "green"},
                "observed": f"screen {index}",
            },
        )
        assert logged.status_code == 200, logged.text

    events = client.get(f"/api/missions/{mission_id}/events").json()
    assert [event["seq"] for event in events] == sorted(event["seq"] for event in events)
    assert len(events) == 3
    assert client.get("/api/missions/not-real").status_code == 404


def test_skills_crud_without_ai_key(client):
    payload = {
        "name": "open_settings",
        "intent": "Open the system settings",
        "triggers": ["open settings"],
        "plan": [{"type": "tap", "node_id": "settings"}],
        "risk": "green",
        "confidence": 0.9,
        "package_hints": ["com.android.settings"],
        "source": "test",
        "version": 1,
    }
    created = client.post("/api/skills", json=payload)
    assert created.status_code == 200, created.text
    skill = created.json()

    assert any(row["id"] == skill["id"] for row in client.get("/api/skills").json())
    patched = client.patch(f"/api/skills/{skill['id']}", json={"version": 2, "confidence": 0.95})
    assert patched.status_code == 200
    assert patched.json()["version"] == 2
    assert client.delete(f"/api/skills/{skill['id']}").json()["deleted"] == 1


def test_planner_with_stubbed_mentor(client, fake_mentor):
    mission = create_mission(client, "Open Android Settings")
    response = client.post(
        "/api/plan",
        json={
            "mission_id": mission["id"],
            "goal": mission["goal"],
            "package": "com.android.launcher",
            "autonomy": "guarded",
            "screen_summary": "Home screen",
            "nodes": [{"id": "n1", "cls": "TextView", "text": "Settings", "clickable": True}],
            "history": [],
        },
    )
    assert response.status_code == 200, response.text
    plan = response.json()
    assert plan["actions"][0]["node_id"] == "n1"
    assert plan["needs_approval"] is False


def test_unconfigured_mentor_is_graceful(client):
    mission = create_mission(client, "Need online planning")
    response = client.post(
        "/api/plan",
        json={"mission_id": mission["id"], "goal": mission["goal"], "nodes": []},
    )
    assert response.status_code == 503
    assert "not configured" in response.json()["detail"].lower()
    # Storage remains healthy after a missing AI key.
    assert client.get("/api/skills").status_code == 200


def test_council_sse_and_history(client, fake_mentor):
    session_id = f"test-{uuid.uuid4().hex[:8]}"
    response = client.post(
        "/api/council/chat",
        json={"session_id": session_id, "text": "Status?", "persona": "council"},
    )
    assert response.status_code == 200
    assert "data:" in response.text
    assert '"done": true' in response.text.lower()
    history = client.get(f"/api/council/history/{session_id}").json()
    assert [row["role"] for row in history] == ["user", "assistant"]


def test_skill_suggestion(client, fake_mentor):
    empty = create_mission(client, "No events")
    no_events = client.post("/api/skills/suggest", json={"mission_id": empty["id"]})
    assert no_events.status_code == 200
    assert no_events.json()["ok"] is False

    mission = create_mission(client, "Open Settings")
    client.post(
        f"/api/missions/{mission['id']}/events",
        json={
            "phase": "success",
            "action": {"type": "tap", "node_id": "n1", "risk": "green"},
            "observed": "Settings opened",
        },
    )
    suggested = client.post("/api/skills/suggest", json={"mission_id": mission["id"]})
    assert suggested.status_code == 200, suggested.text
    assert suggested.json()["suggestion"]["name"] == "open_settings"


def test_websocket_history_and_ping(client):
    mission = create_mission(client, "WebSocket test")
    client.post(
        f"/api/missions/{mission['id']}/events",
        json={"phase": "planned", "action": {"type": "wait", "ms": 500}},
    )
    with client.websocket_connect(f"/api/missions/{mission['id']}/stream") as ws:
        history = ws.receive_json()
        assert history["kind"] == "history"
        assert len(history["events"]) == 1
        ws.send_text("ping")
        assert ws.receive_json()["kind"] == "pong"
