from fastapi.testclient import TestClient

from app.light.v3.model.worklog_index import (
    WorklogLightIndexItem,
    WorklogLightIndexResponse,
)
from app.light.v3.service.lightrag_adapter import LightRagConfigurationError
from app.main import app

client = TestClient(app)


def test_light_worklogs_v3_index_contract(monkeypatch) -> None:
    from app.light.v3.router import worklog_index

    async def fake_index_worklogs(worklog_ids: list[int]) -> WorklogLightIndexResponse:
        assert worklog_ids == [101, 102]
        return WorklogLightIndexResponse(
            items=[
                WorklogLightIndexItem(worklogId=101, indexed=True),
                WorklogLightIndexItem(worklogId=102, indexed=False, error="WORKLOG_NOT_FOUND"),
            ]
        )

    monkeypatch.setattr(
        worklog_index.worklog_index_service,
        "index_worklogs",
        fake_index_worklogs,
    )

    response = client.post(
        "/ai/light/worklogs-v3/index",
        json={"worklogIds": [101, 102]},
    )

    assert response.status_code == 200
    assert response.json() == {
        "items": [
            {"worklogId": 101, "indexed": True, "error": None},
            {"worklogId": 102, "indexed": False, "error": "WORKLOG_NOT_FOUND"},
        ]
    }


def test_light_worklogs_v3_index_allows_single_item_list(monkeypatch) -> None:
    from app.light.v3.router import worklog_index

    async def fake_index_worklogs(worklog_ids: list[int]) -> WorklogLightIndexResponse:
        assert worklog_ids == [101]
        return WorklogLightIndexResponse(
            items=[WorklogLightIndexItem(worklogId=101, indexed=True)]
        )

    monkeypatch.setattr(
        worklog_index.worklog_index_service,
        "index_worklogs",
        fake_index_worklogs,
    )

    response = client.post(
        "/ai/light/worklogs-v3/index",
        json={"worklogIds": [101]},
    )

    assert response.status_code == 200
    assert response.json() == {
        "items": [{"worklogId": 101, "indexed": True, "error": None}]
    }


def test_light_worklogs_v3_index_rejects_singular_field_name() -> None:
    response = client.post(
        "/ai/light/worklogs-v3/index",
        json={"worklogId": 101},
    )

    assert response.status_code == 422


def test_light_worklogs_v3_index_rejects_snake_case_field_name() -> None:
    response = client.post(
        "/ai/light/worklogs-v3/index",
        json={"worklog_ids": [101]},
    )

    assert response.status_code == 422


def test_light_worklogs_v3_index_rejects_extra_fields() -> None:
    response = client.post(
        "/ai/light/worklogs-v3/index",
        json={"worklogIds": [101], "keyword": "검색어"},
    )

    assert response.status_code == 422


def test_light_worklogs_v3_index_rejects_non_positive_worklog_id() -> None:
    response = client.post(
        "/ai/light/worklogs-v3/index",
        json={"worklogIds": [0]},
    )

    assert response.status_code == 422


def test_light_worklogs_v3_index_rejects_batch_size_over_default_limit() -> None:
    response = client.post(
        "/ai/light/worklogs-v3/index",
        json={"worklogIds": list(range(1, 12))},
    )

    assert response.status_code == 422


def test_light_worklogs_v3_index_rejects_batch_size_over_configured_limit(
    monkeypatch,
) -> None:
    from app.light.v3.router import worklog_index

    monkeypatch.setattr(worklog_index.settings, "lightrag_index_max_batch_size", 1)

    response = client.post(
        "/ai/light/worklogs-v3/index",
        json={"worklogIds": [101, 102]},
    )

    assert response.status_code == 422
    assert response.json() == {
        "detail": "worklogIds exceeds LIGHTRAG_INDEX_MAX_BATCH_SIZE"
    }


def test_light_worklogs_v3_index_allows_duplicate_worklog_ids(monkeypatch) -> None:
    from app.light.v3.router import worklog_index

    async def fake_index_worklogs(worklog_ids: list[int]) -> WorklogLightIndexResponse:
        assert worklog_ids == [101, 101]
        return WorklogLightIndexResponse(
            items=[
                WorklogLightIndexItem(worklogId=101, indexed=True),
                WorklogLightIndexItem(worklogId=101, indexed=True),
            ]
        )

    monkeypatch.setattr(
        worklog_index.worklog_index_service,
        "index_worklogs",
        fake_index_worklogs,
    )

    response = client.post(
        "/ai/light/worklogs-v3/index",
        json={"worklogIds": [101, 101]},
    )

    assert response.status_code == 200
    assert response.json() == {
        "items": [
            {"worklogId": 101, "indexed": True, "error": None},
            {"worklogId": 101, "indexed": True, "error": None},
        ]
    }


def test_light_worklogs_v3_index_maps_configuration_error_to_500(monkeypatch) -> None:
    from app.light.v3.router import worklog_index

    async def fake_index_worklogs(worklog_ids: list[int]) -> WorklogLightIndexResponse:
        raise LightRagConfigurationError("missing api key")

    monkeypatch.setattr(
        worklog_index.worklog_index_service,
        "index_worklogs",
        fake_index_worklogs,
    )

    response = client.post(
        "/ai/light/worklogs-v3/index",
        json={"worklogIds": [101]},
    )

    assert response.status_code == 500
    assert response.json() == {"detail": "LIGHTRAG_CONFIGURATION_ERROR"}


def test_light_worklogs_v3_custom_kg_index_endpoint_removed() -> None:
    response = client.post(
        "/ai/light/worklogs-v3/custom-kg/index",
        json={"worklogIds": [101]},
    )

    assert response.status_code == 404
