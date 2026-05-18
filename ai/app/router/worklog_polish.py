from fastapi import APIRouter

from app.model.worklog_polish import WorklogPolishRequest, WorklogPolishResponse
from app.service.worklog_polish_service import WorklogPolishService

router = APIRouter(prefix="/worklogs", tags=["worklog-polish"])
worklog_polish_service = WorklogPolishService()


@router.post("/polish", response_model=WorklogPolishResponse)
async def polish_worklog(
    request: WorklogPolishRequest,
) -> WorklogPolishResponse:
    return await worklog_polish_service.polish_worklog(request)
