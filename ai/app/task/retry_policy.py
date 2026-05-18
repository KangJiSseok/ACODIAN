import logging
from typing import Any

from app.config.settings import settings

logger = logging.getLogger(__name__)

RETRYABLE_AI_STATUS_CODES = {408, 429, 500, 502, 503, 504}
RETRYABLE_AI_ERROR_NAMES = {
    "ServerError",
    "ServiceUnavailable",
    "TooManyRequests",
    "ResourceExhausted",
}


def is_retryable_ai_error(exc: BaseException) -> bool:
    """외부 AI 공급자/네트워크의 일시 장애인지 판별한다."""
    if isinstance(exc, TimeoutError | ConnectionError):
        return True

    status_code = _extract_status_code(exc)
    if status_code in RETRYABLE_AI_STATUS_CODES:
        return True

    return exc.__class__.__name__ in RETRYABLE_AI_ERROR_NAMES


def retry_ai_task(task: Any, exc: Exception, *, task_name: str, target_id: int) -> None:
    """Celery task 레벨에서 일시 AI 장애를 지수 backoff로 재시도한다."""
    max_retries = max(settings.gemini_max_retries, 0)
    current_retries = getattr(task.request, "retries", 0)

    if not is_retryable_ai_error(exc) or current_retries >= max_retries:
        return

    countdown = min(60, 2**current_retries)
    logger.warning(
        "AI task 재시도 예약 task=%s targetId=%s retry=%s/%s countdown=%ss",
        task_name,
        target_id,
        current_retries + 1,
        max_retries,
        countdown,
        exc_info=exc,
    )
    raise task.retry(exc=exc, countdown=countdown, max_retries=max_retries)


def _extract_status_code(exc: BaseException) -> int | None:
    for attr_name in ("status_code", "code"):
        value = getattr(exc, attr_name, None)
        try:
            return int(value)
        except (TypeError, ValueError):
            continue
    return None
