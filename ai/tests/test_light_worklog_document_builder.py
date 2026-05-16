from dataclasses import asdict

from app.light.v3.service.worklog_document_builder import build_worklog_light_document
from app.light.v3.service.worklog_source_reader import (
    LightRagWorklogSource,
    LightRagWorklogTag,
    to_light_worklog_source,
)
from app.light.v3.store.worklog_source_store import LightWorklogTag, LightWorklogSourceRow


def test_to_light_worklog_source_keeps_light_store_source_snapshot() -> None:
    source_row = LightWorklogSourceRow(
        worklog_id=101,
        title="정산 배치 오류 분석",
        request_content="정산 배치 실패 원인을 확인해 주세요.",
        work_content="로그 기준으로 API timeout과 재시도 누락을 확인했습니다.",
        author_id=7,
        author_name="김도윤",
        author_role="팀원 / 사원",
        team_id=3,
        team_name="정산 고도화 TF",
        predecessor_titles=["배치 모니터링 개선"],
        predecessor_worklog_ids=[88],
        tag_ids=[4, 9],
        tags=[
            LightWorklogTag(tag_id=4, tag_name="정산", description="정산 배치와 대사 업무"),
            LightWorklogTag(tag_id=9, tag_name="장애분석", description=None),
        ],
    )

    light_source = to_light_worklog_source(source_row)

    assert asdict(light_source) == {
        "worklog_id": 101,
        "title": "정산 배치 오류 분석",
        "request_content": "정산 배치 실패 원인을 확인해 주세요.",
        "work_content": "로그 기준으로 API timeout과 재시도 누락을 확인했습니다.",
        "author_id": 7,
        "author_name": "김도윤",
        "author_role": "팀원 / 사원",
        "team_id": 3,
        "team_name": "정산 고도화 TF",
        "predecessor_titles": ["배치 모니터링 개선"],
        "predecessor_worklog_ids": [88],
        "tag_ids": [4, 9],
        "tags": [
            {
                "tag_id": 4,
                "tag_name": "정산",
                "description": "정산 배치와 대사 업무",
            },
            {"tag_id": 9, "tag_name": "장애분석", "description": None},
        ],
    }


def test_build_worklog_light_document_preserves_worklog_id_in_three_places() -> None:
    source = LightRagWorklogSource(
        worklog_id=101,
        title="정산 배치 오류 분석",
        request_content=None,
        work_content="로그 기준으로 API timeout과 재시도 누락을 확인했습니다.",
        author_id=7,
        author_name="김도윤",
        author_role="팀원 / 사원",
        team_id=3,
        team_name="정산 고도화 TF",
        predecessor_titles=["배치 모니터링 개선"],
        predecessor_worklog_ids=[88],
        tag_ids=[4, 9],
        tags=[
            LightRagWorklogTag(tag_id=4, tag_name="정산", description="정산 배치와 대사 업무"),
            LightRagWorklogTag(tag_id=9, tag_name="장애분석", description=None),
        ],
    )

    document = build_worklog_light_document(source)

    assert document.document_id == "worklog-101"
    assert document.file_path == "worklog://101"
    assert "source_type: WORKLOG" in document.text
    assert "worklog_id: 101" in document.text
    assert "title: 정산 배치 오류 분석" in document.text
    assert "author_id: 7" in document.text
    assert "team_id: 3" in document.text
    assert "predecessor_worklog_ids: 88" in document.text
    assert "tag_ids: 4, 9" in document.text
    assert "tags: [4] 정산: 정산 배치와 대사 업무 | [9] 장애분석" in document.text
    assert "work_content:\n로그 기준으로 API timeout과 재시도 누락을 확인했습니다." in document.text
