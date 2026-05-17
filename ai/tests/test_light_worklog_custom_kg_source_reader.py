from dataclasses import asdict

from app.light.v3.service.worklog_custom_kg_source_reader import (
    to_light_worklog_custom_kg_source,
)
from app.light.v3.store.worklog_source_store import (
    LightWorklogPredecessor,
    LightWorklogSourceRow,
    LightWorklogTag,
)


def test_to_light_worklog_custom_kg_source_keeps_only_custom_kg_fields() -> None:
    source_row = LightWorklogSourceRow(
        worklog_id=101,
        title="정산 배치 오류 분석",
        request_content="정산 배치 실패 원인을 확인해 주세요.",
        work_content="로그 기준으로 API timeout과 재시도 누락을 확인했습니다.",
        author_id=5,
        author_name="김도윤",
        team_id=7,
        team_name="정산 고도화 TF",
        tags=[
            LightWorklogTag(tag_id=11, tag_name="정산", description="정산 배치와 대사 업무"),
            LightWorklogTag(tag_id=12, tag_name="장애분석", description=None),
        ],
        direct_predecessors=[
            LightWorklogPredecessor(worklog_id=88, title="배치 모니터링 개선"),
        ],
    )

    custom_kg_source = to_light_worklog_custom_kg_source(source_row)

    assert asdict(custom_kg_source) == {
        "worklog_id": 101,
        "title": "정산 배치 오류 분석",
        "author_id": 5,
        "author_name": "김도윤",
        "team_id": 7,
        "team_name": "정산 고도화 TF",
        "tags": [
            {"tag_id": 11, "tag_name": "정산", "description": "정산 배치와 대사 업무"},
            {"tag_id": 12, "tag_name": "장애분석", "description": None},
        ],
        "direct_predecessors": [
            {"worklog_id": 88, "title": "배치 모니터링 개선"},
        ],
    }
