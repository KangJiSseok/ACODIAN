from dataclasses import dataclass
from datetime import datetime, time
from math import ceil

from sqlalchemy import Select, delete, exists, false, func, select
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.sql import Selectable

from app.model.search import SemanticWorklogSearchRequest
from app.store.models import (
    Team,
    User,
    UserTeam,
    Worklog,
    WorklogDependency,
    WorklogEmbedding,
    WorklogTag,
)


@dataclass(frozen=True)
class WorklogEmbeddingSource:
    """임베딩 입력 텍스트를 만들기 위해 DB에서 모은 업무일지 원본 문맥."""

    worklog_id: int
    title: str
    request_content: str | None
    work_content: str
    author_id: int
    author_name: str
    author_role: str
    team_id: int
    team_name: str
    department_id: int
    predecessor_titles: list[str]
    predecessor_worklog_ids: list[int]
    tag_ids: list[int]


@dataclass(frozen=True)
class WorklogEmbeddingChunk:
    """저장 직전의 chunk와 Gemini embedding vector 묶음."""

    chunk_index: int
    chunk_content: str
    embedding: list[float]


@dataclass(frozen=True)
class SemanticSearchRow:
    """pgvector 검색 결과 한 행.

    `score`는 사용자에게 보여주기 쉬운 값이고, 내부 SQL 정렬은 distance로 수행한다.
    """

    worklog_id: int
    score: float
    chunk_index: int
    matched_chunk: str
    predecessor_worklog_ids: list[int]


@dataclass(frozen=True)
class SemanticSearchPage:
    """store 계층의 페이지 결과.

    FastAPI/Pydantic 모델을 store에 직접 섞지 않기 위해 순수 dataclass로 둔다.
    """

    items: list[SemanticSearchRow]
    page: int
    page_size: int
    total_count: int

    @property
    def total_pages(self) -> int:
        return ceil(self.total_count / self.page_size) if self.total_count else 0

    @property
    def is_first(self) -> bool:
        return self.page == 1

    @property
    def is_last(self) -> bool:
        return self.page >= self.total_pages if self.total_pages else True

    @property
    def has_next(self) -> bool:
        return self.page < self.total_pages

    @property
    def has_previous(self) -> bool:
        return self.page > 1 and self.total_count > 0


class EmbeddingStore:
    """업무일지 임베딩 저장소 접근 계층.

    SQLAlchemy 쿼리와 `tb_worklog_embedding` 저장은 이 클래스가 소유한다.
    Service는 이 세부 SQL을 알지 못하고 use case 흐름만 조립한다.
    """

    async def fetch_worklog_source(
        self,
        session: AsyncSession,
        worklog_id: int,
    ) -> WorklogEmbeddingSource | None:
        """임베딩할 업무일지와 안정적인 문맥 정보를 조회한다.

        상태/중요도는 자주 바뀌므로 chunk 텍스트에는 넣지 않는다. 다음 검색 MR에서
        최신 `tb_worklog`와 join해 필터링한다.
        """
        row = (
            await session.execute(
                select(
                    Worklog.worklog_id,
                    Worklog.title,
                    Worklog.request_content,
                    Worklog.work_content,
                    Worklog.author_id,
                    User.user_name,
                    User.role_code,
                    User.position_name,
                    User.title_name,
                    User.department_id,
                    Team.team_id,
                    Team.team_name,
                    UserTeam.team_role,
                    UserTeam.is_leader,
                )
                .join(Team, Team.team_id == Worklog.team_id)
                .join(User, User.user_id == Worklog.author_id)
                .outerjoin(
                    UserTeam,
                    (UserTeam.user_id == Worklog.author_id)
                    & (UserTeam.team_id == Worklog.team_id),
                )
                .where(Worklog.worklog_id == worklog_id)
                .where(Worklog.is_deleted.is_(False))
                .where(Team.deleted_at.is_(None))
            )
        ).one_or_none()
        if row is None:
            return None

        predecessor_rows = (
            await session.execute(
                select(WorklogDependency.depends_on_worklog_id, Worklog.title)
                .join(Worklog, Worklog.worklog_id == WorklogDependency.depends_on_worklog_id)
                .where(WorklogDependency.worklog_id == worklog_id)
                .order_by(WorklogDependency.depends_on_worklog_id)
                .limit(3)
            )
        ).all()
        # 현재 더미 데이터는 tag 연결이 없지만, API 검색 계약에는 tagId 필터가 있으므로
        # embedding metadata에는 배열 형태로 저장할 준비를 해 둔다.
        tag_ids = (
            await session.execute(
                select(WorklogTag.tag_id)
                .where(WorklogTag.worklog_id == worklog_id)
                .order_by(WorklogTag.tag_id)
            )
        ).scalars().all()

        author_role = self._format_author_role(
            role_code=row.role_code,
            position_name=row.position_name,
            title_name=row.title_name,
            team_role=row.team_role,
            is_leader=row.is_leader,
        )
        return WorklogEmbeddingSource(
            worklog_id=row.worklog_id,
            title=row.title,
            request_content=row.request_content,
            work_content=row.work_content,
            author_id=row.author_id,
            author_name=row.user_name,
            author_role=author_role,
            team_id=row.team_id,
            team_name=row.team_name,
            department_id=row.department_id,
            predecessor_titles=[predecessor.title for predecessor in predecessor_rows],
            predecessor_worklog_ids=[
                predecessor.depends_on_worklog_id for predecessor in predecessor_rows
            ],
            tag_ids=list(tag_ids),
        )

    async def replace_worklog_embeddings(
        self,
        session: AsyncSession,
        source: WorklogEmbeddingSource,
        chunks: list[WorklogEmbeddingChunk],
    ) -> None:
        """한 업무일지의 기존 임베딩 chunk를 지우고 새 chunk로 교체한다.

        같은 worklog를 다시 임베딩할 때 중복 chunk가 남지 않도록 delete 후 insert한다.
        commit은 호출한 Service가 트랜잭션 경계에서 수행한다.
        """
        await session.execute(
            delete(WorklogEmbedding).where(WorklogEmbedding.worklog_id == source.worklog_id)
        )
        total_chunks = len(chunks)
        for chunk in chunks:
            session.add(
                WorklogEmbedding(
                    worklog_id=source.worklog_id,
                    chunk_index=chunk.chunk_index,
                    total_chunks=total_chunks,
                    chunk_content=chunk.chunk_content,
                    embedding=chunk.embedding,
                    author_id=source.author_id,
                    team_id=source.team_id,
                    department_id=source.department_id,
                    # 상태/중요도는 캐시 컬럼으로도 쓰지 않는다. 검색 시 최신 원본 테이블을 본다.
                    status_code=None,
                    importance_code=None,
                    tag_ids=source.tag_ids,
                    source_type="WORKLOG",
                    created_at=datetime.now(),
                )
            )

    async def search_worklogs(
        self,
        session: AsyncSession,
        request: SemanticWorklogSearchRequest,
        query_embedding: list[float],
    ) -> SemanticSearchPage:
        """query vector와 가장 가까운 업무일지 chunk를 페이지로 조회한다."""
        ranked = self._ranked_search_subquery(request, query_embedding)
        total_count = (
            await session.execute(
                select(func.count()).select_from(ranked).where(ranked.c.rank == 1)
            )
        ).scalar_one()

        offset = (request.page - 1) * request.page_size
        rows = (
            await session.execute(
                select(
                    ranked.c.worklog_id,
                    ranked.c.distance,
                    ranked.c.chunk_index,
                    ranked.c.chunk_content,
                )
                .where(ranked.c.rank == 1)
                # distance가 작을수록 query와 가까운 chunk다.
                .order_by(ranked.c.distance.asc(), ranked.c.worklog_id.desc())
                .limit(request.page_size)
                .offset(offset)
            )
        ).all()
        # 선행 업무 관계는 랭킹에는 쓰지 않고, API 서버가 문맥을 보여줄 수 있게 결과에만 붙인다.
        predecessor_ids_by_worklog = await self._fetch_predecessor_ids(
            session,
            [row.worklog_id for row in rows],
        )
        return SemanticSearchPage(
            items=[
                SemanticSearchRow(
                    worklog_id=row.worklog_id,
                    score=max(0.0, 1.0 - float(row.distance)),
                    chunk_index=row.chunk_index,
                    matched_chunk=row.chunk_content,
                    predecessor_worklog_ids=predecessor_ids_by_worklog.get(row.worklog_id, []),
                )
                for row in rows
            ],
            page=request.page,
            page_size=request.page_size,
            total_count=total_count,
        )

    def _ranked_search_subquery(
        self,
        request: SemanticWorklogSearchRequest,
        query_embedding: list[float],
    ) -> Selectable:
        """chunk 단위 유사도를 계산하고 worklog별 1등 chunk를 고르기 위한 subquery를 만든다.

        한 업무일지가 여러 chunk를 가질 수 있으므로 `row_number()`로 worklog별 최단 거리
        chunk에 rank=1을 부여한다. 바깥 query는 rank=1만 페이지네이션한다.
        """
        distance = WorklogEmbedding.embedding.cosine_distance(query_embedding)
        query = (
            select(
                WorklogEmbedding.worklog_id.label("worklog_id"),
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
        return self._apply_search_filters(query, request).subquery()

    def _apply_search_filters(
        self,
        query: Select[tuple],
        request: SemanticWorklogSearchRequest,
    ) -> Select[tuple]:
        """API 검색 DTO에서 온 구조적 필터를 SQL 조건으로 추가한다.

        벡터 유사도는 의미 순위를 만들고, 팀/상태/중요도/작성자/태그/기간은 최신 원본
        테이블 기준으로 걸러낸다.
        """
        if request.allowed_team_ids is not None:
            if not request.allowed_team_ids:
                # 빈 배열은 접근 가능한 팀이 없다는 뜻이므로 SQL false 조건으로 0건 처리한다.
                return query.where(false())
            query = query.where(Worklog.team_id.in_(request.allowed_team_ids))
        if request.team_id is not None:
            query = query.where(Worklog.team_id == request.team_id)
        if request.team_status is not None:
            query = query.where(Team.status_code == request.team_status)
        if request.status_code is not None:
            query = query.where(Worklog.status_code == request.status_code)
        if request.importance_code is not None:
            query = query.where(Worklog.importance_code == request.importance_code)
        if request.author_id is not None:
            query = query.where(Worklog.author_id == request.author_id)
        if request.tag_id is not None:
            query = query.where(
                exists()
                .where(WorklogTag.worklog_id == Worklog.worklog_id)
                .where(WorklogTag.tag_id == request.tag_id)
            )
        if request.created_from is not None:
            query = query.where(
                Worklog.created_at >= datetime.combine(request.created_from, time.min)
            )
        return query

    async def _fetch_predecessor_ids(
        self,
        session: AsyncSession,
        worklog_ids: list[int],
    ) -> dict[int, list[int]]:
        """검색 결과에 붙일 선행 업무 ID 목록을 조회한다."""
        if not worklog_ids:
            return {}
        rows = (
            await session.execute(
                select(WorklogDependency.worklog_id, WorklogDependency.depends_on_worklog_id)
                .where(WorklogDependency.worklog_id.in_(worklog_ids))
                .order_by(WorklogDependency.worklog_id, WorklogDependency.depends_on_worklog_id)
            )
        ).all()
        result: dict[int, list[int]] = {}
        for row in rows:
            result.setdefault(row.worklog_id, []).append(row.depends_on_worklog_id)
        return result

    def _format_author_role(
        self,
        *,
        role_code: str,
        position_name: str | None,
        title_name: str | None,
        team_role: str | None,
        is_leader: bool | None,
    ) -> str:
        """작성자 역할을 검색 문맥에 넣기 좋은 짧은 문자열로 정리한다.

        시스템 권한 코드(MEMBER 등)는 역할 설명이 전혀 없을 때만 fallback으로 사용한다.
        """
        parts = [value for value in (team_role, title_name, position_name) if value]
        if not parts:
            parts.append(role_code)
        if is_leader and not any("팀장" in part for part in parts):
            parts.append("팀장")
        return " / ".join(dict.fromkeys(parts))
