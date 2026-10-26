import pytest

from app.graph.v1.service.worklog_graph_builder import (
    GraphContextTextUnit,
    GraphEdge,
    GraphNode,
    GraphRagProvenanceError,
    build_worklog_graph_document,
)
from app.graph.v1.store.worklog_source_store import (
    GraphWorklogPredecessor,
    GraphWorklogSourceRow,
    GraphWorklogTag,
)


def test_build_worklog_graph_document_builds_core_nodes_and_edges() -> None:
    document = build_worklog_graph_document(
        GraphWorklogSourceRow(
            worklog_id=101,
            title="정산 배치 오류 분석",
            request_content="요청",
            work_content="수행",
            author_id=5,
            author_name="김도윤",
            team_id=7,
            team_name="정산 고도화 TF",
            tags=[GraphWorklogTag(tag_id=11, tag_name="정산", description=None)],
            direct_predecessors=[GraphWorklogPredecessor(worklog_id=88, team_id=7, title="직접 선행")],
        )
    )

    assert document.worklog_id == 101
    assert {node.node_id for node in document.nodes} == {
        "worklog:101",
        "user:5",
        "team:7",
        "tag:11",
        "worklog:88",
    }
    assert {edge.type for edge in document.edges} == {
        "AUTHORED_BY",
        "BELONGS_TO",
        "HAS_TAG",
        "DEPENDS_ON",
    }
    worklog = next(node for node in document.nodes if node.node_id == "worklog:101")
    assert worklog.properties["sourceType"] == "CONFIRMED"
    assert worklog.properties["source"] == "postgres"
    assert all(node.source_worklog_ids for node in document.nodes)
    assert all(node.source_team_ids for node in document.nodes)
    assert all(edge.source_worklog_ids for edge in document.edges)
    assert all(edge.source_team_ids for edge in document.edges)
    predecessor = next(node for node in document.nodes if node.node_id == "worklog:88")
    assert predecessor.properties["teamId"] == 7
    assert predecessor.source_worklog_ids == [88]
    assert predecessor.source_team_ids == [7]


def test_graph_node_rejects_missing_provenance() -> None:
    with pytest.raises(GraphRagProvenanceError):
        GraphNode(node_id="worklog:1", label="Worklog", source_worklog_ids=[], source_team_ids=[7])


def test_graph_edge_rejects_missing_provenance() -> None:
    with pytest.raises(GraphRagProvenanceError):
        GraphEdge(
            edge_id="e1",
            source_id="worklog:1",
            target_id="team:7",
            type="BELONGS_TO",
            source_worklog_ids=[1],
            source_team_ids=[],
        )


def test_context_text_unit_rejects_missing_provenance() -> None:
    with pytest.raises(GraphRagProvenanceError):
        GraphContextTextUnit(
            text_unit_id="text:1",
            text="정산 context",
            source_worklog_ids=[101],
            source_team_ids=[],
        )
