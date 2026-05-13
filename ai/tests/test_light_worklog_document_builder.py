from dataclasses import asdict

from app.light.v3.service.worklog_document_builder import build_worklog_light_document
from app.light.v3.service.worklog_source_reader import (
    LightRagWorklogSource,
    to_light_worklog_source,
)
from app.store.embedding_store import WorklogEmbeddingSource


def test_to_light_worklog_source_keeps_embedding_source_contract_snapshot() -> None:
    embedding_source = WorklogEmbeddingSource(
        worklog_id=101,
        title="정산 배치 오류 분석",
        request_content="정산 배치 실패 원인을 확인해 주세요.",
        work_content="로그 기준으로 API timeout과 재시도 누락을 확인했습니다.",
        author_id=7,
        author_name="김도윤",
        author_role="팀원 / 사원",
        team_id=3,
        team_name="정산 고도화 TF",
        department_id=2,
        predecessor_titles=["배치 모니터링 개선"],
        predecessor_worklog_ids=[88],
        tag_ids=[4, 9],
    )

    light_source = to_light_worklog_source(embedding_source)

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
        "department_id": 2,
        "predecessor_titles": ["배치 모니터링 개선"],
        "predecessor_worklog_ids": [88],
        "tag_ids": [4, 9],
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
        department_id=2,
        predecessor_titles=["배치 모니터링 개선"],
        predecessor_worklog_ids=[88],
        tag_ids=[4, 9],
    )

    document = build_worklog_light_document(source)

    assert document.document_id == "worklog-101"
    assert document.file_path == "worklog://101"
    assert "source_type: WORKLOG" in document.text
    assert "worklog_id: 101" in document.text
    assert "title: 정산 배치 오류 분석" in document.text
    assert "author_id: 7" in document.text
    assert "team_id: 3" in document.text
    assert "department_id: 2" in document.text
    assert "predecessor_worklog_ids: 88" in document.text
    assert "tag_ids: 4, 9" in document.text
    assert "work_content:\n로그 기준으로 API timeout과 재시도 누락을 확인했습니다." in document.text
