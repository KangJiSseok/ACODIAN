import asyncio
from types import SimpleNamespace

from app.config.settings import Settings
from app.graph.v1.service.worklog_graph_builder import GraphEdge, GraphNode, WorklogGraphDocument
from app.graph.v1.store.graph_rag_store import GraphRagStore, _records_to_subgraph


class _Tx:
    def __init__(self) -> None:
        self.calls: list[tuple[str, dict]] = []

    async def run(self, cypher: str, **params):
        self.calls.append((cypher, params))
        return []


def test_upsert_nodes_and_edges_uses_parameterized_cypher() -> None:
    tx = _Tx()

    asyncio.run(
        GraphRagStore._upsert_nodes_and_edges(
            tx,
            nodes=[GraphNode(node_id="worklog:101", label="Worklog", properties={"title": "검색어"})],
            edges=[
                GraphEdge(
                    edge_id="e1",
                    source_id="worklog:101",
                    target_id="user:5",
                    type="AUTHORED_BY",
                    properties={"worklogId": 101},
                )
            ],
        )
    )

    assert len(tx.calls) == 2
    assert "$nodes" in tx.calls[0][0]
    assert "$edges" in tx.calls[1][0]
    assert "검색어" not in tx.calls[0][0]
    assert tx.calls[0][1]["nodes"][0]["properties"] == {"title": "검색어"}


def test_query_subgraph_passes_user_input_as_params() -> None:
    class _Record:
        def __getitem__(self, key):
            return {"root": None, "nodes": [], "edges": []}[key]

    class _Result:
        def __aiter__(self):
            self._iter = iter([_Record()])
            return self

        async def __anext__(self):
            try:
                return next(self._iter)
            except StopIteration as exc:
                raise StopAsyncIteration from exc

    class _QueryTx:
        def __init__(self) -> None:
            self.call: tuple[str, dict] | None = None

        async def run(self, cypher: str, **params):
            self.call = (cypher, params)
            return _Result()

    tx = _QueryTx()
    records = asyncio.run(
        GraphRagStore._query_subgraph(
            tx,
            query=" DROP ",
            allowed_team_ids=[7],
            max_depth=2,
            limit=3,
        )
    )

    assert records == [{"root": None, "nodes": [], "edges": []}]
    assert tx.call is not None
    cypher, params = tx.call
    assert "DROP" not in cypher
    assert params["search_text"] == "DROP"
    assert params["allowed_team_ids"] == [7]
    assert params["limit"] == 3
    assert "GRAPH_RAG_EDGE*0..2" in cypher


def test_query_subgraph_bounds_depth_with_static_cypher() -> None:
    class _Result:
        def __aiter__(self):
            self._iter = iter([])
            return self

        async def __anext__(self):
            try:
                return next(self._iter)
            except StopIteration as exc:
                raise StopAsyncIteration from exc

    class _QueryTx:
        def __init__(self) -> None:
            self.cyphers: list[str] = []

        async def run(self, cypher: str, **params):
            self.cyphers.append(cypher)
            return _Result()

    tx = _QueryTx()
    asyncio.run(GraphRagStore._query_subgraph(tx, query="x", allowed_team_ids=None, max_depth=99, limit=1))

    assert "GRAPH_RAG_EDGE*0..3" in tx.cyphers[0]


def test_records_to_subgraph_normalizes_neo4j_relationship_like_entities() -> None:
    class _Entity:
        def __init__(self, **properties) -> None:
            self._properties = properties

        def __iter__(self):
            return iter(self._properties)

        def items(self):
            return self._properties.items()

    class _Relationship(_Entity):
        def __init__(self, start_node, end_node, **properties) -> None:
            super().__init__(**properties)
            self.start_node = start_node
            self.end_node = end_node

    root = _Entity(id="worklog:2050", labels=["Worklog"], worklogId=2050, title="타임아웃")
    user = _Entity(id="user:202", labels=["User"], userId=202)
    edge = _Relationship(
        root,
        user,
        id="worklog:2050:AUTHORED_BY:user:202",
        type="AUTHORED_BY",
        worklogId=2050,
    )

    subgraph = _records_to_subgraph([{"root": root, "nodes": [root, user], "edges": [edge]}])

    assert [node.node_id for node in subgraph.nodes] == ["worklog:2050", "user:202"]
    assert subgraph.edges[0].source_id == "worklog:2050"
    assert subgraph.edges[0].target_id == "user:202"
    assert subgraph.edges[0].type == "AUTHORED_BY"


def test_upsert_worklog_graph_closes_driver() -> None:
    class _Session:
        async def __aenter__(self):
            return self

        async def __aexit__(self, exc_type, exc, tb):
            return None

        async def execute_write(self, func, nodes, edges):
            self.nodes = nodes
            self.edges = edges

    class _Driver:
        def __init__(self) -> None:
            self.session_kwargs: dict | None = None
            self.closed = False

        def session(self, **kwargs):
            self.session_kwargs = kwargs
            return _Session()

        async def close(self):
            self.closed = True

    driver = _Driver()
    store = GraphRagStore(
        settings_obj=Settings(_env_file=None, graphrag_neo4j_database="graphdb"),
        driver_factory=lambda: driver,
    )

    asyncio.run(
        store.upsert_worklog_graph(
            WorklogGraphDocument(
                worklog_id=101,
                nodes=[GraphNode(node_id="worklog:101", label="Worklog", properties={})],
                edges=[],
            )
        )
    )
    asyncio.run(store.close())

    assert driver.session_kwargs == {"database": "graphdb"}
    assert driver.closed is True
