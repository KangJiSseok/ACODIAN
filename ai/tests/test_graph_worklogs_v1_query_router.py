from __future__ import annotations

from typing import Any

from fastapi.testclient import TestClient

from app.graph.v1.model.worklog_query import (
    WorklogGraphEdgeItem,
    WorklogGraphNodeItem,
    WorklogGraphQueryResponse,
    WorklogGraphReferenceItem,
)
from app.graph.v1.store.graph_rag_store import GraphRagConfigurationError, GraphRagQueryFailedError, GraphRagQueryTimeoutError
from app.main import app

client = TestClient(app)


class FakeQueryService:
    def __init__(self, response: WorklogGraphQueryResponse | Exception | None = None) -> None:
        self.calls: list[Any] = []
        self.response = response or WorklogGraphQueryResponse(
            answer=None,
            nodes=[WorklogGraphNodeItem(nodeId="worklog:101", label="Worklog", properties={})],
            edges=[
                WorklogGraphEdgeItem(
                    edgeId="e1",
                    sourceId="worklog:101",
                    targetId="team:7",
                    type="BELONGS_TO",
                    properties={},
                )
            ],
            references=[WorklogGraphReferenceItem(referenceId="worklog-101", filePath="worklog://101")],
            internalOnly=True,
        )

    async def query_worklogs(self, request: Any) -> WorklogGraphQueryResponse:
        self.calls.append(request)
        if isinstance(self.response, Exception):
            raise self.response
        return self.response


def install_fake_service(monkeypatch, service: FakeQueryService) -> None:
    from app.graph.v1.router import worklog_query

    monkeypatch.setattr(worklog_query, "worklog_query_service", service)


def post_query(payload: dict[str, Any] | None = None):
    return client.post("/ai/graph/worklogs-v1/query", json=payload or {"query": "정산"})


def test_graph_worklogs_v1_query_contract(monkeypatch) -> None:
    service = FakeQueryService()
    install_fake_service(monkeypatch, service)

    response = post_query({"query": "정산", "allowedTeamIds": [7], "maxDepth": 2, "limit": 5})

    assert response.status_code == 200
    assert response.json() == {
        "answer": None,
        "nodes": [{"nodeId": "worklog:101", "label": "Worklog", "properties": {}}],
        "edges": [
            {
                "edgeId": "e1",
                "sourceId": "worklog:101",
                "targetId": "team:7",
                "type": "BELONGS_TO",
                "properties": {},
            }
        ],
        "references": [{"referenceId": "worklog-101", "filePath": "worklog://101"}],
        "mode": "graph",
        "internalOnly": True,
    }
    assert service.calls[0].allowed_team_ids == [7]
    assert service.calls[0].max_depth == 2
    assert service.calls[0].limit == 5


def test_graph_worklogs_v1_query_rejects_extra_empty_and_snake_case(monkeypatch) -> None:
    service = FakeQueryService()
    install_fake_service(monkeypatch, service)

    assert post_query({"query": ""}).status_code == 422
    assert post_query({"query": "ok", "stream": True}).status_code == 422
    assert post_query({"query": "ok", "allowed_team_ids": [7]}).status_code == 422
    assert service.calls == []


def test_graph_worklogs_v1_query_maps_store_errors(monkeypatch) -> None:
    cases = [
        (GraphRagConfigurationError("missing"), 500, "GRAPHRAG_CONFIGURATION_ERROR"),
        (GraphRagQueryTimeoutError("timeout"), 504, "GRAPHRAG_QUERY_TIMEOUT"),
        (GraphRagQueryFailedError("failed"), 500, "GRAPHRAG_QUERY_FAILED"),
    ]
    for exc, status_code, detail in cases:
        install_fake_service(monkeypatch, FakeQueryService(exc))
        response = post_query({"query": "ok"})
        assert response.status_code == status_code
        assert response.json() == {"detail": detail}
