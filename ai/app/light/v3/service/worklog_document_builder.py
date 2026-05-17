"""LightRAG v3 업무일지 document contract와 builder."""

from dataclasses import dataclass

from app.light.v3.service.worklog_source_reader import LightRagWorklogSource


@dataclass(frozen=True)
class LightRagWorklogDocument:
    """LightRAG insert에 전달할 업무일지 문서.

    `document_id`, `file_path`, 본문 marker에 같은 업무일지 ID를 반복해 보존한다.
    후속 search 단계가 LightRAG 결과에서 `worklogId`만 안정적으로 회수할 수 있게 하기
    위한 최소 contract다. 확정 관계는 custom KG insert가 전담하므로 이 문서 본문에는
    작성자/팀/태그/선행업무 관계 문맥을 넣지 않는다.
    """

    document_id: str
    file_path: str
    text: str


def build_worklog_light_document(source: LightRagWorklogSource) -> LightRagWorklogDocument:
    """업무일지 source를 LightRAG가 ingest할 단일 문서로 변환한다."""
    document_id = f"worklog-{source.worklog_id}"
    file_path = f"worklog://{source.worklog_id}"
    return LightRagWorklogDocument(
        document_id=document_id,
        file_path=file_path,
        text=_build_document_text(source),
    )


def _build_document_text(source: LightRagWorklogSource) -> str:
    """LightRAG text insert용 업무일지 본문을 만든다."""
    lines = [
        "source_type: WORKLOG",
        f"worklog_id: {source.worklog_id}",
        f"title: {source.title}",
        "",
        "request_content:",
        source.request_content or "",
        "",
        "work_content:",
        source.work_content,
    ]
    return "\n".join(lines).strip()
