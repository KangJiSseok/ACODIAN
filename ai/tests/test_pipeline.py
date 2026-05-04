from fastapi.testclient import TestClient

from app.main import app

client = TestClient(app)


class DummyTask:
    def __init__(self, task_id: str) -> None:
        self.id = task_id


def test_trigger_worklog_pipeline(monkeypatch) -> None:
    from app.service import pipeline_service

    def fake_summary_delay(
        worklog_id: int,
        request_content: str | None,
        work_content: str,
    ) -> DummyTask:
        assert worklog_id == 1
        assert request_content == "재고 동기화 개선 요청"
        assert work_content == "배치 병렬 처리 구조를 적용했다."
        return DummyTask("summary-task-id")

    def fake_tagging_delay(
        worklog_id: int,
        work_content: str,
    ) -> DummyTask:
        assert worklog_id == 1
        assert work_content == "배치 병렬 처리 구조를 적용했다."
        return DummyTask("tagging-task-id")

    monkeypatch.setattr(
        pipeline_service.generate_worklog_summary,
        "delay",
        fake_summary_delay,
    )
    monkeypatch.setattr(
        pipeline_service.generate_worklog_tags,
        "delay",
        fake_tagging_delay,
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
        "summaryTaskId": "summary-task-id",
        "taggingTaskId": "tagging-task-id",
        "status": "ACCEPTED",
    }
