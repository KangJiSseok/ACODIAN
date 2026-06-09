"""GraphRAG v1 업무일지 index orchestration service."""

from dataclasses import dataclass

from app.graph.v1.model.worklog_index import WorklogGraphIndexItem, WorklogGraphIndexResponse
from app.graph.v1.service.graph_rag_adapter import get_graph_rag_worklog_adapter
from app.graph.v1.service.worklog_graph_builder import WorklogGraphDocument, build_worklog_graph_document
from app.graph.v1.service.worklog_source_reader import WorklogGraphSourceRowReader
from app.graph.v1.store.graph_rag_store import GraphRagIndexFailedError, GraphRagIndexTimeoutError

ERROR_WORKLOG_NOT_FOUND = "WORKLOG_NOT_FOUND"
ERROR_GRAPHRAG_INDEX_FAILED = "GRAPHRAG_INDEX_FAILED"
ERROR_GRAPHRAG_INDEX_TIMEOUT = "GRAPHRAG_INDEX_TIMEOUT"


@dataclass(frozen=True)
class PreparedGraphWorklogDocuments:
    """GraphRAG upsert 직전의 업무일지 graph document 준비 결과."""

    documents: list[WorklogGraphDocument]
    found_worklog_ids: list[int]
    missing_worklog_ids: list[int]


class GraphWorklogIndexService:
    """업무일지 GraphRAG index 유스케이스 진입점."""

    async def prepare_documents(self, worklog_ids: list[int]) -> PreparedGraphWorklogDocuments:
        """업무일지 ID 목록을 graph document와 missing ID 목록으로 나눈다."""
        source_rows_by_id = await WorklogGraphSourceRowReader().fetch_source_rows(worklog_ids)
        documents: list[WorklogGraphDocument] = []
        found_worklog_ids: list[int] = []
        missing_worklog_ids: list[int] = []

        for worklog_id in worklog_ids:
            source_row = source_rows_by_id.get(worklog_id)
            if source_row is None:
                missing_worklog_ids.append(worklog_id)
                continue
            found_worklog_ids.append(worklog_id)
            documents.append(build_worklog_graph_document(source_row))

        return PreparedGraphWorklogDocuments(
            documents=documents,
            found_worklog_ids=found_worklog_ids,
            missing_worklog_ids=missing_worklog_ids,
        )

    async def index_worklogs(self, worklog_ids: list[int]) -> WorklogGraphIndexResponse:
        """요청된 업무일지 ID 순서대로 GraphRAG index 결과를 반환한다."""
        prepared = await self.prepare_documents(worklog_ids)
        failed_found_ids = await self._index_found_documents(prepared)

        missing_worklog_ids = set(prepared.missing_worklog_ids)
        indexed_worklog_ids = set(prepared.found_worklog_ids) - set(failed_found_ids)

        return WorklogGraphIndexResponse(
            items=[
                self._build_response_item(
                    worklog_id=worklog_id,
                    missing_worklog_ids=missing_worklog_ids,
                    indexed_worklog_ids=indexed_worklog_ids,
                    failed_found_ids=failed_found_ids,
                )
                for worklog_id in worklog_ids
            ]
        )

    async def _index_found_documents(
        self,
        prepared: PreparedGraphWorklogDocuments,
    ) -> dict[int, str]:
        """조회된 업무일지만 adapter에 전달하고 실패를 ID별 오류로 변환한다."""
        if not prepared.documents:
            return {}

        adapter = get_graph_rag_worklog_adapter()
        failed_found_ids: dict[int, str] = {}
        for document in prepared.documents:
            try:
                await adapter.index_document(document)
            except GraphRagIndexTimeoutError:
                failed_found_ids[document.worklog_id] = ERROR_GRAPHRAG_INDEX_TIMEOUT
            except GraphRagIndexFailedError:
                failed_found_ids[document.worklog_id] = ERROR_GRAPHRAG_INDEX_FAILED
        return failed_found_ids

    def _build_response_item(
        self,
        *,
        worklog_id: int,
        missing_worklog_ids: set[int],
        indexed_worklog_ids: set[int],
        failed_found_ids: dict[int, str],
    ) -> WorklogGraphIndexItem:
        """업무일지 ID 하나의 GraphRAG index 응답 item을 만든다."""
        if worklog_id in missing_worklog_ids:
            return WorklogGraphIndexItem(worklogId=worklog_id, indexed=False, error=ERROR_WORKLOG_NOT_FOUND)
        if worklog_id in indexed_worklog_ids:
            return WorklogGraphIndexItem(worklogId=worklog_id, indexed=True)
        return WorklogGraphIndexItem(
            worklogId=worklog_id,
            indexed=False,
            error=failed_found_ids[worklog_id],
        )
