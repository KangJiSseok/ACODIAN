import asyncio
import logging

from app.client.worklog_client import WorklogClient
from app.service.summary_service import SummaryService
from app.task.celery_app import celery_app
from app.task.retry_policy import retry_ai_task

logger = logging.getLogger(__name__)


@celery_app.task(name="worklog.summary", bind=True)
def generate_worklog_summary(
    self,
    worklog_id: int,
    request_content: str | None,
    work_content: str,
) -> dict[str, int | str]:
    try:
        return asyncio.run(
            _generate_worklog_summary(
                worklog_id=worklog_id,
                request_content=request_content,
                work_content=work_content,
            )
        )
    except Exception as exc:
        retry_ai_task(self, exc, task_name="worklog.summary", target_id=worklog_id)
        logger.exception("Failed to run worklog AI pipeline. worklog_id=%s", worklog_id)
        asyncio.run(_mark_worklog_summary_failed(worklog_id))
        raise


async def _generate_worklog_summary(
        worklog_id: int,
        request_content: str | None,
        work_content: str,
) -> dict[str, int | str]:
    summary = await SummaryService().generate_summary(
        request_content=request_content,
        work_content=work_content,
    )

    async with WorklogClient() as worklog_client:
        await worklog_client.update_ai_result(
            worklog_id=worklog_id,
            ai_summary=summary,
            ai_processing_status="COMPLETED",
        )

    return {
        "worklog_id": worklog_id,
        "summary": summary,
        "status": "COMPLETED",
    }


async def _mark_worklog_summary_failed(worklog_id: int) -> None:
    async with WorklogClient() as worklog_client:
        await worklog_client.update_ai_result(
            worklog_id=worklog_id,
            ai_summary="",
            ai_processing_status="FAILED",
        )
