"""LightRAG v3 업무일지 query 라우터."""

from __future__ import annotations

from typing import Annotated

from fastapi import APIRouter, Depends, HTTPException, status

from app.light.v3.model.worklog_query import (
    WorklogLightQueryRequest,
    WorklogLightQueryResponse,
)
from app.light.v3.service.lightrag_adapter import (
    LightRagConfigurationError,
    LightRagQueryFailedError,
    LightRagQueryTimeoutError,
)
from app.light.v3.service.worklog_query_service import LightWorklogQueryService

router = APIRouter(prefix="/light/worklogs-v3", tags=["light-worklogs-v3"])

worklog_query_service = LightWorklogQueryService()


def get_worklog_query_service() -> LightWorklogQueryService:
    """LightRAG v3 업무일지 query service dependency를 반환한다."""
    return worklog_query_service


@router.post(
    "/query",
    response_model=WorklogLightQueryResponse,
)
async def query_worklogs(
    request: WorklogLightQueryRequest,
    service: Annotated[LightWorklogQueryService, Depends(get_worklog_query_service)],
) -> WorklogLightQueryResponse:
    """내부 검증용 LightRAG `mode="mix"` query를 실행한다."""
    try:
        return await service.query_worklogs(request)
    except LightRagConfigurationError as exc:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="LIGHTRAG_CONFIGURATION_ERROR",
        ) from exc
    except LightRagQueryTimeoutError as exc:
        raise HTTPException(
            status_code=status.HTTP_504_GATEWAY_TIMEOUT,
            detail="LIGHTRAG_QUERY_TIMEOUT",
        ) from exc
    except LightRagQueryFailedError as exc:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="LIGHTRAG_QUERY_FAILED",
        ) from exc
