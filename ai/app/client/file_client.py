"""api 의 파일 AI 결과 콜백 클라이언트.

``PATCH /internal/files/{fileId}/ai-result`` 를 호출해 파일 단위 AI 요약 결과를 반영한다.
``/internal/**`` 은 SecurityConfig 가 인증 면제 처리하며, 외부 노출은 nginx 가 404 로 막는다.
"""

from app.client.base_api_client import BaseApiClient


class FileClient(BaseApiClient):
    async def update_ai_result(
        self,
        file_id: int,
        ai_summary: str,
        ai_processing_status: str,
    ) -> None:
        response = await self.client.patch(
            self.api_path(f"/api/internal/files/{file_id}/ai-result"),
            json={
                "aiSummary": ai_summary,
                "aiProcessingStatus": ai_processing_status,
            },
        )
        response.raise_for_status()
