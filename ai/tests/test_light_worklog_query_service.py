from __future__ import annotations

from typing import Any

import asyncio

import pytest

from app.config.settings import Settings
from app.light.v3.model.worklog_query import WorklogLightQueryRequest
from app.light.v3.service.lightrag_adapter import (
    LightRagQueryFailedError,
    LightRagQueryOptions,
    LightRagQueryResult,
)
from app.light.v3.service.worklog_query_service import LightWorklogQueryService


class FakeAdapter:
    def __init__(self, raw: dict[str, Any]) -> None:
        self.raw = raw
        self.calls: list[LightRagQueryOptions] = []

    async def query_worklogs(self, options: LightRagQueryOptions) -> LightRagQueryResult:
        self.calls.append(options)
        return LightRagQueryResult(raw=self.raw)


def make_request(**overrides: Any) -> WorklogLightQueryRequest:
    payload = {"query": "업무일지 요약", **overrides}
    return WorklogLightQueryRequest.model_validate(payload)


def test_query_service_applies_settings_defaults_and_normalizes_reference() -> None:
    adapter = FakeAdapter(
        {
            "llm_response": {"content": " native answer "},
            "data": {
                "references": [
                    {"reference_id": "1", "file_path": "worklog://1"},
                    {"reference_id": "2", "file_path": "worklog://2"},
                ]
            },
        }
    )
    settings = Settings(
        _env_file=None,
        lightrag_query_top_k=11,
        lightrag_query_chunk_top_k=5,
        lightrag_query_response_type="Multiple Paragraphs",
    )
    service = LightWorklogQueryService(settings_obj=settings, adapter_factory=lambda: adapter)

    response = asyncio.run(service.query_worklogs(make_request()))

    assert len(adapter.calls) == 1
    call = adapter.calls[0]
    assert call.query == "업무일지 요약"
    assert call.top_k == 11
    assert call.chunk_top_k == 5
    assert call.response_type == "Multiple Paragraphs"
    assert call.system_prompt is not None
    assert "allowedTeamIds" not in call.system_prompt
    assert "Access Scope" not in call.system_prompt
    assert response.answer == "native answer"
    assert response.references[0].reference_id == "1"
    assert response.references[0].file_path == "worklog://1"
    assert response.references[1].reference_id == "2"
    assert response.references[1].file_path == "worklog://2"
    assert response.internal_only is True


def test_query_service_omits_allowed_team_ids_from_system_prompt() -> None:
    adapter = FakeAdapter(
        {
            "llm_response": {"content": "answer"},
            "data": {"references": []},
        }
    )
    service = LightWorklogQueryService(
        settings_obj=Settings(_env_file=None),
        adapter_factory=lambda: adapter,
    )

    asyncio.run(service.query_worklogs(make_request(allowedTeamIds=[106])))

    assert len(adapter.calls) == 1
    assert adapter.calls[0].system_prompt is not None
    assert "allowedTeamIds" not in adapter.calls[0].system_prompt
    assert "teamId" not in adapter.calls[0].system_prompt
    assert "https://k14s209.p.ssafy.io:8443/worklog/detail/<worklog_id>" in (
        adapter.calls[0].system_prompt
    )


def test_query_service_does_not_treat_empty_allowed_team_ids_as_prompt_restriction() -> None:
    adapter = FakeAdapter(
        {
            "llm_response": {"content": "answer"},
            "data": {"references": []},
        }
    )
    service = LightWorklogQueryService(
        settings_obj=Settings(_env_file=None),
        adapter_factory=lambda: adapter,
    )

    asyncio.run(service.query_worklogs(make_request(allowedTeamIds=[])))

    assert len(adapter.calls) == 1
    assert adapter.calls[0].system_prompt is not None
    assert "allowedTeamIds" not in adapter.calls[0].system_prompt
    assert "no teams are permitted" not in adapter.calls[0].system_prompt


def test_query_service_does_not_add_all_scope_for_null_allowed_team_ids() -> None:
    adapter = FakeAdapter(
        {
            "llm_response": {"content": "answer"},
            "data": {"references": []},
        }
    )
    service = LightWorklogQueryService(
        settings_obj=Settings(_env_file=None),
        adapter_factory=lambda: adapter,
    )

    asyncio.run(service.query_worklogs(make_request(allowedTeamIds=None)))

    assert len(adapter.calls) == 1
    assert adapter.calls[0].system_prompt is not None
    assert "allowedTeamIds" not in adapter.calls[0].system_prompt
    assert "ALL" not in adapter.calls[0].system_prompt


def test_query_service_rejects_empty_llm_content() -> None:
    adapter = FakeAdapter({"llm_response": {"content": None}, "data": {"references": []}})
    service = LightWorklogQueryService(
        settings_obj=Settings(_env_file=None),
        adapter_factory=lambda: adapter,
    )

    with pytest.raises(LightRagQueryFailedError, match="empty LLM response"):
        asyncio.run(service.query_worklogs(make_request()))


def test_query_service_rejects_missing_llm_response() -> None:
    adapter = FakeAdapter({"data": {"references": []}})
    service = LightWorklogQueryService(
        settings_obj=Settings(_env_file=None),
        adapter_factory=lambda: adapter,
    )

    with pytest.raises(LightRagQueryFailedError, match="invalid LLM response"):
        asyncio.run(service.query_worklogs(make_request()))


def test_query_service_allows_missing_references() -> None:
    adapter = FakeAdapter({"llm_response": {"content": "answer"}})
    service = LightWorklogQueryService(
        settings_obj=Settings(_env_file=None),
        adapter_factory=lambda: adapter,
    )

    response = asyncio.run(service.query_worklogs(make_request()))

    assert response.answer == "answer"
    assert response.references == []
