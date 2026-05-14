from app.client.base_api_client import BaseApiClient
from app.model.tagging_model import MetaTag


class TaggingClient(BaseApiClient):
    async def list_tags(self) -> list[MetaTag]:
        response = await self.client.get(self.api_path("/api/internal/tags"))
        response.raise_for_status()

        data = response.json().get("data", {})
        items = data.get(
            "tags",
            data.get("items", data if isinstance(data, list) else []),
        )
        return [
            MetaTag.model_validate(
                {
                    "tagId": item.get("tagId", item.get("id")),
                    "tagName": item.get("tagName"),
                }
            )
            for item in items
        ]

    async def apply_worklog_ai_tags(
        self,
        worklog_id: int,
        existing_tag_ids: list[int],
        new_tag_names: list[str],
    ) -> None:
        response = await self.client.post(
            self.api_path(f"/api/internal/worklogs/{worklog_id}/ai-tags"),
            json={
                "existingTagIds": existing_tag_ids,
                "newTagNames": new_tag_names,
            },
        )
        response.raise_for_status()
