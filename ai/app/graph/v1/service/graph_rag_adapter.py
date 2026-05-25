"""GraphRAG v1 store lifecycle adapter."""

from __future__ import annotations

from app.config.settings import Settings, settings
from app.graph.v1.service.worklog_graph_builder import WorklogGraphDocument
from app.graph.v1.store.graph_rag_store import GraphRagStore, GraphRagSubgraph


class GraphRagAdapter:
    """Service 계층이 사용하는 GraphRAG graph store adapter."""

    def __init__(self, *, store: GraphRagStore | None = None, settings_obj: Settings = settings) -> None:
        self._settings = settings_obj
        self._store = store or GraphRagStore(settings_obj=settings_obj)

    async def index_document(self, document: WorklogGraphDocument) -> None:
        """업무일지 graph document 1건을 index한다."""
        await self._store.upsert_worklog_graph(document)

    async def fetch_worklog_subgraph(
        self,
        *,
        query: str,
        allowed_team_ids: list[int] | None,
        max_depth: int,
        limit: int,
    ) -> GraphRagSubgraph:
        """업무일지 GraphRAG subgraph를 조회한다."""
        return await self._store.fetch_worklog_subgraph(
            query=query,
            allowed_team_ids=allowed_team_ids,
            max_depth=max_depth,
            limit=limit,
        )

    async def close(self) -> None:
        """GraphRAG store lifecycle을 정리한다."""
        await self._store.close()


_graph_rag_worklog_adapter: GraphRagAdapter | None = None


def get_graph_rag_worklog_adapter() -> GraphRagAdapter:
    """GraphRAG 업무일지 adapter singleton을 반환한다."""
    global _graph_rag_worklog_adapter
    if _graph_rag_worklog_adapter is None:
        _graph_rag_worklog_adapter = GraphRagAdapter()
    return _graph_rag_worklog_adapter


async def close_graph_rag_worklog_adapter() -> None:
    """GraphRAG 업무일지 adapter singleton lifecycle을 종료한다."""
    global _graph_rag_worklog_adapter
    if _graph_rag_worklog_adapter is not None:
        await _graph_rag_worklog_adapter.close()
        _graph_rag_worklog_adapter = None
