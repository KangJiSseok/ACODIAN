from app.chain.worklog_polish_chain import polish_worklog_with_ai, recommend_worklog_titles_with_ai
from app.model.worklog_polish import (
    WorklogPolishRequest,
    WorklogPolishResponse,
    WorklogTitleRecommendationResponse,
)


class WorklogPolishService:
    async def polish_worklog(
        self,
        request: WorklogPolishRequest,
    ) -> WorklogPolishResponse:
        return await polish_worklog_with_ai(request)

    async def recommend_worklog_titles(
        self,
        request: WorklogPolishRequest,
    ) -> WorklogTitleRecommendationResponse:
        return await recommend_worklog_titles_with_ai(request)
