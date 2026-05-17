import asyncio

from app.light.v3.service import worklog_custom_kg_index_service as service_module
from app.light.v3.service.lightrag_adapter import (
    LightRagInsertFailedError,
    LightRagInsertTimeoutError,
)
from app.light.v3.service.worklog_custom_kg_index_service import WorklogCustomKgIndexService
from app.light.v3.service.worklog_custom_kg_source_reader import (
    LightRagCustomKgPredecessor,
    LightRagCustomKgSource,
    LightRagCustomKgTag,
)


def make_source(worklog_id: int) -> LightRagCustomKgSource:
    return LightRagCustomKgSource(
        worklog_id=worklog_id,
        title=f"업무 {worklog_id}",
        author_id=5,
        author_name="김도윤",
        team_id=7,
        team_name="정산 고도화 TF",
        tags=[LightRagCustomKgTag(tag_id=11, tag_name="정산", description=None)],
        direct_predecessors=[LightRagCustomKgPredecessor(worklog_id=88, title="직접 선행")],
    )


def test_custom_kg_index_service_calls_adapter_per_worklog_in_request_order(monkeypatch) -> None:
    class _Reader:
        async def fetch_sources(self, worklog_ids: list[int]):
            assert worklog_ids == [101, 102]
            return {101: make_source(101), 102: make_source(102)}

    class _Adapter:
        def __init__(self) -> None:
            self.calls: list[tuple[dict, str]] = []

        async def index_custom_kg_document(self, document) -> None:
            self.calls.append(document)

    async def run_case() -> None:
        adapter = _Adapter()
        monkeypatch.setattr(service_module, "WorklogCustomKgSourceReader", _Reader)
        monkeypatch.setattr(service_module, "get_lightrag_worklog_index_adapter", lambda: adapter)

        response = await WorklogCustomKgIndexService().index_worklogs([101, 102])

        assert [document.document_id for document in adapter.calls] == [
            "worklog-101",
            "worklog-102",
        ]
        assert [item.model_dump(by_alias=True) for item in response.items] == [
            {"worklogId": 101, "indexed": True, "error": None},
            {"worklogId": 102, "indexed": True, "error": None},
        ]

    asyncio.run(run_case())


def test_custom_kg_index_service_preserves_missing_and_failure_per_item(monkeypatch) -> None:
    class _Reader:
        async def fetch_sources(self, worklog_ids: list[int]):
            return {101: make_source(101), 103: make_source(103)}

    class _Adapter:
        async def index_custom_kg_document(self, document) -> None:
            if document.document_id == "worklog-101":
                raise LightRagInsertTimeoutError("timeout")
            if document.document_id == "worklog-103":
                raise LightRagInsertFailedError("failed")

    async def run_case() -> None:
        monkeypatch.setattr(service_module, "WorklogCustomKgSourceReader", _Reader)
        monkeypatch.setattr(service_module, "get_lightrag_worklog_index_adapter", lambda: _Adapter())

        response = await WorklogCustomKgIndexService().index_worklogs([101, 102, 103])

        assert [item.model_dump(by_alias=True) for item in response.items] == [
            {
                "worklogId": 101,
                "indexed": False,
                "error": "LIGHTRAG_INSERT_TIMEOUT",
            },
            {"worklogId": 102, "indexed": False, "error": "WORKLOG_NOT_FOUND"},
            {
                "worklogId": 103,
                "indexed": False,
                "error": "LIGHTRAG_INSERT_FAILED",
            },
        ]

    asyncio.run(run_case())
