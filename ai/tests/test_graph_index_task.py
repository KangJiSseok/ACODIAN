import asyncio

from app.graph.v1.model.worklog_index import WorklogGraphIndexItem, WorklogGraphIndexResponse
from app.task import graph_index_tasks


def test_graph_index_task_closes_async_connections_before_next_event_loop(monkeypatch) -> None:
    closed = []

    class Service:
        async def index_worklogs(self, ids):
            assert ids == [101]
            return WorklogGraphIndexResponse(items=[WorklogGraphIndexItem(worklogId=101, indexed=True)])

    async def close_postgres():
        closed.append("postgres")

    async def close_graph():
        closed.append("neo4j")

    monkeypatch.setattr(graph_index_tasks, "GraphWorklogIndexService", Service)
    monkeypatch.setattr(graph_index_tasks, "close_graph_rag_worklog_adapter", close_graph)
    monkeypatch.setattr(graph_index_tasks, "dispose_engine", close_postgres)
    result = asyncio.run(graph_index_tasks._index_and_close(101))
    assert result.items[0].indexed
    assert closed == ["neo4j", "postgres"]
