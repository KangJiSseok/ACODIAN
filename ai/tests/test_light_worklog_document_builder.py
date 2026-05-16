from dataclasses import asdict

from app.light.v3.service.worklog_document_builder import build_worklog_light_document
from app.light.v3.service.worklog_source_reader import (
    LightRagWorklogPredecessor,
    LightRagWorklogSource,
    LightRagWorklogTag,
    to_light_worklog_source,
)
from app.light.v3.store.worklog_source_store import (
    LightWorklogPredecessor,
    LightWorklogTag,
    LightWorklogSourceRow,
)


def test_to_light_worklog_source_keeps_light_store_source_snapshot() -> None:
    source_row = LightWorklogSourceRow(
        worklog_id=101,
        title="정산 배치 오류 분석",
        request_content="정산 배치 실패 원인을 확인해 주세요.",
        work_content="로그 기준으로 API timeout과 재시도 누락을 확인했습니다.",
        author_name="김도윤",
        team_name="정산 고도화 TF",
        predecessors=[
            LightWorklogPredecessor(worklog_id=88, title="배치 모니터링 개선"),
        ],
        tags=[
            LightWorklogTag(tag_name="정산", description="정산 배치와 대사 업무"),
            LightWorklogTag(tag_name="장애분석", description=None),
        ],
    )

    light_source = to_light_worklog_source(source_row)

    assert asdict(light_source) == {
        "worklog_id": 101,
        "title": "정산 배치 오류 분석",
        "request_content": "정산 배치 실패 원인을 확인해 주세요.",
        "work_content": "로그 기준으로 API timeout과 재시도 누락을 확인했습니다.",
        "author_name": "김도윤",
        "team_name": "정산 고도화 TF",
        "predecessors": [
            {
                "worklog_id": 88,
                "title": "배치 모니터링 개선",
            },
        ],
        "tags": [
            {
                "tag_name": "정산",
                "description": "정산 배치와 대사 업무",
            },
            {"tag_name": "장애분석", "description": None},
        ],
    }


def test_build_worklog_light_document_preserves_worklog_id_in_three_places() -> None:
    source = LightRagWorklogSource(
        worklog_id=101,
        title="정산 배치 오류 분석",
        request_content=None,
        work_content="로그 기준으로 API timeout과 재시도 누락을 확인했습니다.",
        author_name="김도윤",
        team_name="정산 고도화 TF",
        predecessors=[
            LightRagWorklogPredecessor(worklog_id=88, title="배치 모니터링 개선"),
        ],
        tags=[
            LightRagWorklogTag(tag_name="정산", description="정산 배치와 대사 업무"),
            LightRagWorklogTag(tag_name="장애분석", description=None),
        ],
    )

    document = build_worklog_light_document(source)

    assert document.document_id == "worklog-101"
    assert document.file_path == "worklog://101"
    assert "source_type: WORKLOG" in document.text
    assert "worklog_id: 101" in document.text
    assert "title: 정산 배치 오류 분석" in document.text
    assert "author_id:" not in document.text
    assert "author_role:" not in document.text
    assert "team_id:" not in document.text
    assert "predecessors: [88] 배치 모니터링 개선" in document.text
    assert "tags: 정산: 정산 배치와 대사 업무 | 장애분석" in document.text
    assert "work_content:\n로그 기준으로 API timeout과 재시도 누락을 확인했습니다." in document.text
