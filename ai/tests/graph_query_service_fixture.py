from app.graph.v1.model.worklog_query import WorklogGraphScopeMode
from app.graph.v1.service.jev_router import BOTH
from app.graph.v1.store.graph_authorized_corpus_store import GraphAuthorizedCorpus
from app.graph.v1.store.graph_authorized_corpus_store import GraphAuthorizedWorklogCandidate
from app.graph.v1.store.worklog_source_store import (
    GraphWorklogPredecessor,
    GraphWorklogSourceRow,
    GraphWorklogTag,
)


class FakeGeminiClient:
    def __init__(self) -> None:
        self.embed_calls: list[list[str]] = []
        self.generate_text_calls: list[str] = []

    async def embed(self, texts: list[str]) -> list[list[float]]:
        self.embed_calls.append(texts)
        return [[0.1, 0.2, 0.3]]

    async def generate_text(self, prompt: str, *, model: str | None = None) -> str:
        self.generate_text_calls.append(prompt)
        return "허용팀 정산 답변"


class FakeSessionContext:
    def __init__(self) -> None:
        self.session = object()

    async def __aenter__(self) -> object:
        return self.session

    async def __aexit__(self, *args: object) -> None:
        return None


class FakeRouter:
    async def classify(self, query: str):
        return BOTH


class FakeGraphAdapter:
    def __init__(self, links=None):
        self.links = links or []
        self.calls = []

    async def fetch_worklog_links(self, root_ids, allowed_team_ids, limit):
        self.calls.append((root_ids, allowed_team_ids, limit))
        return self.links


class FakeCorpusStore:
    def __init__(self) -> None:
        self.calls: list[dict] = []
        self.filtered_out_worklog_ids: list[int] = []
        self.filtered_out_predecessor_ids: list[int] = []
        self._last_sources: dict[int, GraphWorklogSourceRow] = {}

    async def fetch_authorized_corpus(self, **kwargs) -> GraphAuthorizedCorpus:
        self.calls.append(kwargs)
        request = kwargs["request"]
        sources = _all_sources()
        if request.scope_mode == WorklogGraphScopeMode.TEAM_IDS:
            allowed_team_ids = {int(team_id) for team_id in request.allowed_team_ids or []}
            filtered_sources = {
                worklog_id: _filter_predecessors(source, allowed_team_ids, self)
                for worklog_id, source in sources.items()
                if source.team_id in allowed_team_ids
            }
        else:
            filtered_sources = sources
        self.filtered_out_worklog_ids = sorted(set(sources) - set(filtered_sources))
        self._last_sources = filtered_sources
        return GraphAuthorizedCorpus(
            candidates=[
                GraphAuthorizedWorklogCandidate(
                    worklog_id=source.worklog_id,
                    score=0.9,
                    chunk_index=0,
                    matched_chunk=source.title,
                )
                for source in filtered_sources.values()
            ],
            worklog_ids=set(filtered_sources),
            team_ids={source.team_id for source in filtered_sources.values()},
            sources=filtered_sources,
        )

    async def fetch_sources_by_ids(self, session, ids, request):
        return {id_: source for id_, source in self._last_sources.items() if id_ in ids}

    async def find_exact_title_ids(self, session, title, request):
        corpus = await self.fetch_authorized_corpus(session=session, request=request, query_embedding=[])
        return [id_ for id_, source in corpus.sources.items() if source.title == title][:2]


def _all_sources() -> dict[int, GraphWorklogSourceRow]:
    return {
        701: _source(
            worklog_id=701,
            team_id=7,
            title="허용팀 정산",
            team_name="허용팀",
            author_name="공유작성자",
            tag_name="공유태그",
            predecessor=GraphWorklogPredecessor(
                worklog_id=702,
                team_id=7,
                title="허용팀 선행업무",
            ),
            cross_team_predecessor=GraphWorklogPredecessor(
                worklog_id=801,
                team_id=8,
                title="비허용팀 선행업무",
            ),
        ),
        801: _source(
            worklog_id=801,
            team_id=8,
            title="비허용팀 선행업무",
            team_name="비허용팀",
            author_name="비허용작성자",
            tag_name="비허용태그",
        ),
    }


def _source(
    *,
    worklog_id: int,
    team_id: int,
    title: str,
    team_name: str,
    author_name: str,
    tag_name: str,
    predecessor: GraphWorklogPredecessor | None = None,
    cross_team_predecessor: GraphWorklogPredecessor | None = None,
) -> GraphWorklogSourceRow:
    predecessors = [
        candidate
        for candidate in (predecessor, cross_team_predecessor)
        if candidate is not None
    ]
    return GraphWorklogSourceRow(
        worklog_id=worklog_id,
        title=title,
        request_content="요청",
        work_content="수행",
        author_id=5,
        author_name=author_name,
        team_id=team_id,
        team_name=team_name,
        tags=[GraphWorklogTag(tag_id=11, tag_name=tag_name, description=None)],
        direct_predecessors=predecessors,
    )


def _filter_predecessors(
    source: GraphWorklogSourceRow,
    allowed_team_ids: set[int],
    store: FakeCorpusStore,
) -> GraphWorklogSourceRow:
    allowed_predecessors = [
        predecessor
        for predecessor in source.direct_predecessors
        if predecessor.team_id in allowed_team_ids
    ]
    store.filtered_out_predecessor_ids.extend(
        predecessor.worklog_id
        for predecessor in source.direct_predecessors
        if predecessor.team_id not in allowed_team_ids
    )
    return GraphWorklogSourceRow(
        worklog_id=source.worklog_id,
        title=source.title,
        request_content=source.request_content,
        work_content=source.work_content,
        author_id=source.author_id,
        author_name=source.author_name,
        team_id=source.team_id,
        team_name=source.team_name,
        tags=source.tags,
        direct_predecessors=allowed_predecessors,
    )
