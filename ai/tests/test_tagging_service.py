import asyncio

from app.model.tagging_model import MetaTag
from app.service.tagging_service import TaggingService


class DummyTaggingChain:
    async def ainvoke(self, payload: dict[str, str]) -> dict[str, list[object]]:
        assert '"tagName": "재고"' in payload["existing_tags"]
        assert '"description": "재고 동기화 업무"' in payload["existing_tags"]
        assert payload["work_content"] == "MCP 개발 작업과 재고 동기화 배치를 개선했다."
        return {
            "existing_tags": ["MCP개발", "재고", "재고"],
            "new_tags": [
                {"tagName": "MCP 개발", "description": "기존 태그와 중복되는 설명"},
                {"tagName": "배치자동화", "description": "정기 배치 자동화 개선 업무에 사용하는 태그"},
                {"tagName": "배치 자동화", "description": "중복 신규 태그"},
            ],
        }


def test_generate_tags_prefers_existing_tags_and_filters_duplicates() -> None:
    service = TaggingService()
    service.chain = DummyTaggingChain()

    result = asyncio.run(
        service.generate_tags(
            work_content="MCP 개발 작업과 재고 동기화 배치를 개선했다.",
            existing_tags=[
                MetaTag(tagId=1, tagName="재고", description="재고 동기화 업무", usageCount=3),
                MetaTag(tagId=2, tagName="MCP 개발", description="MCP 기능 개발 업무", usageCount=2),
            ],
        )
    )

    assert result.existing_tags == ["MCP 개발", "재고"]
    assert [tag.tag_name for tag in result.new_tags] == ["배치자동화"]
    assert result.new_tags[0].description == "정기 배치 자동화 개선 업무에 사용하는 태그"
    assert result.tag_names == ["MCP 개발", "재고", "배치자동화"]
