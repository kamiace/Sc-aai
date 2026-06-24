import importlib
import os
import tempfile
import sys
from pathlib import Path

from fastapi.testclient import TestClient

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))


def load_server():
    tmp = tempfile.TemporaryDirectory(prefix="swurlz-test-")
    os.environ["SWRLZ_SQLITE_PATH"] = str(Path(tmp.name) / "test.sqlite3")
    os.environ.pop("OPENAI_API_KEY", None)
    import server
    importlib.reload(server)
    return tmp, server


def test_health_and_local_storage():
    tmp, server = load_server()
    with TestClient(server.app) as client:
        health = client.get("/api/health")
        assert health.status_code == 200
        assert health.json()["ok"] is True
        mission = client.post("/api/missions", json={"goal": "Open Settings", "autonomy": "guarded"})
        assert mission.status_code == 200
        skill = client.post("/api/skills", json={"name": "test", "intent": "test skill"})
        assert skill.status_code == 200
        assert len(client.get("/api/skills").json()) == 1
    tmp.cleanup()


def test_missing_online_mentor_is_graceful():
    tmp, server = load_server()
    with TestClient(server.app) as client:
        mission = client.post("/api/missions", json={"goal": "Open Settings"}).json()
        response = client.post("/api/plan", json={
            "mission_id": mission["id"],
            "goal": "Open Settings",
            "nodes": [],
            "device_context": {"model": "test"},
            "context_level": "STANDARD",
        })
        assert response.status_code == 503
        assert "not configured" in response.json()["detail"].lower()
    tmp.cleanup()
