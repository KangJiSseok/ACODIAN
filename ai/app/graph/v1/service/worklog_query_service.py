"""GraphRAG v1 업무일지 query orchestration service."""

import logging
import re
import time
from dataclasses import replace
from typing import assert_never

from app.client.gemini_client import get_gemini_client
from app.config.settings import Settings, settings
from app.graph.v1.model.worklog_query import (
    WorklogGraphEdgeItem,
    WorklogGraphNodeItem,
    WorklogGraphQueryRequest,
    WorklogGraphQueryResponse,
    WorklogGraphReferenceItem,
    WorklogGraphScopeMode,
)
from app.graph.v1.service.graph_answer_adapter import GraphAnswerAdapter
from app.graph.v1.service.graph_rag_adapter import GraphRagAdapter, get_graph_rag_worklog_adapter
from app.graph.v1.service.graph_scope_context_builder import GraphScopeContextBuilder
from app.graph.v1.service.jev_router import BOTH, JevRouter, SearchRoute
from app.graph.v1.store.graph_authorized_corpus_store import (
    GraphAuthorizedCorpus,
    GraphWorklogAuthorizedCorpusStore,
)
from app.graph.v1.store.graph_rag_store import (
    GraphRagConfigurationError,
    GraphRagQueryFailedError,
    GraphRagQueryTimeoutError,
    GraphRagSubgraph,
    GraphWorklogLink,
)
from app.store.session import get_session_factory

logger = logging.getLogger(__name__)
_RELATION_SUBJECT = (
    re.compile(r"(.+?)의\s*선행\s*업무"),
    re.compile(r"(.+?)가\s*의존하는\s*업무"),
)


class GraphWorklogQueryService:
    """업무일지 GraphRAG query 유스케이스 진입점."""

    def __init__(
        self,
        *,
        settings_obj: Settings = settings,
        adapter_factory=None,
        session_factory=get_session_factory,
        gemini_client_factory=get_gemini_client,
        corpus_store: GraphWorklogAuthorizedCorpusStore | None = None,
        context_builder: GraphScopeContextBuilder | None = None,
        answer_adapter: GraphAnswerAdapter | None = None,
        graph_adapter=None,
        router=None,
    ) -> None:
        self._settings = settings_obj
        self._adapter_factory = adapter_factory
        self._session_factory = session_factory
        self._gemini_client_factory = gemini_client_factory
        self._corpus_store = corpus_store or GraphWorklogAuthorizedCorpusStore()
        self._context_builder = context_builder or GraphScopeContextBuilder()
        self._answer_adapter = answer_adapter or GraphAnswerAdapter(
            gemini_client_factory=gemini_client_factory
        )
        self._graph_adapter = graph_adapter
        self._router = router or JevRouter(settings_obj)
        self.captured_context_snapshot: dict | None = None

    async def query_worklogs(self, request: WorklogGraphQueryRequest) -> WorklogGraphQueryResponse:
        """GraphRAG subgraph retrieval 결과를 구조화 응답으로 변환한다."""
        if self._adapter_factory is None:
            return await self._query_authorized_context(request)
        adapter: GraphRagAdapter = self._adapter_factory()
        subgraph = await adapter.fetch_worklog_subgraph(
            query=request.query,
            allowed_team_ids=[int(team_id) for team_id in request.allowed_team_ids]
            if request.allowed_team_ids is not None
            else None,
            max_depth=int(request.max_depth or self._settings.graphrag_query_max_depth),
            limit=int(request.limit or self._settings.graphrag_query_limit),
        )
        authorized_subgraph = self._filter_authorized_subgraph(subgraph, request)
        return self._build_response(authorized_subgraph)

    async def _query_authorized_context(
        self,
        request: WorklogGraphQueryRequest,
    ) -> WorklogGraphQueryResponse:
        started = time.monotonic()
        predicted = await self._router.classify(request.query)
        route = predicted if self._settings.graphrag_router_mode == "active" else BOTH
        explicit_title = self._relation_title(request.query)
        if explicit_title:
            route = SearchRoute(relation=True, similarity=route.similarity)
        logger.info(
            "graph route relation=%s similarity=%s shadow=%s",
            predicted.relation,
            predicted.similarity,
            self._settings.graphrag_router_mode != "active",
        )
        relation_title = explicit_title if route.relation else None
        if route.relation and not relation_title and not route.similarity:
            route = BOTH
        session_factory = self._session_factory()
        async with session_factory() as session:
            exact_ids = []
            if relation_title:
                exact_ids = await self._corpus_store.find_exact_title_ids(session, relation_title, request)
                if len(exact_ids) != 1:
                    logger.info(
                        "graph query duration_ms=%d references=0",
                        int((time.monotonic() - started) * 1000),
                    )
                    return self._build_response(
                        GraphRagSubgraph(nodes=[], edges=[]),
                        answer="업무를 정확히 식별할 수 없습니다. 제목을 확인해 주세요.",
                    )
            if route.similarity:
                query_embedding = (await self._gemini_client_factory().embed([request.query]))[0]
                corpus = await self._corpus_store.fetch_authorized_corpus(
                    session=session, request=request, query_embedding=query_embedding
                )
            else:
                corpus = GraphAuthorizedCorpus(candidates=[], worklog_ids=set(), team_ids=set(), sources={})
            root_ids = list(dict.fromkeys([*exact_ids, *(candidate.worklog_id for candidate in corpus.candidates)]))
            if root_ids:
                try:
                    links = await (self._graph_adapter or get_graph_rag_worklog_adapter()).fetch_worklog_links(
                        root_ids, self._allowed_team_ids(request), max(50, int(request.limit or 20) * 3)
                    )
                except (GraphRagConfigurationError, GraphRagQueryFailedError, GraphRagQueryTimeoutError):
                    logger.warning("Neo4j graph expansion failed")
                    if not route.similarity:
                        logger.info(
                            "graph query duration_ms=%d references=0",
                            int((time.monotonic() - started) * 1000),
                        )
                        return self._build_response(
                            GraphRagSubgraph(nodes=[], edges=[]),
                            answer="관계 색인을 확인할 수 없습니다. 잠시 후 다시 시도해 주세요.",
                        )
                    links = []
                ids = list(dict.fromkeys([*root_ids, *(link.target_id for link in links)]))
                sources = await self._corpus_store.fetch_sources_by_ids(session, ids, request)
                valid_links = [
                    link for link in links
                    if link.source_id in sources and link.target_id in sources
                    and self._is_current_link(link, sources)
                ]
                rank = {worklog_id: position for position, worklog_id in enumerate(root_ids)}
                valid_links.sort(key=lambda link: rank.get(link.source_id, len(root_ids)))
                if relation_title:
                    relation_root = root_ids[0]
                    valid_links = [
                        link for link in valid_links
                        if link.source_id == relation_root and link.relation == "DEPENDS_ON"
                    ]
                    if not route.similarity:
                        root_ids = [relation_root]
                selected_ids = self._select_context_ids(
                    root_ids, valid_links, int(request.limit or 20)
                )
                selected_sources = {id_: sources[id_] for id_ in selected_ids if id_ in sources}
                corpus = replace(
                    corpus,
                    candidates=[candidate for candidate in corpus.candidates if candidate.worklog_id in selected_sources],
                    worklog_ids=set(selected_sources),
                    team_ids={source.team_id for source in selected_sources.values()},
                    sources=selected_sources,
                )
        context = self._context_builder.build_context(corpus=corpus, request=request)
        self.captured_context_snapshot = context.snapshot
        answer = await self._answer_adapter.generate_answer(context=context, request=request)
        logger.info(
            "graph query duration_ms=%d references=%d",
            int((time.monotonic() - started) * 1000),
            len(context.subgraph.nodes),
        )
        return self._build_response(context.subgraph, answer=answer)

    @staticmethod
    def _relation_title(query: str) -> str | None:
        for pattern in _RELATION_SUBJECT:
            match = pattern.search(query)
            if match:
                return match.group(1).strip(" \"'‘’“”")
        return None

    @staticmethod
    def _allowed_team_ids(request: WorklogGraphQueryRequest) -> list[int] | None:
        return [int(id_) for id_ in request.allowed_team_ids] if request.allowed_team_ids else None

    @staticmethod
    def _is_current_link(link: GraphWorklogLink, sources: dict) -> bool:
        source = sources[link.source_id]
        target = sources[link.target_id]
        if link.relation == "DEPENDS_ON":
            return any(p.worklog_id == target.worklog_id for p in source.direct_predecessors)
        if link.relation == "AUTHORED_BY":
            return source.author_id == target.author_id
        if link.relation == "HAS_TAG":
            return bool({tag.tag_id for tag in source.tags} & {tag.tag_id for tag in target.tags})
        return False

    @staticmethod
    def _select_context_ids(
        roots: list[int], links: list[GraphWorklogLink], limit: int
    ) -> list[int]:
        selected = roots[:1]
        for link in links:
            pair = [id_ for id_ in (link.source_id, link.target_id) if id_ not in selected]
            if len(selected) + len(pair) <= limit:
                selected.extend(pair)
        for root in roots:
            if len(selected) >= limit:
                break
            if root not in selected:
                selected.append(root)
        return selected

    def _filter_authorized_subgraph(
        self,
        subgraph: GraphRagSubgraph,
        request: WorklogGraphQueryRequest,
    ) -> GraphRagSubgraph:
        """TEAM_IDS 요청에서는 provenance team set이 허용 범위의 subset인 graph 객체만 통과시킨다."""
        match request.scope_mode:
            case WorklogGraphScopeMode.ALL:
                return GraphRagSubgraph(
                    nodes=[
                        node
                        for node in subgraph.nodes
                        if _has_complete_provenance(node.source_worklog_ids, node.source_team_ids)
                    ],
                    edges=[
                        edge
                        for edge in subgraph.edges
                        if _has_complete_provenance(edge.source_worklog_ids, edge.source_team_ids)
                    ],
                )
            case WorklogGraphScopeMode.TEAM_IDS:
                allowed_team_ids = {int(team_id) for team_id in request.allowed_team_ids or []}
                return GraphRagSubgraph(
                    nodes=[
                        node
                        for node in subgraph.nodes
                        if _has_allowed_provenance(
                            node.source_worklog_ids,
                            node.source_team_ids,
                            allowed_team_ids,
                        )
                    ],
                    edges=[
                        edge
                        for edge in subgraph.edges
                        if _has_allowed_provenance(
                            edge.source_worklog_ids,
                            edge.source_team_ids,
                            allowed_team_ids,
                        )
                    ],
                )
            case unreachable:
                assert_never(unreachable)

    def _build_response(
        self,
        subgraph: GraphRagSubgraph,
        answer: str | None = None,
    ) -> WorklogGraphQueryResponse:
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
                    sourceWorklogIds=node.source_worklog_ids,
                    sourceTeamIds=node.source_team_ids,
                )
            )
        return WorklogGraphQueryResponse(
            answer=answer,
            nodes=[
                WorklogGraphNodeItem(
                    nodeId=node.node_id,
                    label=node.label,
                    sourceWorklogIds=node.source_worklog_ids,
                    sourceTeamIds=node.source_team_ids,
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
                    sourceWorklogIds=edge.source_worklog_ids,
                    sourceTeamIds=edge.source_team_ids,
                    properties=edge.properties,
                )
                for edge in subgraph.edges
            ],
            references=references,
            internalOnly=True,
        )


def _has_allowed_provenance(
    source_worklog_ids: list[int],
    source_team_ids: list[int],
    allowed_team_ids: set[int],
) -> bool:
    """worklog/team provenance가 모두 있고 모든 source team이 허용 범위일 때만 통과시킨다."""
    return (
        _has_complete_provenance(source_worklog_ids, source_team_ids)
        and set(source_team_ids).issubset(allowed_team_ids)
    )


def _has_complete_provenance(source_worklog_ids: list[int], source_team_ids: list[int]) -> bool:
    """ALL 범위에서도 출처 없는 graph 객체는 context나 response로 넘기지 않는다."""
    return bool(source_worklog_ids) and bool(source_team_ids)
