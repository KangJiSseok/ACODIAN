"""GraphRAG v1 업무일지 index 라우터."""

from typing import Annotated

from fastapi import APIRouter, Depends, HTTPException, status

from app.config.settings import settings
from app.graph.v1.model.worklog_index import WorklogGraphIndexRequest, WorklogGraphIndexResponse
from app.graph.v1.service.worklog_index_service import GraphWorklogIndexService
from app.graph.v1.store.graph_rag_store import GraphRagConfigurationError

router = APIRouter(prefix="/graph/worklogs-v1", tags=["graph-worklogs-v1"])

worklog_index_service = GraphWorklogIndexService()


def get_worklog_index_service() -> GraphWorklogIndexService:
    """GraphRAG v1 업무일지 index service dependency를 반환한다."""
    return worklog_index_service


def validate_index_batch_size(worklog_ids: list[int]) -> None:
    """GraphRAG index 요청 batch 크기 제한을 검증한다."""
    if len(worklog_ids) > settings.graphrag_index_max_batch_size:
        raise HTTPException(
            status_code=status.HTTP_422_UNPROCESSABLE_CONTENT,
            detail="worklogIds exceeds GRAPHRAG_INDEX_MAX_BATCH_SIZE",
        )


@router.post("/index", response_model=WorklogGraphIndexResponse)
async def index_worklogs(
    request: WorklogGraphIndexRequest,
    service: Annotated[GraphWorklogIndexService, Depends(get_worklog_index_service)],
) -> WorklogGraphIndexResponse:
    """업무일지 GraphRAG graph document를 index한다."""
    worklog_ids = [int(worklog_id) for worklog_id in request.worklog_ids]
    validate_index_batch_size(worklog_ids)

    try:
        return await service.index_worklogs(worklog_ids)
    except GraphRagConfigurationError as exc:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="GRAPHRAG_CONFIGURATION_ERROR",
        ) from exc
