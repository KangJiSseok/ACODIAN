from app.model.pipeline_model import PipelineTriggerRequest, PipelineTriggerResponse


class PipelineService:
    def trigger_worklog_pipeline(self, payload: PipelineTriggerRequest) -> PipelineTriggerResponse:
        return PipelineTriggerResponse(
            worklog_id=payload.worklog_id,
            status="queued",
            message="기본 AI 파이프라인 연결 전 단계입니다.",
        )
