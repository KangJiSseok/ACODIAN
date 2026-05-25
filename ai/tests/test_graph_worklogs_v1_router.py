from fastapi.testclient import TestClient

from app.graph.v1.model.worklog_index import WorklogGraphIndexItem, WorklogGraphIndexResponse
from app.graph.v1.store.graph_rag_store import GraphRagConfigurationError
from app.main import app

client = TestClient(app)


def test_graph_worklogs_v1_index_contract(monkeypatch) -> None:
    from app.graph.v1.router import worklog_index

    async def fake_index_worklogs(worklog_ids: list[int]) -> WorklogGraphIndexResponse:
        assert worklog_ids == [101, 102]
        return WorklogGraphIndexResponse(
            items=[
                WorklogGraphIndexItem(worklogId=101, indexed=True),
                WorklogGraphIndexItem(worklogId=102, indexed=False, error="WORKLOG_NOT_FOUND"),
            ]
        )

    monkeypatch.setattr(worklog_index.worklog_index_service, "index_worklogs", fake_index_worklogs)

    response = client.post("/ai/graph/worklogs-v1/index", json={"worklogIds": [101, 102]})

    assert response.status_code == 200
    assert response.json() == {
        "items": [
            {"worklogId": 101, "indexed": True, "error": None},
            {"worklogId": 102, "indexed": False, "error": "WORKLOG_NOT_FOUND"},
        ]
    }


def test_graph_worklogs_v1_index_rejects_snake_case_and_extra_fields() -> None:
    assert client.post("/ai/graph/worklogs-v1/index", json={"worklog_ids": [101]}).status_code == 422
    assert (
        client.post(
            "/ai/graph/worklogs-v1/index",
            json={"worklogIds": [101], "keyword": "x"},
        ).status_code
        == 422
    )


def test_graph_worklogs_v1_index_rejects_non_positive_and_default_batch_limit() -> None:
    assert client.post("/ai/graph/worklogs-v1/index", json={"worklogIds": [0]}).status_code == 422
    assert (
        client.post(
            "/ai/graph/worklogs-v1/index",
            json={"worklogIds": list(range(1, 12))},
        ).status_code
        == 422
    )


def test_graph_worklogs_v1_index_rejects_batch_size_over_configured_limit(monkeypatch) -> None:
    from app.graph.v1.router import worklog_index

    monkeypatch.setattr(worklog_index.settings, "graphrag_index_max_batch_size", 1)

    response = client.post("/ai/graph/worklogs-v1/index", json={"worklogIds": [101, 102]})

    assert response.status_code == 422
    assert response.json() == {"detail": "worklogIds exceeds GRAPHRAG_INDEX_MAX_BATCH_SIZE"}


def test_graph_worklogs_v1_index_maps_configuration_error_to_500(monkeypatch) -> None:
    from app.graph.v1.router import worklog_index

    async def fake_index_worklogs(worklog_ids: list[int]) -> WorklogGraphIndexResponse:
        raise GraphRagConfigurationError("missing config")

    monkeypatch.setattr(worklog_index.worklog_index_service, "index_worklogs", fake_index_worklogs)

    response = client.post("/ai/graph/worklogs-v1/index", json={"worklogIds": [101]})

    assert response.status_code == 500
    assert response.json() == {"detail": "GRAPHRAG_CONFIGURATION_ERROR"}
