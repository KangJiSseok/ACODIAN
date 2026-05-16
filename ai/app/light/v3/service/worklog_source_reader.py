"""LightRAG v3 업무일지 source reader.

LightRAG v3 source 조회 SQL은 v3-local store가 소유한다. 이 모듈은 그 결과를
LightRAG index 전용 source contract로 변환해 후속 adapter/service가 전역
embedding store에 직접 의존하지 않게 한다.
"""

from dataclasses import dataclass

from app.light.v3.store.worklog_source_store import (
    LightWorklogSourceRow,
    LightWorklogSourceStore,
)
from app.store.session import get_session_factory


"""frozen=true : class 만든 후 변경 불가"""
@dataclass(frozen=True)
class LightRagWorklogTag:
    """LightRAG source contract에 포함할 태그 문맥."""

    tag_id: int
    tag_name: str
    description: str | None


@dataclass(frozen=True)
class LightRagWorklogSource:
    """LightRAG 업무일지 KG 생성을 위한 원문/문맥 source contract."""

    worklog_id: int
    title: str
    request_content: str | None
    work_content: str
    author_id: int
    author_name: str
    author_role: str
    team_id: int
    team_name: str
    predecessor_titles: list[str]
    predecessor_worklog_ids: list[int]
    tag_ids: list[int]
    tags: list[LightRagWorklogTag]


class WorklogLightSourceReader:
    """업무일지 ID 목록을 LightRAG source contract로 조회한다."""

    async def fetch_sources(self, worklog_ids: list[int]) -> dict[int, LightRagWorklogSource]:
        """존재하는 업무일지만 LightRAG source로 반환한다."""
        source_store = LightWorklogSourceStore()
        async with get_session_factory()() as session:
            sources: dict[int, LightRagWorklogSource] = {}
            for worklog_id in worklog_ids:
                source_row = await source_store.fetch_worklog_source(
                    session,
                    worklog_id,
                )
                if source_row is not None:
                    sources[worklog_id] = to_light_worklog_source(source_row)
            return sources


def to_light_worklog_source(source: LightWorklogSourceRow) -> LightRagWorklogSource:
    """LightRAG v3 source row를 LightRAG source contract로 변환한다."""
    return LightRagWorklogSource(
        worklog_id=source.worklog_id,
        title=source.title,
        request_content=source.request_content,
        work_content=source.work_content,
        author_id=source.author_id,
        author_name=source.author_name,
        author_role=source.author_role,
        team_id=source.team_id,
        team_name=source.team_name,
        predecessor_titles=list(source.predecessor_titles),
        predecessor_worklog_ids=list(source.predecessor_worklog_ids),
        tag_ids=list(source.tag_ids),
        tags=[
            LightRagWorklogTag(
                tag_id=tag.tag_id,
                tag_name=tag.tag_name,
                description=tag.description,
            )
            for tag in source.tags
        ],
    )
