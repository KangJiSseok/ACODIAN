import asyncio

from app.model.tagging_model import MetaTag, TaggingResult
from app.task import tagging_tasks


class FakeTaggingClient:
    def __init__(self) -> None:
        self.updated_tag_ids: list[int] | None = None

    async def list_tags(self) -> list[MetaTag]:
        return [
            MetaTag(tagId=1, tagName="재고"),
            MetaTag(tagId=2, tagName="MCP 개발"),
        ]

    async def create_tag(self, tag_name: str) -> MetaTag:
        assert tag_name == "배치자동화"
        return MetaTag(tagId=3, tagName=tag_name)

    async def update_worklog_tags(self, worklog_id: int, tag_ids: list[int]) -> None:
        assert worklog_id == 10
        self.updated_tag_ids = tag_ids


class FakeTaggingService:
    async def generate_tags(
        self,
        work_content: str,
        existing_tags: list[MetaTag],
    ) -> TaggingResult:
        assert work_content == "재고 배치 자동화 작업"
        assert [tag.tag_name for tag in existing_tags] == ["재고", "MCP 개발"]
        return TaggingResult(
            existing_tags=["재고"],
            new_tags=["배치자동화"],
        )


def test_generate_worklog_tags_creates_new_tags_and_updates_worklog(monkeypatch) -> None:
    fake_client = FakeTaggingClient()
    monkeypatch.setattr(tagging_tasks, "TaggingClient", lambda: fake_client)
    monkeypatch.setattr(tagging_tasks, "TaggingService", FakeTaggingService)

    result = asyncio.run(
        tagging_tasks._generate_worklog_tags(
            worklog_id=10,
            work_content="재고 배치 자동화 작업",
        )
    )

    assert fake_client.updated_tag_ids == [1, 3]
    assert result == {
        "worklog_id": 10,
        "tag_ids": [1, 3],
        "existing_tags": ["재고"],
        "new_tags": ["배치자동화"],
    }
