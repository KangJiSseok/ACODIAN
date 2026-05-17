"""LightRAG v3 업무일지 text source reader.

LightRAG v3 source 조회 SQL은 v3-local store가 소유한다. 이 모듈은 그 결과 중
text document 생성에 필요한 필드만 reader contract로 변환해 document builder가
전역 embedding store와 custom KG 문맥에 직접 의존하지 않게 한다.
"""

from dataclasses import dataclass

from app.light.v3.store.worklog_source_store import (
    LightWorklogSourceRow,
    LightWorklogSourceStore,
)
from app.store.session import get_session_factory


@dataclass(frozen=True)
class LightRagWorklogSource:
    """LightRAG 업무일지 text document 생성을 위한 source contract."""

    worklog_id: int
    title: str
    request_content: str | None
    work_content: str


class WorklogLightSourceReader:
    """업무일지 ID 목록을 LightRAG text source contract로 조회한다."""

    async def fetch_source_rows(self, worklog_ids: list[int]) -> dict[int, LightWorklogSourceRow]:
        """존재하는 업무일지만 store row로 batch 반환한다.

        통합 `/index` 유스케이스는 같은 row에서 text/custom KG source를 각각 projection해
        단일 source 조회 흐름을 유지한다.
        """
        source_store = LightWorklogSourceStore()
        async with get_session_factory()() as session:
            return await source_store.fetch_worklog_sources(session, worklog_ids)

    async def fetch_sources(self, worklog_ids: list[int]) -> dict[int, LightRagWorklogSource]:
        """존재하는 업무일지만 LightRAG text source로 batch 반환한다."""
        source_rows = await self.fetch_source_rows(worklog_ids)
        return {
            worklog_id: to_light_worklog_source(source_row)
            for worklog_id, source_row in source_rows.items()
        }


def to_light_worklog_source(source: LightWorklogSourceRow) -> LightRagWorklogSource:
    """LightRAG v3 source row를 text document source contract로 변환한다."""
    return LightRagWorklogSource(
        worklog_id=source.worklog_id,
        title=source.title,
        request_content=source.request_content,
        work_content=source.work_content,
    )
