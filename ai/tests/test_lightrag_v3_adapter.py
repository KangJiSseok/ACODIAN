import asyncio
import os
from dataclasses import dataclass
from typing import Any

import pytest

from app.config.settings import Settings
from app.light.v3.service.lightrag_adapter import (
    LightRagConfigurationError,
    LightRagDependencies,
    LightRagInsertFailedError,
    LightRagInsertTimeoutError,
    LightRagQueryFailedError,
    LightRagQueryOptions,
    LightRagQueryTimeoutError,
    LightRagWorklogIndexAdapter,
    close_lightrag_worklog_index_adapter,
    get_lightrag_worklog_index_adapter,
)
from app.light.v3.service.worklog_document_builder import LightRagWorklogDocument
from app.light.v3.service.worklog_custom_kg_builder import LightRagWorklogCustomKgDocument



def make_settings() -> Settings:
    return Settings(
        _env_file=None,
        gemini_api_key="fake-gemini-api-key",
        lightrag_vector_storage="QdrantVectorDBStorage",
        lightrag_qdrant_url="http://localhost:6333",
    )


def make_document(worklog_id: int = 101) -> LightRagWorklogDocument:
    return LightRagWorklogDocument(
        document_id=f"worklog-{worklog_id}",
        file_path=f"worklog://{worklog_id}",
        text=f"source_type: WORKLOG\nworklog_id: {worklog_id}",
    )


def make_query_options(
    *,
    query: str = "요약",
    top_k: int = 40,
    chunk_top_k: int = 20,
    response_type: str = "Multiple Paragraphs",
) -> LightRagQueryOptions:
    """LightRAG query adapter 테스트용 기본 options를 만든다."""
    return LightRagQueryOptions(
        query=query,
        top_k=top_k,
        chunk_top_k=chunk_top_k,
        response_type=response_type,
    )


@pytest.fixture(autouse=True)
def clear_qdrant_env(monkeypatch: pytest.MonkeyPatch) -> None:
    """Qdrant env bridge 테스트가 process env를 오염시키지 않게 격리한다."""
    monkeypatch.delenv("QDRANT_URL", raising=False)
    monkeypatch.delenv("QDRANT_API_KEY", raising=False)
    monkeypatch.delenv("QDRANT_WORKSPACE", raising=False)


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

    async def ainsert_custom_kg(self, custom_kg: dict[str, Any], *, full_doc_id: str) -> None:
        self.calls.append(("ainsert_custom_kg", custom_kg, full_doc_id))

    async def finalize_storages(self) -> None:
        self.calls.append("finalize_storages")


@dataclass(frozen=True)
class FakeQueryParam:
    """LightRAG QueryParam 주입값을 검증하기 위한 테스트 fake."""

    mode: str
    stream: bool
    include_references: bool
    top_k: int
    chunk_top_k: int
    response_type: str


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
            query_param_class=FakeQueryParam,
        ),
        embed,
        wrapper_calls,
        completion_calls,
    )


def test_lightrag_adapter_initializes_inserts_ids_and_finalizes(monkeypatch) -> None:
    monkeypatch.delenv("QDRANT_WORKSPACE", raising=False)
    monkeypatch.setenv("QDRANT_API_KEY", "stale-api-key")
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
        assert fake_rag.kwargs["vector_storage"] == "QdrantVectorDBStorage"
        assert "workspace" not in fake_rag.kwargs
        assert fake_rag.kwargs["vector_db_storage_cls_kwargs"] == {}
        assert fake_rag.kwargs["addon_params"] == {"language": "Korean"}
        assert os.environ["QDRANT_URL"] == "http://localhost:6333"
        assert "QDRANT_API_KEY" not in os.environ

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
        "embedding_dim": 768,
        "max_token_size": 2048,
    }
    assert os.environ["QDRANT_URL"] == "http://localhost:6333"
    assert "QDRANT_API_KEY" not in os.environ


def test_lightrag_adapter_passes_configured_kg_language_to_lightrag() -> None:
    FakeLightRAG.instances = []
    dependencies, _, _, _ = make_dependencies()
    settings = make_settings()
    settings.lightrag_kg_language = "한국어"
    adapter = LightRagWorklogIndexAdapter(
        settings_obj=settings,
        dependencies=dependencies,
    )

    async def run_case() -> None:
        await adapter.index_documents([make_document()])
        fake_rag = FakeLightRAG.instances[0]

        assert fake_rag.kwargs["addon_params"] == {"language": "한국어"}

    asyncio.run(run_case())




def test_lightrag_adapter_configures_qdrant_vector_kwargs() -> None:
    FakeLightRAG.instances = []
    dependencies, _, _, _ = make_dependencies()
    adapter = LightRagWorklogIndexAdapter(
        settings_obj=make_settings(),
        dependencies=dependencies,
    )

    async def run_case() -> None:
        await adapter.index_documents([make_document()])
        fake_rag = FakeLightRAG.instances[0]

        assert fake_rag.kwargs["vector_storage"] == "QdrantVectorDBStorage"
        assert "workspace" not in fake_rag.kwargs
        assert fake_rag.kwargs["vector_db_storage_cls_kwargs"] == {}

    asyncio.run(run_case())


def test_lightrag_adapter_passes_configured_workspace() -> None:
    FakeLightRAG.instances = []
    dependencies, _, _, _ = make_dependencies()
    settings = make_settings()
    settings.lightrag_workspace = "axwms-http-smoke"
    adapter = LightRagWorklogIndexAdapter(
        settings_obj=settings,
        dependencies=dependencies,
    )

    asyncio.run(adapter.index_documents([make_document()]))

    assert FakeLightRAG.instances[0].kwargs["workspace"] == "axwms-http-smoke"


def test_lightrag_adapter_applies_qdrant_url_and_removes_empty_api_key(monkeypatch) -> None:
    FakeLightRAG.instances = []
    dependencies, _, _, _ = make_dependencies()
    settings = make_settings()
    settings.lightrag_qdrant_url = "http://localhost:6333"
    settings.lightrag_qdrant_api_key = ""
    monkeypatch.setenv("QDRANT_API_KEY", "preexisting")
    adapter = LightRagWorklogIndexAdapter(
        settings_obj=settings,
        dependencies=dependencies,
    )

    async def run_case() -> None:
        await adapter.index_documents([make_document()])

    asyncio.run(run_case())

    assert os.environ.get("QDRANT_URL") == "http://localhost:6333"
    assert "QDRANT_API_KEY" not in os.environ


def test_lightrag_adapter_sets_qdrant_api_key_when_configured(monkeypatch) -> None:
    FakeLightRAG.instances = []
    dependencies, _, _, _ = make_dependencies()
    settings = make_settings()
    settings.lightrag_qdrant_api_key = "secret"
    monkeypatch.delenv("QDRANT_API_KEY", raising=False)
    adapter = LightRagWorklogIndexAdapter(
        settings_obj=settings,
        dependencies=dependencies,
    )

    async def run_case() -> None:
        await adapter.index_documents([make_document()])

    asyncio.run(run_case())

    assert os.environ.get("QDRANT_API_KEY") == "secret"


def test_lightrag_adapter_rejects_non_qdrant_vector_storage() -> None:
    FakeLightRAG.instances = []
    dependencies, _, _, _ = make_dependencies()
    settings = make_settings()
    settings.lightrag_vector_storage = "NanoVectorDBStorage"
    adapter = LightRagWorklogIndexAdapter(
        settings_obj=settings,
        dependencies=dependencies,
    )

    with pytest.raises(LightRagConfigurationError, match="QdrantVectorDBStorage"):
        asyncio.run(adapter.index_documents([make_document()]))


def test_lightrag_adapter_rejects_blank_qdrant_url() -> None:
    FakeLightRAG.instances = []
    dependencies, _, _, _ = make_dependencies()
    settings = make_settings()
    settings.lightrag_qdrant_url = "   "
    adapter = LightRagWorklogIndexAdapter(
        settings_obj=settings,
        dependencies=dependencies,
    )

    with pytest.raises(LightRagConfigurationError, match="LIGHTRAG_QDRANT_URL"):
        asyncio.run(adapter.index_documents([make_document()]))



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



def test_lightrag_adapter_bridges_qdrant_url_and_api_key(
    monkeypatch: pytest.MonkeyPatch,
) -> None:
    FakeLightRAG.instances = []
    dependencies, _, _, _ = make_dependencies()
    settings = make_settings()
    settings.lightrag_qdrant_url = "http://qdrant:6333"
    settings.lightrag_qdrant_api_key = "fake-qdrant-key"
    adapter = LightRagWorklogIndexAdapter(
        settings_obj=settings,
        dependencies=dependencies,
    )

    async def run_case() -> None:
        await adapter.index_documents([make_document()])

    asyncio.run(run_case())

    import os

    assert os.environ["QDRANT_URL"] == "http://qdrant:6333"
    assert os.environ["QDRANT_API_KEY"] == "fake-qdrant-key"


def test_lightrag_adapter_clears_stale_qdrant_api_key_when_setting_empty(
    monkeypatch: pytest.MonkeyPatch,
) -> None:
    monkeypatch.setenv("QDRANT_API_KEY", "stale-api-key")
    dependencies, _, _, _ = make_dependencies()
    settings = make_settings()
    settings.lightrag_qdrant_api_key = ""
    adapter = LightRagWorklogIndexAdapter(
        settings_obj=settings,
        dependencies=dependencies,
    )

    async def run_case() -> None:
        await adapter.index_documents([make_document()])

    asyncio.run(run_case())

    import os

    assert os.environ["QDRANT_URL"] == "http://localhost:6333"
    assert "QDRANT_API_KEY" not in os.environ


def test_lightrag_adapter_requires_qdrant_vector_storage() -> None:
    dependencies, _, _, _ = make_dependencies()
    settings = make_settings()
    settings.lightrag_vector_storage = "NanoVectorDBStorage"
    adapter = LightRagWorklogIndexAdapter(
        settings_obj=settings,
        dependencies=dependencies,
    )

    with pytest.raises(LightRagConfigurationError, match="QdrantVectorDBStorage"):
        asyncio.run(adapter.index_documents([make_document()]))


def test_lightrag_adapter_requires_qdrant_url() -> None:
    dependencies, _, _, _ = make_dependencies()
    settings = make_settings()
    settings.lightrag_qdrant_url = ""
    adapter = LightRagWorklogIndexAdapter(
        settings_obj=settings,
        dependencies=dependencies,
    )

    with pytest.raises(LightRagConfigurationError, match="LIGHTRAG_QDRANT_URL"):
        asyncio.run(adapter.index_documents([make_document()]))



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


def test_lightrag_adapter_requires_qdrant_vector_storage() -> None:
    dependencies, _, _, _ = make_dependencies()
    settings = make_settings()
    settings.lightrag_vector_storage = "NanoVectorDBStorage"
    adapter = LightRagWorklogIndexAdapter(
        settings_obj=settings,
        dependencies=dependencies,
    )

    with pytest.raises(LightRagConfigurationError, match="QdrantVectorDBStorage"):
        asyncio.run(adapter.index_documents([make_document()]))


def test_lightrag_adapter_bridges_qdrant_url_and_api_key() -> None:
    FakeLightRAG.instances = []
    dependencies, _, _, _ = make_dependencies()
    settings = make_settings()
    settings.lightrag_qdrant_url = "https://qdrant.example.test"
    settings.lightrag_qdrant_api_key = "fake-qdrant-api-key"
    adapter = LightRagWorklogIndexAdapter(
        settings_obj=settings,
        dependencies=dependencies,
    )

    asyncio.run(adapter.index_documents([make_document()]))

    assert os.environ["QDRANT_URL"] == "https://qdrant.example.test"
    assert os.environ["QDRANT_API_KEY"] == "fake-qdrant-api-key"
    assert "QDRANT_WORKSPACE" not in os.environ
    assert "workspace" not in FakeLightRAG.instances[0].kwargs


def test_lightrag_adapter_removes_stale_qdrant_workspace_env(
    monkeypatch: pytest.MonkeyPatch,
) -> None:
    FakeLightRAG.instances = []
    dependencies, _, _, _ = make_dependencies()
    monkeypatch.setenv("QDRANT_WORKSPACE", "stale-workspace")
    settings = make_settings()
    settings.lightrag_workspace = "settings-workspace"
    adapter = LightRagWorklogIndexAdapter(
        settings_obj=settings,
        dependencies=dependencies,
    )

    asyncio.run(adapter.index_documents([make_document()]))

    assert "QDRANT_WORKSPACE" not in os.environ
    assert FakeLightRAG.instances[0].kwargs["workspace"] == "settings-workspace"


def test_lightrag_adapter_removes_existing_qdrant_api_key_when_settings_key_is_empty(
    monkeypatch: pytest.MonkeyPatch,
) -> None:
    dependencies, _, _, _ = make_dependencies()
    monkeypatch.setenv("QDRANT_API_KEY", "stale-key")
    settings = make_settings()
    settings.lightrag_qdrant_api_key = ""
    adapter = LightRagWorklogIndexAdapter(
        settings_obj=settings,
        dependencies=dependencies,
    )

    asyncio.run(adapter.index_documents([make_document()]))

    assert "QDRANT_API_KEY" not in os.environ


def test_lightrag_adapter_singleton_close_boundary_resets_instance() -> None:
    first_adapter = get_lightrag_worklog_index_adapter()

    asyncio.run(close_lightrag_worklog_index_adapter())

    second_adapter = get_lightrag_worklog_index_adapter()

    assert second_adapter is not first_adapter
    asyncio.run(close_lightrag_worklog_index_adapter())


class FakeQueryLightRAG(FakeLightRAG):
    async def aquery_llm(self, query: str, *, param: Any) -> dict[str, Any]:
        self.calls.append(("aquery_llm", query, param))
        return {
            "llm_response": {"content": "native answer"},
            "data": {"references": [{"file_path": "worklog://101"}]},
        }


def test_lightrag_adapter_query_uses_mix_non_streaming_with_references() -> None:
    FakeLightRAG.instances = []
    dependencies, _, _, _ = make_dependencies(FakeQueryLightRAG)
    settings = make_settings()
    adapter = LightRagWorklogIndexAdapter(settings_obj=settings, dependencies=dependencies)

    async def run_case() -> None:
        result = await adapter.query_worklogs(
            make_query_options(
                query="업무일지 요약",
                top_k=7,
                chunk_top_k=3,
                response_type="Single Paragraph",
            )
        )
        fake_rag = FakeLightRAG.instances[0]
        _, query, param = fake_rag.calls[-1]

        assert result.raw["llm_response"]["content"] == "native answer"
        assert query == "업무일지 요약"
        assert param.mode == "mix"
        assert param.stream is False
        assert param.include_references is True
        assert param.top_k == 7
        assert param.chunk_top_k == 3
        assert param.response_type == "Single Paragraph"

    asyncio.run(run_case())


def test_lightrag_adapter_query_reuses_initialized_rag() -> None:
    FakeLightRAG.instances = []
    dependencies, _, _, _ = make_dependencies(FakeQueryLightRAG)
    adapter = LightRagWorklogIndexAdapter(settings_obj=make_settings(), dependencies=dependencies)

    async def run_case() -> None:
        options = make_query_options()
        await adapter.query_worklogs(options)
        await adapter.query_worklogs(options)

    asyncio.run(run_case())

    assert len(FakeLightRAG.instances) == 1
    assert FakeLightRAG.instances[0].calls.count("initialize_storages") == 1


def test_lightrag_adapter_query_maps_timeout() -> None:
    class TimeoutQueryLightRAG(FakeLightRAG):
        async def aquery_llm(self, query: str, *, param: Any) -> dict[str, Any]:
            await asyncio.sleep(1)
            return {"answer": "late"}

    dependencies, _, _, _ = make_dependencies(TimeoutQueryLightRAG)
    settings = make_settings()
    settings.lightrag_query_timeout_seconds = 0.001
    adapter = LightRagWorklogIndexAdapter(settings_obj=settings, dependencies=dependencies)

    with pytest.raises(LightRagQueryTimeoutError):
        asyncio.run(adapter.query_worklogs(make_query_options()))


def test_lightrag_adapter_query_maps_general_failure() -> None:
    class FailingQueryLightRAG(FakeLightRAG):
        async def aquery_llm(self, query: str, *, param: Any) -> dict[str, Any]:
            raise ValueError("boom")

    dependencies, _, _, _ = make_dependencies(FailingQueryLightRAG)
    adapter = LightRagWorklogIndexAdapter(settings_obj=make_settings(), dependencies=dependencies)

    with pytest.raises(LightRagQueryFailedError):
        asyncio.run(adapter.query_worklogs(make_query_options()))


def test_lightrag_adapter_query_preserves_configuration_error() -> None:
    dependencies, _, _, _ = make_dependencies(FakeQueryLightRAG)
    adapter = LightRagWorklogIndexAdapter(
        settings_obj=Settings(_env_file=None, gemini_api_key=""),
        dependencies=dependencies,
    )

    with pytest.raises(LightRagConfigurationError):
        asyncio.run(adapter.query_worklogs(make_query_options()))


def test_lightrag_adapter_indexes_custom_kg_documents_with_document_id() -> None:
    FakeLightRAG.instances = []
    dependencies, _, _, _ = make_dependencies()
    adapter = LightRagWorklogIndexAdapter(
        settings_obj=make_settings(),
        dependencies=dependencies,
    )
    custom_kg = {"chunks": [{"content": "c", "source_id": "pg:tb_worklog:101"}]}
    document = LightRagWorklogCustomKgDocument(
        document_id="worklog-101",
        custom_kg=custom_kg,
    )

    async def run_case() -> None:
        await adapter.index_custom_kg_documents([document])
        fake_rag = FakeLightRAG.instances[0]

        assert fake_rag.calls == [
            "initialize_storages",
            ("ainsert_custom_kg", custom_kg, "worklog-101"),
        ]

    asyncio.run(run_case())


def test_lightrag_adapter_maps_custom_kg_insert_timeout() -> None:
    class TimeoutCustomKgLightRAG(FakeLightRAG):
        async def ainsert_custom_kg(self, custom_kg: dict[str, Any], *, full_doc_id: str) -> None:
            await asyncio.sleep(1)

    dependencies, _, _, _ = make_dependencies(TimeoutCustomKgLightRAG)
    settings = make_settings()
    settings.lightrag_insert_timeout_seconds = 0.001
    adapter = LightRagWorklogIndexAdapter(
        settings_obj=settings,
        dependencies=dependencies,
    )

    with pytest.raises(LightRagInsertTimeoutError):
        asyncio.run(
            adapter.index_custom_kg_documents(
                [
                    LightRagWorklogCustomKgDocument(
                        document_id="worklog-101",
                        custom_kg={"chunks": []},
                    )
                ]
            )
        )


def test_lightrag_adapter_maps_custom_kg_insert_failure() -> None:
    class FailingCustomKgLightRAG(FakeLightRAG):
        async def ainsert_custom_kg(self, custom_kg: dict[str, Any], *, full_doc_id: str) -> None:
            raise ValueError("boom")

    dependencies, _, _, _ = make_dependencies(FailingCustomKgLightRAG)
    adapter = LightRagWorklogIndexAdapter(
        settings_obj=make_settings(),
        dependencies=dependencies,
    )

    with pytest.raises(LightRagInsertFailedError):
        asyncio.run(
            adapter.index_custom_kg_documents(
                [
                    LightRagWorklogCustomKgDocument(
                        document_id="worklog-101",
                        custom_kg={"chunks": []},
                    )
                ]
            )
        )
