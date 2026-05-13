"""LightRAG v3 업무일지 index 라우터."""

from typing import Annotated

from fastapi import APIRouter, Depends

from app.light.v3.model.worklog_index import (
    WorklogLightIndexRequest,
    WorklogLightIndexResponse,
)
from app.light.v3.service.worklog_index_service import LightWorklogIndexService

router = APIRouter(prefix="/light/worklogs-v3", tags=["light-worklogs-v3"])
worklog_index_service = LightWorklogIndexService()


def get_worklog_index_service() -> LightWorklogIndexService:
    """LightRAG v3 업무일지 index service dependency를 반환한다."""
    return worklog_index_service


@router.post("/index", response_model=WorklogLightIndexResponse)
async def index_worklogs(
    request: WorklogLightIndexRequest,
    service: Annotated[LightWorklogIndexService, Depends(get_worklog_index_service)],
) -> WorklogLightIndexResponse:
    """업무일지 ID 목록을 LightRAG v3 index 파이프라인에 전달한다."""
    return await service.index_worklogs([int(worklog_id) for worklog_id in request.worklog_ids])
