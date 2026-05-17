"""LightRAG `ainsert_custom_kg` document contract와 builder."""

from dataclasses import dataclass
from typing import Any

from app.light.v3.service.worklog_custom_kg_source_reader import LightRagCustomKgSource

_CONFIRMED_MARKER = "source_type=CONFIRMED"


@dataclass(frozen=True)
class LightRagWorklogCustomKgDocument:
    """LightRAG custom KG insert에 전달할 업무일지 문서.

    `document_id`는 LightRAG `full_doc_id`로 전달된다. `custom_kg`는 LightRAG
    `ainsert_custom_kg`가 요구하는 chunks/entities/relationships payload이며,
    `file_path` provenance는 payload 내부에만 둔다.
    """

    document_id: str
    custom_kg: dict[str, list[dict[str, Any]]]


def build_worklog_custom_kg_document(
    source: LightRagCustomKgSource,
) -> LightRagWorklogCustomKgDocument:
    """업무일지 source를 LightRAG custom KG 단일 문서로 변환한다."""
    document_id = f"worklog-{source.worklog_id}"
    return LightRagWorklogCustomKgDocument(
        document_id=document_id,
        custom_kg=_build_custom_kg(source),
    )


def _build_custom_kg(source: LightRagCustomKgSource) -> dict[str, list[dict[str, Any]]]:
    """LightRAG custom KG chunks/entities/relationships payload를 만든다.

    payload 단계에서 모든 entity/relation `source_id`는 같은 payload의 `chunks[].source_id`와
    일치해야 한다. LightRAG는 저장 시 이 값을 내부 chunk id로 재매핑한다.
    """
    source_id = _worklog_source_id(source.worklog_id)
    worklog_entity = _worklog_entity_name(source.worklog_id)
    file_path = f"worklog://{source.worklog_id}"

    entities = [
        _entity(
            name=worklog_entity,
            entity_type="WORKLOG",
            description=f"{source.title}; {_CONFIRMED_MARKER}",
            source_id=source_id,
            file_path=file_path,
        ),
        _entity(
            name=f"User:{source.author_id}",
            entity_type="USER",
            description=f"{source.author_name}; author_id={source.author_id}; {_CONFIRMED_MARKER}",
            source_id=source_id,
            file_path=file_path,
        ),
        _entity(
            name=f"Team:{source.team_id}",
            entity_type="TEAM",
            description=f"{source.team_name}; team_id={source.team_id}; {_CONFIRMED_MARKER}",
            source_id=source_id,
            file_path=file_path,
        ),
    ]
    entities.extend(
        _entity(
            name=f"Tag:{tag.tag_id}",
            entity_type="TAG",
            description=_tag_description(tag_name=tag.tag_name, description=tag.description),
            source_id=source_id,
            file_path=file_path,
        )
        for tag in source.tags
    )
    entities.extend(
        _entity(
            name=_worklog_entity_name(predecessor.worklog_id),
            entity_type="WORKLOG",
            description=f"{predecessor.title}; {_CONFIRMED_MARKER}",
            source_id=source_id,
            file_path=file_path,
        )
        for predecessor in source.direct_predecessors
    )

    relationships = [
        _relationship(
            src_id=worklog_entity,
            tgt_id=f"User:{source.author_id}",
            label="AUTHORED_BY",
            description=(
                f"Worklog:{source.worklog_id} was authored by "
                f"User:{source.author_id}; {_CONFIRMED_MARKER}"
            ),
            source_id=source_id,
            file_path=file_path,
        ),
        _relationship(
            src_id=worklog_entity,
            tgt_id=f"Team:{source.team_id}",
            label="BELONGS_TO",
            description=(
                f"Worklog:{source.worklog_id} belongs to "
                f"Team:{source.team_id}; {_CONFIRMED_MARKER}"
            ),
            source_id=source_id,
            file_path=file_path,
        ),
    ]
    relationships.extend(
        _relationship(
            src_id=worklog_entity,
            tgt_id=f"Tag:{tag.tag_id}",
            label="HAS_TAG",
            description=(
                f"Worklog:{source.worklog_id} has Tag:{tag.tag_id} "
                f"({tag.tag_name}); {_CONFIRMED_MARKER}"
            ),
            source_id=source_id,
            file_path=file_path,
        )
        for tag in source.tags
    )
    relationships.extend(
        _relationship(
            src_id=worklog_entity,
            tgt_id=_worklog_entity_name(predecessor.worklog_id),
            label="DEPENDS_ON",
            description=(
                f"Worklog:{source.worklog_id} directly depends on "
                f"Worklog:{predecessor.worklog_id} ({predecessor.title}); {_CONFIRMED_MARKER}"
            ),
            source_id=source_id,
            file_path=file_path,
        )
        for predecessor in source.direct_predecessors
    )

    return {
        "chunks": [
            {
                "content": f"Confirmed worklog relation source for Worklog:{source.worklog_id}",
                "source_id": source_id,
                "file_path": file_path,
                "chunk_order_index": 0,
            }
        ],
        "entities": entities,
        "relationships": relationships,
    }


def _entity(
    *,
    name: str,
    entity_type: str,
    description: str,
    source_id: str,
    file_path: str,
) -> dict[str, Any]:
    """LightRAG custom KG entity dict를 만든다."""
    return {
        "entity_name": name,
        "entity_type": entity_type,
        "description": description,
        "source_id": source_id,
        "file_path": file_path,
    }


def _relationship(
    *,
    src_id: str,
    tgt_id: str,
    label: str,
    description: str,
    source_id: str,
    file_path: str,
) -> dict[str, Any]:
    """LightRAG custom KG relationship dict를 만든다."""
    return {
        "src_id": src_id,
        "tgt_id": tgt_id,
        "description": description,
        "keywords": label,
        "weight": 1.0,
        "source_id": source_id,
        "file_path": file_path,
    }


def _tag_description(*, tag_name: str, description: str | None) -> str:
    """태그 이름과 설명을 entity description으로 정규화한다."""
    if description:
        return f"{tag_name}: {description}; {_CONFIRMED_MARKER}"
    return f"{tag_name}; {_CONFIRMED_MARKER}"


def _worklog_entity_name(worklog_id: int) -> str:
    """업무일지 entity name을 만든다."""
    return f"Worklog:{worklog_id}"


def _worklog_source_id(worklog_id: int) -> str:
    """업무일지 source provenance id를 만든다."""
    return f"pg:tb_worklog:{worklog_id}"
