import asyncio

from app.config.settings import Settings
from app.graph.v1.model.worklog_query import WorklogGraphQueryRequest
from app.graph.v1.service.worklog_graph_builder import GraphEdge, GraphNode
from app.graph.v1.service.worklog_query_service import GraphWorklogQueryService
from app.graph.v1.store.graph_rag_store import GraphRagSubgraph


class FakeAdapter:
    def __init__(self) -> None:
        self.calls: list[dict] = []

    async def fetch_worklog_subgraph(self, **kwargs) -> GraphRagSubgraph:
        self.calls.append(kwargs)
        return GraphRagSubgraph(
            nodes=[
                GraphNode(
                    node_id="worklog:101",
                    label="Worklog",
                    properties={"worklogId": 101, "title": "정산"},
                ),
                GraphNode(node_id="team:7", label="Team", properties={"teamId": 7}),
            ],
            edges=[
                GraphEdge(
                    edge_id="e1",
                    source_id="worklog:101",
                    target_id="team:7",
                    type="BELONGS_TO",
                    properties={"worklogId": 101},
                )
            ],
        )


def test_query_service_passes_scope_and_builds_response() -> None:
    adapter = FakeAdapter()
    service = GraphWorklogQueryService(
        settings_obj=Settings(
            _env_file=None,
            graphrag_query_max_depth=2,
            graphrag_query_limit=20,
        ),
        adapter_factory=lambda: adapter,
    )

    response = asyncio.run(
        service.query_worklogs(
            WorklogGraphQueryRequest.model_validate(
                {"query": "정산", "allowedTeamIds": [7], "maxDepth": 1, "limit": 5}
            )
        )
    )

    assert adapter.calls == [
        {"query": "정산", "allowed_team_ids": [7], "max_depth": 1, "limit": 5}
    ]
    assert response.answer is None
    assert response.nodes[0].node_id == "worklog:101"
    assert response.edges[0].type == "BELONGS_TO"
    assert response.references[0].reference_id == "worklog-101"
    assert response.references[0].file_path == "worklog://101"
