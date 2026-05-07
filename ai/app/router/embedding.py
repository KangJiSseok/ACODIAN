from fastapi import APIRouter, status

from app.model.embedding import WorklogEmbeddingAcceptedResponse, WorklogEmbeddingRequest
from app.task.embedding_tasks import generate_worklog_embedding

router = APIRouter(prefix="/embedding", tags=["embedding"])


@router.post(
    "/worklog",
    response_model=WorklogEmbeddingAcceptedResponse,
    status_code=status.HTTP_202_ACCEPTED,
)
async def trigger_worklog_embedding(
    request: WorklogEmbeddingRequest,
) -> WorklogEmbeddingAcceptedResponse:
    """업무일지 임베딩 생성을 큐에 넣는다.

    FastAPI 라우터는 HTTP 계약만 담당한다. 오래 걸리는 Gemini 호출/DB 저장은
    `generate_worklog_embedding` Celery task로 넘기고 202 응답을 즉시 반환한다.
    """
    task = generate_worklog_embedding.delay(request.worklog_id)
    return WorklogEmbeddingAcceptedResponse(
        worklogId=request.worklog_id,
        embeddingTaskId=task.id,
    )
