import json

from sqlalchemy.ext.asyncio import AsyncSession

from app.chain.tag_merge_chain import create_tag_merge_chain
from app.model.tag_merge_model import (
    GenerateTagMergeCandidatesRequest,
    GenerateTagMergeCandidatesResponse,
    TagMergeCandidateGroup,
    TagMergeCandidateTag,
    TagMergeChainResult,
)
from app.model.tagging_model import MetaTag, TAG_DESCRIPTION_MAX_LENGTH
from app.store.tag_store import TagStore


class TagMergeService:
    def __init__(self, tag_store: TagStore | None = None) -> None:
        self.tag_store = tag_store or TagStore()
        self.chain = create_tag_merge_chain()

    async def generate_candidates(
        self,
        session: AsyncSession,
        request: GenerateTagMergeCandidatesRequest,
    ) -> GenerateTagMergeCandidatesResponse:
        tags = await self.tag_store.list_active_tags_for_merge(
            session,
            min_usage_count=request.min_usage_count,
            limit=request.tag_limit,
        )
        if not tags:
            return GenerateTagMergeCandidatesResponse()

        chain_result = await self.chain.ainvoke({"tags": self._format_tags(tags)})
        parsed_result = TagMergeChainResult.model_validate(chain_result)
        return GenerateTagMergeCandidatesResponse(
            items=self._resolve_groups(
                parsed_result=parsed_result,
                tags=tags,
                max_candidate_count=request.max_candidate_count,
                max_group_count=request.max_group_count,
            )
        )

    def _format_tags(self, tags: list[MetaTag]) -> str:
        items = [
            {
                "tagId": tag.tag_id,
                "tagName": tag.tag_name,
                "description": tag.description or "",
                "usageCount": tag.usage_count,
            }
            for tag in tags
        ]
        return json.dumps(items, ensure_ascii=False)

    def _resolve_groups(
        self,
        *,
        parsed_result: TagMergeChainResult,
        tags: list[MetaTag],
        max_candidate_count: int,
        max_group_count: int,
    ) -> list[TagMergeCandidateGroup]:
        tag_by_id = {tag.tag_id: tag for tag in tags}
        used_candidate_ids: set[int] = set()
        groups: list[TagMergeCandidateGroup] = []

        for group in parsed_result.groups:
            source_ids = [
                tag_id
                for tag_id in [group.target_tag_id, *group.candidate_tag_ids]
                if tag_id in tag_by_id
            ]
            unique_ids = list(dict.fromkeys(source_ids))
            if len(unique_ids) < 2:
                continue

            target = self._select_target([tag_by_id[tag_id] for tag_id in unique_ids])
            candidate_ids = [
                tag_id
                for tag_id in unique_ids
                if tag_id != target.tag_id and tag_id not in used_candidate_ids
            ]
            candidates = sorted(
                (tag_by_id[tag_id] for tag_id in candidate_ids),
                key=lambda tag: (-tag.usage_count, tag.tag_name, tag.tag_id),
            )[:max_candidate_count]
            if not candidates:
                continue

            used_candidate_ids.update(tag.tag_id for tag in candidates)
            groups.append(
                TagMergeCandidateGroup(
                    mergeTargetTag=self._to_response_tag(target),
                    resultDescription=self._resolve_description(group.result_description, target.description),
                    mergeCandidateTags=[
                        self._to_response_tag(candidate)
                        for candidate in candidates
                    ],
                )
            )
            if len(groups) >= max_group_count:
                break

        return groups

    def _select_target(self, tags: list[MetaTag]) -> MetaTag:
        return sorted(tags, key=lambda tag: (-tag.usage_count, tag.tag_name, tag.tag_id))[0]

    def _resolve_description(
        self,
        result_description: str | None,
        fallback_description: str | None,
    ) -> str | None:
        description = result_description or fallback_description
        if description is None:
            return None
        trimmed = description.strip()
        if not trimmed:
            return None
        return trimmed[:TAG_DESCRIPTION_MAX_LENGTH]

    def _to_response_tag(self, tag: MetaTag) -> TagMergeCandidateTag:
        return TagMergeCandidateTag(
            tagId=tag.tag_id,
            tagName=tag.tag_name,
            usageCount=tag.usage_count,
        )
