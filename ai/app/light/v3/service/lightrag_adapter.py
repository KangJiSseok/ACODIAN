"""LightRAG v3 업무일지 index adapter와 lifecycle factory.

LightRAG v1.4.10 문서 기준으로 `LightRAG` instance를 만들고,
`initialize_storages()` 이후 `ainsert(..., ids=..., file_paths=...)`를 호출한다.
기본 테스트는 이 module의 dependency injection 경계를 통해 실제 LLM/API key를
사용하지 않는다.
"""

from __future__ import annotations

import asyncio
from collections.abc import Awaitable, Callable
from dataclasses import dataclass
from typing import Any, Protocol

from app.config.settings import Settings, settings
from app.light.v3.service.worklog_document_builder import LightRagWorklogDocument


class LightRagConfigurationError(RuntimeError):
    """LightRAG import, API key, 설정 구성 실패를 나타내는 예외."""


class LightRagInsertTimeoutError(RuntimeError):
    """LightRAG document insert가 설정된 제한 시간을 초과했음을 나타내는 예외."""


class LightRagInsertFailedError(RuntimeError):
    """LightRAG document insert 또는 storage 초기화가 실패했음을 나타내는 예외."""


class WorklogLightIndexAdapter(Protocol):
    """업무일지 문서를 LightRAG에 index하는 adapter interface."""

    async def index_documents(self, documents: list[LightRagWorklogDocument]) -> None:
        """업무일지 문서 목록을 LightRAG에 insert한다."""


@dataclass(frozen=True)
class LightRagDependencies:
    """LightRAG runtime import 결과.

    테스트는 이 dataclass에 fake class/function을 주입해 외부 dependency와 API key
    호출 없이 adapter boundary만 검증한다.
    """

    lightrag_class: type[Any]
    gemini_model_complete: Callable[..., Awaitable[str]]
    gemini_embed: Any
    embedding_wrapper: Callable[..., Callable[[Callable[..., Awaitable[Any]]], Callable[..., Awaitable[Any]]]]


class LightRagWorklogIndexAdapter:
    """LightRAG instance를 lazy 초기화하고 업무일지 document batch를 insert한다."""

    def __init__(
        self,
        *,
        settings_obj: Settings = settings,
        dependencies: LightRagDependencies | None = None,
    ) -> None:
        """adapter 설정과 optional fake dependency를 구성한다."""
        self._settings = settings_obj
        self._dependencies = dependencies
        self._rag: Any | None = None
        self._initialized = False
        self._initialize_lock = asyncio.Lock()

    async def index_documents(self, documents: list[LightRagWorklogDocument]) -> None:
        """문서를 batch로 LightRAG에 insert한다.

        `document_id`는 LightRAG `ids`로, `file_path`는 `file_paths`로 전달해 후속
        search 단계가 업무일지 ID를 회수할 수 있게 한다.
        """
        if not documents:
            return

        try:
            rag = await self._get_initialized_rag()
            await asyncio.wait_for(
                rag.ainsert(
                    [document.text for document in documents],
                    ids=[document.document_id for document in documents],
                    file_paths=[document.file_path for document in documents],
                ),
                timeout=self._settings.lightrag_insert_timeout_seconds,
            )
        except LightRagConfigurationError:
            raise
        except TimeoutError as exc:
            raise LightRagInsertTimeoutError("LightRAG insert timed out") from exc
        except Exception as exc:
            raise LightRagInsertFailedError("LightRAG insert failed") from exc

    async def close(self) -> None:
        """초기화된 LightRAG storage를 finalize하고 singleton 재사용 상태를 초기화한다."""
        rag = self._rag
        if rag is not None and self._initialized:
            await rag.finalize_storages()
        self._rag = None
        self._initialized = False

    async def _get_initialized_rag(self) -> Any:
        """LightRAG instance를 lazy singleton 방식으로 초기화한다."""
        if self._rag is not None and self._initialized:
            return self._rag

        async with self._initialize_lock:
            if self._rag is not None and self._initialized:
                return self._rag

            rag = self._build_lightrag()
            await rag.initialize_storages()
            self._rag = rag
            self._initialized = True
            return rag

    def _build_lightrag(self) -> Any:
        """현재 settings로 LightRAG instance를 생성한다."""
        self._ensure_required_config()
        dependencies = self._dependencies or _load_lightrag_dependencies()
        embedding_func = _build_gemini_embedding_func(
            settings_obj=self._settings,
            dependencies=dependencies,
        )
        llm_model_func = _build_gemini_llm_model_func(
            settings_obj=self._settings,
            dependencies=dependencies,
        )
        return dependencies.lightrag_class(
            working_dir=self._settings.lightrag_working_dir,
            llm_model_func=llm_model_func,
            llm_model_name=self._settings.lightrag_llm_model,
            embedding_func=embedding_func,
            addon_params={"language": self._settings.lightrag_kg_language},
        )

    def _ensure_required_config(self) -> None:
        """LightRAG runtime에 필요한 설정이 있는지 확인한다."""
        if not self._settings.gemini_api_key:
            raise LightRagConfigurationError("GEMINI_API_KEY is required for LightRAG")


def _load_lightrag_dependencies() -> LightRagDependencies:
    """LightRAG package와 Gemini wrapper를 지연 import한다."""
    try:
        from lightrag import LightRAG
        from lightrag.llm.gemini import gemini_embed, gemini_model_complete
        from lightrag.utils import wrap_embedding_func_with_attrs
    except Exception as exc:  # pragma: no cover - 환경별 import 실패는 config error로만 노출한다.
        raise LightRagConfigurationError("LightRAG dependency import failed") from exc

    return LightRagDependencies(
        lightrag_class=LightRAG,
        gemini_model_complete=gemini_model_complete,
        gemini_embed=gemini_embed,
        embedding_wrapper=wrap_embedding_func_with_attrs,
    )


def _build_gemini_llm_model_func(
    *,
    settings_obj: Settings,
    dependencies: LightRagDependencies,
) -> Callable[..., Awaitable[str]]:
    """Settings 기반 Gemini completion wrapper를 만든다."""

    async def llm_model_func(
        prompt: str,
        system_prompt: str | None = None,
        history_messages: list[dict[str, Any]] | None = None,
        keyword_extraction: bool = False,
        **kwargs: Any,
    ) -> str:
        return await dependencies.gemini_model_complete(
            prompt,
            system_prompt=system_prompt,
            history_messages=history_messages or [],
            keyword_extraction=keyword_extraction,
            api_key=settings_obj.gemini_api_key,
            model_name=settings_obj.lightrag_llm_model,
            **kwargs,
        )

    return llm_model_func


def _build_gemini_embedding_func(
    *,
    settings_obj: Settings,
    dependencies: LightRagDependencies,
) -> Callable[[list[str]], Awaitable[Any]]:
    """Settings 기반 Gemini embedding wrapper를 만든다."""

    @dependencies.embedding_wrapper(
        embedding_dim=settings_obj.embedding_dim,
        max_token_size=settings_obj.lightrag_embedding_max_token_size,
        model_name=settings_obj.lightrag_embedding_model,
    )
    async def embedding_func(texts: list[str]) -> Any:
        return await dependencies.gemini_embed.func(
            texts,
            api_key=settings_obj.gemini_api_key,
            model=settings_obj.lightrag_embedding_model,
            embedding_dim=settings_obj.embedding_dim,
            max_token_size=settings_obj.lightrag_embedding_max_token_size,
        )

    return embedding_func


_lightrag_worklog_index_adapter: LightRagWorklogIndexAdapter | None = None


def get_lightrag_worklog_index_adapter() -> LightRagWorklogIndexAdapter:
    """LightRAG 업무일지 index adapter singleton을 반환한다."""
    global _lightrag_worklog_index_adapter
    if _lightrag_worklog_index_adapter is None:
        _lightrag_worklog_index_adapter = LightRagWorklogIndexAdapter()
    return _lightrag_worklog_index_adapter


async def close_lightrag_worklog_index_adapter() -> None:
    """FastAPI shutdown/lifespan에서 호출할 수 있는 LightRAG finalize 경계."""
    global _lightrag_worklog_index_adapter
    if _lightrag_worklog_index_adapter is not None:
        await _lightrag_worklog_index_adapter.close()
    _lightrag_worklog_index_adapter = None
