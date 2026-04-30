from fastapi import APIRouter, status

from app.model.pipeline_model import PipelineAcceptedResponse, WorklogPipelineRequest
from app.service.pipeline_service import PipelineService

router = APIRouter(prefix="/pipeline", tags=["pipeline"])
pipeline_service = PipelineService()


@router.post(
    "/worklog",
    response_model=PipelineAcceptedResponse,
    status_code=status.HTTP_202_ACCEPTED,
)
async def trigger_worklog_pipeline(
    request: WorklogPipelineRequest,
) -> PipelineAcceptedResponse:
    return await pipeline_service.trigger_worklog_pipeline(request)
