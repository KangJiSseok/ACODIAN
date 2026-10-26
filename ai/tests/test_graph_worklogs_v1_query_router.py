from __future__ import annotations

from typing import Any

from fastapi.testclient import TestClient

from app.graph.v1.model.worklog_query import (
    WorklogGraphEdgeItem,
    WorklogGraphNodeItem,
    WorklogGraphQueryResponse,
    WorklogGraphReferenceItem,
)
from app.config.settings import Settings
from app.graph.v1.service.worklog_query_service import GraphWorklogQueryService
from app.graph.v1.store.graph_rag_store import (
    GraphRagConfigurationError,
    GraphRagQueryFailedError,
    GraphRagQueryTimeoutError,
)
from app.main import app
from tests.graph_query_service_fixture import FakeCorpusStore, FakeGeminiClient, FakeSessionContext, FakeGraphAdapter, FakeRouter

client = TestClient(app)


class FakeQueryService:
    def __init__(self, response: WorklogGraphQueryResponse | Exception | None = None) -> None:
        self.calls: list[Any] = []
        self.response = response or WorklogGraphQueryResponse(
            answer=None,
            nodes=[
                WorklogGraphNodeItem(
                    nodeId="worklog:101",
                    label="Worklog",
                    sourceWorklogIds=[101],
                    sourceTeamIds=[7],
                    properties={},
                )
            ],
            edges=[
                WorklogGraphEdgeItem(
                    edgeId="e1",
                    sourceId="worklog:101",
                    targetId="team:7",
                    type="BELONGS_TO",
                    sourceWorklogIds=[101],
                    sourceTeamIds=[7],
                    properties={},
                )
            ],
            references=[
                WorklogGraphReferenceItem(
                    referenceId="worklog-101",
                    filePath="worklog://101",
                    sourceWorklogIds=[101],
                    sourceTeamIds=[7],
                )
            ],
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


def install_authorized_service(
    monkeypatch,
) -> tuple[GraphWorklogQueryService, FakeGeminiClient, FakeCorpusStore]:
    from app.graph.v1.router import worklog_query

    gemini = FakeGeminiClient()
    corpus_store = FakeCorpusStore()
    service = GraphWorklogQueryService(
        settings_obj=Settings(_env_file=None),
        session_factory=lambda: lambda: FakeSessionContext(),
        gemini_client_factory=lambda: gemini,
        corpus_store=corpus_store,
        graph_adapter=FakeGraphAdapter(),
        router=FakeRouter(),
    )
    monkeypatch.setattr(worklog_query, "worklog_query_service", service)
    return service, gemini, corpus_store


def post_query(payload: dict[str, Any] | None = None):
    return client.post(
        "/ai/graph/worklogs-v1/query",
        json=payload or {"query": "정산", "scopeMode": "TEAM_IDS", "allowedTeamIds": [7]},
    )


def test_graph_worklogs_v1_query_contract(monkeypatch) -> None:
    service = FakeQueryService()
    install_fake_service(monkeypatch, service)

    response = post_query(
        {
            "query": "정산",
            "scopeMode": "TEAM_IDS",
            "allowedTeamIds": [7],
            "maxDepth": 2,
            "limit": 5,
        }
    )

    assert response.status_code == 200
    assert response.json() == {
        "answer": None,
        "nodes": [
            {
                "nodeId": "worklog:101",
                "label": "Worklog",
                "sourceWorklogIds": [101],
                "sourceTeamIds": [7],
                "properties": {},
            }
        ],
        "edges": [
            {
                "edgeId": "e1",
                "sourceId": "worklog:101",
                "targetId": "team:7",
                "type": "BELONGS_TO",
                "sourceWorklogIds": [101],
                "sourceTeamIds": [7],
                "properties": {},
            }
        ],
        "references": [
            {
                "referenceId": "worklog-101",
                "filePath": "worklog://101",
                "sourceWorklogIds": [101],
                "sourceTeamIds": [7],
            }
        ],
        "mode": "graph",
        "internalOnly": True,
    }
    assert service.calls[0].allowed_team_ids == [7]
    assert service.calls[0].max_depth == 2
    assert service.calls[0].limit == 5


def test_route_cross_team_predecessor_shared_author_shared_tag_metadata_leak_reference_leak_answer_leak(
    monkeypatch,
) -> None:
    service, gemini, corpus_store = install_authorized_service(monkeypatch)

    response = post_query({"query": "정산", "scopeMode": "TEAM_IDS", "allowedTeamIds": [7]})

    assert response.status_code == 200
    payload = response.json()
    captured_context = service.captured_context_snapshot
    assert captured_context is not None
    assert payload["answer"] == "허용팀 정산 답변"
    assert corpus_store.filtered_out_worklog_ids == [801]
    assert corpus_store.filtered_out_predecessor_ids == [801]
    assert corpus_store.calls[0]["request"].scope_mode.value == "TEAM_IDS"
    for blocked_token in ("비허용팀", "비허용작성자", "비허용태그", "worklog://801"):
        assert blocked_token not in str(captured_context)
        assert blocked_token not in str(payload)
        assert blocked_token not in gemini.generate_text_calls[0]
    assert "허용팀 선행업무" in str(captured_context)
    assert "허용팀 선행업무" in gemini.generate_text_calls[0]
    assert {reference["filePath"] for reference in payload["references"]} == {
        "worklog://701",
        "worklog://702",
    }
    assert all(set(node["sourceTeamIds"]).issubset({7}) for node in payload["nodes"])
    assert all(set(edge["sourceTeamIds"]).issubset({7}) for edge in payload["edges"])


def test_route_all_scope_explicit_includes_disallowed_fixture_when_scopeMode_ALL(monkeypatch) -> None:
    service, gemini, corpus_store = install_authorized_service(monkeypatch)

    response = post_query({"query": "정산", "scopeMode": "ALL"})

    assert response.status_code == 200
    payload = response.json()
    assert payload["answer"] == "허용팀 정산 답변"
    assert service.captured_context_snapshot is not None
    assert gemini.embed_calls == [["정산"]]
    assert corpus_store.filtered_out_worklog_ids == []
    assert corpus_store.filtered_out_predecessor_ids == []
    assert corpus_store.calls[0]["request"].scope_mode.value == "ALL"
    assert "비허용팀 선행업무" in str(service.captured_context_snapshot)
    assert {reference["filePath"] for reference in payload["references"]} == {
        "worklog://701",
        "worklog://702",
        "worklog://801",
    }


def test_scopeMode_team_ids_contract_is_required_before_graph_query(monkeypatch) -> None:
    service = FakeQueryService()
    install_fake_service(monkeypatch, service)

    response = post_query({"query": "정산", "scopeMode": "TEAM_IDS", "allowedTeamIds": [7]})

    assert response.status_code == 200
    assert service.calls[0].scope_mode.value == "TEAM_IDS"
    assert service.calls[0].allowed_team_ids == [7]


def test_null_scope_rejected_and_missing_empty_team_scope_are_rejected(monkeypatch) -> None:
    service = FakeQueryService()
    install_fake_service(monkeypatch, service)

    assert post_query({"query": "정산"}).status_code == 422
    assert post_query({"query": "정산", "scopeMode": "TEAM_IDS"}).status_code == 422
    assert post_query(
        {"query": "정산", "scopeMode": "TEAM_IDS", "allowedTeamIds": []}
    ).status_code == 422
    assert post_query(
        {"query": "정산", "scopeMode": "TEAM_IDS", "allowedTeamIds": None}
    ).status_code == 422
    assert post_query(
        {"query": "정산", "scopeMode": "ALL", "allowedTeamIds": [7]}
    ).status_code == 422
    assert service.calls == []


def test_scopeMode_all_contract_accepts_absent_team_ids(monkeypatch) -> None:
    service = FakeQueryService()
    install_fake_service(monkeypatch, service)

    response = post_query({"query": "정산", "scopeMode": "ALL"})

    assert response.status_code == 200
    assert service.calls[0].scope_mode.value == "ALL"
    assert service.calls[0].allowed_team_ids is None


def test_graph_worklogs_v1_query_rejects_extra_empty_and_snake_case(monkeypatch) -> None:
    service = FakeQueryService()
    install_fake_service(monkeypatch, service)

    assert post_query({"query": ""}).status_code == 422
    assert post_query(
        {"query": "ok", "scopeMode": "TEAM_IDS", "allowedTeamIds": [7], "stream": True}
    ).status_code == 422
    assert post_query(
        {"query": "ok", "scopeMode": "TEAM_IDS", "allowed_team_ids": [7]}
    ).status_code == 422
    assert service.calls == []


def test_graph_worklogs_v1_query_maps_store_errors(monkeypatch) -> None:
    cases = [
        (GraphRagConfigurationError("missing"), 500, "GRAPHRAG_CONFIGURATION_ERROR"),
        (GraphRagQueryTimeoutError("timeout"), 504, "GRAPHRAG_QUERY_TIMEOUT"),
        (GraphRagQueryFailedError("failed"), 500, "GRAPHRAG_QUERY_FAILED"),
    ]
    for exc, status_code, detail in cases:
        install_fake_service(monkeypatch, FakeQueryService(exc))
        response = post_query({"query": "ok", "scopeMode": "TEAM_IDS", "allowedTeamIds": [7]})
        assert response.status_code == status_code
        assert response.json() == {"detail": detail}
