import asyncio
from typing import Any

import pytest

from app.config.settings import Settings
from app.light.v3.service.lightrag_adapter import (
    LightRagConfigurationError,
    LightRagDependencies,
    LightRagInsertFailedError,
    LightRagInsertTimeoutError,
    LightRagWorklogIndexAdapter,
    close_lightrag_worklog_index_adapter,
    get_lightrag_worklog_index_adapter,
)
from app.light.v3.service.worklog_document_builder import LightRagWorklogDocument


def make_settings() -> Settings:
    return Settings(_env_file=None, gemini_api_key="fake-gemini-api-key")


def make_document(worklog_id: int = 101) -> LightRagWorklogDocument:
    return LightRagWorklogDocument(
        document_id=f"worklog-{worklog_id}",
        file_path=f"worklog://{worklog_id}",
        text=f"source_type: WORKLOG\nworklog_id: {worklog_id}",
    )


class FakeGeminiEmbed:
    def __init__(self) -> None:
        self.calls: list[tuple[list[str], dict[str, Any]]] = []

    async def func(self, texts: list[str], **kwargs: Any) -> list[list[float]]:
        self.calls.append((texts, kwargs))
        return [[0.0] * 768 for _ in texts]


class FakeLightRAG:
    instances: list["FakeLightRAG"] = []

    def __init__(self, **kwargs: Any) -> None:
        self.kwargs = kwargs
        self.calls: list[Any] = []
        FakeLightRAG.instances.append(self)

    async def initialize_storages(self) -> None:
        self.calls.append("initialize_storages")

    async def ainsert(
        self,
        texts: list[str],
        *,
        ids: list[str],
        file_paths: list[str],
    ) -> None:
        self.calls.append(("ainsert", texts, ids, file_paths))

    async def finalize_storages(self) -> None:
        self.calls.append("finalize_storages")


def make_dependencies(
    lightrag_class: type[Any] = FakeLightRAG,
) -> tuple[LightRagDependencies, FakeGeminiEmbed, list[dict[str, Any]], list[dict[str, Any]]]:
    embed = FakeGeminiEmbed()
    wrapper_calls: list[dict[str, Any]] = []
    completion_calls: list[dict[str, Any]] = []

    async def fake_complete(prompt: str, **kwargs: Any) -> str:
        completion_calls.append({"prompt": prompt, **kwargs})
        return "ok"

    def fake_embedding_wrapper(**attrs: Any):
        wrapper_calls.append(attrs)

        def decorate(func):
            return func

        return decorate

    return (
        LightRagDependencies(
            lightrag_class=lightrag_class,
            gemini_model_complete=fake_complete,
            gemini_embed=embed,
            embedding_wrapper=fake_embedding_wrapper,
        ),
        embed,
        wrapper_calls,
        completion_calls,
    )


def test_lightrag_adapter_initializes_inserts_ids_and_finalizes() -> None:
    FakeLightRAG.instances = []
    dependencies, embed, wrapper_calls, completion_calls = make_dependencies()
    adapter = LightRagWorklogIndexAdapter(
        settings_obj=make_settings(),
        dependencies=dependencies,
    )
    document = make_document(101)

    async def run_case() -> None:
        await adapter.index_documents([document])
        fake_rag = FakeLightRAG.instances[0]
        assert fake_rag.calls == [
            "initialize_storages",
            (
                "ainsert",
                [document.text],
                ["worklog-101"],
                ["worklog://101"],
            ),
        ]
        assert fake_rag.kwargs["working_dir"] == "./data/lightrag-v3"
        assert fake_rag.kwargs["llm_model_name"] == "gemini-2.5-flash"

        await fake_rag.kwargs["llm_model_func"]("prompt", keyword_extraction=True)
        await fake_rag.kwargs["embedding_func"](["text"])
        await adapter.close()

        assert fake_rag.calls[-1] == "finalize_storages"

    asyncio.run(run_case())

    assert wrapper_calls == [
        {
            "embedding_dim": 768,
            "max_token_size": 2048,
            "model_name": "gemini-embedding-001",
        }
    ]
    assert completion_calls[0]["api_key"] == "fake-gemini-api-key"
    assert completion_calls[0]["model_name"] == "gemini-2.5-flash"
    assert embed.calls[0][1] == {
        "api_key": "fake-gemini-api-key",
        "model": "gemini-embedding-001",
    }


def test_lightrag_adapter_batches_multiple_documents_with_ordered_ids_and_file_paths() -> None:
    FakeLightRAG.instances = []
    dependencies, _, _, _ = make_dependencies()
    adapter = LightRagWorklogIndexAdapter(
        settings_obj=make_settings(),
        dependencies=dependencies,
    )
    documents = [make_document(101), make_document(103)]

    async def run_case() -> None:
        await adapter.index_documents(documents)
        fake_rag = FakeLightRAG.instances[0]

        assert fake_rag.calls == [
            "initialize_storages",
            (
                "ainsert",
                [documents[0].text, documents[1].text],
                ["worklog-101", "worklog-103"],
                ["worklog://101", "worklog://103"],
            ),
        ]

    asyncio.run(run_case())


def test_lightrag_adapter_maps_insert_timeout() -> None:
    class TimeoutLightRAG(FakeLightRAG):
        async def ainsert(
            self,
            texts: list[str],
            *,
            ids: list[str],
            file_paths: list[str],
        ) -> None:
            await asyncio.sleep(1)

    dependencies, _, _, _ = make_dependencies(TimeoutLightRAG)
    settings = make_settings()
    settings.lightrag_insert_timeout_seconds = 0.001
    adapter = LightRagWorklogIndexAdapter(
        settings_obj=settings,
        dependencies=dependencies,
    )

    with pytest.raises(LightRagInsertTimeoutError):
        asyncio.run(adapter.index_documents([make_document()]))


def test_lightrag_adapter_maps_general_insert_failure() -> None:
    class FailingLightRAG(FakeLightRAG):
        async def ainsert(
            self,
            texts: list[str],
            *,
            ids: list[str],
            file_paths: list[str],
        ) -> None:
            raise ValueError("boom")

    dependencies, _, _, _ = make_dependencies(FailingLightRAG)
    adapter = LightRagWorklogIndexAdapter(
        settings_obj=make_settings(),
        dependencies=dependencies,
    )

    with pytest.raises(LightRagInsertFailedError):
        asyncio.run(adapter.index_documents([make_document()]))


def test_lightrag_adapter_requires_gemini_api_key() -> None:
    dependencies, _, _, _ = make_dependencies()
    adapter = LightRagWorklogIndexAdapter(
        settings_obj=Settings(_env_file=None, gemini_api_key=""),
        dependencies=dependencies,
    )

    with pytest.raises(LightRagConfigurationError):
        asyncio.run(adapter.index_documents([make_document()]))


def test_lightrag_adapter_singleton_close_boundary_resets_instance() -> None:
    first_adapter = get_lightrag_worklog_index_adapter()

    asyncio.run(close_lightrag_worklog_index_adapter())

    second_adapter = get_lightrag_worklog_index_adapter()

    assert second_adapter is not first_adapter
    asyncio.run(close_lightrag_worklog_index_adapter())
