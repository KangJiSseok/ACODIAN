"""GraphRAG v1 업무일지 query 라우터."""

from __future__ import annotations

from typing import Annotated

from fastapi import APIRouter, Depends, HTTPException, status

from app.graph.v1.model.worklog_query import WorklogGraphQueryRequest, WorklogGraphQueryResponse
from app.graph.v1.service.worklog_query_service import GraphWorklogQueryService
from app.graph.v1.store.graph_rag_store import (
    GraphRagConfigurationError,
    GraphRagQueryFailedError,
    GraphRagQueryTimeoutError,
)

router = APIRouter(prefix="/graph/worklogs-v1", tags=["graph-worklogs-v1"])

worklog_query_service = GraphWorklogQueryService()


def get_worklog_query_service() -> GraphWorklogQueryService:
    """GraphRAG v1 업무일지 query service dependency를 반환한다."""
    return worklog_query_service


@router.post("/query", response_model=WorklogGraphQueryResponse)
async def query_worklogs(
    request: WorklogGraphQueryRequest,
    service: Annotated[GraphWorklogQueryService, Depends(get_worklog_query_service)],
) -> WorklogGraphQueryResponse:
    """GraphRAG 업무일지 subgraph retrieval query를 실행한다."""
    try:
        return await service.query_worklogs(request)
    except GraphRagConfigurationError as exc:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="GRAPHRAG_CONFIGURATION_ERROR",
        ) from exc
    except GraphRagQueryTimeoutError as exc:
        raise HTTPException(
            status_code=status.HTTP_504_GATEWAY_TIMEOUT,
            detail="GRAPHRAG_QUERY_TIMEOUT",
        ) from exc
    except GraphRagQueryFailedError as exc:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="GRAPHRAG_QUERY_FAILED",
        ) from exc
