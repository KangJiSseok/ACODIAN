from dataclasses import dataclass

from app.graph.v1.service.worklog_graph_builder import GraphEdge, GraphNode


@dataclass(frozen=True)
class GraphRagSubgraph:
    nodes: list[GraphNode]
    edges: list[GraphEdge]
