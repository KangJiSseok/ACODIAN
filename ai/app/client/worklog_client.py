from app.client.base_api_client import BaseApiClient


class WorklogClient(BaseApiClient):
    async def update_ai_result(
            self,
            worklog_id: int,
            ai_summary: str,
            ai_processing_status: str,
    ) -> None:
        response = await self.client.patch(
            self.api_path(f"/api/internal/worklogs/{worklog_id}/ai-result"),
            json={
                "aiSummary": ai_summary,
                "aiProcessingStatus": ai_processing_status,
            },
        )
        response.raise_for_status()
