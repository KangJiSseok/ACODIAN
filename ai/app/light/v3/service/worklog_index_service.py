"""LightRAG v3 업무일지 index orchestration service 경계."""

from collections.abc import Callable
from dataclasses import dataclass
from typing import Protocol

from app.light.v3.model.worklog_index import (
    WorklogLightIndexItem,
    WorklogLightIndexResponse,
)
from app.light.v3.service.worklog_document_builder import (
    LightRagWorklogDocument,
    build_worklog_light_document,
)
from app.light.v3.service.worklog_source_reader import (
    LightRagWorklogSource,
    WorklogLightSourceReader,
)


class WorklogSourceReader(Protocol):
    """LightRAG source reader가 제공해야 하는 최소 interface."""

    async def fetch_sources(self, worklog_ids: list[int]) -> dict[int, LightRagWorklogSource]:
        """존재하는 업무일지 source를 ID keyed mapping으로 반환한다."""


@dataclass(frozen=True)
class PreparedLightWorklogDocuments:
    """LightRAG insert 직전의 업무일지 문서 준비 결과."""

    documents: list[LightRagWorklogDocument]
    missing_worklog_ids: list[int]


class LightWorklogIndexService:
    """업무일지 LightRAG index 유스케이스 진입점.

    Phase 2에서는 DB source reader와 document builder 경계를 연결한다. 실제 LightRAG
    adapter 호출은 후속 phase에서 이 service 내부로 이어 붙인다.
    """

    def __init__(
        self,
        *,
        source_reader: WorklogSourceReader | None = None,
        document_builder: Callable[
            [LightRagWorklogSource],
            LightRagWorklogDocument,
        ] = build_worklog_light_document,
    ) -> None:
        """source reader와 document builder 의존성을 구성한다."""
        self._source_reader = source_reader
        self._document_builder = document_builder

    async def prepare_documents(self, worklog_ids: list[int]) -> PreparedLightWorklogDocuments:
        """업무일지 ID 목록을 LightRAG document 목록과 missing ID 목록으로 나눈다."""
        source_reader = self._source_reader or WorklogLightSourceReader()
        sources_by_id = await source_reader.fetch_sources(worklog_ids)
        documents: list[LightRagWorklogDocument] = []
        missing_worklog_ids: list[int] = []

        for worklog_id in worklog_ids:
            source = sources_by_id.get(worklog_id)
            if source is None:
                missing_worklog_ids.append(worklog_id)
                continue
            documents.append(self._document_builder(source))

        return PreparedLightWorklogDocuments(
            documents=documents,
            missing_worklog_ids=missing_worklog_ids,
        )

    async def index_worklogs(self, worklog_ids: list[int]) -> WorklogLightIndexResponse:
        """요청된 업무일지 ID별 index 결과를 반환한다.

        아직 실제 LightRAG insert를 수행하지 않는다. Phase 2는 source/document 계약만
        추가하므로, 후속 adapter/orchestration phase 전까지는 성공으로 오인되지 않도록
        명시적인 미구현 결과를 반환한다.
        """
        return WorklogLightIndexResponse(
            items=[
                WorklogLightIndexItem(
                    worklogId=worklog_id,
                    indexed=False,
                    error="LIGHTRAG_INDEX_NOT_IMPLEMENTED",
                )
                for worklog_id in worklog_ids
            ]
        )
