from __future__ import annotations

from typing import Any

import asyncio

from app.config.settings import Settings
from app.light.v3.model.worklog_query import WorklogLightQueryRequest
from app.light.v3.service.lightrag_adapter import (
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

    assert adapter.calls == [
        LightRagQueryOptions(
            query="업무일지 요약",
            top_k=11,
            chunk_top_k=5,
            response_type="Multiple Paragraphs",
        )
    ]
    assert response.answer == "native answer"
    assert response.references[0].reference_id == "1"
    assert response.references[0].file_path == "worklog://1"
    assert response.references[1].reference_id == "2"
    assert response.references[1].file_path == "worklog://2"
    assert response.internal_only is True
