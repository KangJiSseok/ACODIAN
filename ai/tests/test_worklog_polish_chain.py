import asyncio

from app.chain.worklog_polish_chain import polish_worklog_with_ai, recommend_worklog_titles_with_ai
from app.model.worklog_polish import WorklogPolishRequest
from app.model.worklog_polish import WorklogPolishResponse
from app.model.worklog_polish import WorklogTitleRecommendationResponse
from app.prompt.worklog_polish_prompt import (
    WORKLOG_POLISH_SYSTEM_PROMPT,
    WORKLOG_POLISH_USER_TEMPLATE,
    WORKLOG_TITLE_RECOMMENDATION_SYSTEM_PROMPT,
    WORKLOG_TITLE_RECOMMENDATION_USER_TEMPLATE,
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


def test_polish_prompt_allows_safe_rationale_expansion_without_fact_leakage() -> None:
    system_prompt = " ".join(WORKLOG_POLISH_SYSTEM_PROMPT.split())
    user_template = " ".join(WORKLOG_POLISH_USER_TEMPLATE.split())

    assert "generally valid technical/domain reasoning" in system_prompt
    assert "cause, comparison, selection rationale" in system_prompt
    assert "follow-up management" in system_prompt
    assert "background/context/style guidance only" in system_prompt
    assert "never as evidence that requested work was performed" in system_prompt
    assert "Do not reduce the output to grammar correction only" in system_prompt
    assert "Avoid promotional, sales, or marketing language" in system_prompt
    assert "Preserve performed-fact boundaries from workContent" in user_template


def test_recommend_worklog_titles_with_gemini_uses_expected_prompt(monkeypatch) -> None:
    from app.chain import worklog_polish_chain

    class FakeTitleClient:
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
                WORKLOG_TITLE_RECOMMENDATION_USER_TEMPLATE.format(
                    request_content="성과 문구는 보수적으로 작성",
                    work_content="장애 원인을 로그에서 확인하고 재시작 절차를 공유했습니다.",
                )
            ]
            assert schema is WorklogTitleRecommendationResponse
            assert instruction == WORKLOG_TITLE_RECOMMENDATION_SYSTEM_PROMPT
            return WorklogTitleRecommendationResponse(titles=["장애 원인 확인 및 재시작 절차 공유"])

    fake_client = FakeTitleClient()
    monkeypatch.setattr(worklog_polish_chain, "get_gemini_client", lambda: fake_client)

    result = asyncio.run(
        recommend_worklog_titles_with_ai(
            WorklogPolishRequest(
                requestContent="성과 문구는 보수적으로 작성",
                workContent="장애 원인을 로그에서 확인하고 재시작 절차를 공유했습니다.",
            )
        )
    )

    assert result.titles == ["장애 원인 확인 및 재시작 절차 공유"]
    assert set(result.model_dump(by_alias=True)) == {"titles"}
    assert len(fake_client.calls) == 1
