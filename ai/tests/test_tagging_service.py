import asyncio

from app.model.tagging_model import MetaTag
from app.service.tagging_service import TaggingService


class DummyTaggingChain:
    async def ainvoke(self, payload: dict[str, str]) -> dict[str, list[str]]:
        assert payload["existing_tags"] == "재고, MCP 개발"
        assert payload["work_content"] == "MCP 개발 작업과 재고 동기화 배치를 개선했다."
        return {
            "existing_tags": ["MCP개발", "재고", "재고"],
            "new_tags": ["MCP 개발", "배치자동화", "배치 자동화"],
        }


def test_generate_tags_prefers_existing_tags_and_filters_duplicates() -> None:
    service = TaggingService()
    service.chain = DummyTaggingChain()

    result = asyncio.run(
        service.generate_tags(
            work_content="MCP 개발 작업과 재고 동기화 배치를 개선했다.",
            existing_tags=[
                MetaTag(tagId=1, tagName="재고"),
                MetaTag(tagId=2, tagName="MCP 개발"),
            ],
        )
    )

    assert result.existing_tags == ["MCP 개발", "재고"]
    assert result.new_tags == ["배치자동화"]
    assert result.tag_names == ["MCP 개발", "재고", "배치자동화"]
