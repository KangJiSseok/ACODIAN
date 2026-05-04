import asyncio
import logging

from app.client.worklog_client import WorklogClient
from app.service.summary_service import SummaryService
from app.task.celery_app import celery_app

logger = logging.getLogger(__name__)


@celery_app.task(name="worklog.summary")
def generate_worklog_summary(
    worklog_id: int,
    request_content: str | None,
    work_content: str,
) -> dict[str, int | str]:
    return asyncio.run(
        _generate_worklog_summary(
            worklog_id=worklog_id,
            request_content=request_content,
            work_content=work_content,
        )
    )


async def _generate_worklog_summary(
        worklog_id: int,
        request_content: str | None,
        work_content: str,
) -> dict[str, int | str]:
    
    try: 

        summary = await SummaryService().generate_summary(
            request_content=request_content,
            work_content=work_content,
        )

        await WorklogClient().update_ai_result(
            worklog_id=worklog_id,
            ai_summary=summary,
            ai_processing_status="COMPLETED",
        )

        return {
            "worklog_id": worklog_id,
            "summary": summary,
            "status": "COMPLETED",
        }

    except Exception:
        logger.exception("Failed to run worklog AI pipeline. worklog_id=%s", worklog_id)

        await WorklogClient().update_ai_result(
            worklog_id=worklog_id,
            ai_summary="",
            ai_processing_status="FAILED",
        )

        raise
