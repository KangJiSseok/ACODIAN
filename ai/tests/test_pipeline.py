from fastapi.testclient import TestClient

from app.main import app

client = TestClient(app)


class DummyTask:
    id = "dummy-task-id"


def test_trigger_worklog_pipeline(monkeypatch) -> None:
    from app.service import pipeline_service

    def fake_delay(
        worklog_id: int,
        request_content: str | None,
        work_content: str,
    ) -> DummyTask:
        assert worklog_id == 1
        assert request_content == "재고 동기화 개선 요청"
        assert work_content == "배치 병렬 처리 구조를 적용했다."
        return DummyTask()

    monkeypatch.setattr(
        pipeline_service.run_worklog_pipeline,
        "delay",
        fake_delay,
    )

    response = client.post(
        "/ai/pipeline/worklog",
        json={
            "worklogId": 1,
            "requestContent": "재고 동기화 개선 요청",
            "workContent": "배치 병렬 처리 구조를 적용했다.",
            "authorId": 1,
            "teamId": 1,
            "departmentId": 1,
        },
    )

    assert response.status_code == 202
    assert response.json() == {
        "worklogId": 1,
        "taskId": "dummy-task-id",
        "status": "ACCEPTED",
    }
