"""LightRAG v3 업무일지 통합 source 조회 store."""

from dataclasses import dataclass

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.store.models import MetaTag, Team, User, Worklog, WorklogDependency, WorklogTag


@dataclass(frozen=True)
class LightWorklogTag:
    """LightRAG custom KG 관계에 포함할 업무일지 태그 정보."""

    tag_id: int
    tag_name: str
    description: str | None


@dataclass(frozen=True)
class LightWorklogPredecessor:
    """LightRAG custom KG 관계에 포함할 직접 선행 업무일지 정보."""

    worklog_id: int
    title: str


@dataclass(frozen=True)
class LightWorklogSourceRow:
    """LightRAG v3 text/custom KG 입력을 함께 만들기 위한 업무일지 source."""

    worklog_id: int
    title: str
    request_content: str | None
    work_content: str
    author_id: int
    author_name: str
    team_id: int
    team_name: str
    tags: list[LightWorklogTag]
    direct_predecessors: list[LightWorklogPredecessor]


class LightWorklogSourceStore:
    """LightRAG v3 업무일지 통합 source 조회 SQL을 소유한다."""

    async def fetch_worklog_sources(
        self,
        session: AsyncSession,
        worklog_ids: list[int],
    ) -> dict[int, LightWorklogSourceRow]:
        """업무일지 목록의 text/custom KG 통합 source를 batch로 조회한다."""
        unique_worklog_ids = list(dict.fromkeys(worklog_ids))
        if not unique_worklog_ids:
            return {}

        rows = (
            await session.execute(
                select(
                    Worklog.worklog_id,
                    Worklog.title,
                    Worklog.request_content,
                    Worklog.work_content,
                    Worklog.author_id,
                    User.user_name,
                    Worklog.team_id,
                    Team.team_name,
                )
                .join(Team, Team.team_id == Worklog.team_id)
                .join(User, User.user_id == Worklog.author_id)
                .where(Worklog.worklog_id.in_(unique_worklog_ids))
                .where(Worklog.is_deleted.is_(False))
                .where(Team.deleted_at.is_(None))
                .order_by(Worklog.worklog_id)
            )
        ).all()
        tags_by_worklog_id = await self._fetch_tags_by_worklog_id(session, unique_worklog_ids)
        direct_predecessors_by_worklog_id = await self._fetch_direct_predecessors_by_worklog_id(
            session,
            unique_worklog_ids,
        )

        return {
            row.worklog_id: LightWorklogSourceRow(
                worklog_id=row.worklog_id,
                title=row.title,
                request_content=row.request_content,
                work_content=row.work_content,
                author_id=row.author_id,
                author_name=row.user_name,
                team_id=row.team_id,
                team_name=row.team_name,
                tags=tags_by_worklog_id.get(row.worklog_id, []),
                direct_predecessors=direct_predecessors_by_worklog_id.get(row.worklog_id, []),
            )
            for row in rows
        }

    async def fetch_worklog_source(
        self,
        session: AsyncSession,
        worklog_id: int,
    ) -> LightWorklogSourceRow | None:
        """업무일지 1건의 text/custom KG 통합 source를 조회한다."""
        return (await self.fetch_worklog_sources(session, [worklog_id])).get(worklog_id)

    async def _fetch_tags_by_worklog_id(
        self,
        session: AsyncSession,
        worklog_ids: list[int],
    ) -> dict[int, list[LightWorklogTag]]:
        """업무일지별 삭제되지 않은 태그 ID, 이름, 설명을 batch 조회한다."""
        rows = (
            await session.execute(
                select(
                    WorklogTag.worklog_id,
                    MetaTag.tag_id,
                    MetaTag.tag_name,
                    MetaTag.description,
                )
                .select_from(WorklogTag)
                .join(MetaTag, MetaTag.tag_id == WorklogTag.tag_id)
                .where(WorklogTag.worklog_id.in_(worklog_ids))
                .where(MetaTag.is_deleted.is_(False))
                .order_by(WorklogTag.worklog_id, MetaTag.tag_id)
            )
        ).all()

        tags_by_worklog_id: dict[int, list[LightWorklogTag]] = {}
        for row in rows:
            tags_by_worklog_id.setdefault(row.worklog_id, []).append(
                LightWorklogTag(
                    tag_id=row.tag_id,
                    tag_name=row.tag_name,
                    description=row.description,
                )
            )
        return tags_by_worklog_id

    async def _fetch_tags(
        self,
        session: AsyncSession,
        worklog_id: int,
    ) -> list[LightWorklogTag]:
        """업무일지 1건의 삭제되지 않은 태그 ID, 이름, 설명을 조회한다."""
        return (await self._fetch_tags_by_worklog_id(session, [worklog_id])).get(worklog_id, [])

    async def _fetch_direct_predecessors_by_worklog_id(
        self,
        session: AsyncSession,
        worklog_ids: list[int],
    ) -> dict[int, list[LightWorklogPredecessor]]:
        """업무일지별 직접 DB edge 기반 선행 업무만 batch 조회한다."""
        rows = (
            await session.execute(
                select(
                    WorklogDependency.worklog_id,
                    WorklogDependency.depends_on_worklog_id,
                    Worklog.title,
                )
                .join(Worklog, Worklog.worklog_id == WorklogDependency.depends_on_worklog_id)
                .where(WorklogDependency.worklog_id.in_(worklog_ids))
                .where(Worklog.is_deleted.is_(False))
                .order_by(WorklogDependency.worklog_id, WorklogDependency.depends_on_worklog_id)
            )
        ).all()

        predecessors_by_worklog_id: dict[int, list[LightWorklogPredecessor]] = {}
        for row in rows:
            predecessors_by_worklog_id.setdefault(row.worklog_id, []).append(
                LightWorklogPredecessor(
                    worklog_id=row.depends_on_worklog_id,
                    title=row.title,
                )
            )
        return predecessors_by_worklog_id

    async def _fetch_direct_predecessors(
        self,
        session: AsyncSession,
        worklog_id: int,
    ) -> list[LightWorklogPredecessor]:
        """업무일지 1건의 직접 DB edge 기반 선행 업무만 조회한다."""
        return (await self._fetch_direct_predecessors_by_worklog_id(session, [worklog_id])).get(
            worklog_id,
            [],
        )
