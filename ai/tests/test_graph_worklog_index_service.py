import asyncio

from app.graph.v1.service import worklog_index_service as service_module
from app.graph.v1.service.worklog_index_service import GraphWorklogIndexService
from app.graph.v1.store.graph_rag_store import GraphRagIndexFailedError, GraphRagIndexTimeoutError
from app.graph.v1.store.worklog_source_store import GraphWorklogPredecessor, GraphWorklogSourceRow, GraphWorklogTag


def make_source_row(worklog_id: int) -> GraphWorklogSourceRow:
    return GraphWorklogSourceRow(
        worklog_id=worklog_id,
        title=f"업무 {worklog_id}",
        request_content="요청",
        work_content="수행",
        author_id=5,
        author_name="김도윤",
        team_id=7,
        team_name="정산 고도화 TF",
        tags=[GraphWorklogTag(tag_id=11, tag_name="정산", description=None)],
        direct_predecessors=[GraphWorklogPredecessor(worklog_id=88, title="직접 선행")],
    )


def test_index_service_preserves_order_and_indexes_found_documents(monkeypatch) -> None:
    class _Reader:
        async def fetch_source_rows(self, worklog_ids: list[int]):
            assert worklog_ids == [101, 102]
            return {101: make_source_row(101), 102: make_source_row(102)}

    class _Adapter:
        def __init__(self) -> None:
            self.document_ids: list[int] = []

        async def index_document(self, document) -> None:
            self.document_ids.append(document.worklog_id)

    async def run_case() -> None:
        adapter = _Adapter()
        monkeypatch.setattr(service_module, "WorklogGraphSourceRowReader", _Reader)
        monkeypatch.setattr(service_module, "get_graph_rag_worklog_adapter", lambda: adapter)

        response = await GraphWorklogIndexService().index_worklogs([101, 102])

        assert adapter.document_ids == [101, 102]
        assert [item.model_dump(by_alias=True) for item in response.items] == [
            {"worklogId": 101, "indexed": True, "error": None},
            {"worklogId": 102, "indexed": True, "error": None},
        ]

    asyncio.run(run_case())


def test_index_service_maps_missing_and_per_item_failures(monkeypatch) -> None:
    class _Reader:
        async def fetch_source_rows(self, worklog_ids: list[int]):
            return {101: make_source_row(101), 103: make_source_row(103), 104: make_source_row(104)}

    class _Adapter:
        async def index_document(self, document) -> None:
            if document.worklog_id == 101:
                raise GraphRagIndexTimeoutError("timeout")
            if document.worklog_id == 103:
                raise GraphRagIndexFailedError("failed")

    async def run_case() -> None:
        monkeypatch.setattr(service_module, "WorklogGraphSourceRowReader", _Reader)
        monkeypatch.setattr(service_module, "get_graph_rag_worklog_adapter", lambda: _Adapter())

        response = await GraphWorklogIndexService().index_worklogs([101, 102, 103, 104])

        assert [item.model_dump(by_alias=True) for item in response.items] == [
            {"worklogId": 101, "indexed": False, "error": "GRAPHRAG_INDEX_TIMEOUT"},
            {"worklogId": 102, "indexed": False, "error": "WORKLOG_NOT_FOUND"},
            {"worklogId": 103, "indexed": False, "error": "GRAPHRAG_INDEX_FAILED"},
            {"worklogId": 104, "indexed": True, "error": None},
        ]

    asyncio.run(run_case())
