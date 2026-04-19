from fastapi import APIRouter, status

from app.model.pipeline_model import PipelineTriggerRequest, PipelineTriggerResponse
from app.service.pipeline_service import PipelineService

router = APIRouter(prefix="/pipeline", tags=["pipeline"])
pipeline_service = PipelineService()


@router.post("/worklogs", response_model=PipelineTriggerResponse, status_code=status.HTTP_202_ACCEPTED)
def trigger_worklog_pipeline(payload: PipelineTriggerRequest) -> PipelineTriggerResponse:
    return pipeline_service.trigger_worklog_pipeline(payload)
