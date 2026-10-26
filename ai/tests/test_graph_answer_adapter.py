import asyncio

import pytest

from app.graph.v1.model.worklog_query import WorklogGraphQueryRequest
from app.graph.v1.service.graph_answer_adapter import (
    GraphAnswerAdapter,
    GraphAnswerOptions,
    GraphAnswerSharedModeError,
)
from app.graph.v1.service.graph_scope_context_builder import GraphScopeContextBuilder
from app.graph.v1.store.graph_authorized_corpus_store import GraphAuthorizedCorpus
from app.graph.v1.store.worklog_source_store import GraphWorklogSourceRow


class FakeAnswerGeminiClient:
    def __init__(self, answer: str = "허용된 정산 업무 답변") -> None:
        self.answer = answer
        self.prompts: list[str] = []

    async def generate_text(self, prompt: str, *, model: str | None = None) -> str:
        self.prompts.append(prompt)
        return self.answer


def test_answer_adapter_generates_korean_answer_from_authorized_context_only() -> None:
    client = FakeAnswerGeminiClient()
    context = GraphScopeContextBuilder().build_context(
        corpus=GraphAuthorizedCorpus(
            candidates=[],
            worklog_ids={701},
            team_ids={7},
            sources={701: _source(701, 7, "허용팀 정산")},
        ),
        request=_request(),
    )
    answer = asyncio.run(
        GraphAnswerAdapter(gemini_client_factory=lambda: client).generate_answer(
            context=context,
            request=_request(),
        )
    )

    assert answer == "허용된 정산 업무 답변"
    assert len(client.prompts) == 1
    assert "Authorized Context" in client.prompts[0]
    assert "허용팀 정산" in client.prompts[0]
    assert "비허용팀" not in client.prompts[0]
    assert "외부 지식" in client.prompts[0]
    assert "Global/DRIFT/community report" in client.prompts[0]


def test_answer_adapter_returns_no_answer_when_authorized_context_is_empty() -> None:
    context = GraphScopeContextBuilder().build_context(
        corpus=GraphAuthorizedCorpus(candidates=[], worklog_ids=set(), team_ids=set(), sources={}),
        request=_request(),
    )
    client = FakeAnswerGeminiClient()

    answer = asyncio.run(
        GraphAnswerAdapter(gemini_client_factory=lambda: client).generate_answer(
            context=context,
            request=_request(),
        )
    )

    assert answer is None
    assert client.prompts == []


@pytest.mark.parametrize(
    "options",
    [
        GraphAnswerOptions(mode="GLOBAL"),
        GraphAnswerOptions(mode="DRIFT"),
        GraphAnswerOptions(mode="LOCAL_BYOG", allow_general_knowledge=True),
        GraphAnswerOptions(mode="LOCAL_BYOG", use_community_reports=True),
    ],
)
def test_answer_adapter_blocks_shared_global_drift_and_general_knowledge_modes(
    options: GraphAnswerOptions,
) -> None:
    context = GraphScopeContextBuilder().build_context(
        corpus=GraphAuthorizedCorpus(
            candidates=[],
            worklog_ids={701},
            team_ids={7},
            sources={701: _source(701, 7, "허용팀 정산")},
        ),
        request=_request(),
    )

    with pytest.raises(GraphAnswerSharedModeError):
        asyncio.run(
            GraphAnswerAdapter(
                gemini_client_factory=lambda: FakeAnswerGeminiClient(),
                options=options,
            ).generate_answer(context=context, request=_request())
        )


def _request() -> WorklogGraphQueryRequest:
    return WorklogGraphQueryRequest.model_validate(
        {"query": "정산 업무 알려줘", "scopeMode": "TEAM_IDS", "allowedTeamIds": [7]}
    )


def _source(worklog_id: int, team_id: int, title: str) -> GraphWorklogSourceRow:
    return GraphWorklogSourceRow(
        worklog_id=worklog_id,
        title=title,
        request_content=None,
        work_content="수행",
        author_id=5,
        author_name="공유작성자",
        team_id=team_id,
        team_name=f"team-{team_id}",
        tags=[],
        direct_predecessors=[],
    )
