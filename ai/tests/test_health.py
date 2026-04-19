from fastapi.testclient import TestClient

from app.main import app

client = TestClient(app)


def test_health_check() -> None:
    response = client.get("/api/v1/health")

    assert response.status_code == 200
    assert response.json() == {"status": "ok"}


def test_pipeline_endpoint_is_not_exposed() -> None:
    response = client.post(
        "/api/v1/pipeline/worklogs",
        json={
            "worklog_id": 1,
            "request_content": "stub",
            "work_content": "stub",
        },
    )

    assert response.status_code == 404


def test_search_endpoint_is_not_exposed() -> None:
    response = client.post(
        "/api/v1/search/semantic",
        json={
            "query": "stub",
            "limit": 5,
        },
    )

    assert response.status_code == 404
