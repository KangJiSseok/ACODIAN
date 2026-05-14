import asyncio

from app.model.tagging_model import MetaTag, TaggingResult
from app.task import tagging_tasks


class FakeTaggingClient:
    def __init__(self) -> None:
        self.created_request: tuple[int, list[int], list[str]] | None = None

    async def __aenter__(self) -> "FakeTaggingClient":
        return self

    async def __aexit__(self, exc_type, exc, traceback) -> None:
        return None

    async def list_tags(self) -> list[MetaTag]:
        return [
            MetaTag(tagId=1, tagName="재고"),
            MetaTag(tagId=2, tagName="MCP 개발"),
        ]

    async def apply_worklog_ai_tags(
        self,
        worklog_id: int,
        existing_tag_ids: list[int],
        new_tag_names: list[str],
    ) -> None:
        assert worklog_id == 10
        assert existing_tag_ids == [1]
        assert new_tag_names == ["배치자동화"]
        self.created_request = (worklog_id, existing_tag_ids, new_tag_names)


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

    assert fake_client.created_request == (10, [1], ["배치자동화"])
    assert result == {
        "worklog_id": 10,
        "existing_tags": ["재고"],
        "new_tags": ["배치자동화"],
    }
