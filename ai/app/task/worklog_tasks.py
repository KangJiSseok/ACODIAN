import asyncio

from app.client.worklog_client import WorklogClient
from app.service.summary_service import SummaryService
from app.task.celery_app import celery_app


@celery_app.task(name="worklog.ping")
def ping_worklog_pipeline(worklog_id: int) -> dict[str, int | str]:
    return {
        "worklog_id": worklog_id,
        "status": "ok",
    }

@celery_app.task(name="worklog.pipeline")
def run_worklog_pipeline(
    worklog_id: int,
    request_content: str | None,
    work_content: str,
) -> dict[str, int | str]:
    return asyncio.run(
        _run_worklog_pipeline(
            worklog_id=worklog_id,
            request_content=request_content,
            work_content=work_content,
        )
    )

async def _run_worklog_pipeline(
        worklog_id: int,
        request_content: str | None,
        work_content: str,
) -> dict[str, int | str]:
    summary = await SummaryService().generate_summary(
        request_content=request_content,
        work_content=work_content,
    )

    await WorklogClient().update_ai_result(
        worklog_id=worklog_id,
        ai_summary=summary,
        ai_summary_edited=False,
        ai_processing_status="COMPLETED",
    )

    return {
        "worklog_id": worklog_id,
        "summary": summary,
        "status": "COMPLETED",
    }