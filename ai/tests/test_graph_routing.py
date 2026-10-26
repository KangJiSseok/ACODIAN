import asyncio

import httpx
from fastapi.testclient import TestClient

from app.config.settings import Settings
from app.graph.v1.service.jev_router import BOTH, JevRouter, SearchRoute
from app.graph.v1.store.graph_rag_store import GraphRagStore, GraphWorklogLink
from app.main import create_app


def test_jev_decides_two_paths_and_falls_back_on_uncertainty(monkeypatch) -> None:
    original = httpx.AsyncClient
    transport = httpx.MockTransport(lambda request: httpx.Response(200, json={"answers": {
        "relation": {"noul": 0.94}, "similarity": {"noul": 0.04}
    }}))
    monkeypatch.setattr(httpx, "AsyncClient", lambda **kwargs: original(transport=transport))
    router = JevRouter(Settings(_env_file=None, jev_url="http://jev.test"))
    assert asyncio.run(router.classify("A 업무의 선행 업무 담당자")) == SearchRoute(True, False)

    uncertain = httpx.MockTransport(lambda request: httpx.Response(200, json={"answers": {
        "relation": {"noul": 0.5}, "similarity": {"noul": 0.9}
    }}))
    monkeypatch.setattr(httpx, "AsyncClient", lambda **kwargs: original(transport=uncertain))
    assert asyncio.run(router.classify("모호한 질문")) == BOTH

    failed = httpx.MockTransport(lambda request: httpx.Response(503))
    monkeypatch.setattr(httpx, "AsyncClient", lambda **kwargs: original(transport=failed))
    assert asyncio.run(router.classify("라우터 장애")) == BOTH


def test_related_ids_are_parameterized_and_scoped() -> None:
    class Result:
        def __aiter__(self):
            self.rows = iter([{"sourceId": 10, "targetId": 11, "relation": "DEPENDS_ON"}])
            return self

        async def __anext__(self):
            try:
                return next(self.rows)
            except StopIteration as exc:
                raise StopAsyncIteration from exc

    class Tx:
        async def run(self, cypher, **kwargs):
            assert "DROP" not in cypher
            assert kwargs == {"root_ids": [10], "allowed_team_ids": [7], "limit": 5}
            assert "all(t IN teams WHERE t IN $allowed_team_ids)" in cypher
            assert "authorEdge:GRAPH_RAG_EDGE {type:'AUTHORED_BY'}" in cypher
            return Result()

    assert asyncio.run(GraphRagStore._query_worklog_links(Tx(), [10], [7], 5)) == [
        GraphWorklogLink(10, 11, "DEPENDS_ON")
    ]


def test_graph_index_queue_accepts_request(monkeypatch) -> None:
    from app.graph.v1.router import worklog_index

    graph_ids = []
    embedding_ids = []
    monkeypatch.setattr(worklog_index.index_graph_worklog, "delay", lambda id_, at: graph_ids.append(id_))
    monkeypatch.setattr(worklog_index.generate_worklog_embedding, "delay", lambda id_: embedding_ids.append(id_))
    with TestClient(create_app()) as client:
        response = client.post(
            "/ai/graph/worklogs-v1/index-requests",
            json={"worklogIds": [10], "refreshEmbedding": True},
        )
    assert response.status_code == 202
    assert response.json() == {"queued": 1}
    assert graph_ids == embedding_ids == [10]
