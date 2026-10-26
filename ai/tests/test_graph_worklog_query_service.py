import asyncio

import pytest
from pydantic import ValidationError

from app.config.settings import Settings
from app.graph.v1.model.worklog_query import (
    WorklogGraphEdgeItem,
    WorklogGraphNodeItem,
    WorklogGraphQueryRequest,
    WorklogGraphReferenceItem,
)
from app.graph.v1.service.worklog_query_service import GraphWorklogQueryService
from app.graph.v1.service.jev_router import SearchRoute
from app.graph.v1.store.graph_rag_store import GraphRagStore, GraphRagSubgraph, GraphWorklogLink
from app.graph.v1.store.worklog_source_store import GraphWorklogPredecessor, GraphWorklogSourceRow
from tests.graph_legacy_adapter_fixture import FakeAdapter, LeakyAdapter
from tests.graph_query_service_fixture import FakeCorpusStore, FakeGeminiClient, FakeSessionContext, FakeGraphAdapter, FakeRouter


def test_query_service_builds_pre_llm_context_from_authorized_corpus() -> None:
    gemini = FakeGeminiClient()
    corpus_store = FakeCorpusStore()
    session_context = FakeSessionContext()
    service = GraphWorklogQueryService(
        settings_obj=Settings(_env_file=None),
        session_factory=lambda: lambda: session_context,
        gemini_client_factory=lambda: gemini,
        corpus_store=corpus_store,
        graph_adapter=FakeGraphAdapter(),
        router=FakeRouter(),
    )

    response = asyncio.run(
        service.query_worklogs(
            WorklogGraphQueryRequest.model_validate(
                {"query": "정산", "scopeMode": "TEAM_IDS", "allowedTeamIds": [7], "limit": 5}
            )
        )
    )

    assert gemini.embed_calls == [["정산"]]
    assert len(gemini.generate_text_calls) == 1
    assert "허용팀 선행업무" in gemini.generate_text_calls[0]
    assert "비허용팀" not in gemini.generate_text_calls[0]
    assert corpus_store.calls[0]["session"] is session_context.session
    assert corpus_store.calls[0]["query_embedding"] == [0.1, 0.2, 0.3]
    captured_context = service.captured_context_snapshot
    assert captured_context is not None
    assert "비허용팀" not in str(captured_context)
    assert "teamId: 8" not in str(captured_context)
    assert "허용팀 선행업무" in str(captured_context)
    assert all(node.source_team_ids == [7] for node in response.nodes)
    assert all(set(edge.source_team_ids).issubset({7}) for edge in response.edges)
    assert {reference.file_path for reference in response.references} == {
        "worklog://701",
        "worklog://702",
    }
    assert response.answer == "허용팀 정산 답변"


def test_default_query_service_never_calls_legacy_subgraph_adapter(monkeypatch) -> None:
    async def fail_if_legacy_subgraph_is_used(self, **kwargs) -> GraphRagSubgraph:
        raise AssertionError("default query path must use authorized corpus store")

    monkeypatch.setattr(GraphRagStore, "fetch_worklog_subgraph", fail_if_legacy_subgraph_is_used)
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

    response = asyncio.run(
        service.query_worklogs(
            WorklogGraphQueryRequest.model_validate(
                {"query": "정산", "scopeMode": "TEAM_IDS", "allowedTeamIds": [7]}
            )
        )
    )

    assert corpus_store.calls[0]["request"].scope_mode.value == "TEAM_IDS"
    assert response.answer == "허용팀 정산 답변"


def test_query_service_passes_scope_and_builds_response() -> None:
    adapter = FakeAdapter()
    service = GraphWorklogQueryService(
        settings_obj=Settings(
            _env_file=None,
            graphrag_query_max_depth=2,
            graphrag_query_limit=20,
        ),
        adapter_factory=lambda: adapter,
    )

    response = asyncio.run(
        service.query_worklogs(
            WorklogGraphQueryRequest.model_validate(
                {
                    "query": "정산",
                    "scopeMode": "TEAM_IDS",
                    "allowedTeamIds": [7],
                    "maxDepth": 1,
                    "limit": 5,
                }
            )
        )
    )

    assert adapter.calls == [
        {"query": "정산", "allowed_team_ids": [7], "max_depth": 1, "limit": 5}
    ]
    assert response.answer is None
    assert response.nodes[0].node_id == "worklog:101"
    assert response.nodes[0].source_worklog_ids == [101]
    assert response.nodes[0].source_team_ids == [7]
    assert response.edges[0].type == "BELONGS_TO"
    assert response.edges[0].source_worklog_ids == [101]
    assert response.edges[0].source_team_ids == [7]
    assert response.references[0].reference_id == "worklog-101"
    assert response.references[0].file_path == "worklog://101"
    assert response.references[0].source_worklog_ids == [101]
    assert response.references[0].source_team_ids == [7]


def test_cross_team_predecessor_and_captured_context_are_excluded_before_response() -> None:
    adapter = LeakyAdapter()
    service = GraphWorklogQueryService(
        settings_obj=Settings(
            _env_file=None,
            graphrag_query_max_depth=2,
            graphrag_query_limit=20,
        ),
        adapter_factory=lambda: adapter,
    )

    response = asyncio.run(
        service.query_worklogs(
            WorklogGraphQueryRequest.model_validate(
                {
                    "query": "정산",
                    "scopeMode": "TEAM_IDS",
                    "allowedTeamIds": [7],
                    "maxDepth": 1,
                    "limit": 5,
                }
            )
        )
    )

    captured_context = response.model_dump(mode="json", by_alias=True)
    assert "비허용팀" not in str(captured_context)
    assert "비허용팀 선행업무" not in str(captured_context)
    assert "worklog://801" not in str(captured_context)
    assert all(node.properties.get("teamId") != 8 for node in response.nodes)
    assert all(node.source_worklog_ids for node in response.nodes)
    assert all(edge.target_id != "worklog:801" for edge in response.edges)


def test_all_scope_still_excludes_objects_without_complete_provenance() -> None:
    adapter = LeakyAdapter()
    service = GraphWorklogQueryService(
        settings_obj=Settings(
            _env_file=None,
            graphrag_query_max_depth=2,
            graphrag_query_limit=20,
        ),
        adapter_factory=lambda: adapter,
    )

    response = asyncio.run(
        service.query_worklogs(
            WorklogGraphQueryRequest.model_validate({"query": "정산", "scopeMode": "ALL"})
        )
    )

    captured_context = response.model_dump(mode="json", by_alias=True)
    assert all(node.source_worklog_ids for node in response.nodes)
    assert all(reference.source_worklog_ids for reference in response.references)


def test_response_node_requires_non_empty_provenance() -> None:
    with pytest.raises(ValidationError):
        WorklogGraphNodeItem.model_validate({"nodeId": "worklog:1", "label": "Worklog"})


def test_response_edge_and_reference_require_non_empty_provenance() -> None:
    with pytest.raises(ValidationError):
        WorklogGraphEdgeItem.model_validate(
            {
                "edgeId": "e1",
                "sourceId": "worklog:1",
                "targetId": "team:7",
                "type": "BELONGS_TO",
            }
        )
    with pytest.raises(ValidationError):
        WorklogGraphReferenceItem.model_validate(
            {"referenceId": "worklog-1", "filePath": "worklog://1"}
        )


def test_relation_path_rehydrates_predecessor_author_and_rejects_stale_or_cross_team_links() -> None:
    class RelationRouter:
        async def classify(self, query):
            return SearchRoute(True, False)

    class RelationCorpusStore:
        async def find_exact_title_ids(self, session, title, request):
            assert title == "A 업무"
            return [701]

        async def fetch_sources_by_ids(self, session, ids, request):
            assert 801 in ids
            return {
                701: GraphWorklogSourceRow(
                    701, "A 업무", None, "본문", 5, "김작성", 7, "허용팀", [],
                    [GraphWorklogPredecessor(702, 7, "선행 업무")],
                ),
                702: GraphWorklogSourceRow(
                    702, "선행 업무", None, "선행 본문", 6, "박담당", 7, "허용팀", [], [],
                ),
                703: GraphWorklogSourceRow(
                    703, "오래된 그래프 연결", None, "무관한 본문", 7, "옛담당", 7, "허용팀", [], [],
                ),
            }

    gemini = FakeGeminiClient()
    service = GraphWorklogQueryService(
        settings_obj=Settings(_env_file=None, graphrag_router_mode="active"),
        session_factory=lambda: lambda: FakeSessionContext(),
        gemini_client_factory=lambda: gemini,
        corpus_store=RelationCorpusStore(),
        graph_adapter=FakeGraphAdapter([
            GraphWorklogLink(701, 702, "DEPENDS_ON"),
            GraphWorklogLink(701, 801, "DEPENDS_ON"),
            GraphWorklogLink(701, 703, "DEPENDS_ON"),
        ]),
        router=RelationRouter(),
    )
    response = asyncio.run(service.query_worklogs(WorklogGraphQueryRequest.model_validate(
        {"query": "A 업무의 선행 업무 담당자는 누구야?", "scopeMode": "TEAM_IDS", "allowedTeamIds": [7]}
    )))
    assert gemini.embed_calls == []
    assert "박담당" in gemini.generate_text_calls[0]
    assert "801" not in gemini.generate_text_calls[0]
    assert "오래된 그래프 연결" not in gemini.generate_text_calls[0]
    assert {ref.file_path for ref in response.references} == {"worklog://701", "worklog://702"}


@pytest.mark.parametrize("route", [SearchRoute(True, False), SearchRoute(True, True)])
def test_duplicate_title_does_not_choose_arbitrary_worklog(route: SearchRoute) -> None:
    class RelationRouter:
        async def classify(self, query):
            return route

    class DuplicateStore:
        async def find_exact_title_ids(self, session, title, request):
            return [701, 702]

    gemini = FakeGeminiClient()
    graph = FakeGraphAdapter()
    service = GraphWorklogQueryService(
        settings_obj=Settings(_env_file=None, graphrag_router_mode="active"),
        session_factory=lambda: lambda: FakeSessionContext(),
        gemini_client_factory=lambda: gemini,
        corpus_store=DuplicateStore(),
        graph_adapter=graph,
        router=RelationRouter(),
    )
    response = asyncio.run(service.query_worklogs(WorklogGraphQueryRequest.model_validate(
        {"query": "A 업무의 선행 업무 담당자는?", "scopeMode": "TEAM_IDS", "allowedTeamIds": [7]}
    )))
    assert response.references == []
    assert "정확히 식별" in response.answer
    assert gemini.embed_calls == []
    assert graph.calls == []


def test_dependency_subject_patterns() -> None:
    assert GraphWorklogQueryService._relation_title("A 업무의 선행 업무 담당자는?") == "A 업무"
    assert GraphWorklogQueryService._relation_title("A 업무가 의존하는 업무는?") == "A 업무"
