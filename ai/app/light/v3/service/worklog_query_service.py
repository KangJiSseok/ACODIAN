"""LightRAG v3 업무일지 query orchestration service 경계."""

from __future__ import annotations

from collections.abc import Callable
from typing import Any

from app.config.settings import Settings, settings
from app.light.v3.model.worklog_query import (
    WorklogLightQueryRequest,
    WorklogLightQueryResponse,
    WorklogLightReferenceItem,
)
from app.light.v3.service.lightrag_adapter import (
    LightRagQueryOptions,
    get_lightrag_worklog_index_adapter,
)


class LightWorklogQueryService:
    """내부 검증용 LightRAG 업무일지 query 유스케이스 진입점."""

    def __init__(
        self,
        *,
        settings_obj: Settings = settings,
        adapter_factory: Callable[..., Any] = get_lightrag_worklog_index_adapter,
    ) -> None:
        """settings와 LightRAG adapter factory를 구성한다."""
        self._settings = settings_obj
        self._adapter_factory = adapter_factory

    async def query_worklogs(
        self,
        request: WorklogLightQueryRequest,
    ) -> WorklogLightQueryResponse:
        """LightRAG query를 실행하고 native answer와 references를 정규화한다."""
        options = LightRagQueryOptions(
            query=request.query,
            top_k=self._settings.lightrag_query_top_k,
            chunk_top_k=self._settings.lightrag_query_chunk_top_k,
            response_type=self._settings.lightrag_query_response_type,
        )
        result = await self._adapter_factory().query_worklogs(options)
        raw = result.raw

        return WorklogLightQueryResponse(
            answer=raw["llm_response"]["content"].strip(),
            references=_extract_references(raw),
            internalOnly=True,
        )


def _extract_references(raw: dict[str, Any]) -> list[WorklogLightReferenceItem]:
    """LightRAG raw payload의 data.references를 업무일지 reference로 변환한다."""
    return [_build_reference_item(reference) for reference in raw["data"]["references"]]


def _build_reference_item(raw_reference: dict[str, Any]) -> WorklogLightReferenceItem:
    """Raw reference 1건을 응답 item으로 변환한다."""
    return WorklogLightReferenceItem(
        referenceId=raw_reference["reference_id"],
        filePath=raw_reference["file_path"],
    )
