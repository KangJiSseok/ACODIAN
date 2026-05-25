from app.graph.v1.service.worklog_graph_builder import build_worklog_graph_document
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
            direct_predecessors=[GraphWorklogPredecessor(worklog_id=88, title="직접 선행")],
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
