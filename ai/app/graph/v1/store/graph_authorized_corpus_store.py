from dataclasses import dataclass
from typing import assert_never

from sqlalchemy import Select, func, select
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.sql import Selectable

from app.graph.v1.model.worklog_query import WorklogGraphQueryRequest, WorklogGraphScopeMode
from app.graph.v1.store.worklog_source_store import (
    GraphWorklogPredecessor,
    GraphWorklogSourceRow,
    GraphWorklogTag,
)
from app.store.models import (
    MetaTag,
    Team,
    User,
    Worklog,
    WorklogDependency,
    WorklogEmbedding,
    WorklogTag,
)


@dataclass(frozen=True)
class GraphAuthorizedWorklogCandidate:
    worklog_id: int
    score: float
    chunk_index: int
    matched_chunk: str


@dataclass(frozen=True)
class GraphAuthorizedCorpus:
    candidates: list[GraphAuthorizedWorklogCandidate]
    worklog_ids: set[int]
    team_ids: set[int]
    sources: dict[int, GraphWorklogSourceRow]


class GraphWorklogAuthorizedCorpusStore:
    async def fetch_authorized_corpus(
        self,
        session: AsyncSession,
        request: WorklogGraphQueryRequest,
        query_embedding: list[float],
    ) -> GraphAuthorizedCorpus:
        ranked = self._ranked_candidates_subquery(request, query_embedding)
        rows = (
            await session.execute(
                select(
                    ranked.c.worklog_id,
                    ranked.c.team_id,
                    ranked.c.distance,
                    ranked.c.chunk_index,
                    ranked.c.chunk_content,
                )
                .where(ranked.c.rank == 1)
                .order_by(ranked.c.distance.asc(), ranked.c.worklog_id.desc())
                .limit(max(50, int(request.limit or 20) * 3))
            )
        ).all()

        candidates = [
            GraphAuthorizedWorklogCandidate(
                worklog_id=row.worklog_id,
                score=max(0.0, 1.0 - float(row.distance)),
                chunk_index=row.chunk_index,
                matched_chunk=row.chunk_content,
            )
            for row in rows
        ]
        worklog_ids = [candidate.worklog_id for candidate in candidates]
        sources = await self._fetch_source_rows(
            session=session,
            worklog_ids=worklog_ids,
            allowed_team_ids=self._allowed_team_ids_or_none(request),
        )
        return GraphAuthorizedCorpus(
            candidates=[candidate for candidate in candidates if candidate.worklog_id in sources],
            worklog_ids=set(sources),
            team_ids={source.team_id for source in sources.values()},
            sources=sources,
        )

    def _ranked_candidates_subquery(
        self,
        request: WorklogGraphQueryRequest,
        query_embedding: list[float],
    ) -> Selectable:
        distance = WorklogEmbedding.embedding.cosine_distance(query_embedding)
        query = (
            select(
                WorklogEmbedding.worklog_id.label("worklog_id"),
                Worklog.team_id.label("team_id"),
                WorklogEmbedding.chunk_index.label("chunk_index"),
                WorklogEmbedding.chunk_content.label("chunk_content"),
                distance.label("distance"),
                func.row_number()
                .over(
                    partition_by=WorklogEmbedding.worklog_id,
                    order_by=distance.asc(),
                )
                .label("rank"),
            )
            .join(Worklog, Worklog.worklog_id == WorklogEmbedding.worklog_id)
            .join(Team, Team.team_id == Worklog.team_id)
            .where(WorklogEmbedding.source_type == "WORKLOG")
            .where(Worklog.is_deleted.is_(False))
            .where(Team.deleted_at.is_(None))
        )
        return self._apply_scope_filter(query, request).subquery()

    def _apply_scope_filter(
        self,
        query: Select[tuple],
        request: WorklogGraphQueryRequest,
    ) -> Select[tuple]:
        match request.scope_mode:
            case WorklogGraphScopeMode.TEAM_IDS:
                allowed_team_ids = [int(team_id) for team_id in request.allowed_team_ids or []]
                return query.where(Worklog.team_id.in_(allowed_team_ids))
            case WorklogGraphScopeMode.ALL:
                return query
            case unreachable:
                assert_never(unreachable)

    async def _fetch_source_rows(
        self,
        *,
        session: AsyncSession,
        worklog_ids: list[int],
        allowed_team_ids: list[int] | None,
    ) -> dict[int, GraphWorklogSourceRow]:
        if not worklog_ids:
            return {}

        query = (
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
            .where(Worklog.worklog_id.in_(worklog_ids))
            .where(Worklog.is_deleted.is_(False))
            .where(Team.deleted_at.is_(None))
            .order_by(Worklog.worklog_id)
        )
        if allowed_team_ids is not None:
            query = query.where(Worklog.team_id.in_(allowed_team_ids))

        rows = (await session.execute(query)).all()
        visible_ids = [row.worklog_id for row in rows]
        if not visible_ids:
            return {}
        tags_by_worklog_id = await self._fetch_tags_by_worklog_id(session, visible_ids)
        direct_predecessors_by_worklog_id = await self._fetch_direct_predecessors_by_worklog_id(
            session=session,
            worklog_ids=visible_ids,
            allowed_team_ids=allowed_team_ids,
        )

        return {
            row.worklog_id: GraphWorklogSourceRow(
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

    async def find_exact_title_ids(
        self, session: AsyncSession, title: str, request: WorklogGraphQueryRequest
    ) -> list[int]:
        """허용 범위의 정확한 제목만 식별하며 중복은 호출자가 처리한다."""
        query = (
            select(Worklog.worklog_id)
            .join(Team, Team.team_id == Worklog.team_id)
            .where(Worklog.title == title, Worklog.is_deleted.is_(False), Team.deleted_at.is_(None))
            .limit(2)
        )
        allowed = self._allowed_team_ids_or_none(request)
        if allowed is not None:
            query = query.where(Worklog.team_id.in_(allowed))
        return list((await session.execute(query)).scalars().all())

    async def fetch_sources_by_ids(
        self, session: AsyncSession, ids: list[int], request: WorklogGraphQueryRequest
    ) -> dict[int, GraphWorklogSourceRow]:
        """그래프가 반환한 ID를 PostgreSQL의 최신 권한/삭제 상태로 다시 확인한다."""
        return await self._fetch_source_rows(
            session=session, worklog_ids=ids, allowed_team_ids=self._allowed_team_ids_or_none(request)
        )

    async def _fetch_tags_by_worklog_id(
        self,
        session: AsyncSession,
        worklog_ids: list[int],
    ) -> dict[int, list[GraphWorklogTag]]:
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

        tags_by_worklog_id: dict[int, list[GraphWorklogTag]] = {}
        for row in rows:
            tags_by_worklog_id.setdefault(row.worklog_id, []).append(
                GraphWorklogTag(
                    tag_id=row.tag_id,
                    tag_name=row.tag_name,
                    description=row.description,
                )
            )
        return tags_by_worklog_id

    async def _fetch_direct_predecessors_by_worklog_id(
        self,
        *,
        session: AsyncSession,
        worklog_ids: list[int],
        allowed_team_ids: list[int] | None,
    ) -> dict[int, list[GraphWorklogPredecessor]]:
        query = (
            select(
                WorklogDependency.worklog_id,
                WorklogDependency.depends_on_worklog_id,
                Worklog.team_id,
                Worklog.title,
            )
            .join(Worklog, Worklog.worklog_id == WorklogDependency.depends_on_worklog_id)
            .join(Team, Team.team_id == Worklog.team_id)
            .where(WorklogDependency.worklog_id.in_(worklog_ids))
            .where(Worklog.is_deleted.is_(False))
            .where(Team.deleted_at.is_(None))
            .order_by(WorklogDependency.worklog_id, WorklogDependency.depends_on_worklog_id)
        )
        if allowed_team_ids is not None:
            query = query.where(Worklog.team_id.in_(allowed_team_ids))

        rows = (await session.execute(query)).all()

        predecessors_by_worklog_id: dict[int, list[GraphWorklogPredecessor]] = {}
        for row in rows:
            predecessors_by_worklog_id.setdefault(row.worklog_id, []).append(
                GraphWorklogPredecessor(
                    worklog_id=row.depends_on_worklog_id,
                    team_id=row.team_id,
                    title=row.title,
                )
            )
        return predecessors_by_worklog_id

    @staticmethod
    def _allowed_team_ids_or_none(request: WorklogGraphQueryRequest) -> list[int] | None:
        match request.scope_mode:
            case WorklogGraphScopeMode.TEAM_IDS:
                return [int(team_id) for team_id in request.allowed_team_ids or []]
            case WorklogGraphScopeMode.ALL:
                return None
            case unreachable:
                assert_never(unreachable)
