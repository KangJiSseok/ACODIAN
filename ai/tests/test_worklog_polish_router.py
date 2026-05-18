import asyncio
from fastapi.testclient import TestClient

from app.main import app
from app.model.worklog_polish import WorklogPolishRequest
from app.model.worklog_polish import WorklogPolishResponse
from app.prompt.worklog_polish_prompt import WORKLOG_POLISH_SYSTEM_PROMPT

client = TestClient(app)


def test_worklog_polish_contract(monkeypatch) -> None:
    from app.chain import worklog_polish_chain

    captured: dict[str, object] = {}

    class FakeGeminiClient:
        async def generate_structured(self, *, contents, schema, instruction):
            captured["contents"] = contents
            captured["schema"] = schema
            captured["instruction"] = instruction
            return WorklogPolishResponse(
                workContent="배치 로그를 비교하고 병목 구간을 분리했습니다.",
            )

    monkeypatch.setattr(
        worklog_polish_chain,
        "get_gemini_client",
        lambda: FakeGeminiClient(),
    )

    response = client.post(
        "/ai/worklogs/polish",
        json={
            "requestContent": "고객사 재고 동기화 지연 원인을 정리해 주세요.",
            "workContent": "배치 로그를 비교하고 병목 구간을 분리했습니다.",
        },
    )

    assert response.status_code == 200
    assert response.json() == {
        "workContent": "배치 로그를 비교하고 병목 구간을 분리했습니다.",
    }
    assert set(response.json()) == {"workContent"}
    assert captured["schema"] is WorklogPolishResponse


def test_worklog_polish_chain_passes_prompt_boundaries(monkeypatch) -> None:
    from app.chain import worklog_polish_chain

    captured: dict[str, object] = {}

    class DummyClient:
        async def generate_structured(self, *, contents, schema, instruction):
            captured["contents"] = contents
            captured["schema"] = schema
            captured["instruction"] = instruction
            return WorklogPolishResponse(work_content="다듬은 본문")

    monkeypatch.setattr(worklog_polish_chain, "get_gemini_client", lambda: DummyClient())

    result = asyncio.run(
        worklog_polish_chain.polish_worklog_with_ai(
            WorklogPolishRequest(
                requestContent="성과 문구는 보수적으로 작성",
                workContent="장애 원인을 로그에서 확인하고 재시작 절차를 공유했습니다.",
            )
        )
    )

    assert result.work_content == "다듬은 본문"
    assert captured["schema"] is WorklogPolishResponse
    assert captured["instruction"] == WORKLOG_POLISH_SYSTEM_PROMPT
    prompt = captured["contents"][0]
    assert "성과 문구는 보수적으로 작성" in prompt
    assert "장애 원인을 로그에서 확인하고 재시작 절차를 공유했습니다." in prompt


def test_worklog_polish_prompt_keeps_factual_boundaries() -> None:
    assert "workContent" in WORKLOG_POLISH_SYSTEM_PROMPT
    assert "requestContent" in WORKLOG_POLISH_SYSTEM_PROMPT
    assert "없는 사실 생성 금지" in WORKLOG_POLISH_SYSTEM_PROMPT
    assert "50" not in WORKLOG_POLISH_SYSTEM_PROMPT
    assert "title" not in WORKLOG_POLISH_SYSTEM_PROMPT.lower()


def test_worklog_polish_response_schema_exposes_work_content_only() -> None:
    schema = WorklogPolishResponse.model_json_schema()

    assert set(schema["properties"]) == {"workContent"}
    assert schema["required"] == ["workContent"]


def test_worklog_polish_service_returns_work_content_only(monkeypatch) -> None:
    from app.service import worklog_polish_service

    async def fake_polish_worklog_with_ai(request):
        return WorklogPolishResponse.model_construct(
            work_content="수행 내용을 정리했습니다.",
        )

    monkeypatch.setattr(worklog_polish_service, "polish_worklog_with_ai", fake_polish_worklog_with_ai)

    service = worklog_polish_service.WorklogPolishService()
    response = asyncio.run(
        service.polish_worklog(
            WorklogPolishRequest(workContent="수행 내용을 정리했습니다."),
        )
    )

    assert response.work_content == "수행 내용을 정리했습니다."
    assert set(response.model_dump(by_alias=True)) == {"workContent"}
