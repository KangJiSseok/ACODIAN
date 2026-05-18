import asyncio

from app.model.tag_merge_model import GenerateTagMergeCandidatesRequest
from app.model.tagging_model import MetaTag
from app.service.tag_merge_service import TagMergeService


class FakeTagStore:
    async def list_active_tags_for_merge(
        self,
        session,
        *,
        min_usage_count: int,
        limit: int,
    ) -> list[MetaTag]:
        assert min_usage_count == 0
        assert limit == 200
        return [
            MetaTag(tagId=1, tagName="a", description="기준 태그", usageCount=10),
            MetaTag(tagId=2, tagName="b", description="유사 태그", usageCount=3),
            MetaTag(tagId=3, tagName="c", description="유사 태그", usageCount=5),
            MetaTag(tagId=4, tagName="d", description="유사 태그", usageCount=1),
            MetaTag(tagId=5, tagName="e", description="유사 태그", usageCount=2),
            MetaTag(tagId=6, tagName="f", description="유사 태그", usageCount=4),
            MetaTag(tagId=7, tagName="g", description="유사 태그", usageCount=6),
        ]


class FakeChain:
    async def ainvoke(self, payload: dict[str, str]) -> dict[str, list[dict[str, object]]]:
        assert '"description": "기준 태그"' in payload["tags"]
        return {
            "groups": [
                {
                    "targetTagId": 2,
                    "resultDescription": "기준 태그로 병합할 설명",
                    "candidateTagIds": [1, 3, 4, 5, 6, 7, 999, 1],
                }
            ]
        }


def test_generate_candidates_reselects_target_and_limits_candidates() -> None:
    service = TagMergeService(tag_store=FakeTagStore())
    service.chain = FakeChain()

    response = asyncio.run(
        service.generate_candidates(
            session=None,
            request=GenerateTagMergeCandidatesRequest(),
        )
    )

    assert len(response.items) == 1
    group = response.items[0]
    assert group.merge_target_tag.tag_id == 1
    assert group.result_description == "기준 태그로 병합할 설명"
    assert [tag.tag_id for tag in group.merge_candidate_tags] == [7, 3, 6, 2, 5]
