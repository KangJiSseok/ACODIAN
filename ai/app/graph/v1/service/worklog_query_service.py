"""GraphRAG v1 업무일지 query orchestration service."""

from app.config.settings import Settings, settings
from app.graph.v1.model.worklog_query import (
    WorklogGraphEdgeItem,
    WorklogGraphNodeItem,
    WorklogGraphQueryRequest,
    WorklogGraphQueryResponse,
    WorklogGraphReferenceItem,
)
from app.graph.v1.service.graph_rag_adapter import GraphRagAdapter, get_graph_rag_worklog_adapter
from app.graph.v1.store.graph_rag_store import GraphRagSubgraph


class GraphWorklogQueryService:
    """업무일지 GraphRAG query 유스케이스 진입점."""

    def __init__(
        self,
        *,
        settings_obj: Settings = settings,
        adapter_factory=get_graph_rag_worklog_adapter,
    ) -> None:
        self._settings = settings_obj
        self._adapter_factory = adapter_factory

    async def query_worklogs(self, request: WorklogGraphQueryRequest) -> WorklogGraphQueryResponse:
        """GraphRAG subgraph retrieval 결과를 구조화 응답으로 변환한다."""
        adapter: GraphRagAdapter = self._adapter_factory()
        subgraph = await adapter.fetch_worklog_subgraph(
            query=request.query,
            allowed_team_ids=[int(team_id) for team_id in request.allowed_team_ids]
            if request.allowed_team_ids is not None
            else None,
            max_depth=int(request.max_depth or self._settings.graphrag_query_max_depth),
            limit=int(request.limit or self._settings.graphrag_query_limit),
        )
        return self._build_response(subgraph)

    def _build_response(self, subgraph: GraphRagSubgraph) -> WorklogGraphQueryResponse:
        """GraphRAG subgraph를 HTTP response model로 변환한다."""
        references: list[WorklogGraphReferenceItem] = []
        for node in subgraph.nodes:
            worklog_id = node.properties.get("worklogId")
            if worklog_id is None or node.label != "Worklog":
                continue
            references.append(
                WorklogGraphReferenceItem(
                    referenceId=f"worklog-{worklog_id}",
                    filePath=f"worklog://{worklog_id}",
                )
            )
        return WorklogGraphQueryResponse(
            answer=None,
            nodes=[
                WorklogGraphNodeItem(
                    nodeId=node.node_id,
                    label=node.label,
                    properties=node.properties,
                )
                for node in subgraph.nodes
            ],
            edges=[
                WorklogGraphEdgeItem(
                    edgeId=edge.edge_id,
                    sourceId=edge.source_id,
                    targetId=edge.target_id,
                    type=edge.type,
                    properties=edge.properties,
                )
                for edge in subgraph.edges
            ],
            references=references,
            internalOnly=True,
        )
