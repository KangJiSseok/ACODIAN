"""LightRAG v3 업무일지 source 조회 store."""

from dataclasses import dataclass

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.store.models import MetaTag, Team, User, Worklog, WorklogDependency, WorklogTag


@dataclass(frozen=True)
class LightWorklogTag:
    """LightRAG 문맥에 포함할 업무일지 태그 정보."""

    tag_name: str
    description: str | None


@dataclass(frozen=True)
class LightWorklogPredecessor:
    """LightRAG 문맥에 포함할 선행 업무일지 정보."""

    worklog_id: int
    title: str


@dataclass(frozen=True)
class LightWorklogSourceRow:
    """LightRAG v3 입력 생성을 위해 DB에서 조회한 업무일지 원본 문맥."""

    worklog_id: int
    title: str
    request_content: str | None
    work_content: str
    author_name: str
    team_name: str
    predecessors: list[LightWorklogPredecessor]
    tags: list[LightWorklogTag]


class LightWorklogSourceStore:
    """LightRAG v3 업무일지 source 조회 SQL을 소유한다."""

    async def fetch_worklog_source(
        self,
        session: AsyncSession,
        worklog_id: int,
    ) -> LightWorklogSourceRow | None:
        """LightRAG v3 index 입력에 필요한 업무일지와 문맥 정보를 조회한다."""
        row = (
            await session.execute(
                select(
                    Worklog.worklog_id,
                    Worklog.title,
                    Worklog.request_content,
                    Worklog.work_content,
                    User.user_name,
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

        predecessors = await self._fetch_predecessors(session, worklog_id)
        tags = await self._fetch_tags(session, worklog_id)

        return LightWorklogSourceRow(
            worklog_id=row.worklog_id,
            title=row.title,
            request_content=row.request_content,
            work_content=row.work_content,
            author_name=row.user_name,
            team_name=row.team_name,
            predecessors=predecessors,
            tags=tags,
        )

    async def _fetch_tags(
        self,
        session: AsyncSession,
        worklog_id: int,
    ) -> list[LightWorklogTag]:
        """업무일지에 연결된 활성 태그의 이름과 설명을 조회한다."""
        rows = (
            await session.execute(
                select(MetaTag.tag_name, MetaTag.description)
                .join(MetaTag, MetaTag.tag_id == WorklogTag.tag_id)
                .where(WorklogTag.worklog_id == worklog_id)
                .where(MetaTag.is_deleted.is_(False))
                .order_by(WorklogTag.tag_id)
            )
        ).all()
        return [
            LightWorklogTag(
                tag_name=row.tag_name,
                description=row.description,
            )
            for row in rows
        ]

    async def _fetch_predecessors(
        self,
        session: AsyncSession,
        worklog_id: int,
    ) -> list[LightWorklogPredecessor]:
        """직접/간접 선행 업무를 depth와 개수 제한 없이 조회한다."""
        predecessors: list[LightWorklogPredecessor] = []
        seen_worklog_ids = {worklog_id}
        frontier_worklog_ids = [worklog_id]

        while frontier_worklog_ids:
            next_frontier_worklog_ids: list[int] = []
            for frontier_worklog_id in frontier_worklog_ids:
                direct_predecessors = await self._fetch_direct_predecessors(
                    session,
                    frontier_worklog_id,
                )
                for predecessor in direct_predecessors:
                    if predecessor.worklog_id in seen_worklog_ids:
                        continue
                    seen_worklog_ids.add(predecessor.worklog_id)
                    predecessors.append(predecessor)
                    next_frontier_worklog_ids.append(predecessor.worklog_id)
            frontier_worklog_ids = next_frontier_worklog_ids

        return predecessors

    async def _fetch_direct_predecessors(
        self,
        session: AsyncSession,
        worklog_id: int,
    ) -> list[LightWorklogPredecessor]:
        """업무일지 하나의 직접 선행 업무를 개수 제한 없이 조회한다."""
        rows = (
            await session.execute(
                select(WorklogDependency.depends_on_worklog_id, Worklog.title)
                .join(Worklog, Worklog.worklog_id == WorklogDependency.depends_on_worklog_id)
                .where(WorklogDependency.worklog_id == worklog_id)
                .order_by(WorklogDependency.depends_on_worklog_id)
            )
        ).all()
        return [
            LightWorklogPredecessor(
                worklog_id=row.depends_on_worklog_id,
                title=row.title,
            )
            for row in rows
        ]
