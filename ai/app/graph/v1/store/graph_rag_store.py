"""GraphRAG v1 Neo4j graph store."""

from __future__ import annotations

import asyncio
from collections.abc import Callable
from dataclasses import dataclass
from typing import Any, Protocol

from app.config.settings import Settings, settings
from app.graph.v1.service.worklog_graph_builder import GraphEdge, GraphNode, WorklogGraphDocument


class GraphRagConfigurationError(RuntimeError):
    """GraphRAG store configuration이 유효하지 않을 때 발생한다."""


class GraphRagIndexFailedError(RuntimeError):
    """GraphRAG index 저장이 실패했을 때 발생한다."""


class GraphRagIndexTimeoutError(TimeoutError):
    """GraphRAG index 저장이 timeout 되었을 때 발생한다."""


class GraphRagQueryFailedError(RuntimeError):
    """GraphRAG query가 실패했을 때 발생한다."""


class GraphRagQueryTimeoutError(TimeoutError):
    """GraphRAG query가 timeout 되었을 때 발생한다."""


class AsyncGraphDriver(Protocol):
    """GraphRAG store가 기대하는 Neo4j async driver protocol."""

    def session(self, **kwargs: Any) -> Any: ...

    async def close(self) -> None: ...


@dataclass(frozen=True)
class GraphRagSubgraph:
    """GraphRAG query 결과로 회수한 subgraph."""

    nodes: list[GraphNode]
    edges: list[GraphEdge]


DriverFactory = Callable[[], AsyncGraphDriver]


class GraphRagStore:
    """Neo4j driver/session/Cypher를 소유하는 GraphRAG store."""

    def __init__(
        self,
        *,
        settings_obj: Settings = settings,
        driver_factory: DriverFactory | None = None,
    ) -> None:
        self._settings = settings_obj
        self._driver_factory = driver_factory
        self._driver: AsyncGraphDriver | None = None

    def _get_driver(self) -> AsyncGraphDriver:
        """GraphRAG Neo4j async driver를 lazy 생성한다."""
        if self._driver is not None:
            return self._driver
        if self._driver_factory is not None:
            self._driver = self._driver_factory()
            return self._driver
        if not self._settings.neo4j_uri or not self._settings.neo4j_user:
            raise GraphRagConfigurationError("missing Neo4j configuration")
        try:
            from neo4j import AsyncGraphDatabase
        except ImportError as exc:  # pragma: no cover - dependency는 requirements로 관리된다.
            raise GraphRagConfigurationError("neo4j package is not installed") from exc
        self._driver = AsyncGraphDatabase.driver(
            self._settings.neo4j_uri,
            auth=(self._settings.neo4j_user, self._settings.neo4j_password),
        )
        return self._driver

    async def upsert_worklog_graph(self, document: WorklogGraphDocument) -> None:
        """업무일지 graph document를 idempotent하게 Neo4j에 upsert한다."""
        try:
            await asyncio.wait_for(
                self._upsert_worklog_graph(document),
                timeout=self._settings.graphrag_insert_timeout_seconds,
            )
        except TimeoutError as exc:
            raise GraphRagIndexTimeoutError("GraphRAG index timeout") from exc
        except GraphRagIndexTimeoutError:
            raise
        except GraphRagConfigurationError:
            raise
        except Exception as exc:
            raise GraphRagIndexFailedError("GraphRAG index failed") from exc

    async def _upsert_worklog_graph(self, document: WorklogGraphDocument) -> None:
        """timeout wrapper 없이 graph document를 저장한다."""
        driver = self._get_driver()
        async with driver.session(database=self._settings.graphrag_neo4j_database) as session:
            await session.execute_write(self._upsert_nodes_and_edges, document.nodes, document.edges)

    @staticmethod
    async def _upsert_nodes_and_edges(tx: Any, nodes: list[GraphNode], edges: list[GraphEdge]) -> None:
        """Node/edge payload를 parameterized Cypher로 upsert한다."""
        await tx.run(
            """
            UNWIND $nodes AS node
            MERGE (n:GraphRagEntity {id: node.id})
            SET n.labels = node.labels,
                n += node.properties
            """,
            nodes=[
                {
                    "id": node.node_id,
                    "labels": [node.label],
                    "properties": node.properties,
                }
                for node in nodes
            ],
        )
        await tx.run(
            """
            UNWIND $edges AS edge
            MATCH (source:GraphRagEntity {id: edge.source_id})
            MATCH (target:GraphRagEntity {id: edge.target_id})
            MERGE (source)-[r:GRAPH_RAG_EDGE {id: edge.id}]->(target)
            SET r.type = edge.type,
                r += edge.properties
            """,
            edges=[
                {
                    "id": edge.edge_id,
                    "source_id": edge.source_id,
                    "target_id": edge.target_id,
                    "type": edge.type,
                    "properties": edge.properties,
                }
                for edge in edges
            ],
        )

    async def fetch_worklog_subgraph(
        self,
        *,
        query: str,
        allowed_team_ids: list[int] | None,
        max_depth: int,
        limit: int,
    ) -> GraphRagSubgraph:
        """업무일지 graph에서 team scope가 적용된 subgraph를 조회한다."""
        try:
            return await asyncio.wait_for(
                self._fetch_worklog_subgraph(
                    query=query,
                    allowed_team_ids=allowed_team_ids,
                    max_depth=max_depth,
                    limit=limit,
                ),
                timeout=self._settings.graphrag_query_timeout_seconds,
            )
        except TimeoutError as exc:
            raise GraphRagQueryTimeoutError("GraphRAG query timeout") from exc
        except GraphRagConfigurationError:
            raise
        except Exception as exc:
            raise GraphRagQueryFailedError("GraphRAG query failed") from exc

    async def _fetch_worklog_subgraph(
        self,
        *,
        query: str,
        allowed_team_ids: list[int] | None,
        max_depth: int,
        limit: int,
    ) -> GraphRagSubgraph:
        """timeout wrapper 없이 Neo4j subgraph를 조회한다."""
        driver = self._get_driver()
        async with driver.session(database=self._settings.graphrag_neo4j_database) as session:
            records = await session.execute_read(
                self._query_subgraph,
                query,
                allowed_team_ids,
                max_depth,
                limit,
            )
        return _records_to_subgraph(records)

    @staticmethod
    async def _query_subgraph(
        tx: Any,
        query: str,
        allowed_team_ids: list[int] | None,
        max_depth: int,
        limit: int,
    ) -> list[dict[str, Any]]:
        """사용자 입력을 parameter로만 전달해 subgraph를 조회한다."""
        result = await tx.run(
            _select_subgraph_cypher(max_depth),
            search_text=query.strip(),
            allowed_team_ids=allowed_team_ids,
            limit=limit,
        )
        return [
            {
                "root": record["root"],
                "nodes": record["nodes"],
                "edges": record["edges"],
            }
            async for record in result
        ]

    async def close(self) -> None:
        """GraphRAG Neo4j driver lifecycle을 정리한다."""
        if self._driver is not None:
            await self._driver.close()
            self._driver = None


_SUBGRAPH_CYPHER_BY_DEPTH = {
    1: """
    MATCH (worklog:GraphRagEntity)
    WHERE worklog.worklogId IS NOT NULL
      AND (
        $search_text = ""
        OR toLower(coalesce(worklog.title, "")) CONTAINS toLower($search_text)
        OR toLower(coalesce(worklog.workContent, "")) CONTAINS toLower($search_text)
      )
      AND (
        $allowed_team_ids IS NULL
        OR worklog.teamId IN $allowed_team_ids
      )
    OPTIONAL MATCH path = (worklog)-[edge:GRAPH_RAG_EDGE*0..1]-(related:GraphRagEntity)
    WITH worklog, nodes(path) AS path_nodes, relationships(path) AS path_edges
    LIMIT $limit
    RETURN worklog AS root, path_nodes AS nodes, path_edges AS edges
    """,
    2: """
    MATCH (worklog:GraphRagEntity)
    WHERE worklog.worklogId IS NOT NULL
      AND (
        $search_text = ""
        OR toLower(coalesce(worklog.title, "")) CONTAINS toLower($search_text)
        OR toLower(coalesce(worklog.workContent, "")) CONTAINS toLower($search_text)
      )
      AND (
        $allowed_team_ids IS NULL
        OR worklog.teamId IN $allowed_team_ids
      )
    OPTIONAL MATCH path = (worklog)-[edge:GRAPH_RAG_EDGE*0..2]-(related:GraphRagEntity)
    WITH worklog, nodes(path) AS path_nodes, relationships(path) AS path_edges
    LIMIT $limit
    RETURN worklog AS root, path_nodes AS nodes, path_edges AS edges
    """,
    3: """
    MATCH (worklog:GraphRagEntity)
    WHERE worklog.worklogId IS NOT NULL
      AND (
        $search_text = ""
        OR toLower(coalesce(worklog.title, "")) CONTAINS toLower($search_text)
        OR toLower(coalesce(worklog.workContent, "")) CONTAINS toLower($search_text)
      )
      AND (
        $allowed_team_ids IS NULL
        OR worklog.teamId IN $allowed_team_ids
      )
    OPTIONAL MATCH path = (worklog)-[edge:GRAPH_RAG_EDGE*0..3]-(related:GraphRagEntity)
    WITH worklog, nodes(path) AS path_nodes, relationships(path) AS path_edges
    LIMIT $limit
    RETURN worklog AS root, path_nodes AS nodes, path_edges AS edges
    """,
}


def _select_subgraph_cypher(max_depth: int) -> str:
    """Cypher path depth를 안전한 상수 query 중 하나로 선택한다."""
    bounded_depth = min(max(max_depth, 1), max(_SUBGRAPH_CYPHER_BY_DEPTH))
    return _SUBGRAPH_CYPHER_BY_DEPTH[bounded_depth]


def _node_to_graph_node(raw: Any) -> GraphNode:
    """Neo4j node-like 객체를 GraphNode로 정규화한다."""
    properties = _graph_entity_properties(raw)
    node_id = str(properties.pop("id"))
    labels = properties.pop("labels", [])
    label = labels[0] if labels else "GraphRagEntity"
    return GraphNode(node_id=node_id, label=label, properties=properties)


def _edge_to_graph_edge(raw: Any) -> GraphEdge:
    """Neo4j relationship-like 객체를 GraphEdge로 정규화한다."""
    properties = _graph_entity_properties(raw)
    edge_id = str(properties.pop("id"))
    source_id = str(_graph_entity_id(getattr(raw, "start_node", None)) or properties.pop("sourceId", ""))
    target_id = str(_graph_entity_id(getattr(raw, "end_node", None)) or properties.pop("targetId", ""))
    edge_type = str(properties.pop("type", "GRAPH_RAG_EDGE"))
    return GraphEdge(
        edge_id=edge_id,
        source_id=source_id,
        target_id=target_id,
        type=edge_type,
        properties=properties,
    )


def _graph_entity_properties(raw: Any) -> dict[str, Any]:
    """Neo4j Node/Relationship 또는 dict-like 객체의 properties를 dict로 변환한다."""
    if isinstance(raw, dict):
        return dict(raw)
    if hasattr(raw, "items"):
        return dict(raw.items())
    return dict(raw)


def _graph_entity_id(raw: Any) -> Any:
    """Neo4j endpoint node에서 GraphRAG entity id property를 추출한다."""
    if raw is None:
        return None
    return _graph_entity_properties(raw).get("id")


def _records_to_subgraph(records: list[dict[str, Any]]) -> GraphRagSubgraph:
    """Neo4j record list를 중복 제거된 GraphRAG subgraph로 변환한다."""
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
