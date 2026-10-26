from typing import Any

from app.graph.v1.service.worklog_graph_builder import GraphEdge, GraphNode
from app.graph.v1.store.graph_rag_types import GraphRagSubgraph


def records_to_subgraph(records: list[dict[str, Any]]) -> GraphRagSubgraph:
    nodes_by_id: dict[str, GraphNode] = {}
    edges_by_id: dict[str, GraphEdge] = {}
    for record in records:
        raw_nodes = [record.get("root"), *record.get("nodes", [])]
        for raw_node in raw_nodes:
            if raw_node is None:
                continue
            node = _node_to_graph_node(raw_node)
            nodes_by_id[node.node_id] = node
        for raw_edge in record.get("edges", []):
            if raw_edge is None:
                continue
            edge = _edge_to_graph_edge(raw_edge)
            edges_by_id[edge.edge_id] = edge
    return GraphRagSubgraph(nodes=list(nodes_by_id.values()), edges=list(edges_by_id.values()))


def _node_to_graph_node(raw: Any) -> GraphNode:
    properties = _graph_entity_properties(raw)
    node_id = str(properties.pop("id"))
    labels = properties.pop("labels", [])
    label = labels[0] if labels else "GraphRagEntity"
    source_worklog_ids = _source_ids(properties, "sourceWorklogIds")
    source_team_ids = _source_ids(properties, "sourceTeamIds")
    return GraphNode(
        node_id=node_id,
        label=label,
        source_worklog_ids=source_worklog_ids,
        source_team_ids=source_team_ids,
        properties=properties,
    )


def _edge_to_graph_edge(raw: Any) -> GraphEdge:
    properties = _graph_entity_properties(raw)
    edge_id = str(properties.pop("id"))
    source_id = str(_graph_entity_id(getattr(raw, "start_node", None)) or properties.pop("sourceId", ""))
    target_id = str(_graph_entity_id(getattr(raw, "end_node", None)) or properties.pop("targetId", ""))
    edge_type = str(properties.pop("type", "GRAPH_RAG_EDGE"))
    source_worklog_ids = _source_ids(properties, "sourceWorklogIds")
    source_team_ids = _source_ids(properties, "sourceTeamIds")
    return GraphEdge(
        edge_id=edge_id,
        source_id=source_id,
        target_id=target_id,
        type=edge_type,
        source_worklog_ids=source_worklog_ids,
        source_team_ids=source_team_ids,
        properties=properties,
    )


def _source_ids(properties: dict[str, Any], list_key: str) -> list[int]:
    raw_ids = properties.get(list_key)
    if isinstance(raw_ids, list):
        return [int(raw_id) for raw_id in raw_ids]
    return []


def _graph_entity_properties(raw: Any) -> dict[str, Any]:
    if isinstance(raw, dict):
        return dict(raw)
    if hasattr(raw, "items"):
        return dict(raw.items())
    return dict(raw)


def _graph_entity_id(raw: Any) -> Any:
    if raw is None:
        return None
    return _graph_entity_properties(raw).get("id")
