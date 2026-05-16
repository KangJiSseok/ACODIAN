"""LightRAG v3 업무일지 document contract와 builder."""

from dataclasses import dataclass

from app.light.v3.service.worklog_source_reader import LightRagWorklogSource


@dataclass(frozen=True)
class LightRagWorklogDocument:
    """LightRAG insert에 전달할 업무일지 문서.

    `document_id`, `file_path`, 본문 marker에 같은 업무일지 ID를 반복해 보존한다.
    후속 search 단계가 LightRAG 결과에서 `worklogId`만 안정적으로 회수할 수 있게 하기
    위한 최소 contract다.
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
    """LightRAG KG 생성을 위한 업무일지 문서 본문을 만든다."""
    lines = [
        "source_type: WORKLOG",
        f"worklog_id: {source.worklog_id}",
        f"title: {source.title}",
        f"author_id: {source.author_id}",
        f"author_name: {source.author_name}",
        f"author_role: {source.author_role}",
        f"team_id: {source.team_id}",
        f"team_name: {source.team_name}",
        f"department_id: {source.department_id}",
        f"predecessor_worklog_ids: {_format_int_list(source.predecessor_worklog_ids)}",
        f"predecessor_titles: {_format_text_list(source.predecessor_titles)}",
        f"tag_ids: {_format_int_list(source.tag_ids)}",
        "",
        "request_content:",
        source.request_content or "",
        "",
        "work_content:",
        source.work_content,
    ]
    return "\n".join(lines).strip()


def _format_int_list(values: list[int]) -> str:
    """ID 목록을 marker 친화적인 짧은 문자열로 직렬화한다."""
    return ", ".join(str(value) for value in values) if values else "-"


def _format_text_list(values: list[str]) -> str:
    """텍스트 목록을 LightRAG 입력에 넣기 좋은 짧은 문자열로 직렬화한다."""
    return " | ".join(values) if values else "-"
