"""LightRAG custom KG 확정 관계 source contract와 변환기."""

from dataclasses import dataclass

from app.light.v3.store.worklog_source_store import LightWorklogSourceRow


@dataclass(frozen=True)
class LightRagCustomKgTag:
    """LightRAG custom KG 관계에 포함할 태그 문맥."""

    tag_id: int
    tag_name: str
    description: str | None


@dataclass(frozen=True)
class LightRagCustomKgPredecessor:
    """LightRAG custom KG DEPENDS_ON 관계에 포함할 직접 선행 업무 문맥."""

    worklog_id: int
    title: str


@dataclass(frozen=True)
class LightRagCustomKgSource:
    """LightRAG 업무일지 custom KG 생성을 위한 source contract."""

    worklog_id: int
    title: str
    author_id: int
    author_name: str
    team_id: int
    team_name: str
    tags: list[LightRagCustomKgTag]
    direct_predecessors: list[LightRagCustomKgPredecessor]


def to_light_worklog_custom_kg_source(source: LightWorklogSourceRow) -> LightRagCustomKgSource:
    """LightRAG v3 source row를 custom KG source contract로 변환한다."""
    return LightRagCustomKgSource(
        worklog_id=source.worklog_id,
        title=source.title,
        author_id=source.author_id,
        author_name=source.author_name,
        team_id=source.team_id,
        team_name=source.team_name,
        tags=[
            LightRagCustomKgTag(
                tag_id=tag.tag_id,
                tag_name=tag.tag_name,
                description=tag.description,
            )
            for tag in source.tags
        ],
        direct_predecessors=[
            LightRagCustomKgPredecessor(
                worklog_id=predecessor.worklog_id,
                title=predecessor.title,
            )
            for predecessor in source.direct_predecessors
        ],
    )
