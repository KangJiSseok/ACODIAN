from dataclasses import dataclass
from typing import Protocol

from app.client.gemini_client import get_gemini_client
from app.graph.v1.model.worklog_query import WorklogGraphQueryRequest
from app.graph.v1.service.graph_scope_context_builder import GraphScopeContext
from app.graph.v1.prompt.worklog_query_prompt import build_graph_worklog_answer_prompt


class GraphAnswerSharedModeError(RuntimeError):
    pass


class GraphAnswerGeminiClient(Protocol):
    async def generate_text(self, prompt: str, *, model: str | None = None) -> str: ...


@dataclass(frozen=True)
class GraphAnswerOptions:
    mode: str = "LOCAL_BYOG"
    allow_general_knowledge: bool = False
    use_community_reports: bool = False


class GraphAnswerAdapter:
    def __init__(
        self,
        *,
        gemini_client_factory=get_gemini_client,
        options: GraphAnswerOptions | None = None,
    ) -> None:
        self._gemini_client_factory = gemini_client_factory
        self._options = options or GraphAnswerOptions()

    async def generate_answer(
        self,
        *,
        context: GraphScopeContext,
        request: WorklogGraphQueryRequest,
    ) -> str | None:
        self._ensure_local_context_only()
        if not context.text_units:
            return None
        prompt = build_graph_worklog_answer_prompt(
            query=request.query,
            text_units=context.text_units,
        )
        answer = await self._gemini_client_factory().generate_text(prompt)
        return answer.strip() or None

    def _ensure_local_context_only(self) -> None:
        mode = self._options.mode.strip().upper()
        if mode not in {"LOCAL_BYOG", "LOCAL"}:
            raise GraphAnswerSharedModeError(
                "GraphRAG answer adapter only allows local BYOG context"
            )
        if self._options.allow_general_knowledge:
            raise GraphAnswerSharedModeError("GraphRAG answer adapter forbids general knowledge")
        if self._options.use_community_reports:
            raise GraphAnswerSharedModeError("GraphRAG answer adapter forbids community reports")
