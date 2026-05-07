import asyncio
import logging

from app.model.embedding import WorklogEmbeddingResult
from app.service.embedding_service import EmbeddingService
from app.task.celery_app import celery_app

logger = logging.getLogger(__name__)


@celery_app.task(name="worklog.embedding")
def generate_worklog_embedding(worklog_id: int) -> dict[str, int]:
    """Celery worker가 호출하는 동기 task entrypoint.

    Celery task 함수는 일반 동기 함수여야 하므로 내부 async 구현을 `asyncio.run`으로 실행한다.
    """
    return asyncio.run(_generate_worklog_embedding(worklog_id=worklog_id))


async def _generate_worklog_embedding(worklog_id: int) -> dict[str, int]:
    """실제 임베딩 생성 로직을 async 서비스로 위임한다."""
    try:
        result = await EmbeddingService().embed_worklog(worklog_id)
        logger.info("업무일지 임베딩 생성 완료: worklog_id=%s", worklog_id)
        return _to_task_result(result)
    except Exception:
        logger.exception("Failed to generate worklog embedding. worklog_id=%s", worklog_id)
        raise


def _to_task_result(result: WorklogEmbeddingResult) -> dict[str, int]:
    return {
        "worklog_id": result.worklog_id,
        "chunk_count": result.chunk_count,
    }
