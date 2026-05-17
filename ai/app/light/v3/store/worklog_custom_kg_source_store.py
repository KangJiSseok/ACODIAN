"""LightRAG custom KG 확정 관계 source 조회 store."""

from dataclasses import dataclass

from sqlalchemy import literal, select, text
from sqlalchemy.ext.asyncio import AsyncSession

from app.store.models import MetaTag, Team, User, Worklog, WorklogDependency, WorklogTag


@dataclass(frozen=True)
class LightWorklogCustomKgTag:
    """custom KG `HAS_TAG` 관계에 필요한 태그 source."""

    tag_id: int
    tag_name: str
    description: str | None


@dataclass(frozen=True)
class LightWorklogCustomKgPredecessor:
    """custom KG `DEPENDS_ON` 관계에 필요한 직접 선행 업무 source."""

    worklog_id: int
    title: str


@dataclass(frozen=True)
class LightWorklogCustomKgSourceRow:
    """LightRAG custom KG 문서 생성을 위해 DB에서 조회한 업무일지 확정 관계 source."""

    worklog_id: int
    title: str
    author_id: int
    author_name: str
    team_id: int
    team_name: str
    tags: list[LightWorklogCustomKgTag]
    direct_predecessors: list[LightWorklogCustomKgPredecessor]


class LightWorklogCustomKgSourceStore:
    """LightRAG custom KG 확정 관계 source 조회 SQL을 소유한다."""

    async def fetch_custom_kg_source(
        self,
        session: AsyncSession,
        worklog_id: int,
    ) -> LightWorklogCustomKgSourceRow | None:
        """업무일지 1건의 custom KG source를 조회한다."""
        row = (
            await session.execute(
                select(
                    Worklog.worklog_id,
                    Worklog.title,
                    Worklog.author_id,
                    User.user_name,
                    Worklog.team_id,
                    Team.team_name,
                )
                .join(Team, Team.team_id == Worklog.team_id)
                .join(User, User.user_id == Worklog.author_id)
                .where(Worklog.worklog_id == worklog_id)
                .where(Worklog.is_deleted.is_(False))
                .where(Team.deleted_at.is_(None))
            )
        ).one_or_none()
        if row is None:
            return None

        return LightWorklogCustomKgSourceRow(
            worklog_id=row.worklog_id,
            title=row.title,
            author_id=row.author_id,
            author_name=row.user_name,
            team_id=row.team_id,
            team_name=row.team_name,
            tags=await self._fetch_tags(session, worklog_id),
            direct_predecessors=await self._fetch_direct_predecessors(session, worklog_id),
        )

    async def _fetch_tags(
        self,
        session: AsyncSession,
        worklog_id: int,
    ) -> list[LightWorklogCustomKgTag]:
        """삭제되지 않은 태그 ID, 이름, 설명을 조회한다."""
        meta_tag_columns = await self._fetch_meta_tag_columns(session)
        description_column = (
            MetaTag.description
            if "description" in meta_tag_columns
            else literal(None).label("description")
        )
        statement = (
            select(MetaTag.tag_id, MetaTag.tag_name, description_column)
            .select_from(WorklogTag)
            .join(MetaTag, MetaTag.tag_id == WorklogTag.tag_id)
            .where(WorklogTag.worklog_id == worklog_id)
            .order_by(MetaTag.tag_id)
        )
        if "is_deleted" in meta_tag_columns:
            statement = statement.where(MetaTag.is_deleted.is_(False))

        rows = (await session.execute(statement)).all()
        return [
            LightWorklogCustomKgTag(
                tag_id=row.tag_id,
                tag_name=row.tag_name,
                description=row.description,
            )
            for row in rows
        ]

    async def _fetch_meta_tag_columns(self, session: AsyncSession) -> set[str]:
        """현재 DB의 `tb_meta_tag` optional column 존재 여부를 조회한다."""
        rows = (
            await session.execute(
                text(
                    """
                    select column_name
                    from information_schema.columns
                    where table_schema = 'public'
                      and table_name = 'tb_meta_tag'
                      and column_name in ('description', 'is_deleted')
                    """
                )
            )
        ).all()
        return {str(row.column_name) for row in rows}

    async def _fetch_direct_predecessors(
        self,
        session: AsyncSession,
        worklog_id: int,
    ) -> list[LightWorklogCustomKgPredecessor]:
        """직접 DB edge 기반 선행 업무만 조회하고 삭제된 업무일지는 제외한다."""
        rows = (
            await session.execute(
                select(WorklogDependency.depends_on_worklog_id, Worklog.title)
                .join(Worklog, Worklog.worklog_id == WorklogDependency.depends_on_worklog_id)
                .where(WorklogDependency.worklog_id == worklog_id)
                .where(Worklog.is_deleted.is_(False))
                .order_by(WorklogDependency.depends_on_worklog_id)
            )
        ).all()
        return [
            LightWorklogCustomKgPredecessor(
                worklog_id=row.depends_on_worklog_id,
                title=row.title,
            )
            for row in rows
        ]
