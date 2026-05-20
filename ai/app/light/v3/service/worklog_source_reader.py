"""LightRAG v3 업무일지 text source contract와 변환기."""

from dataclasses import dataclass

from app.light.v3.store.worklog_source_store import LightWorklogSourceRow


@dataclass(frozen=True)
class LightRagWorklogSource:
    """LightRAG 업무일지 text document 생성을 위한 source contract."""

    worklog_id: int
    title: str
    request_content: str | None
    work_content: str


def to_light_worklog_source(source: LightWorklogSourceRow) -> LightRagWorklogSource:
    """LightRAG v3 source row를 text document source contract로 변환한다."""
    return LightRagWorklogSource(
        worklog_id=source.worklog_id,
        title=source.title,
        request_content=source.request_content,
        work_content=source.work_content,
    )
