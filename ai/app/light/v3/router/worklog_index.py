"""LightRAG v3 업무일지 index 라우터."""

from typing import Annotated

from fastapi import APIRouter, Depends, HTTPException, status

from app.config.settings import settings
from app.light.v3.model.worklog_index import (
    WorklogLightIndexRequest,
    WorklogLightIndexResponse,
)
from app.light.v3.service.lightrag_adapter import LightRagConfigurationError
from app.light.v3.service.worklog_index_service import LightWorklogIndexService

router = APIRouter(prefix="/light/worklogs-v3", tags=["light-worklogs-v3"])


"""
FastAPI의 싱글톤 주입 방식
"""
worklog_index_service = LightWorklogIndexService()
def get_worklog_index_service() -> LightWorklogIndexService:
    """LightRAG v3 업무일지 index service dependency를 반환한다."""
    return worklog_index_service



@router.post("/index", response_model=WorklogLightIndexResponse)
async def index_worklogs(
    request: WorklogLightIndexRequest,
    service: Annotated[LightWorklogIndexService, Depends(get_worklog_index_service)],
) -> WorklogLightIndexResponse:


    worklog_ids = [int(worklog_id) for worklog_id in request.worklog_ids]
    if len(worklog_ids) > settings.lightrag_index_max_batch_size:
        raise HTTPException(
            status_code=status.HTTP_422_UNPROCESSABLE_CONTENT,
            detail="worklogIds exceeds LIGHTRAG_INDEX_MAX_BATCH_SIZE",
        )

    try:
        return await service.index_worklogs(worklog_ids)
    except LightRagConfigurationError as exc:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="LIGHTRAG_CONFIGURATION_ERROR",
        ) from exc
