import pytest

from app.graph.v1.model.worklog_query import WorklogGraphQueryRequest
from app.graph.v1.service.graph_scope_context_builder import (
    GraphScopeContextBuilder,
    GraphScopeContextViolationError,
)
from app.graph.v1.store.graph_authorized_corpus_store import (
    GraphAuthorizedCorpus,
    GraphAuthorizedWorklogCandidate,
)
from app.graph.v1.store.worklog_source_store import (
    GraphWorklogPredecessor,
    GraphWorklogSourceRow,
    GraphWorklogTag,
)


def test_context_builder_uses_only_authorized_corpus_snapshot() -> None:
    context = GraphScopeContextBuilder().build_context(
        corpus=GraphAuthorizedCorpus(
            candidates=[
                GraphAuthorizedWorklogCandidate(
                    worklog_id=701,
                    score=0.9,
                    chunk_index=0,
                    matched_chunk="허용팀 정산 chunk",
                )
            ],
            worklog_ids={701},
            team_ids={7},
            sources={
                701: _source(
                    worklog_id=701,
                    team_id=7,
                    title="허용팀 정산",
                    predecessor=GraphWorklogPredecessor(
                        worklog_id=702,
                        team_id=7,
                        title="허용팀 선행업무",
                    ),
                )
            },
        ),
        request=_team_request(),
    )

    snapshot = context.snapshot
    assert "비허용팀" not in str(snapshot)
    assert "teamId: 8" not in str(snapshot)
    assert "worklog:801" not in str(snapshot)
    assert "공유작성자" in str(snapshot)
    assert "공유태그" in str(snapshot)
    assert {node.node_id for node in context.subgraph.nodes} >= {"worklog:701", "worklog:702"}
    assert all(set(node.source_team_ids).issubset({7}) for node in context.subgraph.nodes)
    assert all(set(edge.source_team_ids).issubset({7}) for edge in context.subgraph.edges)
    assert context.text_units[0].source_worklog_ids == [701]
    assert context.text_units[0].source_team_ids == [7]


def test_context_builder_rejects_cross_team_predecessor_before_llm_context() -> None:
    corpus = GraphAuthorizedCorpus(
        candidates=[],
        worklog_ids={701},
        team_ids={7},
        sources={
            701: _source(
                worklog_id=701,
                team_id=7,
                title="허용팀 정산",
                predecessor=GraphWorklogPredecessor(
                    worklog_id=801,
                    team_id=8,
                    title="비허용팀 선행업무",
                ),
            )
        },
    )

    with pytest.raises(GraphScopeContextViolationError):
        GraphScopeContextBuilder().build_context(corpus=corpus, request=_team_request())


def test_context_builder_merges_shared_nodes_with_provenance_sets() -> None:
    context = GraphScopeContextBuilder().build_context(
        corpus=GraphAuthorizedCorpus(
            candidates=[],
            worklog_ids={701, 702},
            team_ids={7},
            sources={
                701: _source(worklog_id=701, team_id=7, title="정산 A"),
                702: _source(worklog_id=702, team_id=7, title="정산 B"),
            },
        ),
        request=_team_request(),
    )

    user = next(node for node in context.subgraph.nodes if node.node_id == "user:5")
    tag = next(node for node in context.subgraph.nodes if node.node_id == "tag:11")
    assert user.source_worklog_ids == [701, 702]
    assert user.source_team_ids == [7]
    assert user.properties["sourceWorklogIds"] == [701, 702]
    assert tag.source_worklog_ids == [701, 702]


def _team_request() -> WorklogGraphQueryRequest:
    return WorklogGraphQueryRequest.model_validate(
        {"query": "정산", "scopeMode": "TEAM_IDS", "allowedTeamIds": [7], "limit": 5}
    )


def _source(
    *,
    worklog_id: int,
    team_id: int,
    title: str,
    predecessor: GraphWorklogPredecessor | None = None,
) -> GraphWorklogSourceRow:
    return GraphWorklogSourceRow(
        worklog_id=worklog_id,
        title=title,
        request_content="요청",
        work_content="수행",
        author_id=5,
        author_name="공유작성자",
        team_id=team_id,
        team_name=f"team-{team_id}",
        tags=[GraphWorklogTag(tag_id=11, tag_name="공유태그", description=None)],
        direct_predecessors=[predecessor] if predecessor is not None else [],
    )
