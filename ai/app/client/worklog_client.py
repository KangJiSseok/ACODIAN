import httpx

from app.config.settings import settings


class WorklogClient:
    def __init__(self) -> None:
        self.base_url = settings.api_base_url.rstrip("/")

    async def update_ai_result(
            self,
            worklog_id: int,
            ai_summary: str,
            ai_processing_status: str,
    ) -> None:
        async with httpx.AsyncClient(base_url=self.base_url, timeout=30.0) as client:
            response = await client.patch(
                self._api_path(f"/api/internal/worklogs/{worklog_id}/ai-result"),
                json={
                    "aiSummary": ai_summary,
                    "aiProcessingStatus": ai_processing_status,
                },
            )
            response.raise_for_status()

    def _api_path(self, path: str) -> str:
        if self.base_url.endswith("/api") and path.startswith("/api/"):
            return path.removeprefix("/api")
        return path
