"""LightRAG custom KG 업무일지 index orchestration service 경계."""

from dataclasses import dataclass

from app.light.v3.model.worklog_custom_kg_index import (
    WorklogCustomKgIndexItem,
    WorklogCustomKgIndexResponse,
)
from app.light.v3.service.lightrag_adapter import (
    LightRagConfigurationError,
    LightRagInsertFailedError,
    LightRagInsertTimeoutError,
    get_lightrag_worklog_index_adapter,
)
from app.light.v3.service.worklog_custom_kg_builder import (
    LightRagWorklogCustomKgDocument,
    build_worklog_custom_kg_document,
)
from app.light.v3.service.worklog_custom_kg_source_reader import WorklogCustomKgSourceReader

ERROR_WORKLOG_NOT_FOUND = "WORKLOG_NOT_FOUND"
ERROR_LIGHTRAG_INSERT_TIMEOUT = "LIGHTRAG_INSERT_TIMEOUT"
ERROR_LIGHTRAG_INSERT_FAILED = "LIGHTRAG_INSERT_FAILED"


@dataclass(frozen=True)
class PreparedLightWorklogCustomKgDocuments:
    """LightRAG custom KG insert 직전의 업무일지 문서 준비 결과."""

    documents: list[LightRagWorklogCustomKgDocument]
    found_worklog_ids: list[int]
    missing_worklog_ids: list[int]


class WorklogCustomKgIndexService:
    """업무일지 confirmed relation custom KG index 유스케이스 진입점.

    통합 source reader를 재사용해 custom KG document만 LightRAG에 insert하고
    요청 하나의 동기 index 결과를 ID별로 조립한다.
    """

    async def prepare_documents(
        self,
        worklog_ids: list[int],
    ) -> PreparedLightWorklogCustomKgDocuments:
        """업무일지 ID 목록을 LightRAG custom KG document 목록과 missing ID 목록으로 나눈다."""
        sources_by_id = await WorklogCustomKgSourceReader().fetch_sources(worklog_ids)
        documents: list[LightRagWorklogCustomKgDocument] = []
        found_worklog_ids: list[int] = []
        missing_worklog_ids: list[int] = []

        for worklog_id in worklog_ids:
            source = sources_by_id.get(worklog_id)
            if source is None:
                missing_worklog_ids.append(worklog_id)
                continue
            found_worklog_ids.append(worklog_id)
            documents.append(build_worklog_custom_kg_document(source))

        return PreparedLightWorklogCustomKgDocuments(
            documents=documents,
            found_worklog_ids=found_worklog_ids,
            missing_worklog_ids=missing_worklog_ids,
        )

    async def index_worklogs(self, worklog_ids: list[int]) -> WorklogCustomKgIndexResponse:
        """요청된 업무일지 ID 순서대로 custom KG index 결과를 반환한다."""
        prepared = await self.prepare_documents(worklog_ids)
        failed_found_ids = await self._index_found_documents(prepared)

        missing_worklog_ids = set(prepared.missing_worklog_ids)
        indexed_worklog_ids = set(prepared.found_worklog_ids) - set(failed_found_ids)

        return WorklogCustomKgIndexResponse(
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
        prepared: PreparedLightWorklogCustomKgDocuments,
    ) -> dict[int, str]:
        """조회된 업무일지만 adapter에 전달하고 document별 실패를 ID별 오류로 변환한다."""
        if not prepared.documents:
            return {}

        failed_found_ids: dict[int, str] = {}
        adapter = get_lightrag_worklog_index_adapter()
        for worklog_id, document in zip(
            prepared.found_worklog_ids,
            prepared.documents,
            strict=True,
        ):
            try:
                await adapter.index_custom_kg_document(document)
            except LightRagConfigurationError:
                raise
            except LightRagInsertTimeoutError:
                failed_found_ids[worklog_id] = ERROR_LIGHTRAG_INSERT_TIMEOUT
            except LightRagInsertFailedError:
                failed_found_ids[worklog_id] = ERROR_LIGHTRAG_INSERT_FAILED
        return failed_found_ids

    def _build_response_item(
        self,
        *,
        worklog_id: int,
        missing_worklog_ids: set[int],
        indexed_worklog_ids: set[int],
        failed_found_ids: dict[int, str],
    ) -> WorklogCustomKgIndexItem:
        """업무일지 ID 하나의 custom KG index 응답 item을 만든다."""
        if worklog_id in missing_worklog_ids:
            return WorklogCustomKgIndexItem(
                worklogId=worklog_id,
                indexed=False,
                error=ERROR_WORKLOG_NOT_FOUND,
            )
        if worklog_id in indexed_worklog_ids:
            return WorklogCustomKgIndexItem(worklogId=worklog_id, indexed=True)
        return WorklogCustomKgIndexItem(
            worklogId=worklog_id,
            indexed=False,
            error=failed_found_ids[worklog_id],
        )
