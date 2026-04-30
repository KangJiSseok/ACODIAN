from app.model.pipeline_model import PipelineAcceptedResponse, WorklogPipelineRequest
from app.task.worklog_tasks import run_worklog_pipeline


class PipelineService:
    async def trigger_worklog_pipeline(
        self,
        request: WorklogPipelineRequest,
    ) -> PipelineAcceptedResponse:
        task = run_worklog_pipeline.delay(
            request.worklog_id,
            request.request_content,
            request.work_content,
        )
        
        return PipelineAcceptedResponse(
            worklog_id=request.worklog_id,
            task_id=task.id,
        )
