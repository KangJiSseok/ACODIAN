"""LightRAG custom KG 확정 관계 source reader."""

from dataclasses import dataclass

from app.light.v3.store.worklog_custom_kg_source_store import (
    LightWorklogCustomKgPredecessor,
    LightWorklogCustomKgSourceRow,
    LightWorklogCustomKgSourceStore,
    LightWorklogCustomKgTag,
)
from app.store.session import get_session_factory


@dataclass(frozen=True)
class LightRagCustomKgTag:
    """custom KG source contract에 포함할 태그."""

    tag_id: int
    tag_name: str
    description: str | None


@dataclass(frozen=True)
class LightRagCustomKgPredecessor:
    """custom KG source contract에 포함할 직접 선행 업무."""

    worklog_id: int
    title: str


@dataclass(frozen=True)
class LightRagCustomKgSource:
    """업무일지 1건의 LightRAG custom KG source contract."""

    worklog_id: int
    title: str
    author_id: int
    author_name: str
    team_id: int
    team_name: str
    tags: list[LightRagCustomKgTag]
    direct_predecessors: list[LightRagCustomKgPredecessor]


class WorklogCustomKgSourceReader:
    """업무일지 ID 목록을 LightRAG custom KG source contract로 조회한다."""

    async def fetch_sources(self, worklog_ids: list[int]) -> dict[int, LightRagCustomKgSource]:
        """존재하는 업무일지만 custom KG source로 반환한다."""
        source_store = LightWorklogCustomKgSourceStore()
        async with get_session_factory()() as session:
            sources: dict[int, LightRagCustomKgSource] = {}
            for worklog_id in worklog_ids:
                source_row = await source_store.fetch_custom_kg_source(session, worklog_id)
                if source_row is not None:
                    sources[worklog_id] = to_light_worklog_custom_kg_source(source_row)
            return sources


def to_light_worklog_custom_kg_source(
    source: LightWorklogCustomKgSourceRow,
) -> LightRagCustomKgSource:
    """store row를 custom KG source contract로 변환한다."""
    return LightRagCustomKgSource(
        worklog_id=source.worklog_id,
        title=source.title,
        author_id=source.author_id,
        author_name=source.author_name,
        team_id=source.team_id,
        team_name=source.team_name,
        tags=[
            LightRagCustomKgTag(
                tag_id=tag.tag_id,
                tag_name=tag.tag_name,
                description=tag.description,
            )
            for tag in source.tags
        ],
        direct_predecessors=[
            LightRagCustomKgPredecessor(
                worklog_id=predecessor.worklog_id,
                title=predecessor.title,
            )
            for predecessor in source.direct_predecessors
        ],
    )
