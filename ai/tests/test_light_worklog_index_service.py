import asyncio

from app.light.v3.service import worklog_index_service as service_module
from app.light.v3.service.lightrag_adapter import (
    LightRagInsertFailedError,
    LightRagInsertTimeoutError,
)
from app.light.v3.service.worklog_index_service import LightWorklogIndexService
from app.light.v3.store.worklog_source_store import (
    LightWorklogPredecessor,
    LightWorklogSourceRow,
    LightWorklogTag,
)


def make_source_row(worklog_id: int) -> LightWorklogSourceRow:
    return LightWorklogSourceRow(
        worklog_id=worklog_id,
        title=f"업무 {worklog_id}",
        request_content="요청",
        work_content="수행",
        author_id=5,
        author_name="김도윤",
        team_id=7,
        team_name="정산 고도화 TF",
        tags=[LightWorklogTag(tag_id=11, tag_name="정산", description=None)],
        direct_predecessors=[LightWorklogPredecessor(worklog_id=88, title="직접 선행")],
    )


def test_index_service_calls_text_and_custom_kg_from_one_source_fetch(monkeypatch) -> None:
    class _Reader:
        async def fetch_source_rows(self, worklog_ids: list[int]):
            assert worklog_ids == [101, 102]
            return {101: make_source_row(101), 102: make_source_row(102)}

    class _Adapter:
        def __init__(self) -> None:
            self.text_document_ids: list[str] = []
            self.custom_document_ids: list[str] = []

        async def index_documents(self, documents) -> None:
            self.text_document_ids = [document.document_id for document in documents]

        async def index_custom_kg_document(self, document) -> None:
            self.custom_document_ids.append(document.document_id)

    async def run_case() -> None:
        adapter = _Adapter()
        monkeypatch.setattr(service_module, "WorklogLightSourceReader", _Reader)
        monkeypatch.setattr(service_module, "get_lightrag_worklog_index_adapter", lambda: adapter)

        response = await LightWorklogIndexService().index_worklogs([101, 102])

        assert adapter.text_document_ids == ["worklog-101", "worklog-102"]
        assert adapter.custom_document_ids == ["worklog-101", "worklog-102"]
        assert [item.model_dump(by_alias=True) for item in response.items] == [
            {"worklogId": 101, "indexed": True, "error": None},
            {"worklogId": 102, "indexed": True, "error": None},
        ]

    asyncio.run(run_case())


def test_index_service_preserves_missing_and_custom_failure_per_item(monkeypatch) -> None:
    class _Reader:
        async def fetch_source_rows(self, worklog_ids: list[int]):
            return {101: make_source_row(101), 103: make_source_row(103)}

    class _Adapter:
        async def index_documents(self, documents) -> None:
            assert [document.document_id for document in documents] == ["worklog-101", "worklog-103"]

        async def index_custom_kg_document(self, document) -> None:
            if document.document_id == "worklog-101":
                raise LightRagInsertTimeoutError("timeout")
            if document.document_id == "worklog-103":
                raise LightRagInsertFailedError("failed")

    async def run_case() -> None:
        monkeypatch.setattr(service_module, "WorklogLightSourceReader", _Reader)
        monkeypatch.setattr(service_module, "get_lightrag_worklog_index_adapter", lambda: _Adapter())

        response = await LightWorklogIndexService().index_worklogs([101, 102, 103])

        assert [item.model_dump(by_alias=True) for item in response.items] == [
            {"worklogId": 101, "indexed": False, "error": "LIGHTRAG_INSERT_TIMEOUT"},
            {"worklogId": 102, "indexed": False, "error": "WORKLOG_NOT_FOUND"},
            {"worklogId": 103, "indexed": False, "error": "LIGHTRAG_INSERT_FAILED"},
        ]

    asyncio.run(run_case())


def test_index_service_maps_text_batch_failure_to_all_found_ids(monkeypatch) -> None:
    class _Reader:
        async def fetch_source_rows(self, worklog_ids: list[int]):
            return {101: make_source_row(101), 103: make_source_row(103)}

    class _Adapter:
        def __init__(self) -> None:
            self.custom_calls = 0

        async def index_documents(self, documents) -> None:
            raise LightRagInsertFailedError("failed")

        async def index_custom_kg_document(self, document) -> None:
            self.custom_calls += 1

    async def run_case() -> None:
        adapter = _Adapter()
        monkeypatch.setattr(service_module, "WorklogLightSourceReader", _Reader)
        monkeypatch.setattr(service_module, "get_lightrag_worklog_index_adapter", lambda: adapter)

        response = await LightWorklogIndexService().index_worklogs([101, 102, 103])

        assert adapter.custom_calls == 0
        assert [item.model_dump(by_alias=True) for item in response.items] == [
            {"worklogId": 101, "indexed": False, "error": "LIGHTRAG_INSERT_FAILED"},
            {"worklogId": 102, "indexed": False, "error": "WORKLOG_NOT_FOUND"},
            {"worklogId": 103, "indexed": False, "error": "LIGHTRAG_INSERT_FAILED"},
        ]

    asyncio.run(run_case())
