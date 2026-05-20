import logging

from fastapi import APIRouter, HTTPException, status

from app.model.worklog_polish import (
    WorklogPolishRequest,
    WorklogPolishResponse,
    WorklogTitleRecommendationResponse,
)
from app.service.worklog_polish_service import WorklogPolishService
from app.task.retry_policy import is_retryable_ai_error

router = APIRouter(prefix="/worklogs", tags=["worklog-polish"])
worklog_polish_service = WorklogPolishService()
logger = logging.getLogger(__name__)


@router.post("/polish", response_model=WorklogPolishResponse)
async def polish_worklog(
    request: WorklogPolishRequest,
) -> WorklogPolishResponse:
    try:
        return await worklog_polish_service.polish_worklog(request)
    except Exception as exc:
        _raise_provider_unavailable_if_retryable(exc)
        raise


@router.post("/title-recommendations", response_model=WorklogTitleRecommendationResponse)
async def recommend_worklog_titles(
    request: WorklogPolishRequest,
) -> WorklogTitleRecommendationResponse:
    try:
        return await worklog_polish_service.recommend_worklog_titles(request)
    except Exception as exc:
        _raise_provider_unavailable_if_retryable(exc)
        raise


def _raise_provider_unavailable_if_retryable(exc: Exception) -> None:
    if not is_retryable_ai_error(exc):
        return
    logger.warning("Worklog polish provider unavailable error=%s", exc.__class__.__name__)
    raise HTTPException(
        status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
        detail="AI_PROVIDER_UNAVAILABLE",
    ) from exc
