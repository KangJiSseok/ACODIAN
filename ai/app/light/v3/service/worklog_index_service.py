"""LightRAG v3 업무일지 통합 index orchestration service 경계."""

from dataclasses import dataclass

from app.light.v3.model.worklog_index import (
    WorklogLightIndexItem,
    WorklogLightIndexResponse,
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
from app.light.v3.service.worklog_document_builder import (
    LightRagWorklogDocument,
    build_worklog_light_document,
)
from app.light.v3.service.worklog_custom_kg_source_reader import (
    to_light_worklog_custom_kg_source,
)
from app.light.v3.service.worklog_source_reader import (
    WorklogLightSourceReader,
    to_light_worklog_source,
)

ERROR_WORKLOG_NOT_FOUND = "WORKLOG_NOT_FOUND"
ERROR_LIGHTRAG_INSERT_TIMEOUT = "LIGHTRAG_INSERT_TIMEOUT"
ERROR_LIGHTRAG_INSERT_FAILED = "LIGHTRAG_INSERT_FAILED"


@dataclass(frozen=True)
class PreparedLightWorklogDocuments:
    """LightRAG insert 직전의 업무일지 text/custom KG 문서 준비 결과."""

    documents: list[LightRagWorklogDocument]
    custom_kg_documents: list[LightRagWorklogCustomKgDocument]
    found_worklog_ids: list[int]
    missing_worklog_ids: list[int]


class LightWorklogIndexService:
    """업무일지 LightRAG 통합 index 유스케이스 진입점.

    한 번의 source reader 호출 결과에서 text document와 confirmed relation custom KG
    document를 함께 만들고, 둘 다 성공한 업무일지만 index 성공으로 응답한다.
    """

    async def prepare_documents(self, worklog_ids: list[int]) -> PreparedLightWorklogDocuments:
        """업무일지 ID 목록을 LightRAG text/custom KG 문서와 missing ID 목록으로 나눈다."""
        source_rows_by_id = await WorklogLightSourceReader().fetch_source_rows(worklog_ids)
        documents: list[LightRagWorklogDocument] = []
        custom_kg_documents: list[LightRagWorklogCustomKgDocument] = []
        found_worklog_ids: list[int] = []
        missing_worklog_ids: list[int] = []

        for worklog_id in worklog_ids:
            source_row = source_rows_by_id.get(worklog_id)
            if source_row is None:
                missing_worklog_ids.append(worklog_id)
                continue
            found_worklog_ids.append(worklog_id)
            documents.append(build_worklog_light_document(to_light_worklog_source(source_row)))
            custom_kg_documents.append(
                build_worklog_custom_kg_document(to_light_worklog_custom_kg_source(source_row))
            )

        return PreparedLightWorklogDocuments(
            documents=documents,
            custom_kg_documents=custom_kg_documents,
            found_worklog_ids=found_worklog_ids,
            missing_worklog_ids=missing_worklog_ids,
        )

    async def index_worklogs(self, worklog_ids: list[int]) -> WorklogLightIndexResponse:
        """요청된 업무일지 ID 순서대로 text/custom KG 통합 index 결과를 반환한다."""
        prepared = await self.prepare_documents(worklog_ids)
        failed_found_ids = await self._index_found_documents(prepared)

        missing_worklog_ids = set(prepared.missing_worklog_ids)
        indexed_worklog_ids = set(prepared.found_worklog_ids) - set(failed_found_ids)

        return WorklogLightIndexResponse(
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
        prepared: PreparedLightWorklogDocuments,
    ) -> dict[int, str]:
        """조회된 업무일지만 adapter에 전달하고 text/custom KG 실패를 ID별 오류로 변환한다."""
        if not prepared.documents:
            return {}

        adapter = get_lightrag_worklog_index_adapter()
        try:
            await adapter.index_documents(prepared.documents)
        except LightRagConfigurationError:
            raise
        except LightRagInsertTimeoutError:
            return {
                worklog_id: ERROR_LIGHTRAG_INSERT_TIMEOUT
                for worklog_id in prepared.found_worklog_ids
            }
        except LightRagInsertFailedError:
            return {
                worklog_id: ERROR_LIGHTRAG_INSERT_FAILED
                for worklog_id in prepared.found_worklog_ids
            }

        failed_found_ids: dict[int, str] = {}
        for worklog_id, document in zip(
            prepared.found_worklog_ids,
            prepared.custom_kg_documents,
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
    ) -> WorklogLightIndexItem:
        """업무일지 ID 하나의 통합 index 응답 item을 만든다."""
        if worklog_id in missing_worklog_ids:
            return WorklogLightIndexItem(
                worklogId=worklog_id,
                indexed=False,
                error=ERROR_WORKLOG_NOT_FOUND,
            )
        if worklog_id in indexed_worklog_ids:
            return WorklogLightIndexItem(worklogId=worklog_id, indexed=True)
        return WorklogLightIndexItem(
            worklogId=worklog_id,
            indexed=False,
            error=failed_found_ids[worklog_id],
        )
