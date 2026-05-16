from fastapi.testclient import TestClient

import app.main as main


def test_app_lifespan_closes_lightrag_adapter(monkeypatch) -> None:
    close_calls: list[str] = []

    async def fake_close_lightrag_worklog_index_adapter() -> None:
        close_calls.append("closed")

    monkeypatch.setattr(
        main,
        "close_lightrag_worklog_index_adapter",
        fake_close_lightrag_worklog_index_adapter,
    )

    with TestClient(main.create_app()) as client:
        response = client.get("/ai/health")

    assert response.status_code == 200
    assert close_calls == ["closed"]
