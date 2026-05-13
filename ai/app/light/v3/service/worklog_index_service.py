"""LightRAG v3 업무일지 index orchestration service 경계."""

from app.light.v3.model.worklog_index import (
    WorklogLightIndexItem,
    WorklogLightIndexResponse,
)


class LightWorklogIndexService:
    """업무일지 LightRAG index 유스케이스 진입점.

    Phase 1에서는 라우터가 의존할 service 경계만 제공한다. 실제 DB source 조회,
    document build, LightRAG adapter 호출은 후속 phase에서 이 메서드 내부로 연결한다.
    """

    async def index_worklogs(self, worklog_ids: list[int]) -> WorklogLightIndexResponse:
        """요청된 업무일지 ID별 index 결과를 반환한다.

        아직 실제 LightRAG insert를 수행하지 않는 Phase 1 stub이다. 후속 phase에서
        실제 orchestration을 연결하기 전까지는 성공으로 오인되지 않도록 명시적인
        미구현 결과를 반환한다.
        """
        return WorklogLightIndexResponse(
            items=[
                WorklogLightIndexItem(
                    worklogId=worklog_id,
                    indexed=False,
                    error="LIGHTRAG_INDEX_NOT_IMPLEMENTED",
                )
                for worklog_id in worklog_ids
            ]
        )
