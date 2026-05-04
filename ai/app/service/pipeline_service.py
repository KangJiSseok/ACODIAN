from app.model.pipeline_model import PipelineAcceptedResponse, WorklogPipelineRequest
from app.task.tagging_tasks import generate_worklog_tags
from app.task.worklog_tasks import generate_worklog_summary


class PipelineService:
    async def trigger_worklog_pipeline(
        self,
        request: WorklogPipelineRequest,
    ) -> PipelineAcceptedResponse:
        summary_task = generate_worklog_summary.delay(
            request.worklog_id,
            request.request_content,
            request.work_content,
        )
        tagging_task = generate_worklog_tags.delay(
            request.worklog_id,
            request.work_content,
        )

        return PipelineAcceptedResponse(
            worklog_id=request.worklog_id,
            summary_task_id=summary_task.id,
            tagging_task_id=tagging_task.id,
        )
