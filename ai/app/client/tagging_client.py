from app.client.base_api_client import BaseApiClient
from app.model.tagging_model import MetaTag, NewTag


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
                    "description": item.get("description"),
                    "usageCount": item.get("usageCount", item.get("usage_count", 0)),
                }
            )
            for item in items
        ]

    async def apply_worklog_ai_tags(
        self,
        worklog_id: int,
        existing_tag_ids: list[int],
        new_tags: list[NewTag],
    ) -> None:
        response = await self.client.post(
            self.api_path(f"/api/internal/worklogs/{worklog_id}/ai-tags"),
            json={
                "existingTagIds": existing_tag_ids,
                "newTagNames": [
                    tag.tag_name
                    for tag in new_tags
                ],
                "newTags": [
                    tag.model_dump(by_alias=True)
                    for tag in new_tags
                ],
            },
        )
        response.raise_for_status()
