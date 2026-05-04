from app.client.base_api_client import BaseApiClient
from app.model.tagging_model import MetaTag


class TaggingClient(BaseApiClient):
    async def list_tags(self) -> list[MetaTag]:
        response = await self.client.get(self.api_path("/api/tags"))
        response.raise_for_status()

        data = response.json().get("data", {})
        items = data.get("items", data if isinstance(data, list) else [])
        return [MetaTag.model_validate(item) for item in items]

    async def create_tag(self, tag_name: str) -> MetaTag:
        response = await self.client.post(
            self.api_path("/api/tags"),
            json={"tagName": tag_name},
        )
        response.raise_for_status()

        return MetaTag.model_validate(response.json()["data"])

    async def update_worklog_tags(self, worklog_id: int, tag_ids: list[int]) -> None:
        response = await self.client.put(
            self.api_path(f"/api/worklogs/{worklog_id}/tags"),
            json={"tagIds": tag_ids},
        )
        response.raise_for_status()
