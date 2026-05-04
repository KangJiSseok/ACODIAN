import httpx

from app.config.settings import settings
from app.model.tagging_model import MetaTag


class TaggingClient:
    def __init__(self) -> None:
        self.base_url = settings.api_base_url.rstrip("/")

    async def list_tags(self) -> list[MetaTag]:
        async with httpx.AsyncClient(base_url=self.base_url, timeout=30.0) as client:
            response = await client.get(self._api_path("/api/tags"))
            response.raise_for_status()

        data = response.json().get("data", {})
        items = data.get("items", data if isinstance(data, list) else [])
        return [MetaTag.model_validate(item) for item in items]

    async def create_tag(self, tag_name: str) -> MetaTag:
        async with httpx.AsyncClient(base_url=self.base_url, timeout=30.0) as client:
            response = await client.post(
                self._api_path("/api/tags"),
                json={"tagName": tag_name},
            )
            response.raise_for_status()

        return MetaTag.model_validate(response.json()["data"])

    async def update_worklog_tags(self, worklog_id: int, tag_ids: list[int]) -> None:
        async with httpx.AsyncClient(base_url=self.base_url, timeout=30.0) as client:
            response = await client.put(
                self._api_path(f"/api/worklogs/{worklog_id}/tags"),
                json={"tagIds": tag_ids},
            )
            response.raise_for_status()

    def _api_path(self, path: str) -> str:
        if self.base_url.endswith("/api") and path.startswith("/api/"):
            return path.removeprefix("/api")
        return path
