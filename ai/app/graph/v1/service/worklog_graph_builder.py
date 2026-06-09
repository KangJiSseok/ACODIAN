"""GraphRAG v1 업무일지 source row를 graph document로 변환한다."""

from dataclasses import dataclass, field
from typing import Any

from app.graph.v1.store.worklog_source_store import GraphWorklogSourceRow

CONFIRMED_SOURCE_TYPE = "CONFIRMED"
POSTGRES_SOURCE = "postgres"


@dataclass(frozen=True)
class GraphNode:
    """GraphRAG에 저장할 node contract."""

    node_id: str
    label: str
    properties: dict[str, Any] = field(default_factory=dict)


@dataclass(frozen=True)
class GraphEdge:
    """GraphRAG에 저장할 edge contract."""

    edge_id: str
    source_id: str
    target_id: str
    type: str
    properties: dict[str, Any] = field(default_factory=dict)


@dataclass(frozen=True)
class WorklogGraphDocument:
    """업무일지 1건에서 파생된 graph index document."""

    worklog_id: int
    nodes: list[GraphNode]
    edges: list[GraphEdge]


def build_worklog_graph_document(source: GraphWorklogSourceRow) -> WorklogGraphDocument:
    """업무일지 source row를 deterministic GraphRAG node/edge document로 만든다."""
    worklog_node_id = f"worklog:{source.worklog_id}"
    user_node_id = f"user:{source.author_id}"
    team_node_id = f"team:{source.team_id}"
    provenance = {
        "sourceType": CONFIRMED_SOURCE_TYPE,
        "source": POSTGRES_SOURCE,
        "worklogId": source.worklog_id,
    }

    nodes = [
        GraphNode(
            node_id=worklog_node_id,
            label="Worklog",
            properties={
                **provenance,
                "worklogId": source.worklog_id,
                "title": source.title,
                "requestContent": source.request_content,
                "workContent": source.work_content,
                "teamId": source.team_id,
                "authorId": source.author_id,
            },
        ),
        GraphNode(
            node_id=user_node_id,
            label="User",
            properties={
                **provenance,
                "userId": source.author_id,
                "userName": source.author_name,
            },
        ),
        GraphNode(
            node_id=team_node_id,
            label="Team",
            properties={
                **provenance,
                "teamId": source.team_id,
                "teamName": source.team_name,
            },
        ),
    ]
    edges = [
        GraphEdge(
            edge_id=f"{worklog_node_id}:AUTHORED_BY:{user_node_id}",
            source_id=worklog_node_id,
            target_id=user_node_id,
            type="AUTHORED_BY",
            properties=provenance.copy(),
        ),
        GraphEdge(
            edge_id=f"{worklog_node_id}:BELONGS_TO:{team_node_id}",
            source_id=worklog_node_id,
            target_id=team_node_id,
            type="BELONGS_TO",
            properties=provenance.copy(),
        ),
    ]

    for tag in source.tags:
        tag_node_id = f"tag:{tag.tag_id}"
        nodes.append(
            GraphNode(
                node_id=tag_node_id,
                label="Tag",
                properties={
                    **provenance,
                    "tagId": tag.tag_id,
                    "tagName": tag.tag_name,
                    "description": tag.description,
                },
            )
        )
        edges.append(
            GraphEdge(
                edge_id=f"{worklog_node_id}:HAS_TAG:{tag_node_id}",
                source_id=worklog_node_id,
                target_id=tag_node_id,
                type="HAS_TAG",
                properties=provenance.copy(),
            )
        )

    for predecessor in source.direct_predecessors:
        predecessor_node_id = f"worklog:{predecessor.worklog_id}"
        nodes.append(
            GraphNode(
                node_id=predecessor_node_id,
                label="Worklog",
                properties={
                    "sourceType": CONFIRMED_SOURCE_TYPE,
                    "source": POSTGRES_SOURCE,
                    "worklogId": predecessor.worklog_id,
                    "title": predecessor.title,
                },
            )
        )
        edges.append(
            GraphEdge(
                edge_id=f"{worklog_node_id}:DEPENDS_ON:{predecessor_node_id}",
                source_id=worklog_node_id,
                target_id=predecessor_node_id,
                type="DEPENDS_ON",
                properties=provenance.copy(),
            )
        )

    return WorklogGraphDocument(worklog_id=source.worklog_id, nodes=nodes, edges=edges)
