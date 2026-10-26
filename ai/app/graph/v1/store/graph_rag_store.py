"""GraphRAG v1 Neo4j graph store."""

from __future__ import annotations

import asyncio
from dataclasses import dataclass
from collections.abc import Callable
from typing import Any, Protocol

from app.config.settings import Settings, settings
from app.graph.v1.service.worklog_graph_builder import GraphEdge, GraphNode, WorklogGraphDocument
from app.graph.v1.store.graph_rag_subgraph_mapper import records_to_subgraph
from app.graph.v1.store.graph_rag_subgraph_query import select_subgraph_cypher
from app.graph.v1.store.graph_rag_types import GraphRagSubgraph


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


DriverFactory = Callable[[], AsyncGraphDriver]


@dataclass(frozen=True)
class GraphWorklogLink:
    source_id: int
    target_id: int
    relation: str


_RELATED_WORKLOGS_CYPHER = """
UNWIND $root_ids AS rootId
MATCH (root:GraphRagEntity {id: 'worklog:' + toString(rootId)})
WHERE root.worklogId = rootId AND root.teamId IS NOT NULL
  AND ($allowed_team_ids IS NULL OR root.teamId IN $allowed_team_ids)
CALL (root) {
  MATCH (root)-[edge:GRAPH_RAG_EDGE {type:'DEPENDS_ON'}]->(related:GraphRagEntity)
        -[authorEdge:GRAPH_RAG_EDGE {type:'AUTHORED_BY'}]->(author:GraphRagEntity)
  WHERE related.worklogId IS NOT NULL AND related.teamId IS NOT NULL
    AND root.worklogId IN edge.sourceWorklogIds AND related.worklogId IN edge.sourceWorklogIds
    AND author.userId IS NOT NULL AND related.worklogId IN authorEdge.sourceWorklogIds
    AND size(authorEdge.sourceTeamIds) > 0
    AND ($allowed_team_ids IS NULL OR all(t IN authorEdge.sourceTeamIds WHERE t IN $allowed_team_ids))
  RETURN related, edge.type AS relation, edge.sourceTeamIds AS teams
  UNION
  MATCH (root)-[first:GRAPH_RAG_EDGE]->(shared:GraphRagEntity)<-[second:GRAPH_RAG_EDGE]-(related:GraphRagEntity)
  WHERE first.type IN ['AUTHORED_BY', 'HAS_TAG'] AND second.type = first.type
    AND related.worklogId IS NOT NULL AND related.id <> root.id
    AND root.worklogId IN first.sourceWorklogIds
    AND related.worklogId IN second.sourceWorklogIds
    AND size(first.sourceTeamIds) > 0
    AND ($allowed_team_ids IS NULL OR all(t IN first.sourceTeamIds WHERE t IN $allowed_team_ids))
  RETURN related, first.type AS relation, second.sourceTeamIds AS teams
}
WITH root, related, relation, teams
WHERE size(teams) > 0 AND related.teamId IS NOT NULL
  AND ($allowed_team_ids IS NULL OR (related.teamId IN $allowed_team_ids AND all(t IN teams WHERE t IN $allowed_team_ids)))
RETURN DISTINCT root.worklogId AS sourceId, related.worklogId AS targetId, relation
LIMIT $limit
"""


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
            connection_timeout=2.0,
            max_transaction_retry_time=2.0,
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

    async def fetch_worklog_links(
        self, root_ids: list[int], allowed_team_ids: list[int] | None, limit: int
    ) -> list[GraphWorklogLink]:
        """허용 범위에서 최대 2홉 관계의 ID와 종류만 읽는다."""
        if not root_ids:
            return []
        try:
            return await asyncio.wait_for(
                self._fetch_worklog_links(root_ids, allowed_team_ids, limit),
                timeout=self._settings.graphrag_query_timeout_seconds,
            )
        except TimeoutError as exc:
            raise GraphRagQueryTimeoutError("GraphRAG query timeout") from exc
        except GraphRagConfigurationError:
            raise
        except Exception as exc:
            raise GraphRagQueryFailedError("GraphRAG query failed") from exc

    async def _fetch_worklog_links(
        self, root_ids: list[int], allowed_team_ids: list[int] | None, limit: int
    ) -> list[GraphWorklogLink]:
        driver = self._get_driver()
        async with driver.session(database=self._settings.graphrag_neo4j_database) as session:
            return await session.execute_read(
                self._query_worklog_links, root_ids, allowed_team_ids, limit
            )

    @staticmethod
    async def _query_worklog_links(
        tx: Any, root_ids: list[int], allowed_team_ids: list[int] | None, limit: int
    ) -> list[GraphWorklogLink]:
        result = await tx.run(
            _RELATED_WORKLOGS_CYPHER,
            root_ids=root_ids,
            allowed_team_ids=allowed_team_ids,
            limit=limit,
        )
        return [
            GraphWorklogLink(int(row["sourceId"]), int(row["targetId"]), row["relation"])
            async for row in result
        ]

    @staticmethod
    async def _upsert_nodes_and_edges(tx: Any, nodes: list[GraphNode], edges: list[GraphEdge]) -> None:
        """Node/edge payload를 parameterized Cypher로 upsert한다."""
        root_id = next(node.node_id for node in nodes if node.label == "Worklog")
        await tx.run(
            "MATCH (root:GraphRagEntity {id: $root_id})-[edge:GRAPH_RAG_EDGE]->() DELETE edge",
            root_id=root_id,
        )
        await tx.run(
            """
            UNWIND $nodes AS node
            MERGE (n:GraphRagEntity {id: node.id})
            WITH n, node,
                 coalesce(n.sourceWorklogIds, []) AS existingSourceWorklogIds,
                 coalesce(n.sourceTeamIds, []) AS existingSourceTeamIds
            SET n.labels = node.labels,
                n += node.properties
            SET n.sourceWorklogIds = existingSourceWorklogIds
                + [id IN node.properties.sourceWorklogIds WHERE NOT id IN existingSourceWorklogIds],
                n.sourceTeamIds = existingSourceTeamIds
                + [id IN node.properties.sourceTeamIds WHERE NOT id IN existingSourceTeamIds]
            """,
            nodes=[
                {
                    "id": node.node_id,
                    "labels": [node.label],
                    "properties": {
                        **node.properties,
                        "sourceWorklogIds": node.source_worklog_ids,
                        "sourceTeamIds": node.source_team_ids,
                    },
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
            WITH r, edge,
                 coalesce(r.sourceWorklogIds, []) AS existingSourceWorklogIds,
                 coalesce(r.sourceTeamIds, []) AS existingSourceTeamIds
            SET r.type = edge.type,
                r += edge.properties
            SET r.sourceWorklogIds = existingSourceWorklogIds
                + [id IN edge.properties.sourceWorklogIds WHERE NOT id IN existingSourceWorklogIds],
                r.sourceTeamIds = existingSourceTeamIds
                + [id IN edge.properties.sourceTeamIds WHERE NOT id IN existingSourceTeamIds]
            """,
            edges=[
                {
                    "id": edge.edge_id,
                    "source_id": edge.source_id,
                    "target_id": edge.target_id,
                    "type": edge.type,
                    "properties": {
                        **edge.properties,
                        "sourceWorklogIds": edge.source_worklog_ids,
                        "sourceTeamIds": edge.source_team_ids,
                    },
                }
                for edge in edges
            ],
        )

    async def delete_worklog_graph(self, worklog_id: int) -> None:
        """원장에서 사라진 업무일지의 그래프 노드와 연결 관계를 제거한다."""
        driver = self._get_driver()
        async with driver.session(database=self._settings.graphrag_neo4j_database) as session:
            await session.execute_write(self._delete_worklog_node, worklog_id)

    @staticmethod
    async def _delete_worklog_node(tx: Any, worklog_id: int) -> None:
        await tx.run(
            "MATCH (root:GraphRagEntity {id: $root_id}) DETACH DELETE root",
            root_id=f"worklog:{worklog_id}",
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
        return records_to_subgraph(records)

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
            select_subgraph_cypher(max_depth),
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
