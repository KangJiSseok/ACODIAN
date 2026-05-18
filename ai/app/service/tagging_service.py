import json

from app.chain.tagging_chain import create_tagging_chain
from app.model.tagging_model import (
    MetaTag,
    NewTag,
    TAG_DESCRIPTION_MAX_LENGTH,
    TaggingChainResult,
    TaggingResult,
)

MAX_TAG_COUNT = 5


class TaggingService:
    def __init__(self) -> None:
        self.chain = create_tagging_chain()

    async def generate_tags(
        self,
        work_content: str,
        existing_tags: list[MetaTag],
    ) -> TaggingResult:
        chain_result = await self.chain.ainvoke(
            {
                "existing_tags": self._format_existing_tags(existing_tags),
                "work_content": work_content,
            }
        )
        parsed_result = TaggingChainResult.model_validate(chain_result)

        existing_names = self._resolve_existing_tag_names(
            requested_names=parsed_result.existing_tags,
            existing_tags=existing_tags,
        )
        new_tags = self._resolve_new_tags(
            requested_tags=parsed_result.new_tags,
            existing_tags=existing_tags,
        )

        remaining_count = max(MAX_TAG_COUNT - len(existing_names), 0)
        return TaggingResult(
            existing_tags=existing_names[:MAX_TAG_COUNT],
            new_tags=new_tags[:remaining_count],
        )

    def _format_existing_tags(self, existing_tags: list[MetaTag]) -> str:
        if not existing_tags:
            return "(없음)"
        items = [
            {
                "tagName": tag.tag_name,
                "description": tag.description or "",
            }
            for tag in existing_tags
        ]
        return json.dumps(items, ensure_ascii=False)

    def _resolve_existing_tag_names(
        self,
        requested_names: list[str],
        existing_tags: list[MetaTag],
    ) -> list[str]:
        existing_tag_by_key = {
            self._normalize_tag_name(tag.tag_name): tag.tag_name for tag in existing_tags
        }
        selected_names: list[str] = []
        selected_keys: set[str] = set()

        for requested_name in requested_names:
            key = self._normalize_tag_name(requested_name)
            existing_name = existing_tag_by_key.get(key)
            if existing_name is None or key in selected_keys:
                continue
            selected_names.append(existing_name)
            selected_keys.add(key)

        return selected_names

    def _resolve_new_tags(
        self,
        requested_tags: list[NewTag],
        existing_tags: list[MetaTag],
    ) -> list[NewTag]:
        existing_keys = {
            self._normalize_tag_name(tag.tag_name) for tag in existing_tags
        }
        new_tags: list[NewTag] = []
        new_keys: set[str] = set()

        for requested_tag in requested_tags:
            tag_name = requested_tag.tag_name.strip()
            description = requested_tag.description.strip()
            key = self._normalize_tag_name(tag_name)
            if not tag_name or not description or key in existing_keys:
                continue
            if key in new_keys:
                continue
            new_tags.append(
                NewTag(
                    tagName=tag_name,
                    description=description[:TAG_DESCRIPTION_MAX_LENGTH],
                )
            )
            new_keys.add(key)

        return new_tags

    def _normalize_tag_name(self, tag_name: str) -> str:
        return "".join(tag_name.split()).casefold()
