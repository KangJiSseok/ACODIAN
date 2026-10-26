"""GraphRAG v1 업무일지 source row를 graph document로 변환한다."""

from dataclasses import dataclass, field
from typing import Any

from app.graph.v1.store.worklog_source_store import GraphWorklogSourceRow

CONFIRMED_SOURCE_TYPE = "CONFIRMED"
POSTGRES_SOURCE = "postgres"


class GraphRagProvenanceError(RuntimeError):
    """GraphRAG graph 객체가 source worklog/team provenance 없이 생성될 때 발생한다."""


@dataclass(frozen=True)
class GraphNode:
    """GraphRAG에 저장할 node contract."""

    node_id: str
    label: str
    source_worklog_ids: list[int]
    source_team_ids: list[int]
    properties: dict[str, Any] = field(default_factory=dict)

    def __post_init__(self) -> None:
        """node는 context guard 이전에도 worklog/team provenance를 반드시 가져야 한다."""
        _ensure_provenance(self.source_worklog_ids, self.source_team_ids)


@dataclass(frozen=True)
class GraphEdge:
    """GraphRAG에 저장할 edge contract."""

    edge_id: str
    source_id: str
    target_id: str
    type: str
    source_worklog_ids: list[int]
    source_team_ids: list[int]
    properties: dict[str, Any] = field(default_factory=dict)

    def __post_init__(self) -> None:
        """edge는 양 끝점 관계의 출처를 설명하는 worklog/team provenance를 반드시 가져야 한다."""
        _ensure_provenance(self.source_worklog_ids, self.source_team_ids)


@dataclass(frozen=True)
class GraphContextTextUnit:
    """LLM context에 들어갈 text unit은 source worklog/team provenance를 반드시 가진다."""

    text_unit_id: str
    text: str
    source_worklog_ids: list[int]
    source_team_ids: list[int]

    def __post_init__(self) -> None:
        """context text unit도 graph node/edge와 같은 provenance invariant를 공유한다."""
        _ensure_provenance(self.source_worklog_ids, self.source_team_ids)


@dataclass(frozen=True)
class WorklogGraphDocument:
    """업무일지 1건에서 파생된 graph index document."""

    worklog_id: int
    nodes: list[GraphNode]
    edges: list[GraphEdge]


def _ensure_provenance(source_worklog_ids: list[int], source_team_ids: list[int]) -> None:
    """빈 provenance가 graph context까지 전파되지 않도록 생성 경계에서 차단한다."""
    if not source_worklog_ids or not source_team_ids:
        raise GraphRagProvenanceError("sourceWorklogIds and sourceTeamIds must be non-empty")


def build_worklog_graph_document(source: GraphWorklogSourceRow) -> WorklogGraphDocument:
    """업무일지 source row를 deterministic GraphRAG node/edge document로 만든다."""
    worklog_node_id = f"worklog:{source.worklog_id}"
    user_node_id = f"user:{source.author_id}"
    team_node_id = f"team:{source.team_id}"
    provenance = {
        "sourceType": CONFIRMED_SOURCE_TYPE,
        "source": POSTGRES_SOURCE,
        "worklogId": source.worklog_id,
        "sourceWorklogIds": [source.worklog_id],
        "sourceTeamIds": [source.team_id],
    }

    nodes = [
        GraphNode(
            node_id=worklog_node_id,
            label="Worklog",
            source_worklog_ids=[source.worklog_id],
            source_team_ids=[source.team_id],
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
            source_worklog_ids=[source.worklog_id],
            source_team_ids=[source.team_id],
            properties={
                **provenance,
                "userId": source.author_id,
                "userName": source.author_name,
            },
        ),
        GraphNode(
            node_id=team_node_id,
            label="Team",
            source_worklog_ids=[source.worklog_id],
            source_team_ids=[source.team_id],
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
            source_worklog_ids=[source.worklog_id],
            source_team_ids=[source.team_id],
            properties=provenance.copy(),
        ),
        GraphEdge(
            edge_id=f"{worklog_node_id}:BELONGS_TO:{team_node_id}",
            source_id=worklog_node_id,
            target_id=team_node_id,
            type="BELONGS_TO",
            source_worklog_ids=[source.worklog_id],
            source_team_ids=[source.team_id],
            properties=provenance.copy(),
        ),
    ]

    for tag in source.tags:
        tag_node_id = f"tag:{tag.tag_id}"
        nodes.append(
            GraphNode(
                node_id=tag_node_id,
                label="Tag",
                source_worklog_ids=[source.worklog_id],
                source_team_ids=[source.team_id],
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
                source_worklog_ids=[source.worklog_id],
                source_team_ids=[source.team_id],
                properties=provenance.copy(),
            )
        )

    for predecessor in source.direct_predecessors:
        predecessor_node_id = f"worklog:{predecessor.worklog_id}"
        predecessor_provenance = {
            "sourceType": CONFIRMED_SOURCE_TYPE,
            "source": POSTGRES_SOURCE,
            "worklogId": predecessor.worklog_id,
            "sourceWorklogIds": [predecessor.worklog_id],
            "sourceTeamIds": [predecessor.team_id],
        }
        dependency_provenance = {
            "sourceType": CONFIRMED_SOURCE_TYPE,
            "source": POSTGRES_SOURCE,
            "worklogId": source.worklog_id,
            "predecessorWorklogId": predecessor.worklog_id,
            "sourceWorklogIds": [source.worklog_id, predecessor.worklog_id],
            "sourceTeamIds": [source.team_id, predecessor.team_id],
        }
        nodes.append(
            GraphNode(
                node_id=predecessor_node_id,
                label="Worklog",
                source_worklog_ids=[predecessor.worklog_id],
                source_team_ids=[predecessor.team_id],
                properties={
                    **predecessor_provenance,
                    "title": predecessor.title,
                    "teamId": predecessor.team_id,
                },
            )
        )
        edges.append(
            GraphEdge(
                edge_id=f"{worklog_node_id}:DEPENDS_ON:{predecessor_node_id}",
                source_id=worklog_node_id,
                target_id=predecessor_node_id,
                type="DEPENDS_ON",
                source_worklog_ids=[source.worklog_id, predecessor.worklog_id],
                source_team_ids=[source.team_id, predecessor.team_id],
                properties=dependency_provenance,
            )
        )

    return WorklogGraphDocument(worklog_id=source.worklog_id, nodes=nodes, edges=edges)
