from app.light.v3.service.worklog_custom_kg_builder import build_worklog_custom_kg_document
from app.light.v3.service.worklog_custom_kg_source_reader import (
    LightRagCustomKgPredecessor,
    LightRagCustomKgSource,
    LightRagCustomKgTag,
)


def make_custom_kg_source() -> LightRagCustomKgSource:
    return LightRagCustomKgSource(
        worklog_id=101,
        title="정산 배치 오류 분석",
        author_id=5,
        author_name="김도윤",
        team_id=7,
        team_name="정산 고도화 TF",
        tags=[
            LightRagCustomKgTag(tag_id=11, tag_name="정산", description="정산 배치와 대사 업무"),
            LightRagCustomKgTag(tag_id=12, tag_name="장애분석", description=None),
        ],
        direct_predecessors=[LightRagCustomKgPredecessor(worklog_id=88, title="배치 모니터링 개선")],
    )


def test_build_worklog_custom_kg_document_uses_lightrag_field_contract() -> None:
    document = build_worklog_custom_kg_document(make_custom_kg_source())
    payload = document.custom_kg

    assert payload["chunks"] == [
        {
            "content": "Confirmed worklog relation source for Worklog:101",
            "source_id": "pg:tb_worklog:101",
            "file_path": "worklog://101",
            "chunk_order_index": 0,
        }
    ]
    assert {
        "entity_name": "Worklog:101",
        "entity_type": "WORKLOG",
        "description": "정산 배치 오류 분석; source_type=CONFIRMED",
        "source_id": "pg:tb_worklog:101",
        "file_path": "worklog://101",
    } in payload["entities"]
    assert {
        "entity_name": "Tag:11",
        "entity_type": "TAG",
        "description": "정산: 정산 배치와 대사 업무; source_type=CONFIRMED",
        "source_id": "pg:tb_worklog:101",
        "file_path": "worklog://101",
    } in payload["entities"]
    assert {
        "entity_name": "Worklog:88",
        "entity_type": "WORKLOG",
        "description": "배치 모니터링 개선; source_type=CONFIRMED",
        "source_id": "pg:tb_worklog:101",
        "file_path": "worklog://101",
    } in payload["entities"]


def test_build_worklog_custom_kg_document_includes_confirmed_relation_labels() -> None:
    document = build_worklog_custom_kg_document(make_custom_kg_source())
    payload = document.custom_kg

    relation_keywords = [relationship["keywords"] for relationship in payload["relationships"]]

    assert relation_keywords == [
        "AUTHORED_BY",
        "BELONGS_TO",
        "HAS_TAG",
        "HAS_TAG",
        "DEPENDS_ON",
    ]
    assert {
        "src_id": "Worklog:101",
        "tgt_id": "Worklog:88",
        "description": "Worklog:101 directly depends on Worklog:88 (배치 모니터링 개선); source_type=CONFIRMED",
        "keywords": "DEPENDS_ON",
        "weight": 1.0,
        "source_id": "pg:tb_worklog:101",
        "file_path": "worklog://101",
    } in payload["relationships"]


def test_build_worklog_custom_kg_document_source_ids_match_chunk_source_id() -> None:
    document = build_worklog_custom_kg_document(make_custom_kg_source())
    payload = document.custom_kg
    chunk_source_ids = {chunk["source_id"] for chunk in payload["chunks"]}

    assert all(entity["source_id"] in chunk_source_ids for entity in payload["entities"])
    assert all(
        relationship["source_id"] in chunk_source_ids for relationship in payload["relationships"]
    )
