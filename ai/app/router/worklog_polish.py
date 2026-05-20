import logging
import time

from fastapi import APIRouter, HTTPException, Request, status

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
    payload: WorklogPolishRequest,
    http_request: Request,
) -> WorklogPolishResponse:
    started_at = time.monotonic()
    _log_request_start("polish", http_request, payload)
    try:
        response = await worklog_polish_service.polish_worklog(payload)
        _log_request_success(
            "polish",
            started_at,
            response_chars=len(response.work_content),
        )
        return response
    except Exception as exc:
        _log_request_exception("polish", started_at, exc)
        _raise_provider_unavailable_if_retryable(exc)
        raise


@router.post("/title-recommendations", response_model=WorklogTitleRecommendationResponse)
async def recommend_worklog_titles(
    payload: WorklogPolishRequest,
    http_request: Request,
) -> WorklogTitleRecommendationResponse:
    started_at = time.monotonic()
    _log_request_start("title_recommendations", http_request, payload)
    try:
        response = await worklog_polish_service.recommend_worklog_titles(payload)
        _log_request_success(
            "title_recommendations",
            started_at,
            title_count=len(response.titles),
        )
        return response
    except Exception as exc:
        _log_request_exception("title_recommendations", started_at, exc)
        _raise_provider_unavailable_if_retryable(exc)
        raise


def _log_request_start(
    endpoint: str,
    http_request: Request,
    payload: WorklogPolishRequest,
) -> None:
    client_host = http_request.client.host if http_request.client else "unknown"
    logger.info(
        "event=worklog_polish.request.start endpoint=%s method=%s path=%s clientHost=%s "
        "requestContentChars=%s workContentChars=%s",
        endpoint,
        http_request.method,
        http_request.url.path,
        client_host,
        len(payload.request_content or ""),
        len(payload.work_content),
    )


def _log_request_success(endpoint: str, started_at: float, **metadata: int) -> None:
    logger.info(
        "event=worklog_polish.request.success endpoint=%s durationMs=%s %s",
        endpoint,
        _elapsed_ms(started_at),
        _format_metadata(metadata),
    )


def _log_request_exception(endpoint: str, started_at: float, exc: Exception) -> None:
    logger.warning(
        "event=worklog_polish.request.exception endpoint=%s durationMs=%s "
        "exceptionClass=%s retryable=%s",
        endpoint,
        _elapsed_ms(started_at),
        exc.__class__.__name__,
        is_retryable_ai_error(exc),
    )


def _elapsed_ms(started_at: float) -> int:
    return int((time.monotonic() - started_at) * 1000)


def _format_metadata(metadata: dict[str, int]) -> str:
    return " ".join(f"{key}={value}" for key, value in metadata.items())


def _raise_provider_unavailable_if_retryable(exc: Exception) -> None:
    if not is_retryable_ai_error(exc):
        return
    logger.warning("Worklog polish provider unavailable error=%s", exc.__class__.__name__)
    raise HTTPException(
        status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
        detail="AI_PROVIDER_UNAVAILABLE",
    ) from exc
