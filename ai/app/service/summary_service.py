from app.chain.summary_chain import create_summary_chain


class SummaryService:
    def __init__(self) -> None:
        self.chain = create_summary_chain()

    async def generate_summary(
        self,
        request_content: str | None,
        work_content: str,
    ) -> str:
        summary = await self.chain.ainvoke(
            {
                "request_content": request_content or "(없음)",
                "work_content": work_content,
            }
        )

        return summary.strip()
