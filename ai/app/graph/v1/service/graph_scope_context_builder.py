from collections.abc import Iterable
from dataclasses import dataclass
from typing import Any
from typing import assert_never

from app.graph.v1.model.worklog_query import WorklogGraphQueryRequest, WorklogGraphScopeMode
from app.graph.v1.service.worklog_graph_builder import (
    GraphContextTextUnit,
    GraphEdge,
    GraphNode,
    build_worklog_graph_document,
)
from app.graph.v1.store.graph_authorized_corpus_store import GraphAuthorizedCorpus
from app.graph.v1.store.graph_rag_store import GraphRagSubgraph
from app.graph.v1.store.worklog_source_store import GraphWorklogSourceRow


class GraphScopeContextViolationError(RuntimeError):
    pass


@dataclass(frozen=True)
class GraphScopeContext:
    text_units: list[GraphContextTextUnit]
    subgraph: GraphRagSubgraph
    snapshot: dict[str, Any]


class GraphScopeContextBuilder:
    def build_context(
        self,
        *,
        corpus: GraphAuthorizedCorpus,
        request: WorklogGraphQueryRequest,
    ) -> GraphScopeContext:
        allowed_worklog_ids = self._allowed_worklog_ids(corpus)
        allowed_team_ids = self._allowed_team_ids(corpus, request)
        text_units = [
            self._build_text_unit(source)
            for source in corpus.sources.values()
        ]
        nodes, edges = self._build_subgraph_objects(corpus.sources.values())
        self._guard_text_units(text_units, allowed_worklog_ids, allowed_team_ids)
        self._guard_subgraph(nodes, edges, allowed_worklog_ids, allowed_team_ids)
        subgraph = GraphRagSubgraph(nodes=nodes, edges=edges)
        return GraphScopeContext(
            text_units=text_units,
            subgraph=subgraph,
            snapshot=self._snapshot(text_units, subgraph),
        )

    def _build_subgraph_objects(
        self,
        sources: Iterable[GraphWorklogSourceRow],
    ) -> tuple[list[GraphNode], list[GraphEdge]]:
        nodes_by_id: dict[str, GraphNode] = {}
        edges_by_id: dict[str, GraphEdge] = {}
        for source in sources:
            document = build_worklog_graph_document(source)
            for node in document.nodes:
                nodes_by_id[node.node_id] = _merge_node(nodes_by_id.get(node.node_id), node)
            for edge in document.edges:
                edges_by_id[edge.edge_id] = _merge_edge(edges_by_id.get(edge.edge_id), edge)
        return list(nodes_by_id.values()), list(edges_by_id.values())

    def _build_text_unit(self, source: GraphWorklogSourceRow) -> GraphContextTextUnit:
        tags = ", ".join(tag.tag_name for tag in source.tags) or "없음"
        predecessors = ", ".join(
            f"{predecessor.title}(worklog:{predecessor.worklog_id}, team:{predecessor.team_id})"
            for predecessor in source.direct_predecessors
        ) or "없음"
        text = "\n".join(
            [
                f"worklogId: {source.worklog_id}",
                f"teamId: {source.team_id}",
                f"title: {source.title}",
                f"teamName: {source.team_name}",
                f"authorName: {source.author_name}",
                f"tags: {tags}",
                f"requestContent: {source.request_content or ''}",
                f"workContent: {source.work_content}",
                f"directPredecessors: {predecessors}",
            ]
        )
        return GraphContextTextUnit(
            text_unit_id=f"text:worklog:{source.worklog_id}",
            text=text,
            source_worklog_ids=[source.worklog_id],
            source_team_ids=[source.team_id],
        )

    def _guard_text_units(
        self,
        text_units: list[GraphContextTextUnit],
        allowed_worklog_ids: set[int],
        allowed_team_ids: set[int] | None,
    ) -> None:
        for text_unit in text_units:
            self._guard_provenance(
                text_unit.source_worklog_ids,
                text_unit.source_team_ids,
                allowed_worklog_ids,
                allowed_team_ids,
            )

    def _guard_subgraph(
        self,
        nodes: list[GraphNode],
        edges: list[GraphEdge],
        allowed_worklog_ids: set[int],
        allowed_team_ids: set[int] | None,
    ) -> None:
        for node in nodes:
            self._guard_provenance(
                node.source_worklog_ids,
                node.source_team_ids,
                allowed_worklog_ids,
                allowed_team_ids,
            )
        for edge in edges:
            self._guard_provenance(
                edge.source_worklog_ids,
                edge.source_team_ids,
                allowed_worklog_ids,
                allowed_team_ids,
            )

    @staticmethod
    def _guard_provenance(
        source_worklog_ids: list[int],
        source_team_ids: list[int],
        allowed_worklog_ids: set[int],
        allowed_team_ids: set[int] | None,
    ) -> None:
        if not set(source_worklog_ids).issubset(allowed_worklog_ids):
            raise GraphScopeContextViolationError(
                "context contains unauthorized worklog provenance"
            )
        if allowed_team_ids is not None and not set(source_team_ids).issubset(allowed_team_ids):
            raise GraphScopeContextViolationError("context contains unauthorized team provenance")

    @staticmethod
    def _allowed_worklog_ids(corpus: GraphAuthorizedCorpus) -> set[int]:
        predecessor_ids = {
            predecessor.worklog_id
            for source in corpus.sources.values()
            for predecessor in source.direct_predecessors
        }
        return set(corpus.worklog_ids) | predecessor_ids

    @staticmethod
    def _allowed_team_ids(
        corpus: GraphAuthorizedCorpus,
        request: WorklogGraphQueryRequest,
    ) -> set[int] | None:
        match request.scope_mode:
            case WorklogGraphScopeMode.TEAM_IDS:
                return {int(team_id) for team_id in request.allowed_team_ids or []}
            case WorklogGraphScopeMode.ALL:
                predecessor_team_ids = {
                    predecessor.team_id
                    for source in corpus.sources.values()
                    for predecessor in source.direct_predecessors
                }
                return set(corpus.team_ids) | predecessor_team_ids
            case unreachable:
                assert_never(unreachable)

    @staticmethod
    def _snapshot(
        text_units: list[GraphContextTextUnit],
        subgraph: GraphRagSubgraph,
    ) -> dict[str, Any]:
        return {
            "textUnits": [
                {
                    "textUnitId": text_unit.text_unit_id,
                    "text": text_unit.text,
                    "sourceWorklogIds": text_unit.source_worklog_ids,
                    "sourceTeamIds": text_unit.source_team_ids,
                }
                for text_unit in text_units
            ],
            "nodes": [
                {
                    "nodeId": node.node_id,
                    "label": node.label,
                    "sourceWorklogIds": node.source_worklog_ids,
                    "sourceTeamIds": node.source_team_ids,
                    "properties": node.properties,
                }
                for node in subgraph.nodes
            ],
            "edges": [
                {
                    "edgeId": edge.edge_id,
                    "sourceId": edge.source_id,
                    "targetId": edge.target_id,
                    "type": edge.type,
                    "sourceWorklogIds": edge.source_worklog_ids,
                    "sourceTeamIds": edge.source_team_ids,
                    "properties": edge.properties,
                }
                for edge in subgraph.edges
            ],
        }


def _merge_node(existing: GraphNode | None, current: GraphNode) -> GraphNode:
    if existing is None:
        return current
    return GraphNode(
        node_id=current.node_id,
        label=current.label,
        source_worklog_ids=_merge_ids(existing.source_worklog_ids, current.source_worklog_ids),
        source_team_ids=_merge_ids(existing.source_team_ids, current.source_team_ids),
        properties={
            **current.properties,
            "sourceWorklogIds": _merge_ids(
                existing.source_worklog_ids,
                current.source_worklog_ids,
            ),
            "sourceTeamIds": _merge_ids(existing.source_team_ids, current.source_team_ids),
        },
    )


def _merge_edge(existing: GraphEdge | None, current: GraphEdge) -> GraphEdge:
    if existing is None:
        return current
    return GraphEdge(
        edge_id=current.edge_id,
        source_id=current.source_id,
        target_id=current.target_id,
        type=current.type,
        source_worklog_ids=_merge_ids(existing.source_worklog_ids, current.source_worklog_ids),
        source_team_ids=_merge_ids(existing.source_team_ids, current.source_team_ids),
        properties={
            **current.properties,
            "sourceWorklogIds": _merge_ids(
                existing.source_worklog_ids,
                current.source_worklog_ids,
            ),
            "sourceTeamIds": _merge_ids(existing.source_team_ids, current.source_team_ids),
        },
    )


def _merge_ids(left: list[int], right: list[int]) -> list[int]:
    return list(dict.fromkeys([*left, *right]))
