"""LightRAG v3 업무일지 source reader.

기존 임베딩 source 조회 SQL은 `EmbeddingStore`가 계속 소유한다. 이 모듈은 그 결과를
LightRAG index 전용 source contract로 변환해 후속 adapter/service 변경이 기존
embedding DTO에 직접 의존하지 않게 한다.
"""

from dataclasses import dataclass

from app.store.embedding_store import EmbeddingStore, WorklogEmbeddingSource
from app.store.session import get_session_factory


"""frozen=true : class 만든 후 변경 불가"""
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
    department_id: int
    predecessor_titles: list[str]
    predecessor_worklog_ids: list[int]
    tag_ids: list[int]


class WorklogLightSourceReader:
    """업무일지 ID 목록을 LightRAG source contract로 조회한다."""

    async def fetch_sources(self, worklog_ids: list[int]) -> dict[int, LightRagWorklogSource]:
        """존재하는 업무일지만 LightRAG source로 반환한다."""
        embedding_store = EmbeddingStore()
        async with get_session_factory()() as session:
            sources: dict[int, LightRagWorklogSource] = {}
            for worklog_id in worklog_ids:
                embedding_source = await embedding_store.fetch_worklog_source(
                    session,
                    worklog_id,
                )
                if embedding_source is not None:
                    sources[worklog_id] = to_light_worklog_source(embedding_source)
            return sources


def to_light_worklog_source(source: WorklogEmbeddingSource) -> LightRagWorklogSource:
    """임베딩 source DTO를 LightRAG source contract로 변환한다."""
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
        department_id=source.department_id,
        predecessor_titles=list(source.predecessor_titles),
        predecessor_worklog_ids=list(source.predecessor_worklog_ids),
        tag_ids=list(source.tag_ids),
    )
