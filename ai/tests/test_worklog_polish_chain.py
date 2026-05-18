import asyncio

from app.chain.worklog_polish_chain import polish_worklog_with_ai
from app.model.worklog_polish import WorklogPolishRequest
from app.model.worklog_polish import WorklogPolishResponse
from app.prompt.worklog_polish_prompt import (
    WORKLOG_POLISH_SYSTEM_PROMPT,
    WORKLOG_POLISH_USER_TEMPLATE,
)


class FakeGeminiClient:
    def __init__(self) -> None:
        self.calls = []

    async def generate_structured(self, *, contents, schema, instruction, model=None):
        self.calls.append(
            {
                "contents": contents,
                "schema": schema,
                "instruction": instruction,
                "model": model,
            }
        )
        assert contents == [
            WORKLOG_POLISH_USER_TEMPLATE.format(
                request_content="",
                work_content="배치 병렬 처리 구조를 적용했다.",
            )
        ]
        assert schema is WorklogPolishResponse
        assert "title" not in schema.model_fields
        assert instruction == WORKLOG_POLISH_SYSTEM_PROMPT
        return WorklogPolishResponse(workContent="다듬은 본문")


def test_polish_worklog_with_gemini_uses_expected_prompt(monkeypatch) -> None:
    from app.chain import worklog_polish_chain

    fake_client = FakeGeminiClient()
    monkeypatch.setattr(worklog_polish_chain, "get_gemini_client", lambda: fake_client)

    result = asyncio.run(
        polish_worklog_with_ai(
            WorklogPolishRequest(
                requestContent=None,
                workContent="배치 병렬 처리 구조를 적용했다.",
            )
        )
    )

    assert result.work_content == "다듬은 본문"
    assert set(result.model_dump(by_alias=True)) == {"workContent"}
    assert len(fake_client.calls) == 1
