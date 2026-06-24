from fastapi.testclient import TestClient

import local_node_server


def test_status_and_teach(tmp_path, monkeypatch):
    monkeypatch.setattr(local_node_server, "DATA_DIR", tmp_path)
    client = TestClient(local_node_server.app)
    status = client.get("/status")
    assert status.status_code == 200
    assert status.json()["paid_ai_required"] is False
    assert status.json()["core_version"] != "unknown"
    assert status.json()["core_dir"].endswith("core")

    taught = client.post(
        "/teach",
        json={
            "skill_name": "demo_skill",
            "trigger_phrases": ["demo"],
            "steps": ["open app", "tap button"],
            "safety_level": "low",
        },
    )
    assert taught.status_code == 200
    assert taught.json()["delta"]["status"] == "local_candidate"
    logs = client.get("/logs").json()
    assert len(logs["local_deltas"]) == 1


def test_update_check_compare_only(tmp_path, monkeypatch):
    monkeypatch.setattr(local_node_server, "DATA_DIR", tmp_path)
    client = TestClient(local_node_server.app)
    response = client.get("/update-check")
    assert response.status_code == 200
    assert response.json()["mode"] == "compare_only_no_apply"
