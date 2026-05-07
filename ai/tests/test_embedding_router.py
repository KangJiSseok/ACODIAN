from fastapi.testclient import TestClient

from app.main import app

client = TestClient(app)


class DummyTask:
    def __init__(self, task_id: str) -> None:
        self.id = task_id


def test_trigger_worklog_embedding(monkeypatch) -> None:
    from app.router import embedding

    def fake_delay(worklog_id: int) -> DummyTask:
        assert worklog_id == 10
        return DummyTask("embedding-task-id")

    monkeypatch.setattr(embedding.generate_worklog_embedding, "delay", fake_delay)

    response = client.post("/ai/embedding/worklog", json={"worklogId": 10})

    assert response.status_code == 202
    assert response.json() == {
        "worklogId": 10,
        "embeddingTaskId": "embedding-task-id",
        "status": "ACCEPTED",
    }
