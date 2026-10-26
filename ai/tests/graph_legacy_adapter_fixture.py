from app.graph.v1.service.worklog_graph_builder import GraphEdge, GraphNode
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
                    source_worklog_ids=[101],
                    source_team_ids=[7],
                    properties={"worklogId": 101, "title": "정산"},
                ),
                GraphNode(
                    node_id="team:7",
                    label="Team",
                    source_worklog_ids=[101],
                    source_team_ids=[7],
                    properties={"teamId": 7},
                ),
            ],
            edges=[
                GraphEdge(
                    edge_id="e1",
                    source_id="worklog:101",
                    target_id="team:7",
                    type="BELONGS_TO",
                    source_worklog_ids=[101],
                    source_team_ids=[7],
                    properties={"worklogId": 101},
                )
            ],
        )


class LeakyAdapter:
    def __init__(self) -> None:
        self.calls: list[dict] = []

    async def fetch_worklog_subgraph(self, **kwargs) -> GraphRagSubgraph:
        self.calls.append(kwargs)
        return GraphRagSubgraph(
            nodes=[
                GraphNode(
                    node_id="worklog:701",
                    label="Worklog",
                    source_worklog_ids=[701],
                    source_team_ids=[7],
                    properties={
                        "worklogId": 701,
                        "teamId": 7,
                        "title": "허용팀 정산",
                        "authorName": "공유작성자",
                        "tagName": "공유태그",
                    },
                ),
                GraphNode(
                    node_id="worklog:801",
                    label="Worklog",
                    source_worklog_ids=[801],
                    source_team_ids=[8],
                    properties={
                        "worklogId": 801,
                        "teamId": 8,
                        "title": "비허용팀 선행업무",
                        "authorName": "공유작성자",
                        "tagName": "공유태그",
                    },
                ),
                GraphNode(
                    node_id="team:8",
                    label="Team",
                    source_worklog_ids=[801],
                    source_team_ids=[8],
                    properties={"teamId": 8, "teamName": "비허용팀"},
                ),
            ],
            edges=[
                GraphEdge(
                    edge_id="cross-team-predecessor",
                    source_id="worklog:701",
                    target_id="worklog:801",
                    type="DEPENDS_ON",
                    source_worklog_ids=[701, 801],
                    source_team_ids=[7, 8],
                    properties={"worklogId": 701, "predecessorWorklogId": 801, "teamId": 8},
                )
            ],
        )
