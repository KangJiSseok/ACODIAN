"""파일 AI 요약 트리거 디스패치.

api 가 보낸 트리거를 즉시 Celery 큐에 위임하고 task id 만 반환한다.
실제 파싱/요약/콜백은 ``app.task.file_tasks`` 가 처리한다.
"""

from app.model.file_model import FileSummaryAcceptedResponse, FileSummaryRequest
from app.task.file_tasks import generate_file_summary


class FileSummaryService:
    async def trigger_file_summary(
        self,
        request: FileSummaryRequest,
    ) -> FileSummaryAcceptedResponse:
        summary_task = generate_file_summary.delay(
            request.file_id,
            request.worklog_id,
            request.storage_key,
            request.original_name,
            request.file_extension,
        )

        return FileSummaryAcceptedResponse(
            file_id=request.file_id,
            summary_task_id=summary_task.id,
        )
