from fastapi import APIRouter

from app.model.worklog_polish import (
    WorklogPolishRequest,
    WorklogPolishResponse,
    WorklogTitleRecommendationResponse,
)
from app.service.worklog_polish_service import WorklogPolishService

router = APIRouter(prefix="/worklogs", tags=["worklog-polish"])
worklog_polish_service = WorklogPolishService()


@router.post("/polish", response_model=WorklogPolishResponse)
async def polish_worklog(
    request: WorklogPolishRequest,
) -> WorklogPolishResponse:
    return await worklog_polish_service.polish_worklog(request)


@router.post("/title-recommendations", response_model=WorklogTitleRecommendationResponse)
async def recommend_worklog_titles(
    request: WorklogPolishRequest,
) -> WorklogTitleRecommendationResponse:
    return await worklog_polish_service.recommend_worklog_titles(request)
