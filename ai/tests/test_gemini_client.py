from __future__ import annotations

import asyncio
from typing import Any

import pytest
from pydantic import BaseModel

from app.client import gemini_client as gemini_client_module
from app.client.gemini_client import GeminiClient
from app.config.settings import Settings


class RetryableGeminiError(Exception):
    code = 503


class NonRetryableGeminiError(Exception):
    code = 400


class FakeModels:
    def __init__(self, parent: "FakeGenaiClient") -> None:
        self._parent = parent

    def generate_content(self, **kwargs: Any) -> Any:
        self._parent.calls.append(("generate_content", self._parent.api_key, kwargs))
        outcome = self._parent.registry.outcomes.pop(0)
        if isinstance(outcome, BaseException):
            raise outcome
        return outcome

    def embed_content(self, **kwargs: Any) -> Any:
        self._parent.calls.append(("embed_content", self._parent.api_key, kwargs))
        outcome = self._parent.registry.outcomes.pop(0)
        if isinstance(outcome, BaseException):
            raise outcome
        return outcome


class FakeGenaiClient:
    registry: "FakeGenaiRegistry"

    def __init__(self, api_key: str) -> None:
        self.api_key = api_key
        self.calls = self.registry.calls
        self.models = FakeModels(self)


class FakeGenaiRegistry:
    def __init__(self, outcomes: list[Any]) -> None:
        self.outcomes = outcomes
        self.calls: list[tuple[str, str, dict[str, Any]]] = []

    def install(self, monkeypatch: pytest.MonkeyPatch) -> None:
        FakeGenaiClient.registry = self
        monkeypatch.setattr(gemini_client_module.genai, "Client", FakeGenaiClient)


class FakeTextResponse:
    text = "ok"


class FakeEmbedding:
    values = [0.1, 0.2]


class FakeEmbeddingResponse:
    embeddings = [FakeEmbedding()]


class FakeStructuredSchema(BaseModel):
    answer: str


class FakeStructuredResponse:
    parsed = FakeStructuredSchema(answer="ok")


def install_settings(monkeypatch: pytest.MonkeyPatch) -> Settings:
    settings = Settings(
        _env_file=None,
        gemini_api_key="key-a",
        gemini_api_keys="key-b",
        gemini_model="gemini-2.5-flash",
    )
    monkeypatch.setattr(gemini_client_module, "settings", settings)
    return settings


def test_gemini_client_failover_retries_generate_text_with_next_key(monkeypatch) -> None:
    install_settings(monkeypatch)
    registry = FakeGenaiRegistry([RetryableGeminiError(), FakeTextResponse()])
    registry.install(monkeypatch)
    client = GeminiClient()

    result = asyncio.run(client.generate_text("prompt"))

    assert result == "ok"
    assert [(name, key) for name, key, _ in registry.calls] == [
        ("generate_content", "key-a"),
        ("generate_content", "key-b"),
    ]
    assert all(call[2]["model"] == "gemini-2.5-flash" for call in registry.calls)


def test_gemini_client_does_not_failover_non_retryable_error(monkeypatch) -> None:
    install_settings(monkeypatch)
    registry = FakeGenaiRegistry([NonRetryableGeminiError("bad request")])
    registry.install(monkeypatch)
    client = GeminiClient()

    with pytest.raises(NonRetryableGeminiError):
        asyncio.run(client.generate_text("prompt"))

    assert [(name, key) for name, key, _ in registry.calls] == [
        ("generate_content", "key-a"),
    ]


def test_gemini_client_failover_retries_embed_with_next_key(monkeypatch) -> None:
    settings = install_settings(monkeypatch)
    registry = FakeGenaiRegistry([RetryableGeminiError(), FakeEmbeddingResponse()])
    registry.install(monkeypatch)
    client = GeminiClient()

    result = asyncio.run(client.embed(["text"]))

    assert result == [[0.1, 0.2]]
    assert [(name, key) for name, key, _ in registry.calls] == [
        ("embed_content", "key-a"),
        ("embed_content", "key-b"),
    ]
    assert registry.calls[1][2]["model"] == settings.embedding_model


def test_gemini_client_requires_at_least_one_key(monkeypatch) -> None:
    settings = Settings(_env_file=None, gemini_api_key="", gemini_api_keys="")
    monkeypatch.setattr(gemini_client_module, "settings", settings)

    with pytest.raises(RuntimeError, match="GEMINI_API_KEY is not configured"):
        GeminiClient()


def test_gemini_client_failover_retries_generate_structured_with_next_key(monkeypatch) -> None:
    install_settings(monkeypatch)
    registry = FakeGenaiRegistry([RetryableGeminiError(), FakeStructuredResponse()])
    registry.install(monkeypatch)
    client = GeminiClient()

    result = asyncio.run(
        client.generate_structured(
            contents=["prompt"],
            schema=FakeStructuredSchema,
            instruction="extract",
        )
    )

    assert isinstance(result, FakeStructuredSchema)
    assert result.answer == "ok"
    assert [(name, key) for name, key, _ in registry.calls] == [
        ("generate_content", "key-a"),
        ("generate_content", "key-b"),
    ]


def test_gemini_client_does_not_failover_non_retryable_error_in_generate_structured(monkeypatch) -> None:
    install_settings(monkeypatch)
    registry = FakeGenaiRegistry([NonRetryableGeminiError("bad request")])
    registry.install(monkeypatch)
    client = GeminiClient()

    with pytest.raises(NonRetryableGeminiError):
        asyncio.run(
            client.generate_structured(
                contents=["prompt"],
                schema=FakeStructuredSchema,
                instruction="extract",
            )
        )

    assert [(name, key) for name, key, _ in registry.calls] == [
        ("generate_content", "key-a"),
    ]
