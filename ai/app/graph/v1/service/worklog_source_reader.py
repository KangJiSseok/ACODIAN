"""GraphRAG v1 업무일지 source row reader."""

from app.graph.v1.store.worklog_source_store import GraphWorklogSourceRow, GraphWorklogSourceStore
from app.store.session import get_session_factory


class WorklogGraphSourceRowReader:
    """업무일지 ID 목록을 GraphRAG source row로 조회한다."""

    async def fetch_source_rows(self, worklog_ids: list[int]) -> dict[int, GraphWorklogSourceRow]:
        """존재하는 업무일지만 GraphRAG source row로 batch 반환한다."""
        source_store = GraphWorklogSourceStore()
        async with get_session_factory()() as session:
            return await source_store.fetch_worklog_sources(session, worklog_ids)
