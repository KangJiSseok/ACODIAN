from app.client.gemini_client import GeminiClient, get_gemini_client
from app.model.worklog_polish import WorklogPolishRequest, WorklogPolishResponse
from app.prompt.worklog_polish_prompt import (
    WORKLOG_POLISH_SYSTEM_PROMPT,
    WORKLOG_POLISH_USER_TEMPLATE,
)


async def polish_worklog_with_ai(
    request: WorklogPolishRequest,
    client: GeminiClient | None = None,
) -> WorklogPolishResponse:
    generator = client or get_gemini_client()
    prompt = WORKLOG_POLISH_USER_TEMPLATE.format(
        request_content=request.request_content or "",
        work_content=request.work_content,
    )
    return await generator.generate_structured(
        contents=[prompt],
        schema=WorklogPolishResponse,
        instruction=WORKLOG_POLISH_SYSTEM_PROMPT,
    )
