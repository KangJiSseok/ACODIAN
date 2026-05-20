"""LightRAG v3 업무일지 source row reader."""

from app.light.v3.store.worklog_source_store import (
    LightWorklogSourceRow,
    LightWorklogSourceStore,
)
from app.store.session import get_session_factory


class WorklogLightSourceRowReader:
    """업무일지 ID 목록을 source row로 조회한다."""

    async def fetch_source_rows(self, worklog_ids: list[int]) -> dict[int, LightWorklogSourceRow]:
        """존재하는 업무일지만 text/custom KG source row로 batch 반환한다."""
        source_store = LightWorklogSourceStore()
        async with get_session_factory()() as session:
            return await source_store.fetch_worklog_sources(session, worklog_ids)
