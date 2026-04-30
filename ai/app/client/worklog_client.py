class WorklogClient:
    async def update_ai_result(
            self,
            worklog_id: int,
            ai_summary: str,
            ai_summary_edited: bool,
            ai_processing_status: str,
    ) -> None:
        # TODO: WAS callback API 확정 후 실제 HTTP 요청 연결 로직 구현
        return None